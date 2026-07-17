package com.lu.postrobotsystem.model.request.inventory;

import com.lu.postrobotsystem.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存查询请求 DTO。
 * <p>
 * 用于接收前端传递的库存查询条件，支持按商品 ID、样品状态以及账实不一致标记进行筛选。
 * 继承 {@link PageRequest}，支持分页查询。
 * </p>
 */
@Data
@Schema(description = "库存查询")
public class InventoryQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品名称（模糊查询）")
    private String productName;

    @Schema(description = "关联商品ID")
    private Long productId;                     // 关联商品ID，精确匹配

    @Schema(description = "样品状态（NORMAL-正常 MISSING-缺失 DISPLACED-错位）")
    private String sampleStatus;                // 样品状态：NORMAL-正常，MISSING-缺失，DISPLACED-错位

    @Schema(description = "账实不一致标记（0-一致 1-异常）")
    private Boolean mismatchFlag;               // 账实不一致标记：0-账实一致，1-存在异常
}
