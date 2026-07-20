package com.lu.postrobotsystem.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lu.postrobotsystem.common.PostalApiClient;
import com.lu.postrobotsystem.mapper.OrderItemMapper;
import com.lu.postrobotsystem.mapper.OrdersMapper;
import com.lu.postrobotsystem.model.entity.OrderItem;
import com.lu.postrobotsystem.model.entity.Orders;
import com.lu.postrobotsystem.model.enums.OrderStatusEnum;
import com.lu.postrobotsystem.service.InventoryService;
import com.lu.postrobotsystem.service.impl.OrderServiceImpl;
import com.lu.postrobotsystem.service.impl.PaymentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付状态轮询定时任务
 * <p>
 * 定时查询处于 PAYING 状态的订单，调用邮政支付状态查询接口获取最新支付结果，
 * 根据查询结果更新订单状态并处理库存。
 * 作为异步回调的补充机制，确保支付状态的最终一致性。
 * </p>
 *
 * <p><b>轮询策略：</b><br>
 * - 每 30 秒执行一次（可通过配置文件调整）<br>
 * - 查询创建时间超过 2 分钟且状态为 PAYING 的订单（避免刚创建时立即查询）<br>
 * - 单次最多处理 50 个订单，避免一次处理过多<br>
 * - 处理失败仅记录日志，不影响下一轮轮询
 * </p>
 *
 * <p><b>幂等性：</b><br>
 * - 查询前通过订单状态过滤，避免重复处理<br>
 * - 状态更新使用 MyBatis-Plus 乐观锁（version 字段）<br>
 * - 库存操作通过 Lua 脚本保证原子性
 * </p>
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "payment.polling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PaymentPollingTask {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final PostalApiClient postalApiClient;
    private final InventoryService inventoryService;
    private final OrderServiceImpl orderService;

    /**
     * 支付状态轮询任务（每 30 秒执行一次）。
     * <p>
     * // 1. 查询状态为 PAYING 且创建时间超过 2 分钟的订单（避免过早查询）<br>
     * // 2. 逐一调用 PostalApiClient.queryPaymentStatus 查询支付结果<br>
     * // 3. 处理逻辑与回调一致：成功则扣减库存，失败则释放库存<br>
     * // 4. 异常情况记录日志，订单保持 PAYING 状态等待下次轮询
     * </p>
     */
    @Scheduled(fixedRateString = "${payment.polling.interval-ms:30000}")
    public void pollPaymentStatus() {
        log.debug("支付状态轮询任务开始执行");

        // === 1. 查询待处理的 PAYING 订单 ===
        // 查询创建时间超过 2 分钟的 PAYING 订单（避免刚创建即查询）
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        List<Orders> payingOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getStatus, OrderStatusEnum.PAYING)
                        .eq(Orders::getIsDeleted, 0)
                        .lt(Orders::getCreateTime, threshold)
                        .orderByAsc(Orders::getCreateTime)
                        .last("LIMIT 50")); // 单次最多处理 50 个

        if (payingOrders.isEmpty()) {
            return;
        }

        log.info("支付状态轮询: 发现 {} 个待处理的 PAYING 订单", payingOrders.size());

        // === 2. 逐一查询支付状态 ===
        for (Orders order : payingOrders) {
            try {
                processPaymentStatus(order);
            } catch (Exception e) {
                log.error("支付状态轮询处理异常: orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("支付状态轮询任务完成: 处理 {} 个订单", payingOrders.size());
    }

    /**
     * 处理单个订单的支付状态查询结果。
     * <p>
     * // 1. 调用邮政接口查询支付结果<br>
     * // 2. 查询接口返回失败时跳过（可能是网络抖动或邮政系统延迟）<br>
     * // 3. 已支付 → 乐观锁更新为 PAID → 扣减库存<br>
     * // 4. 未支付 → 跳过，等待下次轮询（未超时）<br>
     * // 5. 记录状态变更日志
     * </p>
     *
     * @param order 待查询的订单
     */
    @Transactional(rollbackFor = Exception.class)
    protected void processPaymentStatus(Orders order) {
        String orderNo = order.getOrderNo();
        String payQueryNo = order.getPayQueryNo();

        // === 1. 查询支付结果 ===
        PostalApiClient.PaymentQueryResult queryResult = postalApiClient.queryPaymentStatus(payQueryNo, orderNo);
        if (!queryResult.isSuccess()) {
            log.warn("支付状态查询接口失败: orderNo={}", orderNo);
            return; // 查询失败，下次轮询重试
        }

        // === 2. 根据查询结果处理 ===
        if (!queryResult.isPaid()) {
            // 查询到未支付，跳过（等待下次轮询或超时任务处理）
            return;
        }

        // === 3. 已支付：再次确认订单状态（避免并发修改） ===
        Orders latestOrder = ordersMapper.selectById(order.getId());
        if (latestOrder == null || latestOrder.getStatus() != OrderStatusEnum.PAYING) {
            log.info("支付轮询跳过: 订单状态已变更, orderNo={}, status={}",
                    orderNo, latestOrder != null ? latestOrder.getStatus() : "DELETED");
            return;
        }

        // === 4. 更新订单状态为 PAID（乐观锁） ===
        OrderStatusEnum prevStatus = latestOrder.getStatus();
        latestOrder.setStatus(OrderStatusEnum.PAID);
        latestOrder.setTransactionId(queryResult.getPlatformFlowNo());
        int affected = ordersMapper.updateById(latestOrder);

        if (affected == 0) {
            log.warn("支付轮询乐观锁冲突: orderNo={}", orderNo);
            return;
        }

        // === 5. 扣减库存 ===
        // 查询订单明细获取各商品数量
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .eq(OrderItem::getIsDeleted, 0));

        boolean stockOpSuccess = true;
        for (OrderItem item : orderItems) {
            try {
                boolean deducted = inventoryService.deductStock(item.getProductId(), item.getQuantity());
                if (!deducted) {
                    log.warn("支付轮询库存扣减失败: orderNo={}, productId={}", orderNo, item.getProductId());
                    stockOpSuccess = false;
                }
            } catch (Exception e) {
                log.error("支付轮询库存扣减异常: orderNo={}, productId={}", orderNo, item.getProductId(), e);
                stockOpSuccess = false;
            }
        }

        // === 6. 库存扣减异常处理 ===
        if (!stockOpSuccess) {
            latestOrder.setStatus(OrderStatusEnum.MANUAL_REQUIRED);
            latestOrder.setRemark("轮询支付成功但库存扣减异常，需人工处理");
            ordersMapper.updateById(latestOrder);
            orderService.saveStatusLog(latestOrder.getId(), orderNo, prevStatus,
                    OrderStatusEnum.MANUAL_REQUIRED, "SYSTEM", "PAYMENT_CALLBACK",
                    "轮询支付成功但库存扣减异常");
            log.warn("支付轮询库存扣减异常，转人工处理: orderNo={}", orderNo);
            return;
        }

        // === 7. 记录状态变更日志 ===
        orderService.saveStatusLog(latestOrder.getId(), orderNo, prevStatus, OrderStatusEnum.PAID,
                "SYSTEM", "PAYMENT_CALLBACK", "支付轮询确认成功");

        log.info("支付轮询处理成功: orderNo={}", orderNo);
    }
}
