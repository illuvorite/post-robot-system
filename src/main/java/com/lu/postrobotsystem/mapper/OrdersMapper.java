package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Orders;

/**
 * 订单 Mapper 接口。
 * <p>
 * 负责订单（Orders）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 订单实体记录订单主表信息，包括订单状态、用户 ID、总金额、支付方式等。
 * 与订单项（OrderItem）、支付（Payment）等实体关联，是订单履约流程的核心数据表。
 * </p>
 */
public interface OrdersMapper extends BaseMapper<Orders> {
}
