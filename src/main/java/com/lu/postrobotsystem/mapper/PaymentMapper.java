package com.lu.postrobotsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lu.postrobotsystem.model.entity.Payment;

/**
 * 支付 Mapper 接口。
 * <p>
 * 负责支付（Payment）实体与数据库之间的数据访问操作。
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供基础的 CRUD 功能。
 * 支付实体记录每笔订单的支付交易信息，包括支付金额、支付方式、
 * 支付状态、第三方支付流水号等，关联订单履约流程的支付环节。
 * </p>
 */
public interface PaymentMapper extends BaseMapper<Payment> {
}
