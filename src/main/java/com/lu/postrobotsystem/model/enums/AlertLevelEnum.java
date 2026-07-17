package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum AlertLevelEnum {
    INFO("INFO", "提示"),
    WARNING("WARNING", "警告"),
    CRITICAL("CRITICAL", "严重");

    private final String value;
    private final String text;

    AlertLevelEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AlertLevelEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertLevelEnum level : AlertLevelEnum.values()) {
            if (level.value.equals(value)) return level;
        }
        return null;
    }
}
