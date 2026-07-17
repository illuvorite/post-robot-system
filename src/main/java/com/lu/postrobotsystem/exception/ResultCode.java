package com.lu.postrobotsystem.exception;

import com.lu.postrobotsystem.common.IErrorCode;
import lombok.Getter;

/**
 * 结果码枚举类，实现了IErrorCode接口
 * 用于系统中各种操作结果的统一表示，包括HTTP状态码、业务错误码等
 */
@Getter
public enum ResultCode implements IErrorCode {



    // ==================== 通用成功 ====================
    /**
     * 访问成功
     */
    SUCCESS(0, "访问成功"),

    // ==================== 通用失败 ====================
    /**
     * 操作失败
     */
    FAILURE(1, "操作失败"),

    // ==================== 客户端错误（4xxx） ====================
    /**
     * 请求参数错误
     */
    BAD_REQUEST(40000, "请求参数错误"),
    /**
     * 没有权限访问该资源
     */
    FORBIDDEN(40003, "没有权限访问该资源"),
    /**
     * 请求资源不存在
     */
    NOT_FOUND(40004, "请求资源不存在"),
    /**
     * 请求方法不允许
     */
    METHOD_NOT_ALLOWED(40005, "请求方法不允许"),
    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(40008, "请求超时"),
    /**
     * 不支持的媒体类型
     */
    UNSUPPORTED_MEDIA_TYPE(40015, "不支持的媒体类型"),

    // ==================== 参数校验错误（41xx） ====================
    /**
     * 未登录
     */
    NOT_LOGIN_ERROR(40100, "未登录"),
    /**
     * 参数类型错误
     */
    PARAM_ERROR(40101, "参数类型错误"),
    /**
     * 参数类型错误
     */
    PARAM_TYPE_ERROR(40102, "参数类型错误"),
    /**
     * 参数格式错误
     */
    PARAM_FORMAT_ERROR(40103, "参数格式错误"),
    /**
     * 参数值不合法
     */
    PARAM_VALUE_INVALID(40104, "参数值不合法"),

    // ==================== 接口服务错误（10xx） ====================
    /**
     * 请求参数异常
     */
    INTERFACE_PARAM_ERROR(1000, "请求参数异常"),
    /**
     * 接口服务已关闭
     */
    INTERFACE_SERVICE_CLOSED(1001, "（接口编号）接口服务已关闭"),
    /**
     * 签名错误
     */
    INTERFACE_SIGN_ERROR(1002, "签名错误"),
    /**
     * 系统其他异常
     */
    INTERFACE_SYSTEM_ERROR(1003, "系统其他异常"),
    /**
     * 请求时间异常
     */
    INTERFACE_TIME_ERROR(1004, "请求时间异常"),
    /**
     * 白名单无效或者不存在
     */
    INTERFACE_WHITELIST_INVALID(1005, "白名单无效或者不存在"),
    /**
     * 接口服务范围已经超出当日访问量限制
     */
    INTERFACE_QUOTA_EXCEEDED(1006, "接口服务范围已经超出当日访问量限制"),
    /**
     * 白名单无权访问接口服务
     */
    INTERFACE_WHITELIST_FORBIDDEN(1007, "白名单无权访问（接口编号）接口服务"),
    /**
     * 接口服务不存在
     */
    INTERFACE_NOT_FOUND(1008, "（接口编号）接口服务不存在"),
    /**
     * 接口的http握手失败
     */
    INTERFACE_HANDSHAKE_FAIL(1009, "（接口编号）接口的http握手失败"),

    // ==================== 报文/协议错误（9xxx） ====================
    /**
     * 请求报文解密错误
     */
    DECRYPT_ERROR(9009, "请求报文解密错误"),
    /**
     * 协议编码错误
     */
    PROTOCOL_ENCODE_ERROR(9102, "协议编码错误"),
    /**
     * 发起方编码错误
     */
    INITIATOR_ENCODE_ERROR(9103, "发起方编码错误"),
    /**
     * 落地方编码错误
     */
    TARGET_ENCODE_ERROR(9104, "落地方编码错误"),
    /**
     * 会话控制编码错误
     */
    SESSION_ENCODE_ERROR(9105, "会话控制编码错误"),

    // ==================== 签名验证错误（41xx） ====================
    /**
     * 签名验证失败
     */
    SIGNATURE_ERROR(40110, "签名验证失败"),
    /**
     * 签名已过期
     */
    SIGNATURE_EXPIRED(40111, "签名已过期"),

    // ==================== 用户权限错误（42xx） ====================
    /**
     * 没有权限
     */
    NO_AUTH_ERROR(40900, "没有权限"),

    // ==================== Token/鉴权错误（42xx） ====================
    /**
     * Token无效或已过期
     */
    TOKEN_INVALID(40210, "Token无效或已过期"),
    /**
     * 缺少Token
     */
    TOKEN_MISSING(40211, "缺少Token"),


    // ==================== 业务错误（43xx - 49xx） ====================
    /**
     * 用户不存在
     */
    USER_NOT_EXIST(40200, "用户不存在"),
    /**
     * 账户已被锁定
     */
    USER_ACCOUNT_LOCKED(40201, "账户已被锁定"),
    /**
     * 账户已过期
     */
    USER_ACCOUNT_EXPIRED(40202, "账户已过期"),
    /**
     * 用户名或密码错误
     */
    USER_PASSWORD_ERROR(40203, "用户名或密码错误"),
    /**
     * 验证码错误
     */
    USER_VERIFY_CODE_ERROR(40204, "验证码错误"),

    // ==================== 业务错误（43xx - 49xx） ====================
    /**
     * 业务处理失败
     */
    BUSINESS_ERROR(40300, "业务处理失败"),
    /**
     * 数据不存在
     */
    DATA_NOT_EXIST(40301, "数据不存在"),
    /**
     * 数据已存在
     */
    DATA_ALREADY_EXIST(40302, "数据已存在"),
    /**
     * 数据操作失败
     */
    DATA_OPERATION_FAIL(40303, "数据操作失败"),
    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAIL(40304, "文件上传失败"),
    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_FAIL(40305, "文件下载失败"),

    // ==================== 服务端错误（5xxx） ====================
    /**
     * 系统错误
     */
    SYSTEM_ERROR(50000, "系统错误"),
    /**
     * 服务器内部错误
     */
    INTERNAL_ERROR(50001, "服务器内部错误"),
    /**
     * 数据库操作异常
     */
    DB_ERROR(50002, "数据库操作异常"),
    /**
     * 网络异常
     */
    NETWORK_ERROR(50003, "网络异常"),
    /**
     * 第三方服务异常
     */
    THIRD_PARTY_ERROR(50004, "第三方服务异常"),
    /**
     * 服务暂时不可用
     */
    SERVICE_UNAVAILABLE(50005, "服务暂时不可用"),

    // ==================== 网关/熔断错误（6xxx） ====================
    /**
     * 请求过于频繁，请稍后再试
     */
    RATE_LIMIT_ERROR(60000, "请求过于频繁，请稍后再试"),
    /**
     * 服务熔断，请稍后再试
     */
    CIRCUIT_BREAKER_OPEN(60001, "服务熔断，请稍后再试"),
    /**
     * 服务调用超时
     */
    TIMEOUT_ERROR(60002, "服务调用超时");

    /**
     * 错误码
     */
    private final int code;
    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据code获取枚举
     * @param code 错误码
     * @return 对应的ResultCode枚举，如果不存在则返回null
     */
    public static ResultCode getByCode(int code) {
        for (ResultCode value : ResultCode.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据code获取消息
     * @param code 错误码
     * @return 对应的错误消息，如果不存在则返回null
     */
    public static String getMessageByCode(int code) {
        ResultCode resultCode = getByCode(code);
        return resultCode != null ? resultCode.getMessage() : null;
    }
}
