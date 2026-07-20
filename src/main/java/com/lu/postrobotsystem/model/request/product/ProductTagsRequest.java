package com.lu.postrobotsystem.model.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品标签更新请求 DTO。
 * <p>
 * 专门用于更新商品标签的请求，与全量编辑分离。
 * </p>
 */
@Data
@Schema(description = "商品标签更新")
public class ProductTagsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long id;

    @NotBlank(message = "标签不能为空")
    @Schema(description = "标签（逗号分隔）")
    private String tags;
}
