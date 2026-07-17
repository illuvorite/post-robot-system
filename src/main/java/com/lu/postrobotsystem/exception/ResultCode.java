package com.lu.postrobotsystem.exception;

import com.lu.postrobotsystem.common.IErrorCode;
import lombok.Getter;

/**
 * 结果码枚举类
 * <p>
 * 实现 {@link IErrorCode} 接口，集中定义系统中所有操作结果的错误码和对应描述。
 * 按错误类型分组，采用分层编码规则，便于快速定位问题域：
 * </p>
 *
 * <p>
 * <b>编码规则：</b><br>
 * <pre>
 * 0       - 成功
 * 1       - 通用失败
 * 4xxxx   - 客户端错误（参数、鉴权、权限等）
 *   400xx   - 通用 HTTP 语义错误（Bad Request / Forbidden / Not Found）
 *   401xx   - 参数校验 / 签名相关错误
 *   402xx   - Token / 鉴权错误
 *   403xx   - 业务逻辑错误
 *   409xx   - 权限不足
 * 10xx    - 接口服务错误
 * 9xxx    - 报文/协议错误
 * 5xxxx   - 服务端错误（系统、数据库、网络等）
 * 6xxxx   - 网关/熔断/限流错误
 * </pre>
 * </p>
 *
 * <p>
 * <b>使用方式：</b><br>
 * 在业务代码中通过 {@link BusinessException} 或 {@link ThrowUtils} 抛出：<br>
 * <pre>{@code
 * // 直接抛出
 * throw new BusinessException(ResultCode.NOT_FOUND);
 * throw new BusinessException(ResultCode.NOT_FOUND, "商品ID=" + id + " 不存在");
 *
 * // 通过 ThrowUtils 条件判断抛出
 * ThrowUtils.throwIf(user == null, ResultCode.USER_NOT_EXIST);
 * }</pre>
 * </p>
 *
 * @see BusinessException    业务异常（携带 ResultCode）
 * @see ThrowUtils           异常抛出工具类
 * @see GlobalExceptionHandler 全局异常处理器
 */
@Getter
public enum ResultCode implements IErrorCode {

    // ==================== 通用成功 ====================
    /**
     * 访问成功（操作正常返回）
     */
    SUCCESS(0, "访问成功"),

    // ==================== 通用失败 ====================
    /**
     * 操作失败（通用失败标识）
     */
    FAILURE(1, "操作失败"),

    // ==================== 客户端错误（4xxx） ====================
    /**
     * 请求参数错误（请求体格式、必填参数缺失等）
     */
    BAD_REQUEST(40000, "请求参数错误"),
    /**
     * 没有权限访问该资源（403 Forbidden）
     */
    FORBIDDEN(40003, "没有权限访问该资源"),
    /**
     * 请求资源不存在（404 Not Found）
     */
    NOT_FOUND(40004, "请求资源不存在"),
    /**
     * 请求方法不允许（405 Method Not Allowed）
     */
    METHOD_NOT_ALLOWED(40005, "请求方法不允许"),
    /**
     * 请求超时（408 Request Timeout）
     */
    REQUEST_TIMEOUT(40008, "请求超时"),
    /**
     * 不支持的媒体类型（415 Unsupported Media Type）
     */
    UNSUPPORTED_MEDIA_TYPE(40015, "不支持的媒体类型"),

    // ==================== 参数校验错误（41xx） ====================
    /**
     * 未登录（需要登录才能访问的资源）
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
     * 参数值不合法（如枚举值越界、数值不在允许范围内）
     */
    PARAM_VALUE_INVALID(40104, "参数值不合法"),

    // ==================== 接口服务错误（10xx） ====================
    /**
     * 请求参数异常（接口服务级别的参数问题）
     */
    INTERFACE_PARAM_ERROR(1000, "请求参数异常"),
    /**
     * 接口服务已关闭
     */
    INTERFACE_SERVICE_CLOSED(1001, "（接口编号）接口服务已关闭"),
    /**
     * 签名错误（接口签名验证未通过）
     */
    INTERFACE_SIGN_ERROR(1002, "签名错误"),
    /**
     * 系统其他异常（接口服务内部异常）
     */
    INTERFACE_SYSTEM_ERROR(1003, "系统其他异常"),
    /**
     * 请求时间异常（请求时间戳超出允许偏差范围）
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
     * 没有权限（当前用户角色无权执行该操作）
     */
    NO_AUTH_ERROR(40900, "没有权限"),

    // ==================== Token/鉴权错误（42xx） ====================
    /**
     * Token无效或已过期（JWT 签名验证未通过或超出有效期）
     */
    TOKEN_INVALID(40210, "Token无效或已过期"),
    /**
     * 缺少Token（请求头中未携带 Authorization 或格式错误）
     */
    TOKEN_MISSING(40211, "缺少Token"),

    // ==================== 业务错误（43xx - 49xx） ====================
    /**
     * 用户不存在（查询用户时未找到对应记录）
     */
    USER_NOT_EXIST(40200, "用户不存在"),
    /**
     * 账户已被锁定（连续登录失败等原因导致账户锁定）
     */
    USER_ACCOUNT_LOCKED(40201, "账户已被锁定"),
    /**
     * 账户已过期（账户超过有效使用期限）
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
     * 业务处理失败（通用业务错误，无特定分类时使用）
     */
    BUSINESS_ERROR(40300, "业务处理失败"),
    /**
     * 数据不存在（查询的数据记录不存在）
     */
    DATA_NOT_EXIST(40301, "数据不存在"),
    /**
     * 数据已存在（插入的数据与已有记录冲突，如唯一键重复）
     */
    DATA_ALREADY_EXIST(40302, "数据已存在"),
    /**
     * 数据操作失败（增删改查操作执行失败）
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
     * 服务器内部错误（未分类的服务器异常，兜底错误码）
     */
    INTERNAL_ERROR(50001, "服务器内部错误"),
    /**
     * 数据库操作异常（SQL 执行失败、连接异常等）
     */
    DB_ERROR(50002, "数据库操作异常"),
    /**
     * 网络异常（远程调用失败、连接超时等）
     */
    NETWORK_ERROR(50003, "网络异常"),
    /**
     * 第三方服务异常（调用的外部服务返回错误）
     */
    THIRD_PARTY_ERROR(50004, "第三方服务异常"),
    /**
     * 服务暂时不可用（503 Service Unavailable）
     */
    SERVICE_UNAVAILABLE(50005, "服务暂时不可用"),

    // ==================== 网关/熔断错误（6xxx） ====================
    /**
     * 请求过于频繁，请稍后再试（限流触发）
     */
    RATE_LIMIT_ERROR(60000, "请求过于频繁，请稍后再试"),
    /**
     * 服务熔断，请稍后再试（熔断器打开）
     */
    CIRCUIT_BREAKER_OPEN(60001, "服务熔断，请稍后再试"),
    /**
     * 服务调用超时（超过配置的超时时间）
     */
    TIMEOUT_ERROR(60002, "服务调用超时");

    /**
     * 错误码
     * <p>数字编码，客户端可根据错误码进行国际化展示或特定逻辑处理。</p>
     */
    private final int code;

    /**
     * 错误消息
     * <p>人类可读的错误描述，可直接展示给用户或用于日志。</p>
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param code    错误码（整型，遵循分层编码规则）
     * @param message 错误消息描述
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举常量
     * <p>遍历枚举值列表，返回第一个匹配 code 的枚举。若未匹配到则返回 null。</p>
     *
     * @param code 错误码
     * @return 对应的 ResultCode 枚举常量，不存在则返回 null
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
     * 根据错误码获取错误消息
     * <p>委托给 {@link #getByCode(int)} 获取枚举后提取消息文本。</p>
     *
     * @param code 错误码
     * @return 对应的错误消息，不存在则返回 null
     */
    public static String getMessageByCode(int code) {
        ResultCode resultCode = getByCode(code);
        return resultCode != null ? resultCode.getMessage() : null;
    }
}
