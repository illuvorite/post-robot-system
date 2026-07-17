package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.User;

/**
 * 用户 Mapper 接口。
 * <p>
 * 负责用户（User）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 用户实体记录系统用户的核心信息，包括用户名、密码（BCrypt 加密）、
 * 用户角色、真实姓名、手机号、邮箱等。
 * 该 Mapper 被认证模块和用户管理模块共同使用，是系统最核心的数据访问接口之一。
 * </p>
 */
public interface UserMapper extends BaseMapper<User> {
}
