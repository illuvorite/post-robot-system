package com.lu.postrobotsystem.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.entity.Inventory;
import com.lu.postrobotsystem.model.request.inventory.InventoryQueryRequest;
import com.lu.postrobotsystem.model.response.inventory.InventoryResponse;
import com.lu.postrobotsystem.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;

/**
 * 库存管理控制器。
 * <p>
 * 负责处理商品库存相关的 HTTP 请求，涵盖库存查询、入库、出库、锁定、释放和扣减等操作。
 * 该控制器是订单履约流程中库存环节的核心入口，与订单系统和支付系统存在调用关系：
 * <ul>
 *   <li>下单时调用 {@code /lock} 锁定库存</li>
 *   <li>取消订单或超时回滚调用 {@code /release} 释放库存</li>
 *   <li>支付成功后调用 {@code /deduct} 扣减锁定库存</li>
 * </ul>
 * 库存管理的业务实现委托给 {@link InventoryService}。
 * 管理端接口（查询、入库、出库）要求 ADMIN 或 OPERATOR 角色权限。
 * </p>
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "库存管理")
@RequiredArgsConstructor
public class InventoryController {

    /** 库存服务层，处理库存变更的核心业务逻辑 */
    private final InventoryService inventoryService;

    /**
     * 根据商品 ID 查询库存信息。
     * <p>
     * 查询指定商品当前的库存快照，包括可用库存和锁定库存等数据。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param productId 商品 ID，通过 URL 路径传递
     * @return 统一响应结果，包含 {@link InventoryResponse} 库存信息
     */
    @GetMapping("/get/{productId}")
    @Operation(summary = "根据商品 ID 查询库存")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<InventoryResponse> getInventory(@PathVariable Long productId) {
        // 委托 inventoryService 根据商品 ID 查询并组装库存响应对象
        return Result.success(inventoryService.getByProductId(productId));
    }

    /**
     * 分页查询库存列表。
     * <p>
     * 根据查询条件（如商品 ID、库存阈值等）分页返回库存列表。
     * 流程：构建 QueryWrapper → MyBatis-Plus 分页查询 → 转为 VO 分页结果。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param query    库存查询条件，支持按商品名称、库存范围等筛选
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 统一响应结果，包含 {@link Page <InventoryResponse>} 分页数据
     */
    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询库存列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Page<InventoryResponse>> listInventoryPage(@Valid InventoryQueryRequest query,
                                                             @RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        // 第一步：由 inventoryService 根据查询条件构建 MyBatis-Plus QueryWrapper
        QueryWrapper<Inventory> wrapper = inventoryService.getQueryWrapper(query);
        // 第二步：执行分页查询，获取 Inventory 实体分页数据
        Page<Inventory> inventoryPage = inventoryService.page(new Page<>(pageNum, pageSize), wrapper);
        // 第三步：构建相同分页参数的 VO 分页对象，将实体列表转为响应对象列表
        Page<InventoryResponse> voPage = new Page<>(inventoryPage.getCurrent(), inventoryPage.getSize(), inventoryPage.getTotal());
        voPage.setRecords(inventoryService.getInventoryVOList(inventoryPage.getRecords()));
        return Result.success(voPage);
    }

    /**
     * 商品入库操作。
     * <p>
     * 增加指定商品的可用库存数量。
     * 前置校验：入库数量不能为空且必须大于 0。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param productId 商品 ID，不能为空
     * @param quantity  入库数量，最小为 1
     * @return 统一响应结果，附带"入库成功"提示
     * @throws RuntimeException 如果 quantity 为空或小于等于 0，抛出 PARAM_ERROR 异常
     */
    @PostMapping("/inbound")
    @Operation(summary = "商品入库")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> inbound(@RequestParam @NotNull Long productId,
                                @RequestParam @Min(1) Integer quantity) {
        // 参数校验：入库数量必须为正整数
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_ERROR, "入库数量必须大于0");
        // 执行入库操作，增加可用库存
        inventoryService.inboundStock(productId, quantity);
        return Result.success(null, "入库成功");
    }

    /**
     * 商品出库操作。
     * <p>
     * 减少指定商品的可用库存数量。
     * 前置校验：出库数量不能为空且必须大于 0。
     * 实际扣减时 inventoryService 内部会校验库存是否充足。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param productId 商品 ID，不能为空
     * @param quantity  出库数量，最小为 1
     * @return 统一响应结果，附带"出库成功"提示
     * @throws RuntimeException 如果 quantity 为空或小于等于 0，抛出 PARAM_ERROR 异常
     */
    @PostMapping("/outbound")
    @Operation(summary = "商品出库")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> outbound(@RequestParam @NotNull Long productId,
                                 @RequestParam @Min(1) Integer quantity) {
        // 参数校验：出库数量必须为正整数
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_ERROR, "出库数量必须大于0");
        // 执行出库操作，扣减可用库存（内部会校验库存是否充足）
        inventoryService.outboundStock(productId, quantity);
        return Result.success(null, "出库成功");
    }

    /**
     * 锁定库存（下单时调用）。
     * <p>
     * 创建订单时锁定指定商品的一定数量库存，防止超卖。
     * 这是订单履约流程的第一步库存操作。如果库存不足则锁定失败并抛出异常。
     * 该接口无需 ADMIN 权限，供订单服务内部调用。
     * 后续支付成功后应调用 {@link #deduct} 扣减锁定库存，
     * 或支付超时时调用 {@link #release} 释放锁定库存。
     * </p>
     *
     * @param productId 商品 ID，不能为空
     * @param quantity  锁定数量，最小为 1
     * @return 统一响应结果，附带"锁定成功"提示
     * @throws RuntimeException 如果库存不足，抛出异常提示"库存不足，锁定失败"
     */
    @PostMapping("/lock")
    @Operation(summary = "锁定库存（下单时调用）")
    public Result<Void> lock(@RequestParam @NotNull Long productId,
                             @RequestParam @Min(1) Integer quantity) {
        // 执行库存锁定操作，返回是否锁定成功
        boolean success = inventoryService.lockStock(productId, quantity);
        // 锁定失败（库存不足）时抛异常
        ThrowUtils.throwIfNot(success, "库存不足，锁定失败");
        return Result.success(null, "锁定成功");
    }

    /**
     * 释放锁定库存（取消订单或超时回滚时调用）。
     * <p>
     * 当订单被取消或支付超时，释放之前锁定的库存数量，使其恢复为可用库存。
     * 这是库存回滚操作，与 {@link #lock} 成对出现。
     * </p>
     *
     * @param productId 商品 ID，不能为空
     * @param quantity  释放数量，最小为 1
     * @return 统一响应结果，附带"释放成功"提示
     */
    @PostMapping("/release")
    @Operation(summary = "释放锁定库存（取消订单/超时回滚）")
    public Result<Void> release(@RequestParam @NotNull Long productId,
                                @RequestParam @Min(1) Integer quantity) {
        // 执行库存释放操作，将锁定库存回滚到可用库存
        inventoryService.releaseStock(productId, quantity);
        return Result.success(null, "释放成功");
    }

    /**
     * 扣减锁定库存（支付成功后调用）。
     * <p>
     * 支付成功后实际扣减之前锁定的库存，完成最终扣减。
     * 这是订单履约流程的最后一步库存操作，扣减后库存不可回退。
     * 调用时机在支付回调处理中，调用方为支付服务或订单服务。
     * </p>
     *
     * @param productId 商品 ID，不能为空
     * @param quantity  扣减数量，最小为 1
     * @return 统一响应结果，附带"扣减成功"提示
     * @throws RuntimeException 如果扣减失败（如锁定库存不足），抛出异常
     */
    @PostMapping("/deduct")
    @Operation(summary = "扣减锁定库存（支付成功后调用）")
    public Result<Void> deduct(@RequestParam @NotNull Long productId,
                               @RequestParam @Min(1) Integer quantity) {
        // 执行锁定库存扣减操作，返回是否扣减成功
        boolean success = inventoryService.deductStock(productId, quantity);
        // 扣减失败时抛异常
        ThrowUtils.throwIfNot(success, "扣减锁定库存失败");
        return Result.success(null, "扣减成功");
    }
}
