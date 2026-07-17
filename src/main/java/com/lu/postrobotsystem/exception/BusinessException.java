package com.lu.postrobotsystem.exception;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 系统中所有业务逻辑层抛出的可识别异常均使用此类。
 * 配合 {@link GlobalExceptionHandler} 全局异常处理器，可统一捕获并返回标准化的错误响应。
 * </p>
 *
 * <p>
 * <b>设计说明：</b><br>
 * - 继承 RuntimeException，避免强制 try-catch，简化业务代码<br>
 * - 包含 code（错误码）和 message（错误描述），便于前端精确识别错误类型<br>
 * - 提供了多种构造方式，可灵活使用预定义错误码或自定义错误码<br>
 * - 所有业务异常在 GlobalExceptionHandler 中被统一拦截，返回统一格式的 Result 响应
 * </p>
 *
 * <p>
 * <b>使用示例：</b><br>
 * <pre>{@code
 * // 使用默认业务错误码
 * throw new BusinessException("用户不存在");
 *
 * // 使用预定义错误码
 * throw new BusinessException(ResultCode.UNAUTHORIZED);
 *
 * // 覆盖预定义错误码的描述
 * throw new BusinessException(ResultCode.NOT_FOUND, "商品ID=" + id + " 不存在");
 *
 * // 携带原始异常（保留异常堆栈链）
 * throw new BusinessException("数据库写入失败", dbException);
 * }</pre>
 * </p>
 *
 * @see ResultCode   预定义错误码枚举
 * @see GlobalExceptionHandler  全局异常处理器
 * @see ThrowUtils   异常抛出工具类（提供更简洁的断言式抛出）
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     * <p>与 {@link ResultCode} 中的错误码对应，用于前端区分错误类型并进行国际化或特定处理。</p>
     */
    private final int code;

    /**
     * 使用默认业务错误码构造异常
     * <p>code 默认为 {@link ResultCode#BUSINESS_ERROR}（40300）</p>
     *
     * @param message 错误描述信息
     */
    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    /**
     * 使用预定义错误码构造异常
     * <p>从 {@link ResultCode} 枚举中获取 code 和默认 message。</p>
     *
     * @param resultCode 预定义错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 使用预定义错误码 + 自定义描述构造异常
     * <p>
     * 覆盖枚举中的默认描述，提供更具体的错误信息。
     * 例如：ResultCode.NOT_FOUND 的默认描述为"请求资源不存在"，
     * 可通过此构造方法改为"商品ID=10086 不存在"。
     * </p>
     *
     * @param resultCode 预定义错误码枚举
     * @param message    自定义错误描述信息（覆盖枚举默认值）
     */
    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    /**
     * 使用自定义错误码和描述构造异常
     * <p>适用于需要自定义错误码的特殊场景（如对接外部系统需要特定错误码）。</p>
     *
     * @param code    自定义错误码
     * @param message 错误描述信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用默认业务错误码 + 异常原因构造异常
     * <p>
     * 保留原始异常堆栈链（cause chain），方便排查问题。
     * 例如在 catch 数据库异常后重新包装为业务异常。
     * </p>
     *
     * @param message 错误描述信息
     * @param cause   原始异常（保留异常堆栈链，用于日志追踪）
     */
    public BusinessException(String message, Throwable cause) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message, cause);
    }

    /**
     * 使用自定义错误码 + 异常原因构造异常
     * <p>
     * 最完整的构造方式，支持自定义错误码并保留原始异常堆栈链。
     * </p>
     *
     * @param code    自定义错误码
     * @param message 错误描述信息
     * @param cause   原始异常（保留异常堆栈链，用于日志追踪）
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
