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
@TableName("product")
@Schema(description = "商品")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "商品ID（Snowflake）")
    private Long id;

    @TableField("name")
    @Schema(description = "商品名称")
    private String name;

    @TableField("description")
    @Schema(description = "商品描述")
    private String description;

    @TableField("tags")
    @Schema(description = "标签（逗号分隔）")
    private String tags;

    @TableField("price")
    @Schema(description = "售价")
    private BigDecimal price;

    @TableField("original_price")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @TableField("image_url")
    @Schema(description = "商品图片URL")
    private String imageUrl;

    @TableField("robot_graspable")
    @Schema(description = "是否支持机器人抓取展示（0-否 1-是）")
    private Boolean robotGraspable;

    @TableField("display_point")
    @Schema(description = "陈列点位编号")
    private String displayPoint;

    @TableField("status")
    @Schema(description = "状态（0-下架 1-上架）")
    private Integer status;

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
