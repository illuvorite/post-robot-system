package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum PaymentStatusEnum {
    PAYING("PAYING", "支付中"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    REFUNDED("REFUNDED", "已退款"),
    PARTIAL_REFUND("PARTIAL_REFUND", "部分退款");

    private final String value;
    private final String text;

    PaymentStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static PaymentStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (PaymentStatusEnum status : PaymentStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
