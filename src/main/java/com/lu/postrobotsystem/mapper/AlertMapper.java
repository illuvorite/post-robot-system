package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Alert;

/**
 * 告警 Mapper 接口。
 * <p>
 * 负责告警（Alert）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能，
 * 包括插入、更新、删除、分页查询等，无需手写 XML 映射文件。
 * 如需自定义复杂查询 SQL，可在该接口中添加对应方法并配置 XML 映射。
 * </p>
 */
public interface AlertMapper extends BaseMapper<Alert> {
}
