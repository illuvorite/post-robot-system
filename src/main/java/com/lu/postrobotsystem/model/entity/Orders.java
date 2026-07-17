package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("orders")
@Schema(description = "订单")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "订单ID（Snowflake）")
    private Long id;

    @TableField("order_no")
    @Schema(description = "订单号")
    private String orderNo;

    @TableField("total_amount")
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @TableField("postage")
    @Schema(description = "邮资费用")
    private BigDecimal postage;

    @TableField("status")
    @Schema(description = "订单状态（PENDING_PAY/PAYING/PAID/FAILED/CANCELLED/TIMEOUT/MANUAL_REQUIRED）")
    private String status;

    @TableField("mail_no")
    @Schema(description = "关联邮件号码")
    private String mailNo;

    @TableField("transaction_id")
    @Schema(description = "邮政交易流水号")
    private String transactionId;

    @TableField("payment_flow_no")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    @TableField("qr_code_url")
    @Schema(description = "收款二维码链接")
    private String qrCodeUrl;

    @TableField("pay_query_no")
    @Schema(description = "支付查询流水号")
    private String payQueryNo;

    @TableField("remark")
    @Schema(description = "备注/失败原因")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
