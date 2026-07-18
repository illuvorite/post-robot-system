package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.Product;
import com.lu.postrobotsystem.model.request.product.ProductCreateRequest;
import com.lu.postrobotsystem.model.request.product.ProductQueryRequest;
import com.lu.postrobotsystem.model.request.product.ProductRecommendRequest;
import com.lu.postrobotsystem.model.request.product.ProductUpdateRequest;
import com.lu.postrobotsystem.model.response.product.ProductRecommendItem;
import com.lu.postrobotsystem.model.response.product.ProductResponse;
import com.lu.postrobotsystem.service.impl.ProductServiceImpl;

import java.util.List;

/**
 * 商品服务接口
 * <p>
 * 定义商品管理的核心业务操作，包括：商品创建、编辑、上下架、查询以及智能推荐。
 * 商品与库存模块紧密关联——商品创建时自动初始化库存记录，推荐引擎需结合库存状态筛选可售商品。
 * </p>
 *
 * <p>
 * <b>商品状态机：</b><br>
 * status = 0（下架）：商品不可见、不可购买<br>
 * status = 1（上架）：商品可被推荐引擎召回、可被购买<br>
 * </p>
 *
 * @see ProductServiceImpl
 */
public interface ProductService extends IService<Product> {

    /**
     * 新增商品
     * <p>
     * 校验商品名称是否重复，设置默认值（如 robotGraspable 默认 false、status 默认 0 下架），
     * 保存商品记录后返回自增主键 ID。
     * </p>
     * <p>调用链路：Controller → ProductService.createProduct → 名称查重 → save → 返回 ID</p>
     *
     * @param request 商品创建请求，包含名称、描述、标签、价格等信息
     * @return 新增商品的 ID
     */
    Long createProduct(ProductCreateRequest request);

    /**
     * 编辑商品
     * <p>
     * 支持部分字段更新，仅对请求中非空的字段进行修改。
     * 若修改名称，需检查新名称是否与其他商品重复。
     * </p>
     * <p>调用链路：Controller → ProductService.updateProduct → 查询商品 → 按需设值 → updateById</p>
     *
     * @param request 商品更新请求，包含待修改的字段
     * @return void
     */
    void updateProduct(ProductUpdateRequest request);

    /**
     * 上架/下架商品
     * <p>
     * 简单的状态切换操作，仅允许 0（下架）和 1（上架）两个合法值。
     * </p>
     * <p>调用链路：Controller → ProductService.changeProductStatus → 校验 → updateById</p>
     *
     * @param id     商品ID
     * @param status 目标状态：0-下架，1-上架
     * @return void
     */
    void changeProductStatus(Long id, Integer status);

    /**
     * 更新商品标签。
     * <p>
     * 专门用于更新商品标签的接口，与全量编辑分离，
     * 前端可以只传标签字段进行局部更新。
     * </p>
     *
     * @param id   商品ID
     * @param tags 标签字符串（逗号分隔）
     */
    void updateTags(Long id, String tags);

    /**
     * 将商品实体转换为商品视图对象（VO）
     * <p>使用 BeanUtil 进行属性拷贝，不包含复杂关联查询。</p>
     *
     * @param product 商品实体
     * @return 商品视图对象，若入参为 null 则返回 null
     */
    ProductResponse getProductVO(Product product);

    /**
     * 批量将商品实体列表转换为商品视图对象列表
     *
     * @param productList 商品实体列表
     * @return 商品视图对象列表，若入参为空则返回空列表
     */
    List<ProductResponse> getProductVOList(List<Product> productList);

    /**
     * 商品推荐：根据用户偏好标签、预算、库存状态和活动规则生成个性化推荐列表
     * <p>
     * 推荐算法逻辑：<br>
     * 1. 查询所有上架商品<br>
     * 2. 关联库存表，过滤不可售商品（库存为 0 / 样品缺失 / 账实不一致）<br>
     * 3. 按标签匹配度打分（权重 60%）<br>
     * 4. 按预算匹配度加分（权重 30%）<br>
     * 5. 支持机器人抓取加分（权重 10%）<br>
     * 6. 按总分降序排序，取 topN<br>
     * 7. 组装推荐理由并返回
     * </p>
     * <p>调用链路：Controller → ProductService.recommend → 查询商品 → 查询库存 → 评分 → 排序 → 组装结果</p>
     *
     * @param request 推荐请求，包含用户意向标签、预算范围、筛选条件等
     * @return 推荐商品列表，每项包含商品信息和推荐理由
     */
    List<ProductRecommendItem> recommend(ProductRecommendRequest request);

    /**
     * 构建商品查询的 MyBatis-Plus 条件构造器
     * <p>支持按 ID、名称（模糊）、标签（模糊）、机器人抓取能力、展示点位、状态、价格区间等字段过滤，默认按创建时间降序排序。</p>
     *
     * @param query 商品查询请求对象
     * @return 查询条件包装器 {@link QueryWrapper}
     */
    QueryWrapper<Product> getQueryWrapper(ProductQueryRequest query);
}
