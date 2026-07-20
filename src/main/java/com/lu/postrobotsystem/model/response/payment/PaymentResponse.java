package com.lu.postrobotsystem.model.response.payment;

import com.lu.postrobotsystem.model.enums.PaymentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应数据
 * <p>
 * 提供给前端展示的支付视图数据，包含支付流水号、金额、状态等信息。
 * 不包含支付敏感数据（如完整响应报文、二维码 Base64 内容），
 * 二维码仅返回链接地址（qrCodeUrl）而非原始数据。
 * </p>
 */
@Data
@Schema(description = "支付响应数据")
public class PaymentResponse {

    /** 支付ID */
    @Schema(description = "支付ID")
    private Long id;

    /** 关联订单ID */
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 支付流水号 */
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 二维码链接（非 Base64 原文，不含敏感信息） */
    @Schema(description = "二维码链接")
    private String qrCodeUrl;

    /** 平台流水号（邮政返回的交易凭证号） */
    @Schema(description = "平台流水号（邮政返回）")
    private String platformFlowNo;

    /** 支付状态 */
    @Schema(description = "支付状态")
    private PaymentStatusEnum status;

    /** 支付金额 */
    @Schema(description = "支付金额")
    private BigDecimal amount;

    /** 支付完成时间 */
    @Schema(description = "支付完成时间")
    private LocalDateTime paidTime;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
