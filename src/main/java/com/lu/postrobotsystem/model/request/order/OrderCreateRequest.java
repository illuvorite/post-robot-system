package com.lu.postrobotsystem.model.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建请求
 * <p>
 * 封装用户下单所需的所有参数信息，包括邮资费用和商品明细列表。
 * 订单总金额由服务端根据商品单价和数量计算，客户端仅提供商品选择信息和邮费。
 * </p>
 */
@Data
@Schema(description = "订单创建请求")
public class OrderCreateRequest {

    /** 邮资费用 */
    @Schema(description = "邮资费用", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private BigDecimal postage;

    /** 商品明细列表，至少包含一个商品 */
    @NotEmpty(message = "商品明细不能为空")
    @Valid
    @Schema(description = "商品明细列表")
    private List<OrderItemRequest> items;

    /**
     * 订单商品明细请求体
     * <p>描述单个商品的下单数量，商品 ID 和数量均为必填。</p>
     */
    @Data
    @Schema(description = "订单商品明细")
    public static class OrderItemRequest {

        /** 商品ID */
        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        /** 购买数量 */
        @NotNull(message = "购买数量不能为空")
        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;
    }
}
