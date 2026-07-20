package com.lu.postrobotsystem.model.request.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 任务更新请求
 */
@Data
@Schema(description = "任务更新请求")
public class TaskUpdateRequest {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务类型")
    private String taskType;

    @Min(value = 1, message = "优先级范围为 1-10")
    @Max(value = 10, message = "优先级范围为 1-10")
    @Schema(description = "优先级（1-10，1最高）")
    private Integer priority;

    @Schema(description = "超时阈值（秒）")
    private Integer timeoutSeconds;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "任务输入参数（JSON）")
    private String inputParams;
}
