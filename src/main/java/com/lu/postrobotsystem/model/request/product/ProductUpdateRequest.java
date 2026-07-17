package com.lu.postrobotsystem.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品编辑请求 DTO。
 * <p>
 * 用于接收前端修改商品信息时提交的表单数据。商品 ID 为必填项（用于定位待更新记录），
 * 其余字段均为可选，仅传入需要变更的字段。
 * 字段上已标注 Jakarta Validation 注解，在 Controller 层通过 @Valid 触发校验。
 * </p>
 */
@Data
@Schema(description = "商品编辑")
public class ProductUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long id;                            // 商品ID，必填，用于定位待更新的商品记录

    @Schema(description = "商品名称")
    private String name;                        // 商品名称，可选（仅传入需要修改的字段）

    @Schema(description = "商品描述")
    private String description;                 // 商品详细描述，可选

    @Schema(description = "标签（逗号分隔）")
    private String tags;                        // 商品标签，多个标签以英文逗号分隔

    @DecimalMin(value = "0.01", message = "售价必须大于0")
    @Schema(description = "售价")
    private BigDecimal price;                   // 商品售价，若传入则最小值为 0.01

    @Schema(description = "原价")
    private BigDecimal originalPrice;           // 商品原价（划线价），可选

    @Schema(description = "商品图片URL")
    private String imageUrl;                    // 商品展示图片的 URL 地址

    @Schema(description = "是否支持机器人抓取展示（0-否 1-是）")
    private Boolean robotGraspable;             // 是否支持机器人抓取展示：0-否，1-是

    @Schema(description = "陈列点位编号")
    private String displayPoint;                // 商品在货架上的陈列点位编号

    @Schema(description = "状态（0-下架 1-上架）")
    private Integer status;                     // 商品上下架状态：0-下架，1-上架
}
