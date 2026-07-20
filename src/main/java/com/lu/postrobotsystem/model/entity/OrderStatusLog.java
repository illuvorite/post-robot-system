package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态变更日志实体类
 * <p>
 * 对应数据库 order_status_log 表，记录订单从创建到完成的每一次状态变更历史，
 * 支持按订单号或订单ID查询完整的状态流转轨迹，用于全链路追溯和问题排查。
 * 每条记录包含变更前状态、变更后状态、操作人和操作类型，构成完整的订单状态机审计链。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("order_status_log")
@Schema(description = "订单状态变更日志")
public class OrderStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "日志ID（Snowflake）")
    private Long id;

    /** 关联的订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 订单号（冗余字段，便于独立查询免关联） */
    @TableField("order_no")
    @Schema(description = "订单号")
    private String orderNo;

    /** 变更前状态，null 表示首次创建 */
    @TableField("from_status")
    @Schema(description = "变更前状态")
    private String fromStatus;

    /** 变更后状态 */
    @TableField("to_status")
    @Schema(description = "变更后状态")
    private String toStatus;

    /** 操作人（系统自动触发时为 SYSTEM） */
    @TableField("operator")
    @Schema(description = "操作人（系统触发时为SYSTEM）")
    private String operator;

    /** 操作类型（ORDER_CREATE/ORDER_PAY/PAYMENT_CALLBACK/ORDER_CANCEL/ORDER_TIMEOUT/MANUAL_PROCESS） */
    @TableField("operation_type")
    @Schema(description = "操作类型")
    private String operationType;

    /** 变更备注或原因说明 */
    @TableField("remark")
    @Schema(description = "变更备注/原因")
    private String remark;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
