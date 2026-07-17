package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 样品状态枚举
 * <p>
 * 定义货架陈列样品的物理状态，
 * 由机器人视觉巡检系统自动识别和更新，
 * 用于库存管理和样品完整性监控。
 * </p>
 */
@Getter
public enum SampleStatusEnum {

    /** 正常：样品在正确位置且状态完好 */
    NORMAL("NORMAL", "正常"),
    /** 缺失：样品被取走或丢失，未在正确位置 */
    MISSING("MISSING", "缺失"),
    /** 错位：样品被放置到错误的位置 */
    DISPLACED("DISPLACED", "错位");

    private final String value;
    private final String text;

    SampleStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static SampleStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (SampleStatusEnum status : SampleStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
