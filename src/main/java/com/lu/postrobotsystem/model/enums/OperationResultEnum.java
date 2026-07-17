package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum OperationResultEnum {
    SUCCESS("SUCCESS", "成功"),
    FAIL("FAIL", "失败");

    private final String value;
    private final String text;

    OperationResultEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static OperationResultEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OperationResultEnum result : OperationResultEnum.values()) {
            if (result.value.equals(value)) return result;
        }
        return null;
    }
}
