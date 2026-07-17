package com.lu.postrobotsystem.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.entity.Product;
import com.lu.postrobotsystem.model.request.product.ProductCreateRequest;
import com.lu.postrobotsystem.model.request.product.ProductQueryRequest;
import com.lu.postrobotsystem.model.request.product.ProductRecommendRequest;
import com.lu.postrobotsystem.model.request.product.ProductTagsRequest;
import com.lu.postrobotsystem.model.request.product.ProductUpdateRequest;
import com.lu.postrobotsystem.model.response.product.ProductRecommendItem;
import com.lu.postrobotsystem.model.response.product.ProductResponse;
import com.lu.postrobotsystem.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lu.postrobotsystem.exception.ResultCode.NOT_FOUND;
import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;

/**
 * 商品管理控制器。
 */
@RestController
@RequestMapping("/product")
@Tag(name = "商品管理")
@RequiredArgsConstructor
public class ProductController {

    /** 商品服务层，处理商品相关的业务逻辑 */
    private final ProductService productService;

    /**
     * 根据 ID 查询商品详情。
     * <p>
     * 查询指定 ID 的商品信息并转为响应 VO。
     * 前置校验：商品不存在时抛出 NOT_FOUND 异常。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param id 商品 ID，通过 URL 路径传递
     * @return 统一响应结果，包含 {@link ProductResponse} 商品详情
     * @throws RuntimeException 如果商品不存在，抛出 NOT_FOUND 异常
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据 id 查询商品详情")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ProductResponse> getProductById(@PathVariable Long id) {
        // 根据 ID 查询商品实体
        Product product = productService.getById(id);
        // 校验：商品不存在时抛出异常
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");
        // 将实体转为响应 VO 并返回
        return Result.success(productService.getProductVO(product));
    }

    /**
     * 分页查询商品列表。
     * <p>
     * 根据筛选条件分页返回商品列表。
     * 流程：构建查询条件 QueryWrapper → MyBatis-Plus 分页查询 → 实体列表转为 VO 分页结果。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param query    商品查询条件，支持按名称、分类、状态等筛选
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 统一响应结果，包含 {@link Page<ProductResponse>} 分页数据
     */
    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询商品列表")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Page<ProductResponse>> listProductPage(@Valid ProductQueryRequest query,
                                                         @RequestParam(defaultValue = "1") int pageNum,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        // 第一步：由 productService 根据查询条件构建 QueryWrapper
        QueryWrapper<Product> wrapper = productService.getQueryWrapper(query);
        // 第二步：执行 MyBatis-Plus 分页查询，获取商品实体分页数据
        Page<Product> productPage = productService.page(new Page<>(pageNum, pageSize), wrapper);
        // 第三步：构建同参数 VO 分页对象，将实体列表转为响应 VO 列表
        Page<ProductResponse> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productService.getProductVOList(productPage.getRecords()));
        return Result.success(voPage);
    }

    /**
     * 新增商品。
     * <p>
     * 创建新的商品记录。请求参数经过 {@code @Valid} 校验后，
     * 由 {@link ProductService#createProduct} 完成实体构建和持久化。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param request 商品创建请求体，包含商品名称、分类、价格、描述等信息
     * @return 统一响应结果，包含新创建商品的 ID，附带"新增成功"提示
     */
    @PostMapping("/add")
    @Operation(summary = "新增商品")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Long> addProduct(@Valid @RequestBody ProductCreateRequest request) {
        // 调用 productService 创建商品，返回新商品 ID
        Long productId = productService.createProduct(request);
        return Result.success(productId, "新增成功");
    }

    /**
     * 编辑商品信息。
     * <p>
     * 更新已有商品的基本信息，如名称、分类、价格、描述等。
     * 请求参数经过 {@code @Valid} 校验，由 {@link ProductService#updateProduct} 完成更新逻辑。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param request 商品更新请求体，包含待更新商品的 ID 及字段
     * @return 统一响应结果，附带"更新成功"提示
     */
    @PutMapping("/edit")
    @Operation(summary = "编辑商品信息")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> editProduct(@Valid @RequestBody ProductUpdateRequest request) {
        // 调用 productService 执行商品信息更新
        productService.updateProduct(request);
        return Result.success(null, "更新成功");
    }

    /**
     * 上架/下架商品。
     * <p>
     * 通过 PATCH 请求修改商品上下架状态。
     * status 参数为 1 表示上架，其他值表示下架。
     * ADMIN 和 OPERATOR 角色可访问。
     * </p>
     *
     * @param id     商品 ID，通过 URL 路径传递
     * @param status 目标状态：1 上架，其他值下架
     * @return 统一响应结果，附带"上架成功"或"下架成功"提示
     */
    @PostMapping("/status/{id}")
    @Operation(summary = "上架/下架商品")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 调用 productService 执行上下架操作，内部修改商品状态字段
        productService.changeProductStatus(id, status);
        // 根据状态码返回不同的操作成功提示
        String msg = status == 1 ? "上架成功" : "下架成功";
        return Result.success(null, msg);
    }

    /**
     * 更新商品标签。
     * <p>
     * 专门用于更新商品标签的接口，与全量编辑分离。
     * 只修改标签字段，不影响商品其他信息。
     * </p>
     *
     * @param request 标签更新请求，包含商品 ID 和新的标签字符串
     * @return 统一响应结果，附带"标签更新成功"提示
     */
    @PostMapping("/tags")
    @Operation(summary = "更新商品标签")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> updateTags(@Valid @RequestBody ProductTagsRequest request) {
        productService.updateTags(request.getId(), request.getTags());
        return Result.success(null, "标签更新成功");
    }

    /**
     * 商品推荐。
     * <p>
     * 根据用户的偏好标签和预算上限，获取商品推荐列表。
     * 此接口对前端公开，无需管理员权限。
     * 推荐算法在 {@link ProductService#recommend} 中实现，
     * 基于标签匹配和价格过滤生成推荐结果。
     * </p>
     *
     * @param request 推荐请求体，包含偏好标签列表和预算上限
     * @return 统一响应结果，包含 {@link List<ProductRecommendItem>} 推荐商品列表
     */
    @PostMapping("/recommend")
    @Operation(summary = "商品推荐：按偏好标签和预算获取推荐列表")
    public Result<List<ProductRecommendItem>> recommend(@RequestBody ProductRecommendRequest request) {
        // 调用 productService 执行推荐逻辑，返回推荐商品列表
        List<ProductRecommendItem> items = productService.recommend(request);
        return Result.success(items);
    }

    /**
     * 删除商品（逻辑删除）。
     * <p>
     * 将指定 ID 的商品标记为已删除（逻辑删除），而非物理删除。
     * 前置校验：商品 ID 不能为空，且对应商品必须存在。
     * 仅 ADMIN 角色可访问此接口。
     * </p>
     *
     * @param id 待删除商品的 ID，通过 URL 路径传递
     * @return 统一响应结果，附带"删除成功"提示
     * @throws RuntimeException 如果 ID 为空或商品不存在，抛出对应异常
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除商品（逻辑删除）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        // 前置校验：商品 ID 不能为空
        ThrowUtils.throwIf(ObjectUtil.isNull(id), PARAM_ERROR, "商品ID不能为空");
        // 查询商品是否存在
        Product product = productService.getById(id);
        // 校验：商品不存在时抛出异常
        ThrowUtils.throwIf(ObjectUtil.isNull(product), NOT_FOUND, "商品不存在");
        // 执行逻辑删除（MyBatis-Plus 逻辑删除）
        productService.removeById(id);
        return Result.success(null, "删除成功");
    }
}
