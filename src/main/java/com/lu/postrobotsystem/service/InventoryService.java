package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Inventory;
import com.lu.postrobotsystem.model.request.inventory.InventoryQueryRequest;
import com.lu.postrobotsystem.model.response.inventory.InventoryResponse;

import java.util.List;

/**
 * 库存服务接口
 * <p>
 * 定义库存管理相关的核心业务操作，包括：入库、出库、库存锁定/释放/扣减等。
 * 库存模块采用 Redis + Lua 脚本实现高并发下的原子库存操作，DB 作为持久化存储，
 * 通过 Redisson 分布式锁保证同一商品库存操作的串行化。
 * </p>
 *
 * <p>
 * <b>库存状态流转：</b><br>
 * 入库（inboundStock）     → realStock 增加<br>
 * 出库（outboundStock）    → realStock 减少（不涉及锁定）<br>
 * 下单锁定（lockStock）    → lockedStock 增加，availableStock 减少<br>
 * 取消释放（releaseStock） → lockedStock 减少，availableStock 增加<br>
 * 支付扣减（deductStock）  → lockedStock 减少，realStock 减少<br>
 * </p>
 *
 * @see InventoryServiceImpl
 */
public interface InventoryService extends IService<Inventory> {

    /**
     * 根据商品ID查询库存信息
     * <p>调用链路：Controller → InventoryService.getByProductId → InventoryMapper.selectOne → getInventoryVO</p>
     *
     * @param productId 商品ID，不能为空
     * @return 库存视图对象 {@link InventoryResponse}，包含可用库存、锁定库存等
     */
    InventoryResponse getByProductId(Long productId);

    /**
     * 入库操作：增加商品的实际库存
     * <p>若该商品尚无库存记录则新建，否则累加 realStock。操作后同步更新 Redis 中的可用库存缓存。</p>
     * <p>调用链路：Controller → InventoryService.inboundStock → 校验 → 查询/创建库存记录 → 更新 DB → 刷新 Redis</p>
     *
     * @param productId 商品ID
     * @param quantity  入库数量，必须大于 0
     * @return void
     */
    Void inboundStock(Long productId, Integer quantity);

    /**
     * 出库操作：减少商品的实际库存
     * <p>出库前校验可用库存（realStock - lockedStock）是否充足，不足则抛出业务异常。</p>
     * <p>调用链路：Controller → InventoryService.outboundStock → 校验 → 查询库存 → 校验可用量 → 更新 DB → 刷新 Redis</p>
     *
     * @param productId 商品ID
     * @param quantity  出库数量，必须大于 0
     * @return void
     */
    Void outboundStock(Long productId, Integer quantity);

    /**
     * 锁定库存（下单时调用）
     * <p>
     * 采用 Redisson 分布式锁 + Lua 脚本实现原子操作，防止高并发下超卖。
     * Lua 脚本在 Redis 端原子地扣减 availableStock 并增加 lockedStock。
     * 成功后同步更新 DB 中的 lockedStock 字段。
     * </p>
     * <p>调用链路：OrderService.createOrder → InventoryService.lockStock → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  锁定数量
     * @return true=锁定成功, false=库存不足
     */
    boolean lockStock(Long productId, Integer quantity);

    /**
     * 释放锁定库存（取消订单/超时回滚时调用）
     * <p>
     * 与 lockStock 对应，通过 Lua 脚本原子地将 lockedStock 归还到 availableStock。
     * 用于订单取消、支付超时等场景回滚库存。
     * </p>
     * <p>调用链路：OrderService.cancelOrder / 定时任务 → InventoryService.releaseStock → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  释放数量
     * @return true=释放成功, false=释放失败
     */
    boolean releaseStock(Long productId, Integer quantity);

    /**
     * 扣减锁定库存（支付成功后调用）
     * <p>
     * 支付成功后，将 lockedStock 和 realStock 同时扣减，表示库存已被实际消耗。
     * 通过 Lua 脚本保证 Redis 侧的原子扣减。
     * </p>
     * <p>调用链路：OrderService.paySuccess → InventoryService.deductStock → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return true=扣减成功, false=扣减失败
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 将库存实体转换为库存视图对象（VO）
     * <p>转换过程中会补充商品名称等关联信息。</p>
     *
     * @param inventory 库存实体
     * @return 库存视图对象，若入参为 null 则返回 null
     */
    InventoryResponse getInventoryVO(Inventory inventory);

    /**
     * 批量将库存实体列表转换为库存视图对象列表
     *
     * @param inventoryList 库存实体列表
     * @return 库存视图对象列表，若入参为空则返回空列表
     */
    List<InventoryResponse> getInventoryVOList(List<Inventory> inventoryList);

    /**
     * 构建库存查询的 MyBatis-Plus 条件构造器
     * <p>支持按商品ID、样品状态、账实不一致标志等字段过滤，默认排除已删除记录并按更新时间降序排序。</p>
     *
     * @param query 库存查询请求对象
     * @return 查询条件包装器 {@link QueryWrapper}
     */
    QueryWrapper<Inventory> getQueryWrapper(InventoryQueryRequest query);
}
