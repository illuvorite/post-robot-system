package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum SampleStatusEnum {
    NORMAL("NORMAL", "正常"),
    MISSING("MISSING", "缺失"),
    DISPLACED("DISPLACED", "错位");

    private final String value;
    private final String text;

    SampleStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static SampleStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (SampleStatusEnum status : SampleStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
