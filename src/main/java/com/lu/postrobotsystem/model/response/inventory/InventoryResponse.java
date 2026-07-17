package com.lu.postrobotsystem.model.response.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存信息响应 DTO。
 * <p>
 * 用于封装库存相关的响应数据，包含库存基本信息、关联商品名称、实时库存与锁定库存、
 * 可用库存计算、低库存告警阈值、样品状态以及视觉巡检时间等。
 * </p>
 */
@Data
@Schema(description = "库存信息")
public class InventoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "库存ID")
    private Long id;                            // 库存记录的唯一标识 ID

    @Schema(description = "关联商品ID")
    private Long productId;                     // 关联的商品ID

    @Schema(description = "商品名称")
    private String productName;                 // 关联商品的名称（冗余字段，便于展示）

    @Schema(description = "实时库存")
    private Integer realStock;                  // 当前实物库存数量（账面库存）

    @Schema(description = "锁定库存")
    private Integer lockedStock;                // 已锁定（预占）的库存数量

    @Schema(description = "可用库存（实时库存 - 锁定库存）")
    private Integer availableStock;             // 可用库存 = 实时库存 - 锁定库存

    @Schema(description = "低库存告警阈值")
    private Integer lowStockThreshold;          // 低库存告警阈值，当可用库存低于此值时触发告警

    @Schema(description = "样品状态（NORMAL-正常 MISSING-缺失 DISPLACED-错位）")
    private String sampleStatus;                // 样品状态：NORMAL-正常，MISSING-缺失，DISPLACED-错位

    @Schema(description = "账实不一致标记（0-一致 1-异常）")
    private Boolean mismatchFlag;               // 账实不一致标记：0-账实一致，1-存在异常

    @Schema(description = "最近视觉巡检时间")
    private LocalDateTime visionInspectTime;    // 最近一次视觉巡检的时间

    @Schema(description = "创建时间")
    private LocalDateTime createTime;           // 库存记录的创建时间

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;           // 库存记录的最近更新时间
}
