package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 操作结果枚举
 * <p>
 * 定义审计日志中记录的操作执行结果状态，
 * 用于标记用户操作是成功完成还是执行失败。
 * </p>
 */
@Getter
public enum OperationResultEnum {

    /** 操作成功执行 */
    SUCCESS("SUCCESS", "成功"),
    /** 操作执行失败 */
    FAIL("FAIL", "失败");

    private final String value;
    private final String text;

    OperationResultEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static OperationResultEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (OperationResultEnum result : OperationResultEnum.values()) {
            if (result.value.equals(value)) return result;
        }
        return null;
    }
}
