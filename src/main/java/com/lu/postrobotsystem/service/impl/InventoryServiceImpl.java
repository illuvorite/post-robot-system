package com.lu.postrobotsystem.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.constant.RedisKeyConstants;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.AlertMapper;
import com.lu.postrobotsystem.mapper.InventoryMapper;
import com.lu.postrobotsystem.model.entity.Alert;
import com.lu.postrobotsystem.model.entity.Inventory;
import com.lu.postrobotsystem.model.entity.Product;
import com.lu.postrobotsystem.model.enums.*;
import com.lu.postrobotsystem.model.request.inventory.InventoryAdjustRequest;
import com.lu.postrobotsystem.model.request.inventory.InventoryQueryRequest;
import com.lu.postrobotsystem.model.request.inventory.VisionInspectRequest;
import com.lu.postrobotsystem.model.response.inventory.InventoryResponse;
import com.lu.postrobotsystem.service.InventoryService;
import com.lu.postrobotsystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.lu.postrobotsystem.exception.ResultCode.*;

/**
 * 库存服务实现类
 * <p>
 * 实现库存管理的核心业务逻辑。库存系统采用 <b>Redis + Lua 脚本 + DB</b> 三层架构：
 * <ul>
 *   <li><b>Redis 层：</b>通过 Lua 脚本实现高并发下的原子库存操作（锁定 / 释放 / 扣减）</li>
 *   <li><b>Redisson 层：</b>使用分布式锁确保同一商品库存操作的串行化</li>
 *   <li><b>DB 层：</b>作为库存的持久化存储，Redis 操作成功后同步更新数据库</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>库存状态流转与方法调用关系：</b><br>
 * <pre>
 *     入库（inboundStock）                    — realStock 增加，Redis 同步
 *     出库（outboundStock）                   — realStock 减少，Redis 同步
 *     下单（Controller） → lockStock          — lockedStock +，availableStock -（Lua 原子操作）
 *     取消订单 → releaseStock                — lockedStock -，availableStock +（Lua 原子操作）
 *     支付成功 → deductStock                 — lockedStock -，realStock -（Lua 原子操作）
 * </pre>
 * </p>
 *
 * <p>
 * <b>Lua 脚本说明：</b><br>
 * - stock_lock.lua：原子扣减 availableStock 并增加 lockedStock<br>
 * - stock_release.lua：原子扣减 lockedStock 并归还 availableStock<br>
 * - stock_deduct.lua：原子扣减 lockedStock<br>
 * 所有脚本通过 scriptLoad 缓存 SHA，避免重复传输脚本内容。
 * </p>
 *
 * @see InventoryService
 * @see InventoryMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    private final ProductService productService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final InventoryMapper inventoryMapper;
    private final AlertMapper alertMapper;

    // Lua 脚本的 SHA 缓存（volatile 保证多线程可见性），避免每次调用重复加载脚本
    private volatile String lockScriptSha;
    private volatile String releaseScriptSha;
    private volatile String deductScriptSha;

    // ==================== 查询方法 ====================

    /**
     * 根据商品 ID 查询库存信息
     * <p>调用链路：Controller → this.getByProductId → InventoryMapper.selectOne → this.getInventoryVO</p>
     *
     * @param productId 商品ID
     * @return 库存视图对象，包含库存数量、锁定数量、可用数量、样品状态等
     */
    @Override
    public InventoryResponse getByProductId(Long productId) {
        // 校验商品 ID 不能为空
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");

        // 查询未删除的库存记录
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));
        // 库存记录不存在则抛出异常
        ThrowUtils.throwIf(ObjectUtil.isNull(inventory), NOT_FOUND, "该商品库存记录不存在");

        // 转换为 VO 返回（含商品名称等关联信息）
        return getInventoryVO(inventory);
    }

    // ==================== 入库 / 出库（DB + Redis 同步） ====================

    /**
     * 入库操作
     * <p>
     * 业务流程：<br>
     * 1. 校验参数（商品ID、数量）<br>
     * 2. 检查商品是否存在<br>
     * 3. 若该商品尚无库存记录则新建，否则累加 realStock<br>
     * 4. 同步更新 Redis 中的 availableStock 缓存
     * </p>
     * <p>调用链路：Controller → this.inboundStock → 校验 → 查询商品 → 查询/创建库存 → 更新 DB → 刷新 Redis</p>
     *
     * @param productId 商品ID
     * @param quantity  入库数量
     * @return void
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void inboundStock(Long productId, Integer quantity) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_VALUE_INVALID, "入库数量必须大于0");

        // === 校验商品是否存在 ===
        Product product = productService.getById(productId);
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");

        // === 查询或创建库存记录 ===
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));

        if (ObjectUtil.isNull(inventory)) {
            // 首次入库：创建新库存记录，设置默认低库存阈值和样品状态
            inventory = new Inventory()
                    .setProductId(productId)
                    .setRealStock(quantity)               // 实际库存设为入库数量
                    .setLockedStock(0)                     // 锁定库存初始为 0
                    .setLowStockThreshold(10)              // 低库存预警阈值默认 10
                    .setSampleStatus(SampleStatusEnum.NORMAL)             // 样品状态默认正常
                    .setMismatchFlag(false);                // 账实一致标志默认 false
            save(inventory);
        } else {
            // 已有库存记录：累加实际库存
            inventory.setRealStock(inventory.getRealStock() + quantity);
            updateById(inventory);
        }

        // === 同步 Redis 缓存：更新可用库存 = realStock - lockedStock ===
        String availableKey = RedisKeyConstants.STOCK_AVAILABLE + productId;
        stringRedisTemplate.opsForValue().set(availableKey,
                String.valueOf(inventory.getRealStock() - inventory.getLockedStock()));

        log.info("商品入库成功: productId={}, quantity={}, currentStock={}", productId, quantity, inventory.getRealStock());
        return null;
    }

    /**
     * 出库操作
     * <p>
     * 业务流程：<br>
     * 1. 校验参数（商品ID、数量）<br>
     * 2. 查询库存记录并校验可用库存是否充足<br>
     * 3. 扣减 realStock<br>
     * 4. 同步更新 Redis 中的 availableStock 缓存
     * </p>
     * <p>注意：出库操作不涉及锁定库存的变动，仅减少实际库存。</p>
     * <p>调用链路：Controller → this.outboundStock → 校验 → 查询库存 → 校验可用量 → 更新 DB → 刷新 Redis</p>
     *
     * @param productId 商品ID
     * @param quantity  出库数量
     * @return void
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void outboundStock(Long productId, Integer quantity) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_VALUE_INVALID, "出库数量必须大于0");

        // === 查询库存记录 ===
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(inventory), NOT_FOUND, "该商品库存记录不存在");

        // === 校验可用库存是否充足 ===
        // 可用库存 = 实际库存 - 锁定库存（锁定部分不可用于出库）
        int availableStock = inventory.getRealStock() - inventory.getLockedStock();
        ThrowUtils.throwIf(availableStock < quantity, BUSINESS_ERROR,
                StrUtil.format("库存不足：可用库存={}，需求={}", availableStock, quantity));

        // === 扣减实际库存 ===
        inventory.setRealStock(inventory.getRealStock() - quantity);
        updateById(inventory);

        // === 同步 Redis 缓存 ===
        String availableKey = RedisKeyConstants.STOCK_AVAILABLE + productId;
        stringRedisTemplate.opsForValue().set(availableKey,
                String.valueOf(inventory.getRealStock() - inventory.getLockedStock()));

        // === 检查低库存阈值告警（用可用库存判断） ===
        int remainingAvailable = inventory.getRealStock() - inventory.getLockedStock();
        checkAndCreateLowStockAlert(productId, remainingAvailable, inventory.getLowStockThreshold());

        log.info("商品出库成功: productId={}, quantity={}, remainingReal={}, availableStock={}",
                productId, quantity, inventory.getRealStock(), remainingAvailable);
        return null;
    }

    // ==================== 库存锁定 / 释放 / 扣减（Redisson + Lua 原子操作） ====================

    /**
     * 锁定库存（下单时调用）
     * <p>
     * 业务流程：<br>
     * 1. 确保 Redis 中有该商品的库存缓存（初次访问时从 DB 加载）<br>
     * 2. 获取 Redisson 分布式锁（按 productId 粒度）<br>
     * 3. 执行 Lua 脚本原子扣减 availableStock、增加 lockedStock<br>
     * 4. Lua 脚本返回受影响的记录数，&gt; 0 表示成功<br>
     * 5. 成功后同步更新 DB 中的 lockedStock
     * </p>
     * <p>调用链路：OrderService.createOrder → this.lockStock → ensureStockInRedis → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  锁定数量
     * @return true=锁定成功, false=库存不足
     */
    @Override
    public boolean lockStock(Long productId, Integer quantity) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_VALUE_INVALID, "锁定数量必须大于0");

        // 确保 Redis 中有该商品的库存缓存（懒加载）
        ensureStockInRedis(productId);

        // === 获取分布式锁 ===
        String lockKey = RedisKeyConstants.STOCK_LOCK_KEY + productId;
        var lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // Redis 中需要操作的 key
            String availableKey = RedisKeyConstants.STOCK_AVAILABLE + productId;
            String lockedKey = RedisKeyConstants.STOCK_LOCKED + productId;

            // === Lua 原子操作：扣减 availableStock，增加 lockedStock ===
            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            Long result = script.eval(
                    RScript.Mode.READ_WRITE,
                    loadLockScript(),                     // 缓存了的 stock_lock.lua 脚本 SHA
                    RScript.ReturnType.INTEGER,
                    List.of(availableKey, lockedKey),     // KEYS[1]=可用库存, KEYS[2]=锁定库存
                    quantity, System.currentTimeMillis()); // ARGV[1]=数量, ARGV[2]=时间戳

            // result <= 0 表示库存不足，锁定失败
            if (result == null || result <= 0) {
                log.warn("库存锁定失败: productId={}, result={}", productId, result);
                return false;
            }

            // === 同步更新 DB（在锁内执行，无并发问题） ===
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, productId)
                            .eq(Inventory::getIsDeleted, 0));
            if (inventory != null) {
                inventory.setLockedStock(inventory.getLockedStock() + quantity);
                inventoryMapper.updateById(inventory);
            }

            log.info("库存锁定成功: productId={}, quantity={}", productId, quantity);
            return true;
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
    }

    /**
     * 释放锁定库存（取消订单/超时回滚时调用）
     * <p>
     * 与 lockStock 对应，将 lockedStock 归还到 availableStock。
     * 业务流程：<br>
     * 1. 获取分布式锁<br>
     * 2. 执行 Lua 脚本原子扣减 lockedStock、增加 availableStock<br>
     * 3. 成功后同步更新 DB
     * </p>
     * <p>调用链路：OrderService.cancelOrder / 定时回滚任务 → this.releaseStock → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  释放数量
     * @return true=释放成功, false=释放失败
     */
    @Override
    public boolean releaseStock(Long productId, Integer quantity) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_VALUE_INVALID, "释放数量必须大于0");

        // === 获取分布式锁 ===
        String lockKey = RedisKeyConstants.STOCK_LOCK_KEY + productId;
        var lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // Redis 中需要操作的 key
            String availableKey = RedisKeyConstants.STOCK_AVAILABLE + productId;
            String lockedKey = RedisKeyConstants.STOCK_LOCKED + productId;

            // === Lua 原子操作：扣减 lockedStock，归还 availableStock ===
            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            Long result = script.eval(
                    RScript.Mode.READ_WRITE,
                    loadReleaseScript(),                  // stock_release.lua
                    RScript.ReturnType.INTEGER,
                    List.of(availableKey, lockedKey),     // KEYS
                    quantity, System.currentTimeMillis()); // ARGV

            if (result == null || result <= 0) {
                log.warn("库存释放失败: productId={}, result={}", productId, result);
                return false;
            }

            // === 同步更新 DB ===
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, productId)
                            .eq(Inventory::getIsDeleted, 0));
            if (inventory != null) {
                // 使用 Math.max 防止 lockedStock 被扣为负数（兜底保护）
                inventory.setLockedStock(Math.max(inventory.getLockedStock() - quantity, 0));
                inventoryMapper.updateById(inventory);
            }

            log.info("库存释放成功: productId={}, quantity={}", productId, quantity);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 扣减锁定库存（支付成功后调用）
     * <p>
     * 支付成功确认后，lockedStock 和 realStock 同时扣减，表示库存已被实际消耗。
     * 业务流程：<br>
     * 1. 获取分布式锁<br>
     * 2. 执行 Lua 脚本原子扣减 lockedStock<br>
     * 3. 成功后同步更新 DB（lockedStock 和 realStock 同时扣减）
     * </p>
     * <p>调用链路：OrderService.paySuccess → this.deductStock → Redisson.lock → Lua eval → 更新 DB → unlock</p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return true=扣减成功, false=扣减失败
     */
    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(quantity) || quantity <= 0, PARAM_VALUE_INVALID, "扣减数量必须大于0");

        // === 获取分布式锁 ===
        String lockKey = RedisKeyConstants.STOCK_LOCK_KEY + productId;
        var lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            // === Lua 原子操作：扣减 lockedStock ===
            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            Long result = script.eval(
                    RScript.Mode.READ_WRITE,
                    loadDeductScript(),                   // stock_deduct.lua
                    RScript.ReturnType.INTEGER,
                    List.of(RedisKeyConstants.STOCK_LOCKED + productId), // KEYS[1]=锁定库存
                    quantity, System.currentTimeMillis()); // ARGV

            if (result == null || result <= 0) {
                log.warn("库存扣减失败: productId={}, result={}", productId, result);
                return false;
            }

            // === 同步更新 DB：同时扣减 lockedStock 和 realStock ===
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, productId)
                            .eq(Inventory::getIsDeleted, 0));
            if (inventory != null) {
                // lockedStock 和 realStock 同时扣减，表示库存已被实际消耗
                inventory.setLockedStock(Math.max(inventory.getLockedStock() - quantity, 0));
                inventory.setRealStock(Math.max(inventory.getRealStock() - quantity, 0));
                inventoryMapper.updateById(inventory);

                // 检查低库存阈值告警（用可用库存判断）
                int available = inventory.getRealStock() - inventory.getLockedStock();
                checkAndCreateLowStockAlert(productId, available, inventory.getLowStockThreshold());
            }

            log.info("库存扣减成功: productId={}, quantity={}, availableStock={}", productId, quantity, inventory.getRealStock() - inventory.getLockedStock());
            return true;
        } finally {
            lock.unlock();
        }
    }

    // ==================== 视觉巡检回写 ====================

    /**
     * 视觉巡检结果回写。
     * <p>
     * 机器人视觉系统完成巡检后，将识别到的样品状态和账实一致性标记回写到库存记录。
     * 若巡检发现异常（样品缺失/错位/账实不一致），自动创建或更新告警记录。
     * 异常商品的推荐权重会降低，确保推荐系统优先推荐正常商品。
     * </p>
     *
     * @param productId 商品ID
     * @param request   视觉巡检结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void visionInspect(Long productId, VisionInspectRequest request) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "巡检结果不能为空");

        // 校验样品状态值合法性
        SampleStatusEnum sampleStatus = SampleStatusEnum.getEnumByValue(request.getSampleStatus());
        ThrowUtils.throwIf(ObjectUtil.isNull(sampleStatus), PARAM_VALUE_INVALID, "样品状态值不合法");

        // === 查询库存记录 ===
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(inventory), NOT_FOUND, "该商品库存记录不存在");

        // === 更新库存字段 ===
        boolean prevMismatch = Boolean.TRUE.equals(inventory.getMismatchFlag());
        SampleStatusEnum prevSampleStatus = inventory.getSampleStatus();

        inventory.setSampleStatus(sampleStatus);
        if (request.getMismatchFlag() != null) {
            inventory.setMismatchFlag(request.getMismatchFlag());
        }
        inventory.setVisionInspectTime(java.time.LocalDateTime.now());
        updateById(inventory);

        // === 巡检异常时创建告警 ===
        boolean hasIssue = sampleStatus != SampleStatusEnum.NORMAL
                || Boolean.TRUE.equals(request.getMismatchFlag());

        if (hasIssue) {
            Product product = productService.getById(productId);
            String productName = product != null ? product.getName() : "商品ID:" + productId;

            StringBuilder msg = new StringBuilder("视觉巡检发现异常：");
            if (sampleStatus == SampleStatusEnum.MISSING) {
                msg.append("商品\"").append(productName).append("\"样品缺失");
            } else if (sampleStatus == SampleStatusEnum.DISPLACED) {
                msg.append("商品\"").append(productName).append("\"陈列错位");
            }
            if (Boolean.TRUE.equals(request.getMismatchFlag())) {
                if (sampleStatus != SampleStatusEnum.NORMAL) msg.append("，");
                msg.append("库存账实不一致");
            }

            // 避免重复创建相同来源的未解决告警：若已有同来源 UNRESOLVED 告警则跳过
            Long existingAlert = alertMapper.selectCount(
                    new LambdaQueryWrapper<Alert>()
                            .eq(Alert::getSource, "inventory")
                            .eq(Alert::getSourceId, String.valueOf(productId))
                            .eq(Alert::getStatus, AlertStatusEnum.UNRESOLVED)
                            .eq(Alert::getIsDeleted, 0));
            if (existingAlert == 0) {
                Alert alert = new Alert()
                        .setAlertType(AlertTypeEnum.STOCK_DISCREPANCY)
                        .setAlertLevel(AlertLevelEnum.WARNING)
                        .setSource("inventory")
                        .setSourceId(String.valueOf(productId))
                        .setMessage(msg.toString())
                        .setStatus(AlertStatusEnum.UNRESOLVED)
                        .setIsDeleted(0);
                alertMapper.insert(alert);
                log.warn("视觉巡检异常告警已创建: productId={}, reason={}", productId, msg);
            }
        }

        log.info("视觉巡检结果回写成功: productId={}, sampleStatus={}, mismatchFlag={}",
                productId, sampleStatus, request.getMismatchFlag());
    }

    // ==================== 手动调整与删除 ====================

    /**
     * 手动调整库存。
     * <p>
     * 用于盘点后人工修正：直接设定字段值而非增减。
     * 只修改请求中非空的字段，未传的保持原样。
     * 调整后同步刷新 Redis 缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(InventoryAdjustRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "调整请求不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(request.getProductId()), PARAM_ERROR, "商品ID不能为空");

        // 查询库存记录
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, request.getProductId())
                        .eq(Inventory::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(inventory), NOT_FOUND, "该商品库存记录不存在");

        // 仅更新非空字段
        if (ObjectUtil.isNotNull(request.getRealStock())) {
            ThrowUtils.throwIf(request.getRealStock() < 0, PARAM_VALUE_INVALID, "实时库存不能为负数");
            inventory.setRealStock(request.getRealStock());
        }
        if (ObjectUtil.isNotNull(request.getLockedStock())) {
            ThrowUtils.throwIf(request.getLockedStock() < 0, PARAM_VALUE_INVALID, "锁定库存不能为负数");
            inventory.setLockedStock(request.getLockedStock());
        }
        if (ObjectUtil.isNotNull(request.getLowStockThreshold())) {
            ThrowUtils.throwIf(request.getLowStockThreshold() < 0, PARAM_VALUE_INVALID, "库存阈值不能为负数");
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (StrUtil.isNotBlank(request.getSampleStatus())) {
            SampleStatusEnum sampleStatus = SampleStatusEnum.getEnumByValue(request.getSampleStatus());
            ThrowUtils.throwIf(ObjectUtil.isNull(sampleStatus), PARAM_VALUE_INVALID, "样品状态值不合法");
            inventory.setSampleStatus(sampleStatus);
        }
        if (ObjectUtil.isNotNull(request.getMismatchFlag())) {
            inventory.setMismatchFlag(request.getMismatchFlag());
        }

        updateById(inventory);

        // 同步 Redis 缓存
        String availableKey = RedisKeyConstants.STOCK_AVAILABLE + request.getProductId();
        stringRedisTemplate.opsForValue().set(availableKey,
                String.valueOf(inventory.getRealStock() - inventory.getLockedStock()));
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.STOCK_LOCKED + request.getProductId(), String.valueOf(inventory.getLockedStock()));

        log.info("库存手动调整成功: productId={}, realStock={}, lockedStock={}, threshold={}",
                request.getProductId(), inventory.getRealStock(), inventory.getLockedStock(), inventory.getLowStockThreshold());
    }

    /**
     * 删除库存记录（逻辑删除）。
     * <p>
     * 将库存记录的 is_deleted 置为 1，同时清理 Redis 中的库存缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInventory(Long productId) {
        ThrowUtils.throwIf(ObjectUtil.isNull(productId), PARAM_ERROR, "商品ID不能为空");

        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));
        ThrowUtils.throwIf(ObjectUtil.isNull(inventory), NOT_FOUND, "该商品库存记录不存在");

        // 逻辑删除
        inventory.setIsDeleted(1);
        updateById(inventory);

        // 清理 Redis 缓存
        stringRedisTemplate.delete(RedisKeyConstants.STOCK_AVAILABLE + productId);
        stringRedisTemplate.delete(RedisKeyConstants.STOCK_LOCKED + productId);

        log.info("库存记录已删除: productId={}", productId);
    }

    // ==================== 低库存告警 ====================

    /**
     * 检查库存是否低于阈值，若低于则创建低库存告警。
     * <p>
     * 在出库、扣减等库存减少操作后自动调用。
     * 避免重复创建相同商品的未解决低库存告警。
     * </p>
     */
    private void checkAndCreateLowStockAlert(Long productId, int currentStock, int threshold) {
        if (currentStock > threshold) return;

        // 检查是否已有未解决的低库存告警
        Long existingAlert = alertMapper.selectCount(
                new LambdaQueryWrapper<Alert>()
                        .eq(Alert::getAlertType, AlertTypeEnum.LOW_STOCK)
                        .eq(Alert::getSourceId, String.valueOf(productId))
                        .eq(Alert::getStatus, AlertStatusEnum.UNRESOLVED)
                        .eq(Alert::getIsDeleted, 0));
        if (existingAlert > 0) return; // 已有未解决的告警，不再重复创建

        Product product = productService.getById(productId);
        String productName = product != null ? product.getName() : "商品ID:" + productId;

        Alert alert = new Alert()
                .setAlertType(AlertTypeEnum.LOW_STOCK)
                .setAlertLevel(AlertLevelEnum.WARNING)
                .setSource("inventory")
                .setSourceId(String.valueOf(productId))
                .setMessage(String.format("商品\"%s\"库存低于阈值（当前: %d, 阈值: %d）", productName, currentStock, threshold))
                .setStatus(AlertStatusEnum.UNRESOLVED)
                .setIsDeleted(0);
        alertMapper.insert(alert);
        log.warn("低库存告警已创建: productId={}, currentStock={}, threshold={}", productId, currentStock, threshold);
    }

    // ==================== VO 转换 ====================

    /**
     * 将库存实体转换为库存视图对象
     * <p>
     * 转换时补充关联信息：<br>
     * - 计算 availableStock = realStock - lockedStock<br>
     * - 查询商品名称并设置到 VO
     * </p>
     *
     * @param inventory 库存实体
     * @return 库存视图对象，若入参为 null 则返回 null
     */
    @Override
    public InventoryResponse getInventoryVO(Inventory inventory) {
        if (ObjectUtil.isNull(inventory)) return null;

        // 属性拷贝：基础字段
        InventoryResponse vo = new InventoryResponse();
        vo.setId(inventory.getId());
        vo.setProductId(inventory.getProductId());
        vo.setRealStock(inventory.getRealStock());
        vo.setLockedStock(inventory.getLockedStock());
        vo.setAvailableStock(inventory.getRealStock() - inventory.getLockedStock());
        vo.setLowStockThreshold(inventory.getLowStockThreshold());
        vo.setSampleStatus(inventory.getSampleStatus() != null ? inventory.getSampleStatus().getValue() : null);
        vo.setMismatchFlag(inventory.getMismatchFlag());
        vo.setVisionInspectTime(inventory.getVisionInspectTime());
        vo.setCreateTime(inventory.getCreateTime());
        vo.setUpdateTime(inventory.getUpdateTime());

        // 关联查询：获取商品名称
        Product product = productService.getById(inventory.getProductId());
        if (ObjectUtil.isNotNull(product)) {
            vo.setProductName(product.getName());
        }

        return vo;
    }

    /**
     * 批量将库存实体列表转换为库存视图对象列表
     * <p>遍历调用 {@link #getInventoryVO(Inventory)} 进行逐个转换。</p>
     *
     * @param inventoryList 库存实体列表
     * @return 库存视图对象列表，若入参为空则返回空列表
     */
    @Override
    public List<InventoryResponse> getInventoryVOList(List<Inventory> inventoryList) {
        if (ObjectUtil.isEmpty(inventoryList)) return Collections.emptyList();
        return inventoryList.stream().map(this::getInventoryVO).collect(Collectors.toList());
    }

    // ==================== 查询条件构造 ====================

    /**
     * 构建库存查询的 MyBatis-Plus 条件构造器
     * <p>
     * 按 productId、sampleStatus、mismatchFlag 等字段精确过滤。
     * 默认排除已删除记录（is_deleted = 0），按更新时间降序排列。
     * </p>
     *
     * @param query 库存查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper<Inventory> getQueryWrapper(InventoryQueryRequest query) {
        QueryWrapper<Inventory> wrapper = new QueryWrapper<>();
        // 默认只查询未删除记录
        wrapper.eq("is_deleted", 0);
        if (ObjectUtil.isNotNull(query)) {
            // 按商品名称模糊搜索（先查 product 表获取匹配的 ID，再过滤 inventory）
            if (StrUtil.isNotBlank(query.getProductName())) {
                List<Product> matchedProducts = productService.list(
                        new LambdaQueryWrapper<Product>()
                                .like(Product::getName, query.getProductName().trim())
                                .eq(Product::getIsDeleted, 0)
                                .select(Product::getId));
                if (ObjectUtil.isNotEmpty(matchedProducts)) {
                    wrapper.in("product_id",
                            matchedProducts.stream().map(Product::getId).collect(Collectors.toList()));
                } else {
                    wrapper.eq("product_id", -1); // 无匹配商品，查不出结果
                }
            }
            // 按商品ID精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getProductId()), "product_id", query.getProductId());
            // 按样品状态精确匹配
            wrapper.eq(StrUtil.isNotBlank(query.getSampleStatus()), "sample_status", query.getSampleStatus());
            // 按账实不一致标志精确匹配（Boolean 转 int）
            wrapper.eq(ObjectUtil.isNotNull(query.getMismatchFlag()), "mismatch_flag",
                    Boolean.TRUE.equals(query.getMismatchFlag()) ? 1 : 0);
        }
        // 默认按更新时间降序排序
        wrapper.orderByDesc("update_time");
        return wrapper;
    }

    // ==================== 私有方法 ====================

    /**
     * 确保 Redis 中存在该商品的库存缓存
     * <p>
     * 若 Redis 中没有 availableStock 的缓存，则从 DB 加载并初始化。
     * 懒加载策略，避免在系统启动时预加载所有库存数据。
     * </p>
     *
     * @param productId 商品ID
     */
    private void ensureStockInRedis(Long productId) {
        String availableKey = RedisKeyConstants.STOCK_AVAILABLE + productId;
        // 如果 Redis 中已有缓存，直接返回
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(availableKey))) {
            return;
        }

        // 从 DB 加载库存数据并写入 Redis
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, productId)
                        .eq(Inventory::getIsDeleted, 0));
        if (inventory != null) {
            // 写入可用库存 = realStock - lockedStock
            int available = inventory.getRealStock() - inventory.getLockedStock();
            stringRedisTemplate.opsForValue().set(availableKey, String.valueOf(available));
            // 写入锁定库存
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.STOCK_LOCKED + productId, String.valueOf(inventory.getLockedStock()));
        }
    }

    /**
     * 加载（或从缓存获取）库存锁定的 Lua 脚本 SHA
     * <p>通过 scriptLoad 将脚本注册到 Redis，后续执行时只需传 SHA 而非完整脚本，减少网络开销。</p>
     *
     * @return Lua 脚本的 SHA 标识
     */
    private String loadLockScript() {
        if (lockScriptSha != null) return lockScriptSha;
        lockScriptSha = loadScript("lua/stock_lock.lua");
        return lockScriptSha;
    }

    /**
     * 加载（或从缓存获取）库存释放的 Lua 脚本 SHA
     *
     * @return Lua 脚本的 SHA 标识
     */
    private String loadReleaseScript() {
        if (releaseScriptSha != null) return releaseScriptSha;
        releaseScriptSha = loadScript("lua/stock_release.lua");
        return releaseScriptSha;
    }

    /**
     * 加载（或从缓存获取）库存扣减的 Lua 脚本 SHA
     *
     * @return Lua 脚本的 SHA 标识
     */
    private String loadDeductScript() {
        if (deductScriptSha != null) return deductScriptSha;
        deductScriptSha = loadScript("lua/stock_deduct.lua");
        return deductScriptSha;
    }

    /**
     * 加载指定路径的 Lua 脚本到 Redis，返回 SHA 标识
     * <p>从 classpath 读取 .lua 文件内容，调用 Redisson 的 scriptLoad 注册到 Redis。</p>
     *
     * @param classpath Lua 脚本的 classpath 路径（如 "lua/stock_lock.lua"）
     * @return 脚本的 SHA 标识
     * @throws RuntimeException 脚本加载失败时抛出
     */
    private String loadScript(String classpath) {
        try {
            // 从 classpath 读取 Lua 脚本内容
            ClassPathResource resource = new ClassPathResource(classpath);
            String script = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // 注册到 Redis 并返回 SHA
            return redissonClient.getScript(StringCodec.INSTANCE).scriptLoad(script);
        } catch (Exception e) {
            log.error("加载 Lua 脚本失败: {}", classpath, e);
            throw new RuntimeException("Lua 脚本加载失败: " + classpath, e);
        }
    }
}
