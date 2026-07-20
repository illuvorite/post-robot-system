package com.lu.postrobotsystem.model.response.order;

import com.lu.postrobotsystem.model.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应数据
 * <p>
 * 提供给前端展示的订单视图数据，包含订单基本信息、商品明细列表。
 * 不包含支付敏感信息（如完整二维码 Base64 内容等）。
 * </p>
 */
@Data
@Schema(description = "订单响应数据")
public class OrderResponse {

    /** 订单ID（Snowflake） */
    @Schema(description = "订单ID")
    private Long id;

    /** 订单号 */
    @Schema(description = "订单号")
    private String orderNo;

    /** 订单总金额 */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /** 邮资费用 */
    @Schema(description = "邮资费用")
    private BigDecimal postage;

    /** 订单状态 */
    @Schema(description = "订单状态")
    private OrderStatusEnum status;

    /** 收款二维码链接（用户扫码支付，非 Base64 原文） */
    @Schema(description = "收款二维码链接")
    private String qrCodeUrl;

    /** 支付流水号 */
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 邮政交易流水号 */
    @Schema(description = "邮政交易流水号")
    private String transactionId;

    /** 关联邮件号码 */
    @Schema(description = "关联邮件号码")
    private String mailNo;

    /** 备注/失败原因 */
    @Schema(description = "备注/失败原因")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 商品明细列表 */
    @Schema(description = "商品明细列表")
    private List<OrderItemResponse> items;

    /**
     * 订单商品明细响应
     * <p>包含下单时的商品快照信息（名称、单价、数量、小计）。</p>
     */
    @Data
    @Schema(description = "订单商品明细响应")
    public static class OrderItemResponse {

        /** 商品ID */
        @Schema(description = "商品ID")
        private Long productId;

        /** 商品名称（下单快照） */
        @Schema(description = "商品名称（下单快照）")
        private String productName;

        /** 商品单价（下单快照） */
        @Schema(description = "商品单价（下单快照）")
        private BigDecimal productPrice;

        /** 购买数量 */
        @Schema(description = "购买数量")
        private Integer quantity;

        /** 小计金额 */
        @Schema(description = "小计金额")
        private BigDecimal subtotal;
    }
}
