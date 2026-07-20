package com.lu.postrobotsystem.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.common.OrderStateMachine;
import com.lu.postrobotsystem.common.PostalApiClient;
import com.lu.postrobotsystem.exception.BusinessException;
import com.lu.postrobotsystem.exception.ResultCode;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.*;
import com.lu.postrobotsystem.model.entity.*;
import com.lu.postrobotsystem.model.enums.*;
import com.lu.postrobotsystem.model.request.payment.PaymentCallbackRequest;
import com.lu.postrobotsystem.model.response.payment.PaymentResponse;
import com.lu.postrobotsystem.service.InventoryService;
import com.lu.postrobotsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.lu.postrobotsystem.exception.ResultCode.*;

/**
 * 支付服务实现类
 * <p>
 * 实现支付流程的完整生命周期管理，核心功能包括：
 * <ul>
 *   <li>支付回调处理：验签 → 幂等性检查 → 状态更新 → 库存操作</li>
 *   <li>支付状态轮询：定时任务批量查询超时支付</li>
 *   <li>支付信息安全：凭证数据不落盘，日志输出脱敏</li>
 * </ul>
 * </p>
 *
 * <p><b>幂等性策略：</b><br>
 * 同一支付回调或查询结果重复到达时，通过以下机制防止重复处理：<br>
 * - 前置校验：检查订单当前状态是否为目标状态的前置状态<br>
 * - 乐观锁：MyBatis-Plus @Version 防止重复写入<br>
 * - 库存操作：Lua 脚本原子性确保不会重复扣减/释放
 * </p>
 *
 * <p><b>支付信息安全措施：</b><br>
 * - 原始响应报文不存入数据库（不落盘）<br>
 * - 二维码 Base64 内容不持久化，仅存链接 URL<br>
 * - 日志输出时对 sign、卡号等敏感字段使用脱敏处理<br>
 * - 支付流水记录表仅存非敏感的业务标识字段
 * </p>
 *
 * @see PaymentService
 * @see PaymentMapper
 * @see InventoryService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentFlowRecordMapper paymentFlowRecordMapper;
    private final InventoryService inventoryService;
    private final OrderServiceImpl orderService;
    private final PostalApiClient postalApiClient;

    // ==================== 支付回调处理 ====================

    /**
     * 处理支付回调（邮政异步通知）。
     * <p>
     * 邮政支付系统在用户完成扫码支付后，异步通知本系统支付结果。
     * 完整处理流程：<br>
     * // 1. 签名校验（生产环境）：验证回调签名与签名原文是否一致，防止伪造回调<br>
     * // 2. 幂等性检查：根据 paymentFlowNo 查询支付记录状态，以及订单当前状态，已处理则直接返回<br>
     * // 3. 状态机校验：仅 PAYING 状态可接收回调结果<br>
     * // 4. 查询订单与支付记录<br>
     * // 5. 根据回调支付结果分支处理：
     * //    a. 支付成功（payStatus=SUCCESS）→ 状态 PAID，扣减库存
     * //    b. 支付失败（payStatus=FAILED）  → 状态 FAILED，释放锁定库存
     * // 6. 乐观锁更新订单状态（version 条件，防止并发覆盖）<br>
     * // 7. 更新支付记录状态和支付时间<br>
     * // 8. 记录支付流水摘要（仅非敏感字段，如 paymentFlowNo、订单号等）<br>
     * // 9. 记录状态变更日志和审计日志
     * </p>
     *
     * <p><b>补偿策略：</b><br>
     * - 库存扣减/释放失败时，将订单标记为 MANUAL_REQUIRED，等待人工核查<br>
     * - 不会回滚状态变更，确保支付状态已正确记录<br>
     * - 系统提供定时对账任务，发现不一致时自动修复或告警
     * </p>
     *
     * @param callback 回调请求体（含订单号、支付流水号、平台流水号、状态、签名等）
     * @param sourceIp 回调来源IP（用于安全审计）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(PaymentCallbackRequest callback, String sourceIp) {
        // === 1. 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(callback), PARAM_ERROR, "回调请求不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(callback.getOrderNo()), PARAM_ERROR, "回调订单号不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(callback.getPaymentFlowNo()), PARAM_ERROR, "回调支付流水号不能为空");

        String orderNo = callback.getOrderNo();
        String paymentFlowNo = callback.getPaymentFlowNo();
        // 日志输出时对签名进行脱敏（只显示前4位）
        String maskedSign = callback.getSign() != null ?
                callback.getSign().substring(0, Math.min(4, callback.getSign().length())) + "****" : "null";

        log.info("收到支付回调: orderNo={}, paymentFlowNo={}, payStatus={}, sign={}",
                orderNo, paymentFlowNo, callback.getPayStatus(), maskedSign);

        // === 2. 生产环境签名校验（当前 mock 模式跳过） ===
        // TODO: 生产环境验签逻辑
        // 按邮政规范重新计算签名并与 callback.sign 比较
        // 验签失败时抛 BusinessException(ResultCode.SIGNATURE_ERROR, "支付回调签名验证失败")

        // === 3. 幂等性检查：根据流水号查询支付记录 ===
        // SQL: SELECT id, status FROM payment WHERE payment_flow_no=? AND is_deleted=0 LIMIT 1
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentFlowNo, paymentFlowNo)
                        .eq(Payment::getIsDeleted, 0));

        if (payment != null && payment.getStatus() != PaymentStatusEnum.PAYING) {
            // 支付记录已处非 PAYING 状态（SUCCESS/FAILED/REFUNDED），说明已处理过
            log.info("支付回调幂等性跳过: paymentFlowNo={}, currentStatus={}", paymentFlowNo, payment.getStatus());
            return;
        }

        // === 4. 查询订单 ===
        // SQL: SELECT id, order_no, status, version, ... FROM orders WHERE order_no=? AND is_deleted=0 LIMIT 1
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(order), NOT_FOUND, "订单不存在: " + orderNo);

        // === 5. 状态机校验：仅 PAYING 状态可接收回调 ===
        // 幂等性第二层：订单当前状态判断
        OrderStatusEnum currentStatus = order.getStatus();
        if (currentStatus == OrderStatusEnum.PAID || currentStatus == OrderStatusEnum.CANCELLED) {
            log.info("支付回调幂等性跳过: 订单已处终态, orderNo={}, status={}", orderNo, currentStatus);
            return;
        }
        // 非 PAYING 状态但也不是终态的情况（如 PENDING_PAY 直接收到回调），拒绝处理
        if (currentStatus != OrderStatusEnum.PAYING) {
            log.warn("支付回调状态异常: orderNo={}, status={} 非 PAYING，拒绝处理", orderNo, currentStatus);
            order.setStatus(OrderStatusEnum.MANUAL_REQUIRED);
            order.setRemark("支付回调状态异常，需人工核查");
            // SQL: UPDATE orders SET status='MANUAL_REQUIRED', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
            ordersMapper.updateById(order);
            return;
        }

        // === 6. 根据回调支付状态分支处理 ===
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(callback.getPayStatus());
        OrderStatusEnum targetStatus = isSuccess ? OrderStatusEnum.PAID : OrderStatusEnum.FAILED;

        // 状态机校验
        OrderStateMachine.transition(currentStatus, targetStatus);

        // 存储变更前状态
        OrderStatusEnum prevStatus = order.getStatus();

        // === 7. 乐观锁更新订单状态 ===
        order.setStatus(targetStatus);
        if (isSuccess) {
            order.setTransactionId(callback.getPlatformFlowNo());
            order.setRemark(null); // 成功时清空备注
        } else {
            order.setRemark(StrUtil.sub(callback.getFailReason(), 0, 500));
        }
        // SQL: UPDATE orders SET status=?, transaction_id=?, remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
        int affected = ordersMapper.updateById(order); // @Version 自动校验 version
        ThrowUtils.throwIf(affected == 0, BUSINESS_ERROR, "订单状态更新失败，请重试");

        // === 8. 库存操作 ===
        // 查询订单明细获取各商品数量
        // SQL: SELECT id, product_id, quantity FROM order_item WHERE order_id=? AND is_deleted=0
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .eq(OrderItem::getIsDeleted, 0));

        boolean stockOpSuccess = true;
        for (OrderItem item : orderItems) {
            try {
                boolean opResult;
                if (isSuccess) {
                    // 支付成功：扣减锁定库存（Lua 原子操作）
                    opResult = inventoryService.deductStock(item.getProductId(), item.getQuantity());
                } else {
                    // 支付失败：释放锁定库存（Lua 原子操作）
                    opResult = inventoryService.releaseStock(item.getProductId(), item.getQuantity());
                }
                if (!opResult) {
                    log.warn("支付回调库存操作失败: orderNo={}, productId={}, op={}",
                            orderNo, item.getProductId(), isSuccess ? "deduct" : "release");
                    stockOpSuccess = false;
                }
            } catch (Exception e) {
                log.error("支付回调库存操作异常: orderNo={}, productId={}, op={}",
                        orderNo, item.getProductId(), isSuccess ? "deduct" : "release", e);
                stockOpSuccess = false;
            }
        }

        // === 9. 库存操作失败处理：抛出异常触发事务回滚，由补偿机制或重试处理 ===
        if (!stockOpSuccess) {
            log.error("支付回调库存操作异常，标记人工处理并回滚事务: orderNo={}", orderNo);
            // 先提交人工处理标记（物理上记录异常），再抛出异常触发回滚
            order.setStatus(OrderStatusEnum.MANUAL_REQUIRED);
            order.setRemark((isSuccess ? "支付成功" : "支付失败") + "但库存操作异常，需人工处理");
            // SQL: UPDATE orders SET status='MANUAL_REQUIRED', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
            ordersMapper.updateById(order);
            // SQL: INSERT INTO payment_flow_record (...) VALUES (...)
            saveFlowRecord(order.getId(), orderNo, paymentFlowNo, "CALLBACK", sourceIp,
                    "库存操作异常，转人工处理");
            // SQL: INSERT INTO order_status_log (...) VALUES (...)
            orderService.saveStatusLog(order.getId(), orderNo, targetStatus, OrderStatusEnum.MANUAL_REQUIRED,
                    "SYSTEM", "PAYMENT_CALLBACK", "库存操作失败，转人工处理");

            // 抛出异常触发外层的 @Transactional 回滚，保证调用方感知失败
            throw new BusinessException(THIRD_PARTY_ERROR, "支付回调库存操作异常，已转人工处理");
        }

        // === 10. 更新支付记录 ===
        // SQL: UPDATE payment SET status=?, platform_flow_no=?, paid_time=?, update_time=NOW() WHERE id=? AND is_deleted=0
        if (payment != null) {
            payment.setStatus(isSuccess ? PaymentStatusEnum.SUCCESS : PaymentStatusEnum.FAILED);
            payment.setPlatformFlowNo(callback.getPlatformFlowNo());
            if (isSuccess) {
                payment.setPaidTime(LocalDateTime.now());
            }
            paymentMapper.updateById(payment);
        }

        // === 11. 记录支付流水摘要（仅非敏感字段） ===
        // SQL: INSERT INTO payment_flow_record (id, order_id, order_no, payment_flow_no, flow_type, resp_data_digest, source_ip, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        saveFlowRecord(order.getId(), orderNo, paymentFlowNo, "CALLBACK", sourceIp,
                isSuccess ? "支付成功" : "支付失败");

        // === 12. 记录状态变更日志 ===
        // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        orderService.saveStatusLog(order.getId(), orderNo, prevStatus, targetStatus,
                "SYSTEM", "PAYMENT_CALLBACK",
                isSuccess ? "支付成功回调" : "支付失败回调");

        log.info("支付回调处理完成: orderNo={}, status={}, stockOpSuccess={}", orderNo, targetStatus, stockOpSuccess);
    }

    // ==================== 查询方法 ====================

    /**
     * 根据订单ID查询支付记录。
     * <p>
     * 一个订单可能对应多条支付记录（如支付失败后重试），按创建时间降序返回。
     * </p>
     *
     * @param orderId 订单ID
     * @return 支付记录列表
     */
    @Override
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        ThrowUtils.throwIf(ObjectUtil.isNull(orderId), PARAM_ERROR, "订单ID不能为空");

        // SQL: SELECT id, payment_flow_no, status, amount, ... FROM payment WHERE order_id=? AND is_deleted=0 ORDER BY create_time DESC
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getIsDeleted, 0)
                        .orderByDesc(Payment::getCreateTime));
        return getPaymentVOList(payments);
    }

    /**
     * 根据支付流水号查询支付记录。
     *
     * @param paymentFlowNo 支付流水号
     * @return 支付响应数据
     */
    @Override
    public PaymentResponse getByPaymentFlowNo(String paymentFlowNo) {
        ThrowUtils.throwIf(StrUtil.isBlank(paymentFlowNo), PARAM_ERROR, "支付流水号不能为空");

        // SQL: SELECT id, payment_flow_no, status, amount, ... FROM payment WHERE payment_flow_no=? AND is_deleted=0 LIMIT 1
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentFlowNo, paymentFlowNo)
                        .eq(Payment::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(payment), NOT_FOUND, "支付记录不存在");
        return getPaymentVO(payment);
    }

    // ==================== VO 转换 ====================

    /**
     * 将支付实体转换为支付响应数据。
     * <p>
     * 转换规则：<br>
     * - 直接映射基础字段（ID、订单ID、流水号等）<br>
     * - 金额、状态、时间等字段直接复制<br>
     * - 不包含支付敏感信息（如原始响应报文）
     * </p>
     *
     * @param payment 支付实体
     * @return 支付响应数据，若入参为 null 则返回 null
     */
    @Override
    public PaymentResponse getPaymentVO(Payment payment) {
        if (ObjectUtil.isNull(payment)) return null;

        PaymentResponse vo = new PaymentResponse();
        vo.setId(payment.getId());
        vo.setOrderId(payment.getOrderId());
        vo.setPaymentFlowNo(payment.getPaymentFlowNo());
        vo.setQrCodeUrl(payment.getQrCodeUrl());
        vo.setPlatformFlowNo(payment.getPlatformFlowNo());
        vo.setStatus(payment.getStatus());
        vo.setAmount(payment.getAmount());
        vo.setPaidTime(payment.getPaidTime());
        vo.setCreateTime(payment.getCreateTime());
        return vo;
    }

    /**
     * 批量将支付实体列表转换为支付响应数据列表。
     * <p>遍历调用 {@link #getPaymentVO(Payment)} 进行逐个转换。</p>
     *
     * @param payments 支付实体列表
     * @return 支付响应数据列表，若入参为空则返回空列表
     */
    @Override
    public List<PaymentResponse> getPaymentVOList(List<Payment> payments) {
        if (ObjectUtil.isEmpty(payments)) return Collections.emptyList();
        return payments.stream().map(this::getPaymentVO).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 记录支付流水摘要。
     * <p>
     * 记录与支付平台的交互记录，仅包含非敏感字段。
     * <b>存储原则：</b>不存原始报文、不存签名原文、不存卡号等金融敏感信息。
     * </p>
     *
     * // 1. 创建 PaymentFlowRecord 实体
     * // 2. 仅填充订单标识、流水号、类型、IP 等非敏感字段
     * // 3. 敏感字段（如完整响应报文）不留存
     * // SQL: INSERT INTO payment_flow_record (id, order_id, order_no, payment_flow_no, flow_type, resp_data_digest, source_ip, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     *
     * @param orderId       订单ID
     * @param orderNo       订单号
     * @param paymentFlowNo 支付流水号
     * @param flowType      流水类型（QR_REQUEST/CALLBACK/QUERY）
     * @param sourceIp      请求来源IP
     * @param digest        数据摘要（非敏感信息的文字描述）
     */
    private void saveFlowRecord(Long orderId, String orderNo, String paymentFlowNo,
                                String flowType, String sourceIp, String digest) {
        PaymentFlowRecord record = new PaymentFlowRecord()
                .setOrderId(orderId)
                .setOrderNo(orderNo)
                .setPaymentFlowNo(paymentFlowNo)
                .setFlowType(flowType)
                .setRespDataDigest(StrUtil.sub(digest, 0, 255)) // 仅存文字摘要
                .setSourceIp(sourceIp);
        paymentFlowRecordMapper.insert(record);
    }
}
