package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Task;

/**
 * 任务 Mapper 接口。
 * <p>
 * 负责任务（Task）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 任务实体用于记录系统中后台任务的执行信息，如任务类型、状态、进度、
 * 执行结果等，适用于异步任务调度和跟踪的场景。
 * </p>
 */
public interface TaskMapper extends BaseMapper<Task> {
}
