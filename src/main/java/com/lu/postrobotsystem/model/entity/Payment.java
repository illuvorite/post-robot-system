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
@TableName("payment")
@Schema(description = "支付")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "支付ID（Snowflake）")
    private Long id;

    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    @TableField("payment_flow_no")
    @Schema(description = "支付流水号")
    private String paymentFlowNo;

    @TableField("pay_query_no")
    @Schema(description = "查询流水号")
    private String payQueryNo;

    @TableField("qr_code_url")
    @Schema(description = "二维码链接")
    private String qrCodeUrl;

    @TableField("platform_flow_no")
    @Schema(description = "平台流水号（邮政返回）")
    private String platformFlowNo;

    @TableField("status")
    @Schema(description = "支付状态（PAYING/SUCCESS/FAILED/REFUNDED/PARTIAL_REFUND）")
    private String status;

    @TableField("amount")
    @Schema(description = "支付金额")
    private BigDecimal amount;

    @TableField("paid_time")
    @Schema(description = "支付完成时间")
    private LocalDateTime paidTime;

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
