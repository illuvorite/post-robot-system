package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum AlertTypeEnum {
    LOW_STOCK("LOW_STOCK", "低库存"),
    STOCK_DISCREPANCY("STOCK_DISCREPANCY", "账实不一致"),
    SAMPLE_MISSING("SAMPLE_MISSING", "样品缺失"),
    TASK_FAILURE("TASK_FAILURE", "任务失败"),
    PAYMENT_TIMEOUT("PAYMENT_TIMEOUT", "支付超时"),
    NETWORK_DOWN("NETWORK_DOWN", "网络中断"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常");

    private final String value;
    private final String text;

    AlertTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AlertTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertTypeEnum type : AlertTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
