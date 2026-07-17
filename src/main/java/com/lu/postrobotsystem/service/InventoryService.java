package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Inventory;
import com.lu.postrobotsystem.model.request.inventory.InventoryAdjustRequest;
import com.lu.postrobotsystem.model.request.inventory.InventoryQueryRequest;
import com.lu.postrobotsystem.model.request.inventory.VisionInspectRequest;
import com.lu.postrobotsystem.model.response.inventory.InventoryResponse;

import java.util.List;

/**
 * 库存服务接口

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
     * 视觉巡检结果回写。
     * <p>
     * 机器人视觉系统完成库存巡检后，将识别结果回写到库存记录中，
     * 包括样品状态（正常/缺失/错位）和账实一致性标记。
     * 若巡检发现异常（样品缺失、错位或账实不一致），自动创建告警记录。
     * 异常状态会联动商品推荐引擎，排除异常商品。
     * </p>
     *
     * @param productId 商品ID
     * @param request   视觉巡检结果，包含样品状态和账实一致性标记
     */
    void visionInspect(Long productId, VisionInspectRequest request);

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
     * 手动调整库存。
     * <p>
     * 用于盘点修正：直接设定 realStock / lockedStock / 阈值 / 样品状态等字段。
     * 与入库/出库不同，adjust 是直接设值而非增减，操作后同步刷新 Redis 缓存。
     * </p>
     *
     * @param request 调整请求，包含商品ID 和需要修改的字段
     */
    void adjustStock(InventoryAdjustRequest request);

    /**
     * 删除库存记录（逻辑删除）。
     * <p>
     * 当商品停售或废弃时，逻辑删除对应的库存记录。
     * 同时清理 Redis 中的库存缓存。
     * </p>
     *
     * @param productId 商品ID
     */
    void deleteInventory(Long productId);

    /**
     * 构建库存查询的 MyBatis-Plus 条件构造器
     * <p>支持按商品ID、样品状态、账实不一致标志等字段过滤，默认排除已删除记录并按更新时间降序排序。</p>
     *
     * @param query 库存查询请求对象
     * @return 查询条件包装器 {@link QueryWrapper}
     */
    QueryWrapper<Inventory> getQueryWrapper(InventoryQueryRequest query);
}
