package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Product;

/**
 * 商品 Mapper 接口。
 * <p>
 * 负责商品（Product）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 商品实体记录系统内所有商品信息，包括名称、分类、价格、描述、上下架状态等。
 * 该 Mapper 被 {@link com.lu.postrobotsystem.service.ProductService} 调用，
 * 支撑商品管理的核心数据访问需求。
 * </p>
 */
public interface ProductMapper extends BaseMapper<Product> {
}
