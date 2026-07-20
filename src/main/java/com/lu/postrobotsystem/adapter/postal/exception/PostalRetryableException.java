package com.lu.postrobotsystem.adapter.postal.exception;

import com.lu.postrobotsystem.adapter.postal.model.enums.PostalErrorCode;

/**
 * 可重试的邮政 API 异常
 * <p>
 * 当邮政接口返回可重试的错误码（如网络超时、服务暂不可用）时抛出此异常，
 * 触发重试机制。不可重试的错误（如签名错误、参数错误）应抛出 {@link PostalApiException}。
 * </p>
 */
public class PostalRetryableException extends PostalApiException {

    /**
     * 构造可重试异常
     *
     * @param postalErrorCode 邮政侧错误码映射
     * @param message         错误描述
     */
    public PostalRetryableException(PostalErrorCode postalErrorCode, String message) {
        super(postalErrorCode, message);
    }

    /**
     * 构造可重试异常（带原始异常）
     *
     * @param postalErrorCode 邮政侧错误码映射
     * @param message         错误描述
     * @param cause           原始异常
     */
    public PostalRetryableException(PostalErrorCode postalErrorCode, String message, Throwable cause) {
        super(postalErrorCode, message, cause);
    }
}
