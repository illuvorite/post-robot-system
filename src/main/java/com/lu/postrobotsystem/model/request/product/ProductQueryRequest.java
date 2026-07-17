package com.lu.postrobotsystem.model.request.product;

import com.lu.postrobotsystem.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品查询请求 DTO。
 * <p>
 * 用于接收前端传递的商品查询条件，支持按商品 ID、名称（模糊）、标签、售价区间、
 * 机器人抓取支持、陈列点位以及上下架状态等多维度筛选。
 * 继承 {@link PageRequest}，支持分页查询。
 * </p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品查询")
public class ProductQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long id;                            // 商品ID，精确匹配

    @Schema(description = "商品名称（模糊）")
    private String name;                        // 商品名称，支持模糊查询

    @Schema(description = "标签（逗号分隔）")
    private String tags;                        // 商品标签，多个标签以英文逗号分隔

    @Schema(description = "售价下限")
    private BigDecimal priceMin;                // 售价范围下限（最小值），用于价格区间筛选

    @Schema(description = "售价上限")
    private BigDecimal priceMax;                // 售价范围上限（最大值），用于价格区间筛选

    @Schema(description = "是否支持机器人抓取展示")
    private Boolean robotGraspable;             // 是否支持机器人抓取展示

    @Schema(description = "陈列点位编号")
    private String displayPoint;                // 陈列点位编号，精确匹配

    @Schema(description = "状态（0-下架 1-上架）")
    private Integer status;                     // 商品上下架状态：0-下架，1-上架
}
