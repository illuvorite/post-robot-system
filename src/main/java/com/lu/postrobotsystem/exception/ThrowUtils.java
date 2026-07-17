package com.lu.postrobotsystem.exception;

/**
 * 异常抛出工具类
 * <p>
 * 提供便捷的条件断言式异常抛出方法，显著减少业务代码中的 if-throw 样板代码。
 * 所有方法最终都通过 {@link BusinessException} 抛出异常。
 * </p>
 *
 * <p>
 * <b>方法分类：</b><br>
 * <ul>
 *   <li><b>条件抛出系列（throwIf / throwIfNot）：</b>条件满足/不满足时抛出异常，用于前置校验</li>
 *   <li><b>快捷抛出系列（fail）：</b>无条件直接抛出异常，用于确定性的错误场景</li>
 *   <li><b>场景快捷方法（badRequest / unauthorized / forbidden 等）：</b>常用错误场景封装，提升代码可读性</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>设计说明：</b><br>
 * - 工具类不允许实例化（私有构造 + 抛出 UnsupportedOperationException）<br>
 * - 支持多种异常构造方式：字符串消息 / ResultCode 枚举 / 自定义错误码<br>
 * - 方法名采用"throwIf"前缀，语义清晰，符合断言风格
 * </p>
 *
 * <p>
 * <b>使用示例：</b><br>
 * <pre>{@code
 * // 条件为 true 时抛出（最常用）
 * ThrowUtil.throwIf(id == null, "ID不能为空");
 * ThrowUtil.throwIf(user == null, ResultCode.USER_NOT_EXIST);
 * ThrowUtil.throwIf(count <= 0, ResultCode.PARAM_VALUE_INVALID, "数量必须大于0");
 *
 * // 条件为 false 时抛出
 * ThrowUtil.throwIfNot(hasPermission, ResultCode.FORBIDDEN);
 *
 * // 直接抛出
 * ThrowUtil.fail("系统繁忙");
 *
 * // 场景快捷方法
 * ThrowUtil.badRequest("参数不合法");
 * ThrowUtil.unauthorized("登录已过期");
 * ThrowUtil.notFound("商品不存在");
 * }</pre>
 * </p>
 *
 * @see BusinessException 业务异常
 * @see ResultCode        预定义错误码
 */
public final class ThrowUtils {

    /**
     * 私有构造函数
     * <p>工具类禁止实例化，调用构造方法将抛出 UnsupportedOperationException。</p>
     */
    private ThrowUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 条件抛出系列 ====================

    /**
     * 条件为 true 时抛出业务异常（使用默认业务错误码）
     * <p>适用于简单的参数校验场景，code 默认为 {@link ResultCode#BUSINESS_ERROR}（40300）。</p>
     *
     * @param condition 触发条件（true 时抛出异常）
     * @param message   异常描述信息
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 条件为 true 时抛出业务异常（使用预定义错误码）
     * <p>适用于需要精确错误码的校验场景，如资源不存在、权限不足等。</p>
     *
     * @param condition  触发条件（true 时抛出异常）
     * @param resultCode 预定义错误码枚举
     */
    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 条件为 true 时抛出业务异常（预定义错误码 + 自定义描述）
     * <p>覆盖枚举中的默认描述，提供更具体的错误信息。例如：<br>
     * {@code ThrowUtils.throwIf(user == null, ResultCode.NOT_FOUND, "用户ID=" + id + " 不存在");}</p>
     *
     * @param condition  触发条件（true 时抛出异常）
     * @param resultCode 预定义错误码枚举
     * @param message    自定义错误描述（覆盖枚举默认值）
     */
    public static void throwIf(boolean condition, ResultCode resultCode, String message) {
        if (condition) {
            throw new BusinessException(resultCode, message);
        }
    }

    /**
     * 条件为 true 时抛出业务异常（自定义错误码 + 自定义描述）
     * <p>适用于需要自定义非标准错误码的特殊场景，如对接外部系统。</p>
     *
     * @param condition 触发条件（true 时抛出异常）
     * @param code      自定义错误码
     * @param message   异常描述信息
     */
    public static void throwIf(boolean condition, int code, String message) {
        if (condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 条件为 false 时抛出业务异常（使用默认业务错误码）
     * <p>与 {@link #throwIf(boolean, String)} 逻辑相反，条件不满足时抛出。</p>
     *
     * @param condition 触发条件（false 时抛出异常）
     * @param message   异常描述信息
     */
    public static void throwIfNot(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 条件为 false 时抛出业务异常（使用预定义错误码）
     *
     * @param condition  触发条件（false 时抛出异常）
     * @param resultCode 预定义错误码枚举
     */
    public static void throwIfNot(boolean condition, ResultCode resultCode) {
        if (!condition) {
            throw new BusinessException(resultCode);
        }
    }

    // ==================== 快捷抛出系列 ====================

    /**
     * 直接抛出业务异常（使用默认业务错误码）
     *
     * @param message 异常描述信息
     */
    public static void fail(String message) {
        throw new BusinessException(message);
    }

    /**
     * 直接抛出业务异常（使用预定义错误码）
     *
     * @param resultCode 预定义错误码枚举
     */
    public static void fail(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    /**
     * 直接抛出业务异常（自定义错误码 + 自定义描述）
     *
     * @param code    自定义错误码
     * @param message 异常描述信息
     */
    public static void fail(int code, String message) {
        throw new BusinessException(code, message);
    }

    // ==================== 常用场景快捷方法 ====================

    /**
     * 抛出请求参数错误异常（40000 Bad Request）
     * <p>用于请求参数格式错误、必填参数缺失等场景。</p>
     *
     * @param message 错误描述信息
     */
    public static void badRequest(String message) {
        throw new BusinessException(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 抛出未授权异常（40102 参数类型错误）
     * <p>用于用户未登录或登录已过期等场景。</p>
     *
     * @param message 错误描述信息
     */
    public static void unauthorized(String message) {
        throw new BusinessException(ResultCode.PARAM_TYPE_ERROR, message);
    }

    /**
     * 抛出无权限异常（40003 Forbidden）
     * <p>用于当前用户角色无权执行某操作的场景。</p>
     *
     * @param message 错误描述信息
     */
    public static void forbidden(String message) {
        throw new BusinessException(ResultCode.FORBIDDEN, message);
    }

    /**
     * 抛出资源不存在异常（40004 Not Found）
     * <p>用于查询的资源在数据库中不存在的场景。</p>
     *
     * @param message 错误描述信息
     */
    public static void notFound(String message) {
        throw new BusinessException(ResultCode.NOT_FOUND, message);
    }

    /**
     * 抛出数据已存在异常（40302 DATA_ALREADY_EXIST）
     * <p>用于插入操作违反唯一约束的场景，如用户名重复、商品名重复等。</p>
     *
     * @param message 错误描述信息
     */
    public static void alreadyExist(String message) {
        throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, message);
    }

    /**
     * 抛出数据不存在异常（40301 DATA_NOT_EXIST）
     * <p>用于更新或删除操作时目标数据不存在的场景。</p>
     *
     * @param message 错误描述信息
     */
    public static void notExist(String message) {
        throw new BusinessException(ResultCode.DATA_NOT_EXIST, message);
    }
}
