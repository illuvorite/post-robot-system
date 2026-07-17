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
@TableName("order_item")
@Schema(description = "订单明细")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "明细ID（Snowflake）")
    private Long id;

    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    @TableField("product_id")
    @Schema(description = "关联商品ID")
    private Long productId;

    @TableField("product_name")
    @Schema(description = "商品名称（下单快照）")
    private String productName;

    @TableField("product_price")
    @Schema(description = "商品单价（下单快照）")
    private BigDecimal productPrice;

    @TableField("quantity")
    @Schema(description = "购买数量")
    private Integer quantity;

    @TableField("subtotal")
    @Schema(description = "小计金额")
    private BigDecimal subtotal;

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
