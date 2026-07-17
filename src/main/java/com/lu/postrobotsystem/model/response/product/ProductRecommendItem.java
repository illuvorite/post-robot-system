package com.lu.postrobotsystem.model.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 商品推荐条目响应 DTO。
 * <p>
 * 继承商品基本信息的同时附加推荐理由和标签匹配度评分，用于智能推荐引擎
 * 向前端返回推荐结果。每个条目代表一个被推荐的商品以及对应的推荐依据。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商品推荐条目")
public class ProductRecommendItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long id;                            // 商品唯一标识 ID

    @Schema(description = "商品名称")
    private String name;                        // 商品名称

    @Schema(description = "商品描述")
    private String description;                 // 商品详细描述

    @Schema(description = "标签（逗号分隔）")
    private String tags;                        // 商品标签，多个标签以英文逗号分隔

    @Schema(description = "售价")
    private java.math.BigDecimal price;         // 商品实际售价

    @Schema(description = "原价")
    private java.math.BigDecimal originalPrice; // 商品原价（划线价）

    @Schema(description = "商品图片URL")
    private String imageUrl;                    // 商品展示图片的 URL 地址

    @Schema(description = "是否支持机器人抓取展示")
    private Boolean robotGraspable;             // 是否支持机器人抓取展示

    @Schema(description = "陈列点位编号")
    private String displayPoint;                // 商品在货架上的陈列点位编号

    @Schema(description = "推荐理由")
    private String recommendReason;             // 系统推荐该商品的理由说明

    @Schema(description = "标签匹配度（0-100）")
    private Integer matchScore;                 // 与用户偏好标签的匹配度评分，取值范围 0-100
}
