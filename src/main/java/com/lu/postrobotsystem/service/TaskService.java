package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Task;

/**
 * 任务服务接口
 */
public interface TaskService extends IService<Task> {

    /**
     * 构建任务查询条件
     */
    QueryWrapper<Task> getQueryWrapper(String status, String type, String taskNo);
}
