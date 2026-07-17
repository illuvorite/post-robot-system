package com.lu.postrobotsystem.model.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息响应 DTO。
 * <p>
 * 用于封装商品相关的响应数据，包含商品基本信息、定价、展示信息、
 * 上下架状态以及时间戳等。作为服务层返回给控制层/前端的数据载体。
 * </p>
 */
@Data
@Schema(description = "商品信息")
public class ProductResponse implements Serializable {

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
    private BigDecimal price;                   // 商品实际售价

    @Schema(description = "原价")
    private BigDecimal originalPrice;           // 商品原价（划线价）

    @Schema(description = "商品图片URL")
    private String imageUrl;                    // 商品展示图片的 URL 地址

    @Schema(description = "是否支持机器人抓取展示（0-否 1-是）")
    private Boolean robotGraspable;             // 是否支持机器人抓取展示：0-否，1-是

    @Schema(description = "陈列点位编号")
    private String displayPoint;                // 商品在货架上的陈列点位编号

    @Schema(description = "状态（0-下架 1-上架）")
    private Integer status;                     // 商品上下架状态：0-下架，1-上架

    @Schema(description = "创建时间")
    private LocalDateTime createTime;           // 商品的创建时间

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;           // 商品的最近更新时间
}
