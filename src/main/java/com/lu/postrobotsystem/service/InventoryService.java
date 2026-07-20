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
     */
    InventoryResponse getByProductId(Long productId);

    /**
     * 入库操作：增加商品的实际库存
     */
    Void inboundStock(Long productId, Integer quantity);

    /**
     * 出库操作：减少商品的实际库存
     */
    Void outboundStock(Long productId, Integer quantity);

    /**
     * 锁定库存（下单时调用）
     */
    boolean lockStock(Long productId, Integer quantity);

    /**
     * 释放锁定库存（取消订单/超时回滚时调用）
     */
    boolean releaseStock(Long productId, Integer quantity);

    /**
     * 扣减锁定库存（支付成功后调用）
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 视觉巡检结果回写
     */
    void visionInspect(Long productId, VisionInspectRequest request);

    /**
     * 将库存实体转换为库存视图对象
     */
    InventoryResponse getInventoryVO(Inventory inventory);

    /**
     * 批量将库存实体列表转换为库存视图对象列表
     */
    List<InventoryResponse> getInventoryVOList(List<Inventory> inventoryList);

    /**
     * 手动调整库存
     */
    void adjustStock(InventoryAdjustRequest request);

    /**
     * 删除库存记录（逻辑删除）
     */
    void deleteInventory(Long productId);

    /**
     * 创建库存记录
     */
    void createInventory(Long productId);

    /**
     * 构建库存查询条件构造器
     */
    QueryWrapper<Inventory> getQueryWrapper(InventoryQueryRequest query);
}
