package com.lu.postrobotsystem.model.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮政支付异步回调请求体
 * <p>
 * 接收邮政系统异步通知的支付结果数据。
 * 包含支付流水号、平台流水号、支付状态及签名。
 * 处理流程：验签 → 幂等性检查 → 更新订单状态 → 扣减库存。
 * </p>
 *
 * <p><b>支付信息安全：</b><br>
 * - 原始报文仅用于验签，验签通过后提取必要业务字段<br>
 * - 签名原文和完整报文不持久化到数据库<br>
 * - 仅持久化 paymentFlowNo、platformFlowNo 等非敏感业务标识<br>
 * - 日志输出时对 sign 字段进行脱敏处理
 * </p>
 */
@Data
@Schema(description = "支付回调请求体")
public class PaymentCallbackRequest {

    /** 订单号 */
    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号")
    private String orderNo;

    /** 支付流水号（系统内部生成的流水号） */
    @NotBlank(message = "支付流水号不能为空")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 平台流水号（邮政端生成的交易凭证号） */
    @Schema(description = "平台流水号（邮政返回）")
    private String platformFlowNo;

    /** 支付状态（SUCCESS-支付成功 FAILED-支付失败） */
    @Schema(description = "支付状态（SUCCESS/FAILED）")
    private String payStatus;

    /** 支付完成时间（yyyyMMddHHmmss） */
    @Schema(description = "支付完成时间")
    private String paidTime;

    /** 失败原因（支付失败时填写） */
    @Schema(description = "失败原因")
    private String failReason;

    /** 签名（用于验签，日志输出时需脱敏） */
    @Schema(description = "签名")
    private String sign;

    /** 请求时间戳 */
    @Schema(description = "请求时间戳")
    private String reqTime;

    /** 事务ID */
    @Schema(description = "事务ID")
    private String transactionId;
}
