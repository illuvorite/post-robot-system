package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体类
 * <p>
 * 对应数据库 order_item 表，记录订单中包含的每件商品的购买信息，
 * 包括商品名称、单价、数量和小计金额。商品信息为下单时的快照数据，
 * 确保历史订单不受后续商品信息变更的影响。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("order_item")
@Schema(description = "订单明细")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "明细ID（Snowflake）")
    private Long id;

    /** 关联的订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 关联的商品ID */
    @TableField("product_id")
    @Schema(description = "关联商品ID")
    private Long productId;

    /** 商品名称（下单时的快照值，不受后续商品改名影响） */
    @TableField("product_name")
    @Schema(description = "商品名称（下单快照）")
    private String productName;

    /** 商品单价（下单时的快照值，不受后续调价影响） */
    @TableField("product_price")
    @Schema(description = "商品单价（下单快照）")
    private BigDecimal productPrice;

    /** 购买数量 */
    @TableField("quantity")
    @Schema(description = "购买数量")
    private Integer quantity;

    /** 小计金额 = 单价 * 数量 */
    @TableField("subtotal")
    @Schema(description = "小计金额")
    private BigDecimal subtotal;

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
