package com.lu.postrobotsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.common.annotation.AuditLog;
import com.lu.postrobotsystem.model.entity.Orders;
import com.lu.postrobotsystem.model.request.order.OrderCreateRequest;
import com.lu.postrobotsystem.model.response.order.OrderResponse;
import com.lu.postrobotsystem.model.response.order.OrderStatusLogResponse;
import com.lu.postrobotsystem.service.OrderService;
import com.lu.postrobotsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import static com.lu.postrobotsystem.model.enums.OperationTypeEnum.ORDER_CANCEL;
import static com.lu.postrobotsystem.model.enums.OperationTypeEnum.ORDER_CREATE;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器。
 * <p>
 * 提供订单的创建、查询、取消以及状态追溯等 RESTful 接口。
 * 所有接口统一返回 {@link Result<T>} 格式的响应。
 * </p>
 */
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理")
@RequiredArgsConstructor
public class OrderController {

    /** 订单服务层 */
    private final OrderService orderService;

    /** 用户服务层（用于获取当前登录用户） */
    private final UserService userService;

    /**
     * 创建订单。
     * <p>
     * 用户提交商品选择后创建订单，流程包括库存锁定和二维码获取。
     * 返回的订单数据中包含二维码链接，供前端展示用于用户扫码支付。
     * 需要用户已登录（JWT 认证）。
     * </p>
     *
     * // 1. 从 JWT 上下文获取当前登录用户ID
     * // 2. 调用 orderService.createOrder 执行完整下单流程
     * // 3. 返回含二维码链接的订单响应
     *
     * @param request      订单创建请求（含商品明细和邮资）
     * @param httpRequest  HTTP 请求对象（用于提取 JWT 中的用户信息）
     * @return 统一响应，包含订单详情（含二维码链接）
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单")
    @AuditLog(operationType = ORDER_CREATE, targetType = "ORDER", targetIdExpression = "", detail = "创建订单")
    public Result<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request,
                                              HttpServletRequest httpRequest) {
        // 1. 从 JWT 认证上下文获取当前登录用户ID
        Long userId = userService.getLoginUser(httpRequest).getId();
        // 2. 执行完整下单流程（锁定库存 → 获取二维码 → 保存订单与支付记录）
        OrderResponse response = orderService.createOrder(request, userId);
        return Result.success(response, "订单创建成功");
    }

    /**
     * 根据订单ID查询订单详情。
     * <p>
     * 查询指定订单的完整信息，包括商品明细列表。
     * </p>
     *
     * @param id 订单ID（Snowflake）
     * @return 统一响应，包含订单详情
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据订单ID查询订单")
    public Result<OrderResponse> getOrderById(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    /**
     * 根据订单号查询订单详情。
     * <p>
     * 通过业务订单号查询订单完整信息，适用于按订单号追溯的场景。
     * </p>
     *
     * @param orderNo 订单号（ORD 开头的业务编号）
     * @return 统一响应，包含订单详情
     */
    @GetMapping("/getByNo/{orderNo}")
    @Operation(summary = "根据订单号查询订单")
    public Result<OrderResponse> getOrderByNo(@PathVariable String orderNo) {
        return Result.success(orderService.getOrderByNo(orderNo));
    }

    /**
     * 分页查询订单列表。
     * <p>
     * 按指定条件分页查询订单列表，支持按订单号、状态等信息过滤。
     * 默认按创建时间降序排列。
     * </p>
     *
     * // 1. 构建 LambdaQueryWrapper 查询条件
     * // 2. MyBatis-Plus 分页查询
     * // 3. 转为 VO 分页结果
     *
     * @param orderNo  订单号（可选过滤条件）
     * @param status   订单状态（可选过滤条件）
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 统一响应，包含分页订单数据
     */
    @GetMapping("/list/page")
    @Operation(summary = "分页查询订单列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Page<OrderResponse>> listOrdersPage(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 1. 构建查询条件（默认排除已删除记录）
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getIsDeleted, 0);
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Orders::getOrderNo, orderNo);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getCreateTime);

        // 2. 分页查询，获取 Orders 实体分页数据
        Page<Orders> orderPage = orderService.page(new Page<>(pageNum, pageSize), wrapper);

        // 3. 构建同页参数的 VO 分页结果，将实体列表转为响应对象
        Page<OrderResponse> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderService.getOrderVOList(orderPage.getRecords()));
        return Result.success(voPage);
    }

    /**
     * 取消订单。
     * <p>
     * 用户主动取消处于 PENDING_PAY 或 FAILED 状态的订单。
     * 取消后释放已锁定的库存，若库存释放失败则标记为需人工处理。
     * </p>
     *
     * // 1. 从 JWT 上下文获取当前用户名
     * // 2. 调用 orderService.cancelOrder 执行取消流程（含状态机校验和库存释放）
     *
     * @param orderNo     待取消的订单号
     * @param httpRequest HTTP 请求对象（用于提取操作用户名）
     * @return 统一响应，附带取消成功提示
     */
    @PostMapping("/cancel/{orderNo}")
    @Operation(summary = "取消订单")
    @AuditLog(operationType = ORDER_CANCEL, targetType = "ORDER", targetIdExpression = "#orderNo", detail = "取消订单")
    public Result<Void> cancelOrder(@PathVariable @NotBlank String orderNo,
                                     HttpServletRequest httpRequest) {
        // 从 JWT 上下文获取当前用户名
        String operator = userService.getLoginUser(httpRequest).getUsername();
        orderService.cancelOrder(orderNo, operator);
        return Result.success(null, "订单取消成功");
    }

    /**
     * 获取订单状态变更历史。
     * <p>
     * 查询指定订单的完整状态流转轨迹，按时间升序返回。
     * 可用于前端展示订单处理进度时间线。
     * </p>
     *
     * @param orderNo 订单号
     * @return 统一响应，包含状态变更历史列表
     */
    @GetMapping("/statusLogs/{orderNo}")
    @Operation(summary = "获取订单状态变更历史")
    public Result<List<OrderStatusLogResponse>> getOrderStatusLogs(@PathVariable String orderNo) {
        return Result.success(orderService.getOrderStatusLogs(orderNo));
    }
}
