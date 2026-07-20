package com.lu.postrobotsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.AuditLogMapper;
import com.lu.postrobotsystem.model.entity.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import static com.lu.postrobotsystem.exception.ResultCode.NOT_FOUND;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志查询控制器
 */
@RestController
@RequestMapping("/audit-log")
@Tag(name = "审计日志")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询审计日志")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<AuditLog>> listAuditLogPage(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String traceId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (operator != null && !operator.isEmpty()) {
            wrapper.eq(AuditLog::getOperator, operator);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(AuditLog::getOperationType, operationType);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(AuditLog::getTargetType, targetType);
        }
        if (result != null && !result.isEmpty()) {
            wrapper.eq(AuditLog::getResult, result);
        }
        if (traceId != null && !traceId.isEmpty()) {
            wrapper.eq(AuditLog::getTraceId, traceId);
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);

        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "查询审计日志详情")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AuditLog> getAuditLog(@PathVariable Long id) {
        AuditLog log = auditLogMapper.selectById(id);
        ThrowUtils.throwIf(log == null, NOT_FOUND, "审计日志不存在");
        return Result.success(log);
    }
}
