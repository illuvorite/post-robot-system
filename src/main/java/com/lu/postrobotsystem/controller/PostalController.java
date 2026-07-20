package com.lu.postrobotsystem.controller;

import com.lu.postrobotsystem.adapter.postal.model.request.*;
import com.lu.postrobotsystem.adapter.postal.model.response.*;
import com.lu.postrobotsystem.adapter.postal.service.PostalAdapterService;
import com.lu.postrobotsystem.adapter.postal.spi.PostalApiMock;
import com.lu.postrobotsystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮政系统对接管理控制器
 * <p>
 * 提供邮政接口的手动测试端点和管理功能（Mock 状态查看/重置）。
 * 仅 ADMIN 角色可访问。
 * </p>
 */
@RestController
@RequestMapping("/postal")
@Tag(name = "邮政系统对接管理")
@RequiredArgsConstructor
public class PostalController {

    private final PostalAdapterService postalAdapterService;
    private final PostalApiMock postalApiMock;

    /**
     * F1：邮件资费查询（测试用）
     */
    @PostMapping("/postage-query")
    @Operation(summary = "邮件资费查询")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PostageQueryResponse> queryPostage(@RequestBody PostageQueryBody body) {
        PostageQueryResponse response = postalAdapterService.queryPostage(body);
        return Result.success(response);
    }

    /**
     * F2：邮件号码生成（测试用）
     */
    @PostMapping("/mail-number")
    @Operation(summary = "邮件号码生成")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MailNumberResponse> generateMailNumber(@RequestBody MailNumberBody body) {
        MailNumberResponse response = postalAdapterService.generateMailNumber(body);
        return Result.success(response);
    }

    /**
     * F3：收寄订单提交（测试用）
     */
    @PostMapping("/order-submit")
    @Operation(summary = "收寄订单提交")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<OrderSubmitResponse> submitOrder(@RequestBody OrderSubmitBody body) {
        OrderSubmitResponse response = postalAdapterService.submitOrder(body);
        return Result.success(response);
    }

    /**
     * F4：收款二维码生成（测试用）
     */
    @PostMapping("/qr-code")
    @Operation(summary = "生成收款二维码")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<QrCodeResponse> generateQrCode(@RequestParam String orderNo,
                                                   @RequestParam String amount,
                                                   @RequestParam String paymentFlowNo) {
        QrCodeResponse response = postalAdapterService.generateQrCode(orderNo, amount, paymentFlowNo);
        return Result.success(response);
    }

    /**
     * F5：支付状态查询（测试用）
     */
    @GetMapping("/payment-status")
    @Operation(summary = "查询支付状态")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PaymentStatusResponse> queryPaymentStatus(@RequestParam String queryNo,
                                                             @RequestParam String paymentFlowNo) {
        PaymentStatusResponse response = postalAdapterService.queryPaymentStatus(queryNo, paymentFlowNo);
        return Result.success(response);
    }

    /**
     * 查看 Mock 状态机当前快照
     */
    @GetMapping("/mock/status")
    @Operation(summary = "查看 Mock 状态机")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getMockStatus() {
        ConcurrentHashMap<String, Long> snapshot = postalApiMock.getPaymentStatusSnapshot();
        Map<String, Object> status = Map.of(
                "mockEnabled", true,
                "paymentStatusCount", snapshot.size(),
                "paymentStatuses", snapshot
        );
        return Result.success(status);
    }

    /**
     * 重置 Mock 状态机
     */
    @PostMapping("/mock/reset")
    @Operation(summary = "重置 Mock 状态机")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetMock() {
        postalApiMock.resetMockState();
        return Result.success(null, "Mock 状态已重置");
    }
}
