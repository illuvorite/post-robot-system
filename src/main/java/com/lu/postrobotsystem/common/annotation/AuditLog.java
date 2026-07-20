package com.lu.postrobotsystem.common.annotation;

import com.lu.postrobotsystem.model.enums.OperationTypeEnum;
import com.lu.postrobotsystem.model.enums.OperationResultEnum;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * <p>
 * 标注在 Controller 方法上，由 {@link com.lu.postrobotsystem.common.aspect.AuditLogAspect} 拦截，
 * 自动记录操作人、操作类型、操作对象、结果、详情等到 audit_log 表。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 操作类型 */
    OperationTypeEnum operationType();

    /** 操作对象类型（ORDER / PRODUCT / INVENTORY / TASK / USER / ALERT / SYSTEM） */
    String targetType() default "SYSTEM";

    /**
     * 操作对象 ID 的 SpEL 表达式（从方法参数中提取）
     * 例如： "#orderNo" 或 "#id" 或 "#request.orderNo"
     */
    String targetIdExpression() default "";

    /** 操作详情（支持 {} 占位符，从 SpEL 提取） */
    String detail() default "";
}
