package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.OrderItem;

/**
 * 订单项 Mapper 接口。
 * <p>
 * 负责订单项（OrderItem）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 订单项实体记录单个订单中包含的每一件商品信息，包括商品 ID、数量、单价等，
 * 与订单（Orders）实体构成一对多的关联关系。
 * </p>
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
