package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Payment;
import com.lu.postrobotsystem.model.entity.PaymentFlowRecord;
import com.lu.postrobotsystem.model.request.payment.PaymentCallbackRequest;
import com.lu.postrobotsystem.model.response.payment.PaymentResponse;

import java.util.List;

/**
 * 支付服务接口
 * <p>
 * 定义支付全生命周期的管理操作，包括：
 * - 支付回调处理（邮政异步通知）<br>
 * - 支付状态轮询（系统定时任务）<br>
 * - 支付记录查询<br>
 * </p>
 *
 * <p>
 * <b>核心约束：</b><br>
 * - 幂等性：同一支付回调或查询结果重复到达时，不会导致重复状态变更、重复扣减库存或重复释放库存<br>
 * - 支付信息安全：支付凭证原始数据不落盘、不入库；日志输出自动脱敏<br>
 * - 事务一致性：支付状态变更与库存扣减/释放需保持最终一致性
 * </p>
 *
 * @see Payment
 * @see PaymentFlowRecord
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 处理支付回调（邮政异步通知）。
     * <p>
     * // 1. 验签：验证回调请求的签名合法性<br>
     * // 2. 幂等性检查：根据 paymentFlowNo + 当前订单状态判断是否已处理<br>
     * // 3. 通过状态机校验：仅 PAYING 状态可接收回调<br>
     * // 4. 乐观锁更新订单状态（PAYING → PAID 或 PAYING → FAILED）<br>
     * // 5. 支付成功 → 调用 InventoryService.deductStock 扣减库存<br>
     * //   支付失败 → 调用 InventoryService.releaseStock 释放锁定库存<br>
     * // 6. 更新支付记录状态<br>
     * // 7. 记录支付流水摘要（仅非敏感字段）<br>
     * // 8. 记录状态变更日志和审计日志
     * </p>
     *
     * @param callback 回调请求体（含订单号、支付流水号、平台流水号、状态、签名等）
     * @param sourceIp 回调来源IP
     */
    void handlePaymentCallback(PaymentCallbackRequest callback, String sourceIp);

    /**
     * 查询支付记录。
     *
     * @param orderId 订单ID
     * @return 支付记录列表
     */
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);

    /**
     * 根据支付流水号查询支付记录。
     *
     * @param paymentFlowNo 支付流水号
     * @return 支付响应数据
     */
    PaymentResponse getByPaymentFlowNo(String paymentFlowNo);

    /**
     * 将支付实体转换为支付响应数据。
     *
     * @param payment 支付实体
     * @return 支付响应数据
     */
    PaymentResponse getPaymentVO(Payment payment);

    /**
     * 批量将支付实体列表转换为支付响应数据列表。
     *
     * @param payments 支付实体列表
     * @return 支付响应数据列表
     */
    List<PaymentResponse> getPaymentVOList(List<Payment> payments);
}
