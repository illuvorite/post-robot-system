package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Orders;
import com.lu.postrobotsystem.model.request.order.OrderCreateRequest;
import com.lu.postrobotsystem.model.response.order.OrderResponse;
import com.lu.postrobotsystem.model.response.order.OrderStatusLogResponse;

import java.util.List;

/**
 * 订单服务接口
 * <p>
 * 定义订单从创建到完成的完整生命周期管理操作。
 * 涵盖订单创建（含库存锁定和二维码获取）、状态更新、取消、超时处理、
 * 状态追溯查询等核心业务功能。
 * </p>
 *
 * <p>
 * <b>订单状态机流转：</b><br>
 * <pre>
 * PENDING_PAY → PAYING → PAID（终态）
 *            → CANCELLED（终态）
 *            → TIMEOUT
 * PAYING     → FAILED → PAYING（重试）
 *                      → CANCELLED（终态）
 *                      → TIMEOUT
 *            → MANUAL_REQUIRED → 人工处理后进入对应状态
 * </pre>
 * 所有状态变更均通过 {@link com.lu.postrobotsystem.common.OrderStateMachine} 验证合法性，
 * 并使用数据库乐观锁（version 字段）防止并发覆盖。
 * </p>
 *
 * @see Orders
 * @see com.lu.postrobotsystem.common.OrderStateMachine
 */
public interface OrderService extends IService<Orders> {

    /**
     * 创建订单。
     * <p>
     * 完整的下单流程：<br>
     * // 1. 参数校验与商品查询，计算总价<br>
     * // 2. 锁定库存（调用 InventoryService.lockStock 的 Lua 原子操作）<br>
     * // 3. 生成订单号和支付流水号<br>
     * // 4. 调用 PostalApiClient 获取收款二维码<br>
     * // 5. 保存订单主表和订单明细<br>
     * // 6. 创建支付记录（状态为 PAYING）<br>
     * // 7. 记录状态变更日志和审计日志<br>
     * // 8. 库存锁定失败时整体回滚
     * </p>
     *
     * @param request  订单创建请求（含商品明细）
     * @param userId   当前操作用户ID
     * @return 订单响应（含二维码链接、支付流水号等）
     */
    OrderResponse createOrder(OrderCreateRequest request, Long userId);

    /**
     * 根据主键查询订单。
     * <p>
     * 查询订单基本信息并组装商品明细列表。
     * </p>
     *
     * @param id 订单ID
     * @return 订单响应数据
     */
    OrderResponse getOrderById(Long id);

    /**
     * 根据订单号查询订单。
     *
     * @param orderNo 订单号
     * @return 订单响应数据
     */
    OrderResponse getOrderByNo(String orderNo);

    /**
     * 用户取消订单。
     * <p>
     * // 1. 校验状态机：仅 PENDING_PAY / FAILED 可取消<br>
     * // 2. 乐观锁更新订单状态为 CANCELLED<br>
     * // 3. 释放已锁定库存（调用 InventoryService.releaseStock）<br>
     * // 4. 记录状态变更日志和审计日志<br>
     * // 5. 库存释放失败时标记为需人工处理
     * </p>
     *
     * @param orderNo  订单号
     * @param operator 操作人用户名
     */
    void cancelOrder(String orderNo, String operator);

    /**
     * 订单超时处理。
     * <p>
     * 系统定时任务调用，将超时未支付的订单状态更新为 TIMEOUT。
     * // 1. 校验状态机：PENDING_PAY / PAYING / FAILED 可超时<br>
     * // 2. 乐观锁更新状态<br>
     * // 3. 释放已锁定库存<br>
     * // 4. 记录状态变更日志
     * </p>
     *
     * @param orderNo 订单号
     */
    void timeoutOrder(String orderNo);

    /**
     * 查询订单的状态变更历史。
     * <p>
     * 按时间升序返回订单的完整状态流转轨迹。
     * </p>
     *
     * @param orderNo 订单号
     * @return 状态变更日志列表
     */
    List<OrderStatusLogResponse> getOrderStatusLogs(String orderNo);

    /**
     * 将订单实体转换为订单响应数据。
     * <p>
     * 组装商品明细列表，不包含支付敏感信息。
     * </p>
     *
     * @param order 订单实体
     * @return 订单响应数据
     */
    OrderResponse getOrderVO(Orders order);

    /**
     * 批量将订单实体列表转换为订单响应数据列表。
     *
     * @param orders 订单实体列表
     * @return 订单响应数据列表
     */
    List<OrderResponse> getOrderVOList(List<Orders> orders);
}
