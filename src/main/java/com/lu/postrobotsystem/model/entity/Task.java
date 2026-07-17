package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.TaskStatusEnum;
import com.lu.postrobotsystem.model.enums.TaskTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务实体类
 * <p>
 * 对应数据库 task 表，管理机器人执行的各种任务的完整生命周期。
 * 任务类型涵盖导航、抓取、展示、讲解、结算、库存巡检等场景，
 * 支持优先级调度、依赖关系、超时控制、重试机制和状态流转管理。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("task")
@Schema(description = "任务")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "任务ID（Snowflake）")
    private Long id;

    /** 任务编号，业务展示用唯一标识 */
    @TableField("task_no")
    @Schema(description = "任务编号")
    private String taskNo;

    /** 任务类型（NAVIGATION-导航 / GRASP-抓取 / DISPLAY-展示 / EXPLAIN-讲解 / SETTLEMENT-结算 / INVENTORY_CHECK-库存巡检 / PATROL-巡检 / OTHER-其他），参见 TaskTypeEnum */
    @TableField("task_type")
    @Schema(description = "任务类型（NAVIGATION/GRASP/DISPLAY/EXPLAIN/SETTLEMENT/INVENTORY_CHECK/PATROL/OTHER）")
    private TaskTypeEnum taskType;

    /** 任务状态（CREATED-已创建 / QUEUED-已排队 / RUNNING-执行中 / PAUSED-已暂停 / SUCCEEDED-已完成 / FAILED-失败 / CANCELLED-已取消 / MANUAL_REQUIRED-需人工处理），参见 TaskStatusEnum */
    @TableField("status")
    @Schema(description = "任务状态（CREATED/QUEUED/RUNNING/PAUSED/SUCCEEDED/FAILED/CANCELLED/MANUAL_REQUIRED）")
    private TaskStatusEnum status;

    /** 优先级（1-10，1为最高优先级），用于任务调度排序 */
    @TableField("priority")
    @Schema(description = "优先级（1-10，1最高）")
    private Integer priority;

    /** 依赖的任务编号，当前任务需在该任务完成后才能执行 */
    @TableField("dependency_task_no")
    @Schema(description = "依赖任务编号")
    private String dependencyTaskNo;

    /** 超时阈值（秒），任务执行超过此时间则判定为超时 */
    @TableField("timeout_seconds")
    @Schema(description = "超时阈值（秒）")
    private Integer timeoutSeconds;

    /** 当前已重试次数 */
    @TableField("retry_count")
    @Schema(description = "当前重试次数")
    private Integer retryCount;

    /** 最大允许重试次数，超过后任务标记为失败 */
    @TableField("max_retry")
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    /** 任务输入参数（JSON格式），传递给机器人执行时使用 */
    @TableField("input_params")
    @Schema(description = "任务输入参数（JSON）")
    private String inputParams;

    /** 任务输出结果（JSON格式），机器人执行完成后返回 */
    @TableField("output_result")
    @Schema(description = "任务输出结果（JSON）")
    private String outputResult;

    /** 失败原因，任务执行失败时填写详细错误信息 */
    @TableField("fail_reason")
    @Schema(description = "失败原因")
    private String failReason;

    /** 任务开始执行时间 */
    @TableField("started_time")
    @Schema(description = "开始执行时间")
    private LocalDateTime startedTime;

    /** 任务完成时间 */
    @TableField("completed_time")
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    /** 任务执行耗时（毫秒） */
    @TableField("duration_ms")
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

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
