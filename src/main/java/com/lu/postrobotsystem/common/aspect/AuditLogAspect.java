package com.lu.postrobotsystem.common.aspect;

import com.lu.postrobotsystem.common.annotation.AuditLog;
import com.lu.postrobotsystem.mapper.AuditLogMapper;
import com.lu.postrobotsystem.model.enums.OperationResultEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志 AOP 切面
 * <p>
 * 拦截所有标注 {@link AuditLog} 的 Controller 方法，自动记录操作轨迹到 audit_log 表。
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest httpServletRequest;

    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        // 执行前准备
        String operator = resolveOperator();
        String ipAddress = resolveIp();

        // 解析 SpEL 表达式获取 targetId
        String targetId = resolveExpression(auditLogAnnotation.targetIdExpression(), joinPoint);

        // 执行原方法
        Object result = null;
        OperationResultEnum opResult = OperationResultEnum.SUCCESS;
        String detail = auditLogAnnotation.detail();
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            opResult = OperationResultEnum.FAIL;
            detail = e.getMessage();
            throw e; // 继续抛出，不影响业务逻辑
        } finally {
            // 无论成功失败都记录审计日志（失败时记录异常信息）
            try {
                com.lu.postrobotsystem.model.entity.AuditLog auditLog = new com.lu.postrobotsystem.model.entity.AuditLog()
                        .setOperator(operator)
                        .setOperationType(auditLogAnnotation.operationType())
                        .setTargetType(auditLogAnnotation.targetType())
                        .setTargetId(targetId)
                        .setResult(opResult)
                        .setDetail(detail != null && !detail.isEmpty() ? detail : null)
                        .setIpAddress(ipAddress);
                auditLogMapper.insert(auditLog);
                log.debug("审计日志已记录: type={}, target={}, result={}", auditLogAnnotation.operationType(), targetId, opResult);
            } catch (Exception e) {
                log.error("审计日志写入失败", e);
            }
        }
        return result;
    }

    /**
     * 从 Spring Security 上下文获取当前操作人
     */
    private String resolveOperator() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            // ignore
        }
        return "SYSTEM";
    }

    /**
     * 从请求中获取客户端 IP
     */
    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }

    /**
     * 解析 SpEL 表达式
     */
    private String resolveExpression(String expression, ProceedingJoinPoint joinPoint) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        try {
            String[] paramNames = paramNameDiscoverer.getParameterNames(((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod());
            Object[] args = joinPoint.getArgs();
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            return expressionParser.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            log.warn("审计日志 SpEL 解析失败: expression={}", expression, e);
            return null;
        }
    }
}
