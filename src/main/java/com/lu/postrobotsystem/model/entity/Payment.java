package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.PaymentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付实体类
 * <p>
 * 对应数据库 payment 表，记录每笔订单的支付交易信息，
 * 包含支付流水号、二维码、平台流水号、支付金额和状态等。
 * 与邮政支付系统对接，支持支付中、成功、失败、退款等全生命周期管理。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("payment")
@Schema(description = "支付")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "支付ID（Snowflake）")
    private Long id;

    /** 关联的订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 支付流水号（系统内部生成，用于标识本次支付请求） */
    @TableField("payment_flow_no")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 查询流水号，用于向支付平台查询支付结果 */
    @TableField("pay_query_no")
    @Schema(description = "查询流水号")
    private String payQueryNo;

    /** 收款二维码链接（用户扫码支付时使用） */
    @TableField("qr_code_url")
    @Schema(description = "二维码链接")
    private String qrCodeUrl;

    /** 平台流水号（邮政支付系统返回的交易凭证号） */
    @TableField("platform_flow_no")
    @Schema(description = "平台流水号（邮政返回）")
    private String platformFlowNo;

    /** 支付状态（PAYING-支付中 / SUCCESS-支付成功 / FAILED-支付失败 / REFUNDED-已退款 / PARTIAL_REFUND-部分退款），参见 PaymentStatusEnum */
    @TableField("status")
    @Schema(description = "支付状态（PAYING/SUCCESS/FAILED/REFUNDED/PARTIAL_REFUND）")
    private PaymentStatusEnum status;

    /** 支付金额 */
    @TableField("amount")
    @Schema(description = "支付金额")
    private BigDecimal amount;

    /** 支付完成时间 */
    @TableField("paid_time")
    @Schema(description = "支付完成时间")
    private LocalDateTime paidTime;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 记录更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-正常 1-删除） */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
