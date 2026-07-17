package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 * <p>
 * 定义机器人任务从创建到完成的完整生命周期状态，
 * 涵盖创建、排队、执行、暂停、成功、失败、取消和人工介入等阶段，
 * 支持任务调度系统对任务进行状态管理和流程控制。
 * </p>
 */
@Getter
public enum TaskStatusEnum {

    /** 已创建：任务记录已生成，等待调度 */
    CREATED("CREATED", "已创建"),
    /** 已排队：任务已进入调度队列，等待分配执行 */
    QUEUED("QUEUED", "已排队"),
    /** 执行中：任务正在被机器人执行 */
    RUNNING("RUNNING", "执行中"),
    /** 已暂停：任务执行被暂停，可恢复 */
    PAUSED("PAUSED", "已暂停"),
    /** 已完成：任务执行成功，正常结束 */
    SUCCEEDED("SUCCEEDED", "已完成"),
    /** 失败：任务执行失败，已耗尽重试次数 */
    FAILED("FAILED", "失败"),
    /** 已取消：任务被手动取消 */
    CANCELLED("CANCELLED", "已取消"),
    /** 需人工处理：任务异常无法自动处理，需运营人员介入 */
    MANUAL_REQUIRED("MANUAL_REQUIRED", "需人工处理");

    private final String value;
    private final String text;

    TaskStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static TaskStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (TaskStatusEnum status : TaskStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
