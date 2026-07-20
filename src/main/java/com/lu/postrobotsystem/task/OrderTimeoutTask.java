package com.lu.postrobotsystem.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lu.postrobotsystem.mapper.OrdersMapper;
import com.lu.postrobotsystem.model.entity.Orders;
import com.lu.postrobotsystem.model.enums.OrderStatusEnum;
import com.lu.postrobotsystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时处理定时任务
 * <p>
 * 定时扫描超过支付时限仍未支付的订单，自动将其状态更新为 TIMEOUT 并释放锁定库存。
 * 作为支付流程的兜底机制，防止无限占用库存。
 * </p>
 *
 * <p><b>处理策略：</b><br>
 * - 每 2 分钟执行一次<br>
 * - 处理创建时间超过配置的超时阈值（默认 30 分钟）且状态为 PENDING_PAY / PAYING 的订单<br>
 * - 单次最多处理 100 个订单，避免长时间占用数据库连接<br>
 * - 单个订单处理异常仅记录日志，不影响其他订单
 * </p>
 *
 * <p><b>幂等性：</b><br>
 * - 查询条件限定 status = PENDING_PAY 或 PAYING，避免重复处理<br>
 * - 状态更新通过乐观锁（version）保证仅更新一次<br>
 * - 库存释放通过 Lua 脚本保证原子性
 * </p>
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "order.timeout.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrdersMapper ordersMapper;
    private final OrderService orderService;

    /** 订单超时阈值（分钟），从配置文件读取，默认 30 分钟 */
    @Value("${order.timeout.minutes:30}")
    private int timeoutMinutes;

    /**
     * 订单超时处理任务（每 2 分钟执行一次）。
     * <p>
     * // 1. 查询创建时间超过阈值且状态为 PENDING_PAY / PAYING 的订单<br>
     * // 2. 逐一调用 orderService.timeoutOrder 执行超时处理<br>
     * // 3. 每个订单独立事务，异常不影响其他订单
     * </p>
     */
    @Scheduled(fixedRate = 120000) // 每 2 分钟执行一次
    public void processTimeoutOrders() {
        log.debug("订单超时处理任务开始执行: timeoutMinutes={}", timeoutMinutes);

        // === 1. 查询超时订单 ===
        // 查询创建时间超过阈值的 PENDING_PAY / PAYING 状态订单
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Orders> timeoutOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getIsDeleted, 0)
                        .in(Orders::getStatus, OrderStatusEnum.PENDING_PAY, OrderStatusEnum.PAYING)
                        .lt(Orders::getCreateTime, deadline)
                        .orderByAsc(Orders::getCreateTime)
                        .last("LIMIT 100")); // 单次最多处理 100 个

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("订单超时处理: 发现 {} 个超时订单（阈值={}分钟）", timeoutOrders.size(), timeoutMinutes);

        // === 2. 逐一处理超时订单 ===
        for (Orders order : timeoutOrders) {
            try {
                orderService.timeoutOrder(order.getOrderNo());
            } catch (Exception e) {
                log.error("订单超时处理异常: orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("订单超时处理任务完成: 处理 {} 个订单", timeoutOrders.size());
    }
}
