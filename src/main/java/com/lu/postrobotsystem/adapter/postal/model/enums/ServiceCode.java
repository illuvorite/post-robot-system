package com.lu.postrobotsystem.adapter.postal.model.enums;

import lombok.Getter;

/**
 * 邮政接口服务代码枚举
 * <p>
 * 对应 YYRoot SessionHeader 中的 ServiceCode 字段，
 * 每个枚举值代表一个具体的邮政业务接口。
 * </p>
 */
@Getter
public enum ServiceCode {

    /** F1：邮件资费查询 */
    POSTAGE_QUERY("F1", "邮件资费查询"),

    /** F2：邮件号码生成 */
    MAIL_NUMBER_GEN("F2", "邮件号码生成"),

    /** F3：业务办理-收寄订单提交 */
    ORDER_SUBMIT("F3", "业务办理-收寄订单提交"),

    /** F4：生成订单收款二维码 */
    QR_CODE_GEN("F4", "生成订单收款二维码"),

    /** F5：支付状态查询 */
    PAYMENT_STATUS_QUERY("F5", "支付状态查询");

    /** 接口服务代码（F1~F5） */
    private final String code;

    /** 接口中文描述 */
    private final String description;

    ServiceCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码查找枚举
     *
     * @param code 服务代码字符串
     * @return 匹配的枚举，未找到返回 null
     */
    public static ServiceCode getByCode(String code) {
        for (ServiceCode value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
