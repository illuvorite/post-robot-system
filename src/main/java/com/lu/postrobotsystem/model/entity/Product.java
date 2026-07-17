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
 * 商品实体类
 * <p>
 * 对应数据库 product 表，管理机器人商店中的商品信息，
 * 包括商品名称、描述、价格、标签、展示图片等基础信息，
 * 以及机器人相关属性（是否可抓取、陈列点位编号）和上下架状态。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("product")
@Schema(description = "商品")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "商品ID（Snowflake）")
    private Long id;

    /** 商品名称 */
    @TableField("name")
    @Schema(description = "商品名称")
    private String name;

    /** 商品描述，详细介绍商品信息 */
    @TableField("description")
    @Schema(description = "商品描述")
    private String description;

    /** 标签（多个标签用逗号分隔），用于商品分类和检索 */
    @TableField("tags")
    @Schema(description = "标签（逗号分隔）")
    private String tags;

    /** 售价，用户实际支付金额 */
    @TableField("price")
    @Schema(description = "售价")
    private BigDecimal price;

    /** 原价，用于展示折扣信息 */
    @TableField("original_price")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    /** 商品展示图片的URL地址 */
    @TableField("image_url")
    @Schema(description = "商品图片URL")
    private String imageUrl;

    /** 是否支持机器人抓取展示（false-否 / true-是），标识该商品是否可由机器人物理抓取 */
    @TableField("robot_graspable")
    @Schema(description = "是否支持机器人抓取展示（0-否 1-是）")
    private Boolean robotGraspable;

    /** 陈列点位编号，标识商品在货架上的具体位置 */
    @TableField("display_point")
    @Schema(description = "陈列点位编号")
    private String displayPoint;

    /** 商品状态（0-下架 / 1-上架），参见 ProductStatusEnum */
    @TableField("status")
    @Schema(description = "状态（0-下架 1-上架）")
    private Integer status;

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
