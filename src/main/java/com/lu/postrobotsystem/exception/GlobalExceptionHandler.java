package com.lu.postrobotsystem.exception;

import cn.hutool.core.util.StrUtil;
import com.lu.postrobotsystem.common.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 通过 {@link RestControllerAdvice} 统一拦截系统中所有 Controller 层抛出的异常，
 * 将其转换为统一格式的 {@link Result} 响应返回给前端。
 * </p>
 *
 * <p>
 * <b>异常处理分类：</b><br>
 * <ol>
 *   <li><b>业务异常（BusinessException）：</b>业务逻辑中主动抛出的可识别异常，直接返回错误码和描述</li>
 *   <li><b>参数校验异常：</b>Spring Validation 框架在校验失败时抛出的异常，提取具体字段错误信息</li>
 *   <li><b>HTTP 异常：</b>请求方法不支持、资源不存在等</li>
 *   <li><b>数据库异常：</b>主键重复、数据冲突等</li>
 *   <li><b>兜底异常：</b>以上未覆盖的所有异常（500 系统错误）</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>数据流转：</b><br>
 * Controller 方法执行 → 业务层抛出 BusinessException / 框架抛出校验异常 → GlobalExceptionHandler
 * → 按异常类型路由到对应处理方法 → 记录日志 → 组装 Result.fail() → 返回给前端
 * </p>
 *
 * @see BusinessException 业务异常
 * @see ResultCode        预定义错误码
 * @see Result            统一响应体
 */
@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 处理业务异常（BusinessException）
     * <p>
     * 拦截由业务逻辑层主动抛出的 BusinessException，记录警告日志后返回包含错误码的响应。
     * 这是系统中出现频率最高的异常处理入口。
     * </p>
     *
     * @param e 业务异常对象，包含 code（错误码）和 message（错误描述）
     * @return 统一错误响应 {@link Result#fail(int, String)}
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * 处理方法参数校验失败异常（@Valid / @Validated 注解校验失败时抛出）
     * <p>
     * 提取所有字段的错误信息，拼接为可读的字符串返回。
     * 例如：请求体中 name 字段为 null 时，提取 "name不能为空"。
     * </p>
     *
     * @param e 方法参数校验异常对象
     * @return 包含字段级错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> StrUtil.format("{}{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALUE_INVALID.getCode(), message);
    }

    /**
     * 处理参数绑定异常（参数格式或类型转换失败时抛出）
     * <p>
     * 例如：传入字符串 "abc" 给 Integer 类型字段时，BindException 会被触发。
     * </p>
     *
     * @param e 参数绑定异常对象
     * @return 包含绑定错误信息的响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALUE_INVALID, message);
    }

    /**
     * 处理约束校验异常（方法参数上的校验注解失败时抛出）
     * <p>
     * 与 MethodArgumentNotValidException 类似，但适用于非请求体参数的校验
     * （如 {@link jakarta.validation.Validated} 标注在类级别的方法参数上）。
     * </p>
     *
     * @param e 约束校验异常对象
     * @return 包含约束违背信息的响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALUE_INVALID, message);
    }

    /**
     * 处理请求体解析失败异常（如 JSON 格式错误、请求体为空等）
     * <p>
     * 当请求体无法被反序列化为目标对象时触发。例如：JSON 语法错误、缺少必要的请求体。
     * </p>
     *
     * @param e 请求体解析失败异常对象
     * @return 包含解析错误信息的响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误或为空");
    }

    // ==================== HTTP 异常 ====================

    /**
     * 处理不支持的请求方法异常（如 POST 接口收到了 GET 请求）
     *
     * @param e 不支持的请求方法异常对象
     * @return 包含方法不允许信息的响应（HTTP 405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理资源不存在异常（请求的 URL 路径没有对应的处理器）
     * <p>
     * 同时处理 Spring MVC 的 NoHandlerFoundException 和 NoResourceFoundException
     * （后者在 Spring Boot 3.x + 静态资源场景中常见）。
     * </p>
     *
     * @param e 资源不存在异常对象
     * @return 包含资源不存在信息的响应（HTTP 404）
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Result<Void> handleNotFound(Exception e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    // ==================== 数据库异常 ====================

    /**
     * 处理主键/唯一索引重复异常
     * <p>
     * 当插入或更新操作违反了数据库的唯一约束时触发。
     * 例如：插入重复的用户名、重复的商品名称等。
     * </p>
     *
     * @param e 主键重复异常对象
     * @return 包含数据已存在信息的响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("数据重复: {}", e.getMessage());
        return Result.fail(ResultCode.DATA_ALREADY_EXIST);
    }

    // ==================== 兜底异常 ====================

    /**
     * 处理系统其他未预见的异常（兜底处理）
     * <p>
     * 作为最后一道防线，捕获所有未被其他异常处理方法捕获的 Exception。
     * 记录 error 日志（含完整堆栈），返回标准的服务器内部错误响应。
     * </p>
     *
     * @param e 系统异常对象
     * @return 包含服务器内部错误信息的响应（HTTP 500）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
