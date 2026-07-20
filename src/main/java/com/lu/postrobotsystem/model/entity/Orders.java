package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * <p>
 * 对应数据库 orders 表，记录用户购物的完整订单信息，
 * 包含订单金额、邮资、支付相关信息（二维码、流水号等）以及订单状态流转。
 * 订单支持待支付、支付中、已支付、失败、取消、超时等状态，
 * 并与邮政系统交互完成支付和物流对接。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("orders")
@Schema(description = "订单")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "订单ID（Snowflake）")
    private Long id;

    /** 订单号，业务展示用唯一编号 */
    @TableField("order_no")
    @Schema(description = "订单号")
    private String orderNo;

    /** 用户ID，关联用户表 */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /** 订单总金额（商品总价 + 邮资） */
    @TableField("total_amount")
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /** 邮资费用 */
    @TableField("postage")
    @Schema(description = "邮资费用")
    private BigDecimal postage;

    /** 订单状态（PENDING_PAY-待支付 / PAYING-支付中 / PAID-支付成功 / FAILED-支付失败 / CANCELLED-已取消 / TIMEOUT-已超时 / MANUAL_REQUIRED-需人工处理），参见 OrderStatusEnum */
    @TableField("status")
    @Schema(description = "订单状态（PENDING_PAY/PAYING/PAID/FAILED/CANCELLED/TIMEOUT/MANUAL_REQUIRED）")
    private OrderStatusEnum status;

    /** 关联的邮件号码，用于物流追踪 */
    @TableField("mail_no")
    @Schema(description = "关联邮件号码")
    private String mailNo;

    /** 邮政系统返回的交易流水号 */
    @TableField("transaction_id")
    @Schema(description = "邮政交易流水号")
    private String transactionId;

    /** 支付流水号，用于查询支付结果 */
    @TableField("payment_flow_no")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    /** 收款二维码链接，用户扫码完成支付 */
    @TableField("qr_code_url")
    @Schema(description = "收款二维码链接")
    private String qrCodeUrl;

    /** 支付查询流水号，用于主动轮询支付状态 */
    @TableField("pay_query_no")
    @Schema(description = "支付查询流水号")
    private String payQueryNo;

    /** 备注信息，支付失败时记录失败原因 */
    @TableField("remark")
    @Schema(description = "备注/失败原因")
    private String remark;

    /** 乐观锁版本号，用于状态更新的并发控制 */
    @TableField("version")
    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

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
