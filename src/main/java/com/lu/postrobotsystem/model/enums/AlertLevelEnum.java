package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 告警级别枚举
 * <p>
 * 定义告警的严重程度分级，用于区分不同紧急程度的异常事件。
 * 系统根据告警级别决定通知方式、响应优先级和处理流程。
 * </p>
 */
@Getter
public enum AlertLevelEnum {

    /** 提示级别，信息性告警，无需立即处理 */
    INFO("INFO", "提示"),
    /** 警告级别，需关注并尽快处理 */
    WARNING("WARNING", "警告"),
    /** 严重级别，需立即处理，可能影响系统正常运行 */
    CRITICAL("CRITICAL", "严重");

    private final String value;
    private final String text;

    AlertLevelEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static AlertLevelEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertLevelEnum level : AlertLevelEnum.values()) {
            if (level.value.equals(value)) return level;
        }
        return null;
    }
}
