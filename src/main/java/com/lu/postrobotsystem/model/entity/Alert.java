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
@TableName("alert")
@Schema(description = "告警")
public class Alert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "告警ID（Snowflake）")
    private Long id;

    @TableField("alert_type")
    @Schema(description = "告警类型（LOW_STOCK/STOCK_DISCREPANCY/SAMPLE_MISSING/TASK_FAILURE/PAYMENT_TIMEOUT/NETWORK_DOWN/SYSTEM_ERROR）")
    private String alertType;

    @TableField("alert_level")
    @Schema(description = "告警级别（INFO/WARNING/CRITICAL）")
    private String alertLevel;

    @TableField("source")
    @Schema(description = "告警来源")
    private String source;

    @TableField("source_id")
    @Schema(description = "来源ID")
    private String sourceId;

    @TableField("message")
    @Schema(description = "告警描述")
    private String message;

    @TableField("status")
    @Schema(description = "处理状态（UNRESOLVED/PROCESSING/RESOLVED）")
    private String status;

    @TableField("handler")
    @Schema(description = "处理人")
    private String handler;

    @TableField("handle_time")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @TableField("handle_note")
    @Schema(description = "处理备注")
    private String handleNote;

    @TableField("resolved_time")
    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

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
