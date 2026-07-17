package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.AuditLog;

/**
 * 审计日志 Mapper 接口。
 * <p>
 * 负责审计日志（AuditLog）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 审计日志用于记录系统中关键操作的变更历史，包括操作人、操作时间、
 * 操作类型、操作详情等，满足合规审计和数据溯源的需求。
 * </p>
 */
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
