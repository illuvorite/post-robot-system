package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    PENDING_PAY("PENDING_PAY", "待支付"),
    PAYING("PAYING", "支付中"),
    PAID("PAID", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CANCELLED("CANCELLED", "已取消"),
    TIMEOUT("TIMEOUT", "已超时"),
    MANUAL_REQUIRED("MANUAL_REQUIRED", "需人工处理");

    private final String value;
    private final String text;

    OrderStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static OrderStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
