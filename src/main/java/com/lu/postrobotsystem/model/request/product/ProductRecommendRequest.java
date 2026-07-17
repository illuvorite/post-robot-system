package com.lu.postrobotsystem.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品推荐请求 DTO。
 * <p>
 * 用于接收前端推荐引擎的请求参数，包括用户的意图偏好标签、预算范围、
 * 是否仅推荐机器人可抓取的商品以及期望的推荐数量。
 * </p>
 */
@Data
@Schema(description = "商品推荐请求")
public class ProductRecommendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户意图/偏好标签列表（如：礼品、纪念、收藏）")
    private List<String> intentTags;            // 用户意图或偏好标签，例如：礼品、纪念、收藏等

    @Schema(description = "预算上限")
    private BigDecimal budgetMax;               // 用户预算上限（最高可接受价格）

    @Schema(description = "预算下限")
    private BigDecimal budgetMin;               // 用户预算下限（最低可接受价格）

    @Schema(description = "是否仅推荐机器人可抓取的商品")
    private Boolean onlyGraspable;              // 是否仅推荐机器人可抓取的商品

    @Schema(description = "推荐数量（默认 10）")
    private Integer limit = 10;                 // 推荐结果数量上限，默认为 10 条
}
