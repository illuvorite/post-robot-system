package com.lu.postrobotsystem.model.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单查询请求
 * <p>
 * 支持按订单号、订单状态、用户ID等条件筛选订单列表。
 * 分页参数在 Controller 层通过 @RequestParam 注入。
 * </p>
 */
@Data
@Schema(description = "订单查询请求")
public class OrderQueryRequest {

    /** 订单号（精确匹配） */
    @Schema(description = "订单号")
    private String orderNo;

    /** 订单状态（PENDING_PAY/PAYING/PAID/FAILED/CANCELLED/TIMEOUT/MANUAL_REQUIRED） */
    @Schema(description = "订单状态")
    private String status;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;
}
