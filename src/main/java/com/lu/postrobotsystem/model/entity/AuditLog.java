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
@TableName("audit_log")
@Schema(description = "审计日志")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "审计日志ID（Snowflake）")
    private Long id;

    @TableField("operator")
    @Schema(description = "操作人用户名")
    private String operator;

    @TableField("operation_type")
    @Schema(description = "操作类型（LOGIN/LOGOUT/ORDER_CREATE/ORDER_PAY/...）")
    private String operationType;

    @TableField("target_type")
    @Schema(description = "操作对象类型（ORDER/PRODUCT/INVENTORY/TASK/USER/ALERT/SYSTEM）")
    private String targetType;

    @TableField("target_id")
    @Schema(description = "操作对象ID")
    private String targetId;

    @TableField("result")
    @Schema(description = "操作结果（SUCCESS/FAIL）")
    private String result;

    @TableField("detail")
    @Schema(description = "操作详情")
    private String detail;

    @TableField("trace_id")
    @Schema(description = "关联流水号/追踪ID")
    private String traceId;

    @TableField("ip_address")
    @Schema(description = "请求IP")
    private String ipAddress;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
