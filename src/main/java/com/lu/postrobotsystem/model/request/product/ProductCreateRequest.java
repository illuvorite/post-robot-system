package com.lu.postrobotsystem.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品新增请求 DTO。
 * <p>
 * 用于接收前端新增商品时提交的表单数据，包含商品基本信息、定价、展示信息等。
 * 字段上已标注 Jakarta Validation 注解，在 Controller 层通过 @Valid 触发校验。
 * </p>
 */
@Data
@Schema(description = "商品新增")
public class ProductCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称")
    private String name;                        // 商品名称，必填且不能为空白

    @Schema(description = "商品描述")
    private String description;                 // 商品详细描述，可选

    @Schema(description = "标签（逗号分隔）")
    private String tags;                        // 商品标签，多个标签以英文逗号分隔

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    @Schema(description = "售价")
    private BigDecimal price;                   // 商品售价（实际销售价格），必填，最小值为 0.01

    @Schema(description = "原价")
    private BigDecimal originalPrice;           // 商品原价（划线价），可选，用于展示折扣

    @Schema(description = "商品图片URL")
    private String imageUrl;                    // 商品展示图片的 URL 地址

    @Schema(description = "是否支持机器人抓取展示（0-否 1-是）")
    private Boolean robotGraspable;             // 是否支持机器人抓取展示：0-不支持，1-支持

    @Schema(description = "陈列点位编号")
    private String displayPoint;                // 商品在货架上的陈列点位编号
}
