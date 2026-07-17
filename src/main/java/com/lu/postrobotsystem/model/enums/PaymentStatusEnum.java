package com.lu.postrobotsystem.model.enums;

import lombok.Getter;

/**
 * 支付状态枚举
 * <p>
 * 定义支付交易的完整生命周期状态，
 * 覆盖从发起支付到成功、失败以及退款等全流程状态管理。
 * </p>
 */
@Getter
public enum PaymentStatusEnum {

    /** 支付中：支付请求已发出，等待支付平台结果返回 */
    PAYING("PAYING", "支付中"),
    /** 支付成功：支付交易已成功完成 */
    SUCCESS("SUCCESS", "支付成功"),
    /** 支付失败：支付交易处理失败 */
    FAILED("FAILED", "支付失败"),
    /** 已退款：全额退款完成 */
    REFUNDED("REFUNDED", "已退款"),
    /** 部分退款：部分金额已退还 */
    PARTIAL_REFUND("PARTIAL_REFUND", "部分退款");

    private final String value;
    private final String text;

    PaymentStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 值获取对应的枚举常量
     *
     * @param value 枚举值字符串
     * @return 匹配的枚举常量，未匹配时返回 null
     */
    public static PaymentStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (PaymentStatusEnum status : PaymentStatusEnum.values()) {
            if (status.value.equals(value)) return status;
        }
        return null;
    }
}
