package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.SampleStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存实体类
 * <p>
 * 对应数据库 inventory 表，管理商品的实时库存数量、锁定库存、低库存阈值等信息，
 * 同时记录样品状态与视觉巡检结果，支持库存异常告警与账实一致性校验。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("inventory")
@Schema(description = "库存")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "库存ID（Snowflake）")
    private Long id;

    /** 关联的商品ID */
    @TableField("product_id")
    @Schema(description = "关联商品ID")
    private Long productId;

    /** 实时库存数量，表示当前实际可用的库存量 */
    @TableField("real_stock")
    @Schema(description = "实时库存")
    private Integer realStock;

    /** 锁定库存数量，已被订单占用但尚未出库的数量 */
    @TableField("locked_stock")
    @Schema(description = "锁定库存")
    private Integer lockedStock;

    /** 低库存告警阈值，当实时库存低于此值时触发告警 */
    @TableField("low_stock_threshold")
    @Schema(description = "低库存告警阈值")
    private Integer lowStockThreshold;

    /** 样品状态（NORMAL-正常 / MISSING-缺失 / DISPLACED-错位），参见 SampleStatusEnum */
    @TableField("sample_status")
    @Schema(description = "样品状态（NORMAL-正常 MISSING-缺失 DISPLACED-错位）")
    private SampleStatusEnum sampleStatus;

    /** 账实不一致标记（false-一致 / true-异常），标识系统库存与实际盘点是否匹配 */
    @TableField("mismatch_flag")
    @Schema(description = "账实不一致标记（0-一致 1-异常）")
    private Boolean mismatchFlag;

    /** 最近一次视觉巡检时间，由机器人视觉系统自动记录 */
    @TableField("vision_inspect_time")
    @Schema(description = "最近视觉巡检时间")
    private LocalDateTime visionInspectTime;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 记录更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-正常 1-删除） */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "逻辑删除（0-正常 1-删除）")
    private Integer isDeleted;
}
