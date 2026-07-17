package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {
    CREATED("CREATED", "已创建"),
    QUEUED("QUEUED", "已排队"),
    RUNNING("RUNNING", "执行中"),
    PAUSED("PAUSED", "已暂停"),
    SUCCEEDED("SUCCEEDED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    MANUAL_REQUIRED("MANUAL_REQUIRED", "需人工处理");

    private final String value;
    private final String text;

    TaskStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static TaskStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (TaskStatusEnum status : TaskStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
