package com.lu.postrobotsystem.model.request.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务创建请求
 */
@Data
@Schema(description = "任务创建请求")
public class TaskCreateRequest {

    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型（NAVIGATION/GRASP/DISPLAY/EXPLAIN/SETTLEMENT/INVENTORY_CHECK/PATROL/OTHER）")
    private String taskType;

    @Min(value = 1, message = "优先级范围为 1-10")
    @Max(value = 10, message = "优先级范围为 1-10")
    @Schema(description = "优先级（1-10，1最高）")
    private Integer priority = 5;

    @Schema(description = "依赖任务编号")
    private String dependencyTaskNo;

    @Schema(description = "超时阈值（秒）")
    private Integer timeoutSeconds = 300;

    @Schema(description = "最大重试次数")
    private Integer maxRetry = 3;

    @Schema(description = "任务输入参数（JSON）")
    private String inputParams;
}
