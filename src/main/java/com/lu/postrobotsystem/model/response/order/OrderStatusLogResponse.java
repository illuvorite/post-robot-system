package com.lu.postrobotsystem.model.response.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态变更日志响应
 * <p>
 * 记录订单的一条状态变更历史，包括变更前后的状态、操作人和时间。
 * 支持按订单号查询完整的订单生命周期轨迹。
 * </p>
 */
@Data
@Schema(description = "订单状态变更日志响应")
public class OrderStatusLogResponse {

    /** 变更前状态，null 表示首次创建 */
    @Schema(description = "变更前状态")
    private String fromStatus;

    /** 变更后状态 */
    @Schema(description = "变更后状态")
    private String toStatus;

    /** 操作人（系统触时为 SYSTEM） */
    @Schema(description = "操作人")
    private String operator;

    /** 操作类型 */
    @Schema(description = "操作类型")
    private String operationType;

    /** 变更备注/原因 */
    @Schema(description = "变更备注/原因")
    private String remark;

    /** 变更时间 */
    @Schema(description = "变更时间")
    private LocalDateTime createTime;
}
