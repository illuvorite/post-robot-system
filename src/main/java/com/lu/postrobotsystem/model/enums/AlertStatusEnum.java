package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum AlertStatusEnum {
    UNRESOLVED("UNRESOLVED", "未处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决");

    private final String value;
    private final String text;

    AlertStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AlertStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertStatusEnum status : AlertStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
