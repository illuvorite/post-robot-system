package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 告警类型枚举
 * <p>
 * 定义系统中可能产生的各类异常告警类型，
 * 覆盖库存管理、任务执行、支付交易、网络通信和系统运行等核心领域。
 * </p>
 */
@Getter
public enum AlertTypeEnum {

    /** 低库存告警：商品库存低于设定阈值时触发 */
    LOW_STOCK("LOW_STOCK", "低库存"),
    /** 账实不一致告警：系统库存与实际盘点数量不匹配时触发 */
    STOCK_DISCREPANCY("STOCK_DISCREPANCY", "账实不一致"),
    /** 样品缺失告警：陈列样品被取走或丢失时触发 */
    SAMPLE_MISSING("SAMPLE_MISSING", "样品缺失"),
    /** 任务失败告警：机器人执行任务失败时触发 */
    TASK_FAILURE("TASK_FAILURE", "任务失败"),
    /** 支付超时告警：用户支付操作超过限定时间时触发 */
    PAYMENT_TIMEOUT("PAYMENT_TIMEOUT", "支付超时"),
    /** 网络中断告警：系统与外部服务网络连接断开时触发 */
    NETWORK_DOWN("NETWORK_DOWN", "网络中断"),
    /** 系统异常告警：系统内部发生未预期错误时触发 */
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常");

    private final String value;
    private final String text;

    AlertTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static AlertTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (AlertTypeEnum type : AlertTypeEnum.values()) {
            if (type.value.equals(value)) return type;
        }
        return null;
    }
}
