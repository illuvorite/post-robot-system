package com.lu.postrobotsystem.model.request.alert;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 告警更新请求
 */
@Data
@Schema(description = "告警更新请求")
public class AlertUpdateRequest {

    @NotNull(message = "告警ID不能为空")
    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "处理人")
    private String handler;

    @Schema(description = "处理备注")
    private String handleNote;

    @Schema(description = "告警级别（INFO/WARNING/CRITICAL）")
    private String alertLevel;
}
