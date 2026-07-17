package com.lu.postrobotsystem.common;

/**
 * 错误码接口
 * <p>
 * 定义系统内所有错误码的抽象契约，要求所有错误码枚举必须实现该接口。
 * 通过统一 {@link #getCode()} 和 {@link #getMessage()} 方法，
 * 确保全局异常处理和响应封装时可以一致地获取错误信息。
 * </p>
 *
 * <p><b>实现类：</b>
 * <ul>
 *   <li>{@link com.lu.postrobotsystem.exception.ResultCode} -- 系统预定义的错误码枚举</li>
 * </ul>
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>{@link com.lu.postrobotsystem.common.Result} 的 {@code fail()} 方法接收 {@code IErrorCode} 实现类</li>
 *   <li>各业务异常类通过实现该接口提供标准化的错误码</li>
 * </ul>
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
public interface IErrorCode {

    /**
     * 获取错误码的整数值
     * <p>返回一个唯一的整型错误码，用于前端判断错误类型。</p>
     *
     * @return 错误码数值（如 0 表示成功，1001 表示参数错误等）
     */
    int getCode();

    /**
     * 获取错误码对应的描述信息
     * <p>返回人类可读的错误描述字符串，用于展示给用户或记录日志。</p>
     *
     * @return 错误信息的字符串描述
     */
    String getMessage();
}
