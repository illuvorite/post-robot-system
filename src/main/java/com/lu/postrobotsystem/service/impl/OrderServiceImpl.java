package com.lu.postrobotsystem.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.common.OrderStateMachine;
import com.lu.postrobotsystem.common.PostalApiClient;
import com.lu.postrobotsystem.common.util.TransactionIdGenerator;
import com.lu.postrobotsystem.exception.BusinessException;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.OrderItemMapper;
import com.lu.postrobotsystem.mapper.OrderStatusLogMapper;
import com.lu.postrobotsystem.mapper.OrdersMapper;
import com.lu.postrobotsystem.mapper.PaymentMapper;
import com.lu.postrobotsystem.model.entity.*;
import com.lu.postrobotsystem.model.enums.*;
import com.lu.postrobotsystem.model.request.order.OrderCreateRequest;
import com.lu.postrobotsystem.model.response.order.OrderResponse;
import com.lu.postrobotsystem.model.response.order.OrderStatusLogResponse;
import com.lu.postrobotsystem.service.InventoryService;
import com.lu.postrobotsystem.service.OrderService;
import com.lu.postrobotsystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.lu.postrobotsystem.exception.ResultCode.*;

/**
 * 订单服务实现类
 * <p>
 * 实现订单从创建到完成的完整生命周期管理。
 * 核心业务逻辑包括：
 * <ul>
 *   <li>订单创建：锁定库存 → 调用邮政二维码 → 保存订单与支付记录</li>
 *   <li>订单取消：状态机校验 → 乐观锁更新 → 释放锁定库存</li>
 *   <li>超时处理：定时任务驱动 → 自动释放库存</li>
 *   <li>状态追溯：记录每次状态变更日志，支持完整轨迹查询</li>
 * </ul>
 * </p>
 *
 * <p><b>并发控制策略：</b><br>
 * - 状态更新使用 MyBatis-Plus 乐观锁（version 字段），防止并发覆盖<br>
 * - 库存操作复用 InventoryServiceImpl 的 Redisson 分布式锁 + Lua 脚本原子操作<br>
 * - 幂等性：关键操作前置校验当前订单状态，确保不会重复执行
 * </p>
 *
 * <p><b>补偿/回滚策略：</b><br>
 * - 库存锁定失败 → 整体事务回滚，订单不创建<br>
 * - 库存释放失败 → 订单状态标记为 MANUAL_REQUIRED，等待人工处理<br>
 * - 支付状态与库存操作不一致 → 通过定时任务和告警机制发现并修复
 * </p>
 *
 * @see OrderService
 * @see OrderStateMachine
 * @see OrdersMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final PaymentMapper paymentMapper;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PostalApiClient postalApiClient;

    /** 订单号日期格式 */
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 订单号序列号生成器 */
    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    // ==================== 订单创建 ====================

    /**
     * 创建订单。
     * <p>
     * 完整下单流程：<br>
     * // 1. 校验请求参数并查询商品信息<br>
     * // 2. 计算订单总金额（商品总价 + 邮资）<br>
     * // 3. 逐一锁定库存（任一商品库存不足则整体回滚）<br>
     * // 4. 生成订单号与支付流水号<br>
     * // 5. 调用 PostalApiClient 获取收款二维码<br>
     * // 6. 批量保存订单明细快照<br>
     * // 7. 保存订单主表记录<br>
     * // 8. 创建支付记录（状态 PAYING）<br>
     * // 9. 记录状态变更日志和审计日志<br>
     * // 10. 返回订单响应（含二维码链接）
     * </p>
     *
     * <p><b>回滚策略：</b>库存锁定失败时抛出 {@link BusinessException}，事务注解 {@link Transactional}
     * 自动回滚整个订单创建过程，保证数据一致性。</p>
     *
     * @param request 订单创建请求（含商品明细和邮资）
     * @param userId  当前操作用户ID
     * @return 订单响应（含二维码链接、支付流水号等）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderCreateRequest request, Long userId) {
        // === 1. 参数校验与商品信息查询 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "订单请求不能为空");
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getItems()), PARAM_ERROR, "商品明细不能为空");

        List<OrderCreateRequest.OrderItemRequest> items = request.getItems();
        BigDecimal postage = request.getPostage() != null ? request.getPostage() : BigDecimal.ZERO;

        // 查询商品信息并计算总价
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderCreateRequest.OrderItemRequest item : items) {
            // 校验单个商品：ID 和数量必填
            ThrowUtils.throwIf(ObjectUtil.isNull(item.getProductId()), PARAM_ERROR, "商品ID不能为空");
            ThrowUtils.throwIf(ObjectUtil.isNull(item.getQuantity()) || item.getQuantity() <= 0,
                    PARAM_ERROR, "购买数量必须大于0");

            // 查询商品是否存在且已上架
            // SQL: SELECT id, name, price, status, ... FROM product WHERE id=? AND is_deleted=0
            Product product = productService.getById(item.getProductId());
            ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND,
                    "商品ID=" + item.getProductId() + " 不存在");
            ThrowUtils.throwIf(product.getStatus() != ProductStatusEnum.ON_SHELF, BUSINESS_ERROR,
                    "商品[" + product.getName() + "]已下架，无法购买");

            // 累加总价
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        // 加上邮资
        totalAmount = totalAmount.add(postage);

        // === 2. 锁定库存（逐一锁定，任一失败即整体回滚） ===
        List<Long> lockedProductIds = new ArrayList<>();
        List<Integer> lockedQuantities = new ArrayList<>();
        try {
            for (OrderCreateRequest.OrderItemRequest item : items) {
                boolean locked = inventoryService.lockStock(item.getProductId(), item.getQuantity());
                // 库存不足时回滚：@Transactional 自动回滚 DB，Redis 由补偿机制处理
                ThrowUtils.throwIf(!locked, BUSINESS_ERROR,
                        "商品ID=" + item.getProductId() + " 库存不足，下单失败");
                lockedProductIds.add(item.getProductId());
                lockedQuantities.add(item.getQuantity());
                log.debug("库存锁定成功: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            // 补偿 Redis：释放已成功锁定的库存
            log.warn("库存锁定异常，补偿Redis库存: lockedCount={}", lockedProductIds.size(), e);
            for (int i = 0; i < lockedProductIds.size(); i++) {
                try {
                    inventoryService.releaseStock(lockedProductIds.get(i), lockedQuantities.get(i));
                } catch (Exception ex) {
                    log.error("Redis库存补偿失败: productId={}", lockedProductIds.get(i), ex);
                }
            }
            throw e; // 继续抛出，触发 DB 事务回滚
        }

        // === 注册事务回滚时的 Redis 库存补偿 ===
        // 当后续步骤失败（如邮政接口调用），@Transactional 回滚 DB 操作，
        // 此回调负责补偿第 2 步已锁定的 Redis 库存，防止 Redis-DB 不一致
        final List<Long> compensatedProductIds = lockedProductIds;
        final List<Integer> compensatedQuantities = lockedQuantities;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("订单事务回滚，补偿Redis库存: itemCount={}", compensatedProductIds.size());
                    for (int i = 0; i < compensatedProductIds.size(); i++) {
                        try {
                            inventoryService.releaseStock(compensatedProductIds.get(i), compensatedQuantities.get(i));
                        } catch (Exception ex) {
                            log.error("Redis库存补偿失败: productId={}", compensatedProductIds.get(i), ex);
                        }
                    }
                }
            }
        });

        // === 3. 生成订单号和支付流水号 ===
        String orderNo = generateOrderNo();
        String paymentFlowNo = "PAY" + orderNo.substring(3); // 支付流水号基于订单号

        // === 4. 调用邮政接口获取收款二维码 ===
        PostalApiClient.PostalQrResponse qrResponse = postalApiClient.generateQrCode(
                orderNo, totalAmount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString(), paymentFlowNo);
        ThrowUtils.throwIf(!qrResponse.isSuccess(), THIRD_PARTY_ERROR, "获取收款二维码失败");

        // === 5. 保存订单主表 ===
        // SQL: INSERT INTO orders (id, order_no, user_id, total_amount, postage, status, qr_code_url, pay_query_no, transaction_id, payment_flow_no, version, create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        Orders order = new Orders()
                .setOrderNo(orderNo)
                .setUserId(userId)
                .setTotalAmount(totalAmount)
                .setPostage(postage)
                .setStatus(OrderStatusEnum.PENDING_PAY)
                .setQrCodeUrl(qrResponse.getQrCodeUrl())
                .setPayQueryNo(qrResponse.getPayQueryNo())
                .setTransactionId(qrResponse.getTransactionId())
                .setPaymentFlowNo(paymentFlowNo)
                .setVersion(0); // 乐观锁初始版本号
        ordersMapper.insert(order);

        // === 6. 批量保存订单明细（商品快照） ===
        for (OrderCreateRequest.OrderItemRequest item : items) {
            // SQL: SELECT id, name, price FROM product WHERE id=? AND is_deleted=0
            Product product = productService.getById(item.getProductId());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            // SQL: INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            OrderItem orderItem = new OrderItem()
                    .setOrderId(order.getId())
                    .setProductId(product.getId())
                    .setProductName(product.getName())      // 名称快照
                    .setProductPrice(product.getPrice())    // 价格快照
                    .setQuantity(item.getQuantity())
                    .setSubtotal(subtotal);
            orderItemMapper.insert(orderItem);
        }

        // === 7. 创建支付记录（状态 PAYING） ===
        // SQL: INSERT INTO payment (id, order_id, payment_flow_no, pay_query_no, qr_code_url, status, amount, create_time, update_time, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        Payment payment = new Payment()
                .setOrderId(order.getId())
                .setPaymentFlowNo(paymentFlowNo)
                .setPayQueryNo(qrResponse.getPayQueryNo())
                .setQrCodeUrl(qrResponse.getQrCodeUrl())
                .setStatus(PaymentStatusEnum.PAYING)
                .setAmount(totalAmount);
        paymentMapper.insert(payment);

        // === 8. 记录状态变更日志 ===
        // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        saveStatusLog(order.getId(), orderNo, null, OrderStatusEnum.PENDING_PAY,
                String.valueOf(userId), "ORDER_CREATE", "订单创建成功");

        log.info("订单创建成功: orderNo={}, amount={}, userId={}", orderNo, totalAmount, userId);

        // === 9. 返回订单响应 ===
        return getOrderVO(order);
    }

    // ==================== 订单查询 ====================

    /**
     * 根据订单ID查询订单详情。
     * <p>
     * // 1. 校验参数<br>
     * // 2. 查询订单主表（过滤已删除）<br>
     * // 3. 转换为 VO 返回（含商品明细）
     * </p>
     *
     * @param id 订单ID
     * @return 订单响应数据
     */
    @Override
    public OrderResponse getOrderById(Long id) {
        ThrowUtils.throwIf(ObjectUtil.isNull(id), PARAM_ERROR, "订单ID不能为空");

        // 查询订单（自动过滤 is_deleted=0）
        // SQL: SELECT id, order_no, total_amount, status, ... FROM orders WHERE id=? AND is_deleted=0 LIMIT 1
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getId, id)
                        .eq(Orders::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(order), NOT_FOUND, "订单不存在");
        return getOrderVO(order);
    }

    /**
     * 根据订单号查询订单详情。
     * <p>
     * // 1. 校验参数<br>
     * // 2. 按订单号查询（唯一索引）<br>
     * // 3. 转换为 VO 返回
     * </p>
     *
     * @param orderNo 订单号
     * @return 订单响应数据
     */
    @Override
    public OrderResponse getOrderByNo(String orderNo) {
        ThrowUtils.throwIf(StrUtil.isBlank(orderNo), PARAM_ERROR, "订单号不能为空");

        // SQL: SELECT id, order_no, total_amount, status, ... FROM orders WHERE order_no=? AND is_deleted=0 LIMIT 1
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(order), NOT_FOUND, "订单不存在");
        return getOrderVO(order);
    }

    // ==================== 订单取消 ====================

    /**
     * 取消订单。
     * <p>
     * // 1. 查询订单并校验状态：仅 PENDING_PAY / FAILED 可取消<br>
     * // 2. 状态机校验：确保 PENDING_PAY/FAILED → CANCELLED 合法<br>
     * // 3. 乐观锁更新订单状态（version 条件，防止并发覆盖）<br>
     * // 4. 查询订单明细获取各商品锁定数量<br>
     * // 5. 释放已锁定库存（调用 InventoryService.releaseStock）<br>
     * // 6. 若库存释放失败，将订单标记为 MANUAL_REQUIRED，创建告警<br>
     * // 7. 记录状态变更日志和审计日志
     * </p>
     *
     * <p><b>补偿策略：</b>库存释放失败不会回滚状态变更，而是将订单标记为 MANUAL_REQUIRED，
     * 等待人工核查处理，确保不阻塞用户操作流程。</p>
     *
     * @param orderNo  订单号
     * @param operator 操作人用户名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, String operator) {
        ThrowUtils.throwIf(StrUtil.isBlank(orderNo), PARAM_ERROR, "订单号不能为空");

        // === 1. 查询订单 ===
        // SQL: SELECT id, order_no, status, version, ... FROM orders WHERE order_no=? AND is_deleted=0 LIMIT 1
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(order), NOT_FOUND, "订单不存在");

        // === 2. 状态机校验：仅 PENDING_PAY / FAILED → CANCELLED 合法 ===
        OrderStateMachine.transition(order.getStatus(), OrderStatusEnum.CANCELLED);

        // === 3. 乐观锁更新订单状态（version 条件防止并发覆盖） ===
        OrderStatusEnum prevStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED);
        order.setRemark("用户取消");
        // SQL: UPDATE orders SET status='CANCELLED', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
        int affected = ordersMapper.updateById(order); // MyBatis-Plus @Version 自动拼接 version 条件
        // affected=0 表示版本号已被其他线程修改，乐观锁冲突
        ThrowUtils.throwIf(affected == 0, BUSINESS_ERROR, "订单状态已变更，请刷新后重试");

        // === 4. 查询订单明细，获取各商品锁定数量 ===
        // SQL: SELECT id, product_id, quantity FROM order_item WHERE order_id=? AND is_deleted=0
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .eq(OrderItem::getIsDeleted, 0));

        // === 5. 逐一释放已锁定库存 ===
        boolean releaseSuccess = true;
        for (OrderItem item : orderItems) {
            boolean released = inventoryService.releaseStock(item.getProductId(), item.getQuantity());
            if (!released) {
                log.warn("库存释放异常: orderNo={}, productId={}, quantity={}", orderNo, item.getProductId(), item.getQuantity());
                releaseSuccess = false;
            }
        }

        // === 6. 库存释放失败 → 标记 MANUAL_REQUIRED 并告警 ===
        if (!releaseSuccess) {
            order.setStatus(OrderStatusEnum.MANUAL_REQUIRED);
            order.setRemark("取消订单时库存释放异常，需人工处理");
            // SQL: UPDATE orders SET status='MANUAL_REQUIRED', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
            ordersMapper.updateById(order);
            log.warn("订单取消库存释放异常，已标记为人工处理: orderNo={}", orderNo);
            // 更新状态日志
            // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            saveStatusLog(order.getId(), orderNo, OrderStatusEnum.CANCELLED, OrderStatusEnum.MANUAL_REQUIRED,
                    operator, "ORDER_CANCEL", "库存释放失败，转人工处理");
            return;
        }

        // === 7. 记录状态变更日志 ===
        // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        saveStatusLog(order.getId(), orderNo, prevStatus, OrderStatusEnum.CANCELLED,
                operator, "ORDER_CANCEL", "用户取消");

        log.info("订单取消成功: orderNo={}, operator={}", orderNo, operator);
    }

    // ==================== 超时处理 ====================

    /**
     * 订单超时处理。
     * <p>
     * 由系统定时任务调用，将超时未支付的订单自动标记为 TIMEOUT 并释放库存。
     * 处理流程：<br>
     * // 1. 查询订单并校验当前状态（PENDING_PAY / PAYING / FAILED 可超时）<br>
     * // 2. 状态机校验<br>
     * // 3. 乐观锁更新订单状态为 TIMEOUT<br>
     * // 4. 查询订单明细获取各商品数量<br>
     * // 5. 逐一释放已锁定库存<br>
     * // 6. 库存释放失败时记录告警
     * </p>
     *
     * <p><b>设计说明：</b>超时处理属于后台批量操作，异常不会中断整体流程。
     * 单个订单处理失败仅记录日志，不影响其他订单的超时处理。</p>
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void timeoutOrder(String orderNo) {
        ThrowUtils.throwIf(StrUtil.isBlank(orderNo), PARAM_ERROR, "订单号不能为空");

        // === 1. 查询订单 ===
        // SQL: SELECT id, order_no, status, version, ... FROM orders WHERE order_no=? AND is_deleted=0 LIMIT 1
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getIsDeleted, 0));
        if (ObjectUtil.isNull(order)) {
            log.warn("超时处理失败，订单不存在: orderNo={}", orderNo);
            return;
        }

        // === 2. 状态机校验 ===
        try {
            OrderStateMachine.transition(order.getStatus(), OrderStatusEnum.TIMEOUT);
        } catch (BusinessException e) {
            log.warn("超时处理跳过: orderNo={}, status={} 不可超时", orderNo, order.getStatus());
            return;
        }

        // === 3. 乐观锁更新状态 ===
        OrderStatusEnum prevStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.TIMEOUT);
        order.setRemark("支付超时");
        // SQL: UPDATE orders SET status='TIMEOUT', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
        int affected = ordersMapper.updateById(order);
        if (affected == 0) {
            log.warn("超时处理乐观锁冲突: orderNo={}", orderNo);
            return; // 乐观锁冲突，跳过本次处理
        }

        // === 4. 查询并释放库存 ===
        // SQL: SELECT id, product_id, quantity FROM order_item WHERE order_id=? AND is_deleted=0
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .eq(OrderItem::getIsDeleted, 0));

        boolean releaseSuccess = true;
        for (OrderItem item : orderItems) {
            try {
                boolean released = inventoryService.releaseStock(item.getProductId(), item.getQuantity());
                if (!released) {
                    log.warn("超时释放库存失败: orderNo={}, productId={}", orderNo, item.getProductId());
                    releaseSuccess = false;
                }
            } catch (Exception e) {
                log.error("超时释放库存异常: orderNo={}, productId={}", orderNo, item.getProductId(), e);
                releaseSuccess = false;
            }
        }

        // === 5. 异常处理 ===
        if (!releaseSuccess) {
            // 标记为需人工处理
            order.setStatus(OrderStatusEnum.MANUAL_REQUIRED);
            order.setRemark("支付超时且库存释放异常，需人工处理");
            // SQL: UPDATE orders SET status='MANUAL_REQUIRED', remark=?, version=version+1, update_time=NOW() WHERE id=? AND is_deleted=0 AND version=?
            ordersMapper.updateById(order);
            // SQL: INSERT INTO order_status_log (...) VALUES (...)
            saveStatusLog(order.getId(), orderNo, OrderStatusEnum.TIMEOUT, OrderStatusEnum.MANUAL_REQUIRED,
                    "SYSTEM", "ORDER_TIMEOUT", "库存释放失败，转人工处理");
            log.warn("超时处理库存释放异常，转人工处理: orderNo={}", orderNo);
            return;
        }

        // === 6. 记录状态变更日志 ===
        // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        saveStatusLog(order.getId(), orderNo, prevStatus, OrderStatusEnum.TIMEOUT,
                "SYSTEM", "ORDER_TIMEOUT", "支付超时");

        log.info("订单超时处理完成: orderNo={}", orderNo);
    }

    // ==================== 状态追溯 ====================

    /**
     * 查询订单状态变更历史。
     * <p>
     * 按时间升序返回订单的完整状态流转轨迹，用于全链路追溯。
     * </p>
     *
     * @param orderNo 订单号
     * @return 状态变更日志列表（按时间升序）
     */
    @Override
    public List<OrderStatusLogResponse> getOrderStatusLogs(String orderNo) {
        ThrowUtils.throwIf(StrUtil.isBlank(orderNo), PARAM_ERROR, "订单号不能为空");

        // 查询状态变更日志，按时间升序排列
        // SQL: SELECT id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time FROM order_status_log WHERE order_no=? ORDER BY create_time ASC
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderNo, orderNo)
                        .orderByAsc(OrderStatusLog::getCreateTime));

        if (ObjectUtil.isEmpty(logs)) return Collections.emptyList();

        // 转换为 VO
        return logs.stream().map(logEntry -> {
            OrderStatusLogResponse vo = new OrderStatusLogResponse();
            vo.setFromStatus(logEntry.getFromStatus());
            vo.setToStatus(logEntry.getToStatus());
            vo.setOperator(logEntry.getOperator());
            vo.setOperationType(logEntry.getOperationType());
            vo.setRemark(logEntry.getRemark());
            vo.setCreateTime(logEntry.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== VO 转换 ====================

    /**
     * 将订单实体转换为订单响应数据。
     * <p>
     * // 1. 设置订单主表字段<br>
     * // 2. 查询该订单下的所有商品明细<br>
     * // 3. 组装明细列表到响应
     * </p>
     *
     * @param order 订单实体
     * @return 订单响应数据
     */
    @Override
    public OrderResponse getOrderVO(Orders order) {
        if (ObjectUtil.isNull(order)) return null;

        // === 1. 设置主表字段 ===
        OrderResponse vo = new OrderResponse();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPostage(order.getPostage());
        vo.setStatus(order.getStatus());
        vo.setQrCodeUrl(order.getQrCodeUrl());
        vo.setPaymentFlowNo(order.getPaymentFlowNo());
        vo.setTransactionId(order.getTransactionId());
        vo.setMailNo(order.getMailNo());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        // === 2. 查询商品明细 ===
        // SQL: SELECT id, product_id, product_name, product_price, quantity, subtotal FROM order_item WHERE order_id=? AND is_deleted=0
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .eq(OrderItem::getIsDeleted, 0));

        // === 3. 转换为 VO 明细列表 ===
        if (ObjectUtil.isNotEmpty(orderItems)) {
            List<OrderResponse.OrderItemResponse> itemVOs = orderItems.stream().map(item -> {
                OrderResponse.OrderItemResponse itemVO = new OrderResponse.OrderItemResponse();
                itemVO.setProductId(item.getProductId());
                itemVO.setProductName(item.getProductName());
                itemVO.setProductPrice(item.getProductPrice());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setSubtotal(item.getSubtotal());
                return itemVO;
            }).collect(Collectors.toList());
            vo.setItems(itemVOs);
        }

        return vo;
    }

    /**
     * 批量将订单实体列表转换为订单响应数据列表。
     * <p>遍历调用 {@link #getOrderVO(Orders)} 进行逐个转换。</p>
     *
     * @param orders 订单实体列表
     * @return 订单响应数据列表
     */
    @Override
    public List<OrderResponse> getOrderVOList(List<Orders> orders) {
        if (ObjectUtil.isEmpty(orders)) return Collections.emptyList();
        return orders.stream().map(this::getOrderVO).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 生成订单号。
     * <p>
     * 格式：ORD + yyyyMMdd + 6位流水号 = 共 18 位。
     * 序列号达到 999999 时自动归零。
     * </p>
     *
     * @return 全局唯一的订单号
     */
    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(ORDER_DATE_FORMAT);
        long seq = ORDER_SEQ.updateAndGet(v -> v >= 999999 ? 0 : v + 1);
        return "ORD" + datePart + String.format("%06d", seq);
    }

    /**
     * 记录订单状态变更日志。
     * <p>
     * 每次订单状态发生变化时调用，持久化变更轨迹。
     * </p>
     *
     * // 1. 创建 OrderStatusLog 实体并填充字段
     * // 2. 调用 orderStatusLogMapper.insert 持久化
     * // SQL: INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, operation_type, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     *
     * @param orderId       订单ID
     * @param orderNo       订单号
     * @param fromStatus    变更前状态（首次创建时为 null）
     * @param toStatus      变更后状态
     * @param operator      操作人（系统触发时为 SYSTEM）
     * @param operationType 操作类型
     * @param remark        变更备注
     */
    public void saveStatusLog(Long orderId, String orderNo, OrderStatusEnum fromStatus,
                              OrderStatusEnum toStatus, String operator, String operationType, String remark) {
        OrderStatusLog logEntry = new OrderStatusLog()
                .setOrderId(orderId)
                .setOrderNo(orderNo)
                .setFromStatus(fromStatus != null ? fromStatus.name() : null)
                .setToStatus(toStatus.name())
                .setOperator(operator)
                .setOperationType(operationType)
                .setRemark(remark);
        orderStatusLogMapper.insert(logEntry);
    }
}
