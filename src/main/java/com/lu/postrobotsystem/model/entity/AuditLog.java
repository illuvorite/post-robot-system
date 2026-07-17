package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * <p>
 * 对应数据库 audit_log 表，记录用户在系统中的关键操作轨迹，
 * 支持操作类型、操作对象、操作结果的全量追溯，用于安全审计与合规要求。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("audit_log")
@Schema(description = "审计日志")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计日志ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "审计日志ID（Snowflake）")
    private Long id;

    /** 操作人用户名 */
    @TableField("operator")
    @Schema(description = "操作人用户名")
    private String operator;

    /** 操作类型，取值参见 OperationTypeEnum（LOGIN/LOGOUT/ORDER_CREATE/ORDER_PAY/...） */
    @TableField("operation_type")
    @Schema(description = "操作类型（LOGIN/LOGOUT/ORDER_CREATE/ORDER_PAY/...）")
    private String operationType;

    /** 操作对象类型（ORDER-订单 / PRODUCT-商品 / INVENTORY-库存 / TASK-任务 / USER-用户 / ALERT-告警 / SYSTEM-系统） */
    @TableField("target_type")
    @Schema(description = "操作对象类型（ORDER/PRODUCT/INVENTORY/TASK/USER/ALERT/SYSTEM）")
    private String targetType;

    /** 操作对象的业务主键ID */
    @TableField("target_id")
    @Schema(description = "操作对象ID")
    private String targetId;

    /** 操作结果（SUCCESS-成功 / FAIL-失败），参见 OperationResultEnum */
    @TableField("result")
    @Schema(description = "操作结果（SUCCESS/FAIL）")
    private String result;

    /** 操作详情，记录操作前后的关键数据变化 */
    @TableField("detail")
    @Schema(description = "操作详情")
    private String detail;

    /** 关联的流水号或追踪ID，用于串联多个相关操作 */
    @TableField("trace_id")
    @Schema(description = "关联流水号/追踪ID")
    private String traceId;

    /** 发起操作的客户端IP地址 */
    @TableField("ip_address")
    @Schema(description = "请求IP")
    private String ipAddress;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
