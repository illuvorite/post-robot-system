package com.lu.postrobotsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.common.annotation.AuditLog;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.entity.Task;
import com.lu.postrobotsystem.model.enums.TaskStatusEnum;
import com.lu.postrobotsystem.model.enums.TaskTypeEnum;
import com.lu.postrobotsystem.model.request.task.TaskCreateRequest;
import com.lu.postrobotsystem.model.request.task.TaskUpdateRequest;
import com.lu.postrobotsystem.service.impl.TaskServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.lu.postrobotsystem.exception.ResultCode.*;

/**
 * 任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/task")
@Tag(name = "任务管理")
@RequiredArgsConstructor
public class TaskController {

    private static final AtomicLong TASK_SEQ = new AtomicLong(0);
    private static final DateTimeFormatter TASK_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TaskServiceImpl taskService;

    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询任务列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Page<Task>> listTaskPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String taskNo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<Task> page = taskService.page(
                new Page<>(pageNum, pageSize),
                taskService.getQueryWrapper(status, type, taskNo));
        return Result.success(page);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "查询任务详情")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Task> getTask(@PathVariable Long id) {
        Task task = taskService.getById(id);
        ThrowUtils.throwIf(task == null, NOT_FOUND, "任务不存在");
        return Result.success(task);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消任务")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> cancelTask(@PathVariable Long id) {
        boolean ok = taskService.cancelTask(id);
        ThrowUtils.throwIfNot(ok, "任务取消失败，仅运行中或排队中的任务可取消");
        return Result.success(null, "任务已取消");
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试任务")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> retryTask(@PathVariable Long id) {
        boolean ok = taskService.retryTask(id);
        ThrowUtils.throwIfNot(ok, "任务重试失败，仅失败或需人工处理的任务可重试");
        return Result.success(null, "任务已重试");
    }

    @PostMapping("/create")
    @Operation(summary = "创建任务")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> createTask(@Valid @RequestBody TaskCreateRequest request) {
        String taskNo = "TASK" + LocalDateTime.now().format(TASK_DATE_FMT)
                + String.format("%04d", TASK_SEQ.updateAndGet(v -> v >= 9999 ? 0 : v + 1));
        Task task = new Task()
                .setTaskNo(taskNo)
                .setTaskType(TaskTypeEnum.valueOf(request.getTaskType()))
                .setStatus(TaskStatusEnum.CREATED)
                .setPriority(request.getPriority() != null ? request.getPriority() : 5)
                .setDependencyTaskNo(request.getDependencyTaskNo())
                .setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 300)
                .setRetryCount(0)
                .setMaxRetry(request.getMaxRetry() != null ? request.getMaxRetry() : 3)
                .setInputParams(request.getInputParams());
        taskService.save(task);
        return Result.success(null, "任务创建成功");
    }

    @PutMapping("/edit")
    @Operation(summary = "编辑任务")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> editTask(@Valid @RequestBody TaskUpdateRequest request) {
        Task task = taskService.getById(request.getId());
        ThrowUtils.throwIf(task == null, NOT_FOUND, "任务不存在");
        if (request.getTaskType() != null) task.setTaskType(TaskTypeEnum.valueOf(request.getTaskType()));
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getTimeoutSeconds() != null) task.setTimeoutSeconds(request.getTimeoutSeconds());
        if (request.getMaxRetry() != null) task.setMaxRetry(request.getMaxRetry());
        if (request.getInputParams() != null) task.setInputParams(request.getInputParams());
        taskService.updateById(task);
        return Result.success(null, "任务更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除任务")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTAINER')")
    public Result<Void> deleteTask(@PathVariable Long id) {
        boolean ok = taskService.removeById(id);
        ThrowUtils.throwIfNot(ok, "任务不存在");
        return Result.success(null, "任务已删除");
    }

    @PostMapping("/robot-status")
    @Operation(summary = "机器人状态上报")
    public Result<Void> reportRobotStatus(@RequestBody Map<String, Object> status) {
        log.info("机器人状态上报: {}", status);
        return Result.success(null, "状态已接收");
    }
}
