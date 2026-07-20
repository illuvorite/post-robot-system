package com.lu.postrobotsystem.adapter.postal.exception;

import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;
import com.lu.postrobotsystem.exception.BusinessException;

/**
 * 邮政 API 调用异常
 * <p>
 * 封装邮政侧返回的错误码和错误描述，继承 {@link BusinessException}
 * 以便被 {@link com.lu.postrobotsystem.exception.GlobalExceptionHandler} 统一捕获。
 * </p>
 */
public class PostalApiException extends BusinessException {

    /** 邮政侧原始错误码 */
    private final String postalErrorCode;

    /**
     * 构造邮政 API 异常
     *
     * @param postalErrorCode 邮政侧错误码映射
     * @param message         错误描述
     */
    public PostalApiException(PostalErrorCode postalErrorCode, String message) {
        super(postalErrorCode.getCode(), message);
        this.postalErrorCode = postalErrorCode.getPostalCode();
    }

    /**
     * 构造邮政 API 异常（带原始异常）
     *
     * @param postalErrorCode 邮政侧错误码映射
     * @param message         错误描述
     * @param cause           原始异常
     */
    public PostalApiException(PostalErrorCode postalErrorCode, String message, Throwable cause) {
        super(postalErrorCode.getCode(), message, cause);
        this.postalErrorCode = postalErrorCode.getPostalCode();
    }

    /**
     * 构造邮政 API 异常（仅错误码，使用默认描述）
     *
     * @param postalErrorCode 邮政侧错误码映射
     */
    public PostalApiException(PostalErrorCode postalErrorCode) {
        super(postalErrorCode.getCode(), postalErrorCode.getMessage());
        this.postalErrorCode = postalErrorCode.getPostalCode();
    }

    public String getPostalErrorCode() {
        return postalErrorCode;
    }
}
