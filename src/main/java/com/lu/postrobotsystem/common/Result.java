package com.lu.postrobotsystem.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lu.postrobotsystem.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一API响应结果封装类
 * <p>
 * 为系统内所有 Controller 接口提供统一的 JSON 响应格式。
 * 通过静态工厂方法（success/fail）构建不同场景的响应结果，
 * 确保前端接收到的数据结构一致，便于统一处理和错误拦截。
 * </p>
 *
 * <p><b>响应格式：</b>
 * <pre>
 * {
 *   "code": 0,           // 状态码，0表示成功，非0表示失败
 *   "message": "成功",    // 响应消息
 *   "data": {...},       // 响应数据（可选）
 *   "traceId": "...",    // 请求追踪ID（可选）
 *   "timestamp": 1620000000000  // 响应时间戳（可选）
 * }
 * </pre>
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>Controller 层方法返回 {@code Result<T>}，Spring 自动序列化为 JSON</li>
 *   <li>{@link com.lu.postrobotsystem.exception.GlobalExceptionHandler} 捕获异常后调用 {@link #fail} 返回错误响应</li>
 *   <li>前端根据 {@link #code} 判断请求是否成功，根据 {@link #data} 获取业务数据</li>
 * </ul>
 * </p>
 *
 * @param <T> 响应数据的泛型类型
 * @author lu
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // 序列化时忽略值为 null 的字段，减少传输数据量
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：0 表示成功，非 0 表示各类错误 */
    private int code;

    /** 响应消息：成功时为 "成功"，失败时为具体错误描述 */
    private String message;

    /** 响应数据：泛型类型，承载业务返回的具体数据对象 */
    private T data;

    /** 请求追踪ID：用于分布式链路追踪，关联一次请求的全链路日志 */
    private String traceId;

    /** 响应时间戳：服务器生成响应的时间，单位毫秒 */
    private Long timestamp;

    // ==================== 成功响应（静态工厂方法） ====================

    /**
     * 构建成功响应（无数据返回）
     * <p>仅返回状态码和默认成功消息，适用于删除、状态变更等不需要返回数据的操作。</p>
     *
     * @param <T> 响应数据的类型
     * @return 成功响应 Result 对象
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    /**
     * 构建成功响应（带数据）
     * <p>返回状态码、默认成功消息和业务数据，适用于查询类操作。</p>
     *
     * @param <T> 响应数据的类型
     * @param data 要返回的业务数据对象
     * @return 成功响应 Result 对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 构建成功响应（带数据和自定义消息）
     * <p>在返回业务数据的同时，允许调用方自定义成功提示消息。</p>
     *
     * @param <T> 响应数据的类型
     * @param data 要返回的业务数据对象
     * @param message 自定义成功消息
     * @return 成功响应 Result 对象
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    // ==================== 失败响应（静态工厂方法） ====================

    /**
     * 构建失败响应（使用枚举）
     * <p>根据预定义的 {@link ResultCode} 枚举构建标准化错误响应。</p>
     *
     * @param <T> 响应数据的类型
     * @param resultCode 错误码枚举，包含错误码和默认错误消息
     * @return 失败响应 Result 对象
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 构建失败响应（使用枚举 + 自定义消息）
     * <p>复用预定义的错误码，但允许覆盖默认错误消息以提供更详细的错误描述。</p>
     *
     * @param <T> 响应数据的类型
     * @param resultCode 错误码枚举
     * @param message 自定义错误消息
     * @return 失败响应 Result 对象
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return fail(resultCode.getCode(), message);
    }

    /**
     * 构建失败响应（自定义状态码和消息）
     * <p>最灵活的失败响应构建方法，允许完全自定义错误码和错误消息。
     * 通常由 {@link com.lu.postrobotsystem.exception.GlobalExceptionHandler} 在捕获业务异常时调用。</p>
     *
     * @param <T> 响应数据的类型
     * @param code 自定义错误码
     * @param message 自定义错误消息
     * @return 失败响应 Result 对象
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 构建失败响应（仅自定义消息，使用默认失败码）
     * <p>简化版本，使用 {@link ResultCode#FAILURE} 的默认错误码，
     * 仅需传入自定义错误消息。</p>
     *
     * @param <T> 响应数据的类型
     * @param message 自定义错误消息
     * @return 失败响应 Result 对象
     */
    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.FAILURE.getCode(), message);
    }

    // ==================== 链式调用方法 ====================

    /**
     * 设置请求追踪ID
     * <p>用于在分布式链路追踪场景下，为当前响应关联一个唯一的追踪标识。
     * 调用方可通过该方法实现链式调用：{@code Result.success(data).withTraceId(traceId)}。</p>
     *
     * @param traceId 追踪ID字符串，由调用方传入（通常来自 MDC 或请求拦截器）
     * @return 当前 Result 对象，支持链式调用
     */
    public Result<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    // ==================== 便捷判断方法 ====================

    /**
     * 判断当前响应是否为成功状态
     * <p>以 {@link ResultCode#SUCCESS} 的状态码（0）为判定标准。</p>
     *
     * @return 如果 {@link #code} 等于 0 返回 true，否则返回 false
     */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    /**
     * 判断当前响应是否为失败状态
     * <p>对 {@link #isSuccess()} 的结果取反。</p>
     *
     * @return 如果 {@link #code} 不等于 0 返回 true，否则返回 false
     */
    public boolean isFail() {
        return !isSuccess();
    }
}
