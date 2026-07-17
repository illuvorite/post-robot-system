package com.lu.postrobotsystem.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lu.postrobotsystem.model.enums.AlertLevelEnum;
import com.lu.postrobotsystem.model.enums.AlertStatusEnum;
import com.lu.postrobotsystem.model.enums.AlertTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警实体类
 * <p>
 * 对应数据库 alert 表，用于记录系统中产生的各类异常告警信息。
 * 告警来源包括库存异常、任务失败、支付超时、网络中断、系统错误等场景，
 * 支持分级（INFO/WARNING/CRITICAL）和全生命周期状态管理。
 * </p>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("alert")
@Schema(description = "告警")
public class Alert implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 告警ID，使用 Snowflake 算法生成 */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    @Schema(description = "告警ID（Snowflake）")
    private Long id;

    /** 告警类型，取值参见 AlertTypeEnum（LOW_STOCK/STOCK_DISCREPANCY/SAMPLE_MISSING/TASK_FAILURE/PAYMENT_TIMEOUT/NETWORK_DOWN/SYSTEM_ERROR） */
    @TableField("alert_type")
    @Schema(description = "告警类型（LOW_STOCK/STOCK_DISCREPANCY/SAMPLE_MISSING/TASK_FAILURE/PAYMENT_TIMEOUT/NETWORK_DOWN/SYSTEM_ERROR）")
    private AlertTypeEnum alertType;

    /** 告警级别，取值参见 AlertLevelEnum（INFO-提示 / WARNING-警告 / CRITICAL-严重） */
    @TableField("alert_level")
    @Schema(description = "告警级别（INFO/WARNING/CRITICAL）")
    private AlertLevelEnum alertLevel;

    /** 告警来源模块或组件名称 */
    @TableField("source")
    @Schema(description = "告警来源")
    private String source;

    /** 告警来源对应的业务记录ID */
    @TableField("source_id")
    @Schema(description = "来源ID")
    private String sourceId;

    /** 告警内容描述，详细说明异常现象 */
    @TableField("message")
    @Schema(description = "告警描述")
    private String message;

    /** 处理状态，取值参见 AlertStatusEnum（UNRESOLVED-未处理 / PROCESSING-处理中 / RESOLVED-已解决） */
    @TableField("status")
    @Schema(description = "处理状态（UNRESOLVED/PROCESSING/RESOLVED）")
    private AlertStatusEnum status;

    /** 告警处理人用户名 */
    @TableField("handler")
    @Schema(description = "处理人")
    private String handler;

    /** 告警处理时间 */
    @TableField("handle_time")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    /** 处理备注说明 */
    @TableField("handle_note")
    @Schema(description = "处理备注")
    private String handleNote;

    /** 告警解决时间 */
    @TableField("resolved_time")
    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

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
