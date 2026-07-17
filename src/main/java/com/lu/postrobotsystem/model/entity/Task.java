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
@TableName("task")
@Schema(description = "任务")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "任务ID（Snowflake）")
    private Long id;

    @TableField("task_no")
    @Schema(description = "任务编号")
    private String taskNo;

    @TableField("task_type")
    @Schema(description = "任务类型（NAVIGATION/GRASP/DISPLAY/EXPLAIN/SETTLEMENT/INVENTORY_CHECK/PATROL/OTHER）")
    private String taskType;

    @TableField("status")
    @Schema(description = "任务状态（CREATED/QUEUED/RUNNING/PAUSED/SUCCEEDED/FAILED/CANCELLED/MANUAL_REQUIRED）")
    private String status;

    @TableField("priority")
    @Schema(description = "优先级（1-10，1最高）")
    private Integer priority;

    @TableField("dependency_task_no")
    @Schema(description = "依赖任务编号")
    private String dependencyTaskNo;

    @TableField("timeout_seconds")
    @Schema(description = "超时阈值（秒）")
    private Integer timeoutSeconds;

    @TableField("retry_count")
    @Schema(description = "当前重试次数")
    private Integer retryCount;

    @TableField("max_retry")
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @TableField("input_params")
    @Schema(description = "任务输入参数（JSON）")
    private String inputParams;

    @TableField("output_result")
    @Schema(description = "任务输出结果（JSON）")
    private String outputResult;

    @TableField("fail_reason")
    @Schema(description = "失败原因")
    private String failReason;

    @TableField("started_time")
    @Schema(description = "开始执行时间")
    private LocalDateTime startedTime;

    @TableField("completed_time")
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @TableField("duration_ms")
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

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
