package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 * <p>
 * 定义订单从创建到完成的完整生命周期状态，
 * 涵盖待支付、支付处理中、支付成功、失败、取消、超时以及需要人工介入等场景。
 * </p>
 */
@Getter
public enum OrderStatusEnum {

    /** 待支付：订单已创建，等待用户完成支付 */
    PENDING_PAY("PENDING_PAY", "待支付"),
    /** 支付中：用户已发起支付，等待支付平台回调确认 */
    PAYING("PAYING", "支付中"),
    /** 支付成功：支付已完成，订单生效 */
    PAID("PAID", "支付成功"),
    /** 支付失败：支付处理未成功 */
    FAILED("FAILED", "支付失败"),
    /** 已取消：用户主动取消订单 */
    CANCELLED("CANCELLED", "已取消"),
    /** 已超时：超过支付时限未完成支付，系统自动取消 */
    TIMEOUT("TIMEOUT", "已超时"),
    /** 需人工处理：订单异常，需要运营人员介入处理 */
    MANUAL_REQUIRED("MANUAL_REQUIRED", "需人工处理");

    private final String value;
    private final String text;

    OrderStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static OrderStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
