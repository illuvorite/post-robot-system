package com.lu.postrobotsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.mapper.TaskMapper;
import com.lu.postrobotsystem.model.entity.Task;
import com.lu.postrobotsystem.model.enums.TaskStatusEnum;
import com.lu.postrobotsystem.model.enums.TaskTypeEnum;
import com.lu.postrobotsystem.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务服务实现
 */
@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Override
    public QueryWrapper<Task> getQueryWrapper(String status, String type, String taskNo) {
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);

        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq("task_type", type);
        }
        if (taskNo != null && !taskNo.isEmpty()) {
            wrapper.like("task_no", taskNo);
        }
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(Long id) {
        Task task = getById(id);
        if (task == null) return false;
        if (task.getStatus() != TaskStatusEnum.RUNNING && task.getStatus() != TaskStatusEnum.QUEUED) {
            return false;
        }
        task.setStatus(TaskStatusEnum.CANCELLED);
        task.setCompletedTime(LocalDateTime.now());
        return updateById(task);
    }

    /**
     * 重试任务
     */
    public boolean retryTask(Long id) {
        Task task = getById(id);
        if (task == null) return false;
        if (task.getStatus() != TaskStatusEnum.FAILED && task.getStatus() != TaskStatusEnum.MANUAL_REQUIRED) {
            return false;
        }
        task.setStatus(TaskStatusEnum.QUEUED);
        task.setFailReason(null);
        task.setRetryCount(task.getRetryCount() != null ? task.getRetryCount() + 1 : 1);
        return updateById(task);
    }
}
