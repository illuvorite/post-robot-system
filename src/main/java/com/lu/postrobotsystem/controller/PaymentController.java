package com.lu.postrobotsystem.controller;

import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.common.annotation.AuditLog;
import com.lu.postrobotsystem.model.request.payment.PaymentCallbackRequest;
import static com.lu.postrobotsystem.model.enums.OperationTypeEnum.PAYMENT_CALLBACK;
import com.lu.postrobotsystem.model.response.payment.PaymentResponse;
import com.lu.postrobotsystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付管理控制器。
 * <p>
 * 提供支付回调接收、支付记录查询等接口。
 * 支付回调接口对邮政系统开放，无需 JWT 认证（通过在 SecurityConfig 中配置白名单实现）。
 * 查询接口需要登录认证。
 * </p>
 *
 * <p><b>支付信息安全：</b><br>
 * - 回调处理中不持久化原始报文和签名原文<br>
 * - 响应数据中不包含二维码 Base64 内容等敏感信息<br>
 * - 日志输出时对签名等敏感字段进行脱敏处理
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@Tag(name = "支付管理")
@RequiredArgsConstructor
public class PaymentController {

    /** 支付服务层 */
    private final PaymentService paymentService;

    /**
     * 接收邮政支付异步回调通知。
     * <p>
     * 该接口对邮政系统开放，用于接收支付结果的异步通知。
     * 处理流程：验签 → 幂等性检查 → 更新订单状态 → 扣减/释放库存。
     * 接口不要求 JWT 认证（白名单路径），但会验证回调签名。
     * </p>
     *
     * <p><b>安全说明：</b><br>
     * - 回调请求来源IP会被记录用于审计<br>
     * - 签名仅用于首次验证，验签通过后不持久化到数据库<br>
     * - 日志中签名内容被脱敏处理（仅显示前4位）
     * </p>
     *
     * // 1. 记录回调来源IP（用于安全审计）
     * // 2. 日志输出时对 sign 字段进行脱敏
     * // 3. 委托 paymentService 执行验签、幂等性检查、状态更新和库存操作
     *
     * @param callback 回调请求体（含订单号、支付流水号、平台流水号、状态、签名等）
     * @param request  HTTP 请求对象（用于获取来源 IP）
     * @return 统一响应，附带"处理成功"提示
     */
    @PostMapping("/callback")
    @Operation(summary = "接收支付异步回调（邮政通知）")
    @AuditLog(operationType = PAYMENT_CALLBACK, targetType = "ORDER", targetIdExpression = "#callback.orderNo", detail = "支付回调")
    public Result<Void> handlePaymentCallback(@Valid @RequestBody PaymentCallbackRequest callback,
                                               HttpServletRequest request) {
        // 获取回调来源 IP（用于审计日志记录）
        String sourceIp = getClientIp(request);

        // 委托支付服务处理回调（含验签、幂等性检查、状态更新和库存操作）
        paymentService.handlePaymentCallback(callback, sourceIp);

        return Result.success(null, "回调处理成功");
    }

    /**
     * 根据订单ID查询支付记录。
     * <p>
     * 查询指定订单的所有支付记录（一个订单可能因失败重试有多条记录）。
     * 需要登录认证。
     * </p>
     *
     * @param orderId 订单ID
     * @return 统一响应，包含支付记录列表
     */
    @GetMapping("/list/{orderId}")
    @Operation(summary = "查询订单的支付记录")
    public Result<List<PaymentResponse>> getPayments(@PathVariable Long orderId) {
        return Result.success(paymentService.getPaymentsByOrderId(orderId));
    }

    /**
     * 根据支付流水号查询支付记录。
     * <p>
     * 通过支付流水号精确查询支付详情，用于支付对账和问题排查。
     * </p>
     *
     * @param paymentFlowNo 支付流水号
     * @return 统一响应，包含支付详情
     */
    @GetMapping("/getByFlow/{paymentFlowNo}")
    @Operation(summary = "根据支付流水号查询支付记录")
    public Result<PaymentResponse> getByPaymentFlowNo(@PathVariable String paymentFlowNo) {
        return Result.success(paymentService.getByPaymentFlowNo(paymentFlowNo));
    }

    /**
     * 获取客户端真实IP地址。
     * <p>
     * 优先从 X-Forwarded-For 头获取（经过代理时），
     * 否则直接从 request.getRemoteAddr() 获取。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能返回多个IP（逗号分隔），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
