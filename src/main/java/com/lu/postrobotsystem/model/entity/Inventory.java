package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("inventory")
@Schema(description = "库存")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "库存ID（Snowflake）")
    private Long id;

    @TableField("product_id")
    @Schema(description = "关联商品ID")
    private Long productId;

    @TableField("real_stock")
    @Schema(description = "实时库存")
    private Integer realStock;

    @TableField("locked_stock")
    @Schema(description = "锁定库存")
    private Integer lockedStock;

    @TableField("low_stock_threshold")
    @Schema(description = "低库存告警阈值")
    private Integer lowStockThreshold;

    @TableField("sample_status")
    @Schema(description = "样品状态（NORMAL-正常 MISSING-缺失 DISPLACED-错位）")
    private String sampleStatus;

    @TableField("mismatch_flag")
    @Schema(description = "账实不一致标记（0-一致 1-异常）")
    private Boolean mismatchFlag;

    @TableField("vision_inspect_time")
    @Schema(description = "最近视觉巡检时间")
    private LocalDateTime visionInspectTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
