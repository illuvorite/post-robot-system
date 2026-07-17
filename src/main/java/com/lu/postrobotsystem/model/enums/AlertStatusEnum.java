package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 告警处理状态枚举
 * <p>
 * 定义告警从产生到关闭的全生命周期状态，
 * 用于追踪告警的处理进度和当前处理阶段。
 * </p>
 */
@Getter
public enum AlertStatusEnum {

    /** 未处理，告警已产生但尚未有人处理 */
    UNRESOLVED("UNRESOLVED", "未处理"),
    /** 处理中，告警正在被调查和处理 */
    PROCESSING("PROCESSING", "处理中"),
    /** 已解决，告警对应的问题已修复或确认无影响 */
    RESOLVED("RESOLVED", "已解决");

    private final String value;
    private final String text;

    AlertStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static AlertStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertStatusEnum status : AlertStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
