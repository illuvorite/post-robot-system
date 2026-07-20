package com.lu.postrobotsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.common.annotation.AuditLog;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.entity.Alert;
import com.lu.postrobotsystem.model.enums.AlertLevelEnum;
import com.lu.postrobotsystem.model.enums.AlertStatusEnum;
import com.lu.postrobotsystem.model.request.alert.AlertUpdateRequest;
import com.lu.postrobotsystem.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.lu.postrobotsystem.exception.ResultCode.NOT_FOUND;
import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;
import static com.lu.postrobotsystem.model.enums.OperationTypeEnum.TASK_MANUAL;

/**
 * 告警管理控制器
 */
@RestController
@RequestMapping("/alert")
@Tag(name = "告警管理")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/get/{id}")
    @Operation(summary = "查询告警详情")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Alert> getAlert(@PathVariable Long id) {
        Alert alert = alertService.getById(id);
        ThrowUtils.throwIf(alert == null, NOT_FOUND, "告警不存在");
        return Result.success(alert);
    }

    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询告警列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Page<Alert>> listAlertPage(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<Alert>()
                .eq(Alert::getIsDeleted, 0);
        if (alertType != null && !alertType.isEmpty()) {
            wrapper.eq(Alert::getAlertType, alertType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Alert::getStatus, status);
        }
        if (source != null && !source.isEmpty()) {
            wrapper.eq(Alert::getSource, source);
        }
        wrapper.orderByDesc(Alert::getCreateTime);

        Page<Alert> page = alertService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @PostMapping("/handle/{id}")
    @Operation(summary = "处理告警")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    @AuditLog(operationType = TASK_MANUAL, targetType = "ALERT", targetIdExpression = "#id", detail = "处理告警")
    public Result<Void> handleAlert(@PathVariable Long id,
                                     @RequestParam String handler,
                                     @RequestParam(required = false) String note) {
        Alert alert = alertService.getById(id);
        ThrowUtils.throwIf(alert == null, NOT_FOUND, "告警不存在");
        alert.setStatus(AlertStatusEnum.RESOLVED);
        alert.setHandler(handler);
        alert.setHandleNote(note);
        alert.setHandleTime(java.time.LocalDateTime.now());
        alert.setResolvedTime(java.time.LocalDateTime.now());
        alertService.updateById(alert);
        return Result.success(null, "告警已处理");
    }

    @GetMapping("/count/unresolved")
    @Operation(summary = "查询未处理告警数量")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER', 'OPERATOR')")
    public Result<Long> countUnresolved() {
        Long count = alertService.lambdaQuery()
                .eq(Alert::getStatus, AlertStatusEnum.UNRESOLVED)
                .eq(Alert::getIsDeleted, 0)
                .count();
        return Result.success(count);
    }

    @PutMapping("/update")
    @Operation(summary = "更新告警")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> updateAlert(@Valid @RequestBody AlertUpdateRequest request) {
        Alert alert = alertService.getById(request.getId());
        ThrowUtils.throwIf(alert == null, NOT_FOUND, "告警不存在");
        if (request.getHandler() != null) alert.setHandler(request.getHandler());
        if (request.getHandleNote() != null) alert.setHandleNote(request.getHandleNote());
        if (request.getAlertLevel() != null) alert.setAlertLevel(AlertLevelEnum.valueOf(request.getAlertLevel()));
        alertService.updateById(alert);
        return Result.success(null, "告警更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除告警")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteAlert(@PathVariable Long id) {
        boolean ok = alertService.removeById(id);
        ThrowUtils.throwIfNot(ok, "告警不存在");
        return Result.success(null, "告警已删除");
    }
}
