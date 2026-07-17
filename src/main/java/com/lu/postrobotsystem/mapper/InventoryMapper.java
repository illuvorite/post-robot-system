package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Inventory;

/**
 * 库存 Mapper 接口。
 * <p>
 * 负责库存（Inventory）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 库存实体记录每个商品的库存数量、锁定数量等关键数据，
 * 对应订单履约流程中的库存锁定、释放、扣减等操作的持久化层。
 * </p>
 */
public interface InventoryMapper extends BaseMapper<Inventory> {
}
