package com.lu.postrobotsystem.adapter.postal.model.enums;

import com.lu.postrobotsystem.common.IErrorCode;
import lombok.Getter;

/**
 * 邮政接口错误码枚举
 * <p>
 * 实现 {@link IErrorCode} 接口，将邮政侧返回的错误码映射为本地系统可理解的错误码。
 * 参见邮政接口文档的"返回编码表"。
 * </p>
 */
@Getter
public enum PostalErrorCode implements IErrorCode {

    /** 成功 */
    SUCCESS("0000", 0, "成功"),

    /** 报文解密错误 */
    DECRYPT_ERROR("9009", 50004, "请求报文解密错误"),

    /** 协议编码错误 */
    PROTOCOL_ENCODE_ERROR("9102", 40101, "协议编码错误"),

    /** 发起方编码错误 */
    INITIATOR_ENCODE_ERROR("9103", 40101, "发起方编码错误"),

    /** 落地方编码错误 */
    TARGET_ENCODE_ERROR("9104", 40101, "落地方编码错误"),

    /** 请求参数异常 */
    PARAM_ERROR("1000", 40101, "请求参数异常"),

    /** 接口服务已关闭 */
    SERVICE_CLOSED("1001", 50005, "接口服务已关闭"),

    /** 签名错误 */
    SIGN_ERROR("1002", 40110, "签名验证失败"),

    /** 系统其他异常 */
    SYSTEM_ERROR("1003", 50000, "系统其他异常"),

    /** 请求时间异常 */
    TIME_ERROR("1004", 40008, "请求时间异常"),

    /** 白名单无效或不存在 */
    WHITELIST_INVALID("1005", 40003, "白名单无效或不存在"),

    /** 访问量超限 */
    QUOTA_EXCEEDED("1006", 60000, "接口服务范围已经超出当日访问量限制"),

    /** 白名单无权访问 */
    WHITELIST_FORBIDDEN("1007", 40003, "白名单无权访问接口服务"),

    /** 接口服务不存在 */
    SERVICE_NOT_FOUND("1008", 40004, "接口服务不存在"),

    /** HTTP 握手失败 */
    HANDSHAKE_FAIL("1009", 50003, "接口HTTP握手失败"),

    /** 未知错误（兜底） */
    UNKNOWN("9999", 50000, "未知错误");

    /** 邮政侧返回的错误码 */
    private final String postalCode;

    /** 映射到本地的错误码（ResultCode 中的 code 值） */
    private final int code;

    /** 错误描述 */
    private final String message;

    PostalErrorCode(String postalCode, int code, String message) {
        this.postalCode = postalCode;
        this.code = code;
        this.message = message;
    }

    /**
     * 根据邮政侧错误码查找映射
     *
     * @param postalCode 邮政侧返回的错误码
     * @return 映射后的 PostalErrorCode，未找到返回 UNKNOWN
     */
    public static PostalErrorCode getByPostalCode(String postalCode) {
        if (postalCode == null) return UNKNOWN;
        for (PostalErrorCode value : values()) {
            if (value.postalCode.equals(postalCode)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
