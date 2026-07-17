package com.lu.postrobotsystem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.AlertMapper;
import com.lu.postrobotsystem.mapper.InventoryMapper;
import com.lu.postrobotsystem.mapper.ProductMapper;
import com.lu.postrobotsystem.model.entity.Alert;
import com.lu.postrobotsystem.model.entity.Inventory;
import com.lu.postrobotsystem.model.entity.Product;
import com.lu.postrobotsystem.model.enums.AlertStatusEnum;
import com.lu.postrobotsystem.model.enums.AlertTypeEnum;
import com.lu.postrobotsystem.model.enums.ProductStatusEnum;
import com.lu.postrobotsystem.model.enums.SampleStatusEnum;
import com.lu.postrobotsystem.model.request.product.ProductCreateRequest;
import com.lu.postrobotsystem.model.request.product.ProductQueryRequest;
import com.lu.postrobotsystem.model.request.product.ProductRecommendRequest;
import com.lu.postrobotsystem.model.request.product.ProductUpdateRequest;
import com.lu.postrobotsystem.model.response.product.ProductRecommendItem;
import com.lu.postrobotsystem.model.response.product.ProductResponse;
import com.lu.postrobotsystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.lu.postrobotsystem.exception.ResultCode.*;

/**
 * 商品服务实现类
 * <p>
 * 实现商品管理的核心业务逻辑，包括商品增删改查、上下架管理以及智能推荐引擎。
 * 商品模块与库存模块紧密关联：推荐引擎需调用 InventoryMapper 查询库存状态，
 * 筛选出可售商品（有库存、样品正常、账实一致）。
 * </p>
 *
 * <p>
 * <b>推荐引擎算法概要：</b><br>
 * <pre>
 * 输入：用户意向标签、预算范围、筛选条件
 * 流程：
 *   1. 查询所有上架商品（status=1, is_deleted=0）
 *   2. 关联库存表，过滤不可售商品
 *   3. 为每个商品计算综合得分：
 *      - 标签匹配（权重 60%）：用户标签与商品标签的交集
 *      - 预算匹配（权重 30%）：价格在预算范围内
 *      - 机器人抓取（权重 10%）：支持则加分
 *   4. 按得分降序取 topN
 *   5. 组装推荐理由
 * 输出：推荐商品列表（含匹配分和推荐理由）
 * </pre>
 * </p>
 *
 * @see ProductService
 * @see ProductMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final InventoryMapper inventoryMapper;
    private final AlertMapper alertMapper;

    // ==================== 商品 CRUD ====================

    /**
     * 新增商品
     * <p>
     * 业务流程：<br>
     * 1. 校验请求参数和商品名称是否为空<br>
     * 2. 检查商品名称是否已存在（同一名称不允许重复）<br>
     * 3. 构造 Product 实体，设置默认状态为下架（status=0）<br>
     * 4. 保存到数据库<br>
     * 5. 自动创建库存记录（初始库存为 0，后续通过入库接口增加）
     * </p>
     * <p>调用链路：Controller → this.createProduct → 参数校验 → 名称查重 → save → 初始化库存 → 返回 ID</p>
     *
     * @param request 商品创建请求
     * @return 新增商品的 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductCreateRequest request) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "商品信息不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getName()), PARAM_ERROR, "商品名称不能为空");

        // === 检查商品名称是否重复（去除首尾空格后匹配） ===
        boolean exists = baseMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getName, request.getName().trim())
                        .eq(Product::getIsDeleted, 0)
        ) > 0;
        ThrowUtils.throwIf(exists, DATA_ALREADY_EXIST, "商品名称已存在");

        // === 构造商品实体，设置默认值 ===
        Product product = new Product()
                .setName(request.getName().trim())
                .setDescription(request.getDescription())
                .setTags(request.getTags())
                .setPrice(request.getPrice())
                .setOriginalPrice(request.getOriginalPrice())
                .setImageUrl(request.getImageUrl())
                .setRobotGraspable(ObjectUtil.defaultIfNull(request.getRobotGraspable(), false))  // 默认不支持机器人抓取
                .setDisplayPoint(request.getDisplayPoint())
                .setStatus(ProductStatusEnum.OFF_SHELF); // 0=下架（新建商品默认下架，需手动上架）

        // === 保存到数据库 ===
        save(product);

        // === 自动创建库存记录（初始为 0，后续通过入库操作增加） ===
        Inventory inventory = new Inventory()
                .setProductId(product.getId())
                .setRealStock(0)
                .setLockedStock(0)
                .setLowStockThreshold(10)
                .setSampleStatus(SampleStatusEnum.NORMAL)
                .setMismatchFlag(false);
        inventoryMapper.insert(inventory);
        log.info("库存记录已初始化: productId={}", product.getId());

        log.info("新增商品成功: id={}, name={}", product.getId(), product.getName());
        return product.getId();
    }

    /**
     * 编辑商品
     * <p>
     * 支持部分字段更新，仅对请求中非空的字段进行修改。
     * 若修改名称，需检查新名称是否与其他商品重复（排除自身）。
     * </p>
     * <p>调用链路：Controller → this.updateProduct → 查询商品 → 按需设值 → updateById</p>
     *
     * @param request 商品更新请求
     * @return void
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void updateProduct(ProductUpdateRequest request) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "商品信息不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(request.getId()), PARAM_ERROR, "商品ID不能为空");

        // === 查询现有商品 ===
        Product product = getById(request.getId());
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");

        // === 处理名称更新（含重复检查） ===
        if (StrUtil.isNotBlank(request.getName()) && !request.getName().trim().equals(product.getName())) {
            // 检查新名称是否被其他商品占用（排除当前商品自身）
            boolean exists = baseMapper.selectCount(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getName, request.getName().trim())
                            .ne(Product::getId, request.getId())
                            .eq(Product::getIsDeleted, 0)
            ) > 0;
            ThrowUtils.throwIf(exists, DATA_ALREADY_EXIST, "商品名称已存在");
            product.setName(request.getName().trim());
        }

        // === 按需更新字段（仅更新非空值） ===
        if (StrUtil.isNotBlank(request.getDescription())) product.setDescription(request.getDescription());
        if (StrUtil.isNotBlank(request.getTags())) product.setTags(request.getTags());
        if (ObjectUtil.isNotNull(request.getPrice())) product.setPrice(request.getPrice());
        if (ObjectUtil.isNotNull(request.getOriginalPrice())) product.setOriginalPrice(request.getOriginalPrice());
        if (StrUtil.isNotBlank(request.getImageUrl())) product.setImageUrl(request.getImageUrl());
        if (ObjectUtil.isNotNull(request.getRobotGraspable())) product.setRobotGraspable(request.getRobotGraspable());
        if (StrUtil.isNotBlank(request.getDisplayPoint())) product.setDisplayPoint(request.getDisplayPoint());
        if (ObjectUtil.isNotNull(request.getStatus())) {
            ProductStatusEnum statusEnum = ProductStatusEnum.getEnumByCode(request.getStatus());
            if (statusEnum != null) product.setStatus(statusEnum);
        }

        // === 保存更新 ===
        updateById(product);
        log.info("编辑商品成功: id={}", product.getId());
        return null;
    }

    /**
     * 上架/下架商品
     * <p>
     * 简单的状态切换操作，仅允许 0（下架）和 1（上架）两个合法值。
     * 上架后商品将在推荐引擎中可见；下架后不可被推荐和购买。
     * </p>
     * <p>调用链路：Controller → this.changeProductStatus → 校验 → updateById</p>
     *
     * @param id     商品ID
     * @param status 目标状态：0-下架，1-上架
     * @return void
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void changeProductStatus(Long id, Integer status) {
        // === 参数校验 ===
        ThrowUtils.throwIf(ObjectUtil.isNull(id), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(status), PARAM_ERROR, "状态不能为空");

        // === 查询商品是否存在 ===
        Product product = getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");

        // === 校验状态值合法性 ===
        ThrowUtils.throwIf(status != 0 && status != 1, PARAM_VALUE_INVALID, "状态值不合法（0-下架 1-上架）");

        // === 更新状态 ===
        product.setStatus(ProductStatusEnum.getEnumByCode(status));
        updateById(product);
        log.info("商品状态变更: id={}, status={}", id, status);
        return null;
    }

    // ==================== 标签维护 ====================

    /**
     * 更新商品标签。
     * <p>
     * 专门用于更新商品标签的接口，与全量编辑分离。
     * 前端可以只传标签字段进行局部更新。
     * </p>
     *
     * @param id   商品ID
     * @param tags 标签字符串（逗号分隔）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTags(Long id, String tags) {
        ThrowUtils.throwIf(ObjectUtil.isNull(id), PARAM_ERROR, "商品ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(tags), PARAM_ERROR, "标签不能为空");

        Product product = getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");

        product.setTags(tags.trim());
        updateById(product);
        log.info("商品标签更新成功: id={}, tags={}", id, tags);
    }

    // ==================== VO 转换 ====================

    /**
     * 将商品实体转换为商品视图对象
     * <p>使用 BeanUtil 进行属性拷贝，不含关联查询。</p>
     *
     * @param product 商品实体
     * @return 商品视图对象，若入参为 null 则返回 null
     */
    @Override
    public ProductResponse getProductVO(Product product) {
        if (ObjectUtil.isNull(product)) return null;
        ProductResponse vo = new ProductResponse();
        BeanUtil.copyProperties(product, vo);
        if (product.getStatus() != null) {
            vo.setStatus(product.getStatus().getCode());
        }
        return vo;
    }

    /**
     * 批量将商品实体列表转换为商品视图对象列表
     * <p>遍历调用 {@link #getProductVO(Product)} 进行逐个转换。</p>
     *
     * @param productList 商品实体列表
     * @return 商品视图对象列表，若入参为空则返回空列表
     */
    @Override
    public List<ProductResponse> getProductVOList(List<Product> productList) {
        if (ObjectUtil.isEmpty(productList)) return Collections.emptyList();
        return productList.stream().map(this::getProductVO).collect(Collectors.toList());
    }

    // ==================== 智能推荐引擎 ====================

    /**
     * 商品推荐：根据用户偏好标签、预算、库存状态和活动规则生成个性化推荐列表
     * <p>
     * 推荐算法详细流程（6 个步骤）：<br>
     * <b>步骤 1 - 查询候选商品：</b>筛选所有上架且未删除的商品<br>
     * <b>步骤 2 - 关联库存：</b>查询这些商品的库存记录，建立 productId → Inventory 映射<br>
     * <b>步骤 3 - 过滤不可售：</b>排除库存为 0、样品缺失（MISSING）、账实不一致（mismatchFlag）的商品<br>
     * <b>步骤 4 - 评分计算：</b>为每个可用商品计算综合得分：<br>
     * &nbsp;&nbsp;4a. 标签匹配（权重 60%）：用户意向标签与商品标签集合的交集覆盖度<br>
     * &nbsp;&nbsp;4b. 预算匹配（权重 30%）：商品价格在用户预算范围内得满分<br>
     * &nbsp;&nbsp;4c. 机器人抓取（权重 10%）：支持机器人抓取的商家加分<br>
     * &nbsp;&nbsp;4d. 仅抓取过滤：若用户要求仅展示可抓取商品，排除不支持的商品<br>
     * <b>步骤 5 - 排序取 topN：</b>按得分降序排列，取前 limit 个<br>
     * <b>步骤 6 - 组装结果：</b>生成推荐理由并返回
     * </p>
     * <p>调用链路：Controller → this.recommend → 查商品 → 查库存 → 评分 → 排序 → 组装推荐理由</p>
     *
     * @param request 推荐请求
     * @return 推荐商品列表（含匹配分和推荐理由）
     */
    @Override
    public List<ProductRecommendItem> recommend(ProductRecommendRequest request) {
        // 默认推荐数量为 10
        int limit = ObjectUtil.defaultIfNull(request.getLimit(), 10);
        ThrowUtils.throwIf(limit <= 0, PARAM_ERROR, "推荐数量必须大于0");

        // ========== 步骤 1：查询所有上架商品 ==========
        List<Product> allProducts = baseMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatusEnum.ON_SHELF)      // 仅上架商品
                        .eq(Product::getIsDeleted, 0)); // 未删除

        if (ObjectUtil.isEmpty(allProducts)) {
            return Collections.emptyList();
        }

        // ========== 步骤 2：关联查询库存信息 ==========
        // 收集所有商品 ID
        Set<Long> productIds = allProducts.stream().map(Product::getId).collect(Collectors.toSet());
        // 批量查询库存记录
        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .in(Inventory::getProductId, productIds)
                        .eq(Inventory::getIsDeleted, 0));
        // 转为 Map 方便快速查找：productId → Inventory
        Map<Long, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));

        // ========== 步骤 3：过滤不可售商品 ==========
        // 不可售条件：库存为空 / 库存为 0 / 样品缺失 / 账实不一致
        List<Product> available = allProducts.stream()
                .filter(p -> {
                    Inventory inv = inventoryMap.get(p.getId());
                    if (inv == null) return false;                       // 无库存记录 → 不可售
                    int availableStock = inv.getRealStock() - inv.getLockedStock();
                    return availableStock > 0                            // 有可用库存
                            && inv.getSampleStatus() != SampleStatusEnum.MISSING // 样品未缺失
                            && !Boolean.TRUE.equals(inv.getMismatchFlag()); // 账实一致
                })
                .collect(Collectors.toList());

        if (ObjectUtil.isEmpty(available)) {
            return Collections.emptyList();
        }

        // ========== 步骤 3b：查询低库存告警列表，用于评分扣减 ==========
        Set<Long> lowStockAlertProductIds = alertMapper.selectList(
                        new LambdaQueryWrapper<Alert>()
                                .eq(Alert::getAlertType, AlertTypeEnum.LOW_STOCK)
                                .eq(Alert::getStatus, AlertStatusEnum.UNRESOLVED)
                                .eq(Alert::getIsDeleted, 0))
                .stream()
                .map(a -> {
                    try { return Long.parseLong(a.getSourceId()); } catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ========== 步骤 4：评分计算 ==========
        // 处理用户意向标签：去空白、去空
        List<String> rawTags = request.getIntentTags();
        List<String> intentTags = (rawTags != null ? rawTags : Collections.<String>emptyList())
                .stream().filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());

        // 带评分的商品列表
        List<ProductWithScore> scored = new ArrayList<>();
        for (Product p : available) {
            int score = 0;
            List<String> matchedTags = new ArrayList<>();

            // 4a. 标签匹配（权重 60%）
            // 商品标签用逗号分隔，计算用户标签与商品标签的交集覆盖度
            if (ObjectUtil.isNotEmpty(intentTags) && StrUtil.isNotBlank(p.getTags())) {
                String[] productTagArr = p.getTags().split(",");
                for (String pt : productTagArr) {
                    String tag = pt.trim();
                    if (intentTags.contains(tag)) {
                        // 每个匹配标签的得分 = 60 / 商品标签总数（均匀分配权重）
                        score += 60 / Math.max(productTagArr.length, 1);
                        matchedTags.add(tag);
                    }
                }
            }

            // 4b. 预算匹配（权重 30%）
            boolean budgetOk = true;
            if (ObjectUtil.isNotNull(request.getBudgetMin()) && p.getPrice().compareTo(request.getBudgetMin()) < 0) {
                budgetOk = false;  // 价格低于最低预算
            }
            if (ObjectUtil.isNotNull(request.getBudgetMax()) && p.getPrice().compareTo(request.getBudgetMax()) > 0) {
                budgetOk = false;  // 价格超过最高预算
            }
            if (budgetOk) {
                score += 30;  // 价格在预算范围内得满分
            }

            // 4c. 支持机器人抓取加分（权重 10%）
            if (Boolean.TRUE.equals(p.getRobotGraspable())) {
                score += 10;
            }

            // 4d. 低库存告警扣减（存在未解决的低库存告警时扣 20 分）
            if (lowStockAlertProductIds.contains(p.getId())) {
                score -= 20;
            }

            // 4e. 仅抓取要求过滤
            // 若用户要求仅展示可机器人抓取的商品，排除不支持的商品
            if (Boolean.TRUE.equals(request.getOnlyGraspable()) && !Boolean.TRUE.equals(p.getRobotGraspable())) {
                continue;
            }

            scored.add(new ProductWithScore(p, score, matchedTags));
        }

        // ========== 步骤 5：排序取 topN ==========
        scored.sort((a, b) -> Integer.compare(b.score, a.score));       // 按得分降序
        List<ProductWithScore> top = scored.subList(0, Math.min(limit, scored.size()));

        // ========== 步骤 6：组装推荐结果 ==========
        return top.stream().map(ps -> {
            Product p = ps.product;
            ProductRecommendItem item = new ProductRecommendItem();
            BeanUtil.copyProperties(p, item);

            // == 生成推荐理由（按得分区间给出不同文案） ==
            StringBuilder reason = new StringBuilder();
            if (ps.score >= 60) {
                // 高分段：标签匹配度高
                reason.append("符合您的偏好");
                if (ObjectUtil.isNotEmpty(ps.matchedTags)) {
                    reason.append("「").append(String.join("、", ps.matchedTags)).append("」");
                }
            } else if (ps.score >= 30) {
                // 中分段：主要是预算匹配
                reason.append("价格合适");
            } else {
                // 低分段：作为热门推荐
                reason.append("热门推荐");
            }
            // 机器人抓取能力作为额外亮点
            if (Boolean.TRUE.equals(p.getRobotGraspable())) {
                reason.append("，支持机器人抓取展示");
            }
            reason.append("，库存充足");

            item.setRecommendReason(reason.toString());
            item.setMatchScore(ps.score);  // 设置匹配分，便于前端展示匹配度
            return item;
        }).collect(Collectors.toList());
    }

    // ==================== 查询条件构造 ====================

    /**
     * 构建商品查询的 MyBatis-Plus 条件构造器
     * <p>
     * 支持按 ID 精确匹配、名称模糊、标签模糊、机器人抓取、展示点位、状态精确匹配，
     * 以及价格区间范围查询。默认按创建时间降序排列。
     * </p>
     *
     * @param query 商品查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper<Product> getQueryWrapper(ProductQueryRequest query) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        // 默认只查询未删除记录
        wrapper.eq("is_deleted", 0);
        if (ObjectUtil.isNotNull(query)) {
            // ID 精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getId()), "id", query.getId());
            // 名称模糊搜索
            wrapper.like(StrUtil.isNotBlank(query.getName()), "name", query.getName());
            // 标签模糊搜索
            wrapper.like(StrUtil.isNotBlank(query.getTags()), "tags", query.getTags());
            // 机器人抓取能力匹配（先判空，避免 null 拆箱 NPE）
            if (ObjectUtil.isNotNull(query.getRobotGraspable())) {
                wrapper.eq("robot_graspable", query.getRobotGraspable() ? 1 : 0);
            }
            // 展示点位精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getDisplayPoint()), "display_point", query.getDisplayPoint());
            // 状态精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getStatus()), "status", query.getStatus());

            // 价格区间范围查询
            if (ObjectUtil.isNotNull(query.getPriceMin())) {
                wrapper.ge("price", query.getPriceMin());  // 价格 ≥ 最低价
            }
            if (ObjectUtil.isNotNull(query.getPriceMax())) {
                wrapper.le("price", query.getPriceMax());  // 价格 ≤ 最高价
            }
        }
        // 默认按创建时间降序排序（最新发布的优先）
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    // ==================== 内部类 ====================

    /**
     * 商品评分包装类（内部使用）
     * <p>用于在推荐算法中暂存商品及其评分和匹配标签列表，方便排序和组装推荐理由。</p>
     */
    @lombok.AllArgsConstructor
    private static class ProductWithScore {
        final Product product;           // 商品实体
        final int score;                 // 综合匹配得分（满分 100）
        final List<String> matchedTags;  // 匹配上的用户标签列表（用于生成推荐理由）
    }
}
