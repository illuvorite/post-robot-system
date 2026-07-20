package com.lu.postrobotsystem.model.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存手动调整请求 DTO。
 * <p>
 * 用于运营/管理员手动修正库存数据，例如盘点后调整实际库存、
 * 修正锁定库存、变更低库存阈值或样品状态。
 * 与入库/出库不同，调整是直接设定值而非增减。
 * </p>
 */
@Data
@Schema(description = "库存手动调整")
public class InventoryAdjustRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "调整后的实时库存（不传则不修改）")
    private Integer realStock;

    @Schema(description = "调整后的锁定库存（不传则不修改）")
    private Integer lockedStock;

    @Schema(description = "低库存告警阈值（不传则不修改）")
    private Integer lowStockThreshold;

    @Schema(description = "样品状态 NORMAL/MISSING/DISPLACED（不传则不修改）")
    private String sampleStatus;

    @Schema(description = "账实不一致标记（不传则不修改）")
    private Boolean mismatchFlag;
}
