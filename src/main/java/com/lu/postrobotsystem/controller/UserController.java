package com.lu.postrobotsystem.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lu.postrobotsystem.common.Result;
import com.lu.postrobotsystem.exception.BusinessException;
import com.lu.postrobotsystem.exception.ResultCode;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.model.entity.User;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import com.lu.postrobotsystem.model.request.user.UserAddRequest;
import com.lu.postrobotsystem.model.request.user.UserEditRequest;
import com.lu.postrobotsystem.model.request.user.UserQueryRequest;
import com.lu.postrobotsystem.model.response.user.UserResponse;
import com.lu.postrobotsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
@RequiredArgsConstructor
public class UserController {

    /** 用户服务层，处理用户管理的核心业务逻辑 */
    private final UserService userService;

    /**
     * 根据 ID 查询用户详情。
     * <p>
     * 权限控制：管理员可以查看任意用户信息，普通用户只能查看自己的信息。
     * 如果是普通用户尝试查看其他用户的信息，则抛出 FORBIDDEN 异常。
     * 用户身份通过 Spring Security 的 {@link Authentication} 获取。
     * </p>
     *
     * @param id             目标用户 ID，通过 URL 路径传递
     * @param authentication 当前认证信息，从中提取当前用户的 ID 和角色
     * @return 统一响应结果，包含 {@link UserResponse} 用户详情
     * @throws BusinessException 如果非管理员查看他人信息，抛出 FORBIDDEN 异常
     * @throws RuntimeException  如果用户不存在，抛出 NOT_FOUND 异常
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据 id 查询用户详情")
    public Result<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        // 判断当前用户是否为 ADMIN 角色
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + UserRoleEnum.ADMIN.getValue()));
        // 从认证信息中提取当前用户的 ID
        Long currentUserId = Long.parseLong(authentication.getName());
        // 非管理员不能查看其他用户的信息
        if (!isAdmin && !currentUserId.equals(id)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看其他用户信息");
        }
        // 根据 ID 查询用户实体
        User user = userService.getById(id);
        // 校验：用户不存在时报错
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ResultCode.NOT_FOUND, "用户不存在");
        // 将用户实体转为响应 VO 并返回
        return Result.success(userService.getUserVO(user));
    }

    /**
     * 获取当前登录用户信息。
     * <p>
     * 从 Spring Security 的 {@link Authentication} 中提取当前用户 ID，
     * 然后查询并返回用户的详细信息。
     * 无需管理员权限，所有登录用户均可调用。
     * </p>
     *
     * @param authentication 当前认证信息，包含当前用户的 ID
     * @return 统一响应结果，包含当前用户的 {@link UserResponse} 信息
     * @throws RuntimeException 如果用户不存在，抛出 NOT_FOUND 异常
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserResponse> getCurrentUser(Authentication authentication) {
        // 从认证信息中提取当前用户 ID
        Long currentUserId = Long.parseLong(authentication.getName());
        // 根据 ID 查询用户实体
        User user = userService.getById(currentUserId);
        // 校验：用户不存在时报错
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ResultCode.NOT_FOUND, "用户不存在");
        // 转为响应 VO 并返回
        return Result.success(userService.getUserVO(user));
    }

    /**
     * 分页查询用户列表（管理员专用）。
     * <p>
     * 根据筛选条件分页返回用户列表。
     * 流程：构建查询条件 QueryWrapper → MyBatis-Plus 分页查询 → 实体列表转为 VO 分页结果。
     * 仅 ADMIN 角色可访问。
     * </p>
     *
     * @param query    用户查询条件，支持按用户名、邮箱、角色等筛选
     * @param pageNum  当前页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 统一响应结果，包含 {@link Page<UserResponse>} 用户分页数据
     */
    @GetMapping("/list/page/vo")
    @Operation(summary = "分页查询用户列表（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<UserResponse>> listUserPage(@Valid UserQueryRequest query,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        // 第一步：由 userService 根据查询条件构建 QueryWrapper
        QueryWrapper<User> wrapper = userService.getQueryWrapper(query);
        // 第二步：执行 MyBatis-Plus 分页查询，获取用户实体分页数据
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), wrapper);
        // 第三步：构建同参数 VO 分页对象，将实体列表转为响应 VO 列表
        Page<UserResponse> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userService.getUserVOList(userPage.getRecords()));
        return Result.success(voPage);
    }

    /**
     * 编辑个人信息/修改密码。
     * <p>
     * 权限控制：管理员可编辑任意用户，普通用户只能编辑自己的信息。
     * 仅当请求中提供了新值时才覆盖对应字段，避免空值覆盖已有数据。
     * 业务逻辑委托给 {@link UserService#updateUser} 处理。
     * </p>
     *
     * @param request        编辑请求体，可包含 id、realName、phone、email、password、newPassword
     * @param authentication 当前认证信息，用于获取当前用户 ID 和角色
     * @return 统一响应结果，附带"更新成功"提示
     */
    @PutMapping("/edit")
    @Operation(summary = "编辑个人信息/修改密码")
    public Result<Void> updateUser(@Valid @RequestBody UserEditRequest request,
                                   Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + UserRoleEnum.ADMIN.getValue()));

        userService.updateUser(request, currentUserId, isAdmin);
        return Result.success(null, "更新成功");
    }

    /**
     * 删除用户（逻辑删除）。
     * <p>
     * 将指定 ID 的用户标记为已删除（逻辑删除），而非物理删除。
     * 前置校验：用户 ID 不能为空，且对应用户必须存在。
     * 仅 ADMIN 角色可访问。
     * </p>
     *
     * @param id 待删除用户的 ID，通过 URL 路径传递
     * @return 统一响应结果，附带"删除成功"提示
     * @throws RuntimeException 如果 ID 为空或用户不存在，抛出对应异常
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户（逻辑删除）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        // 前置校验：用户 ID 不能为空
        ThrowUtils.throwIf(ObjectUtil.isNull(id), PARAM_ERROR, "用户ID不能为空");
        // 查询用户是否存在
        User user = userService.getById(id);
        // 校验：用户不存在时报错
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ResultCode.NOT_FOUND, "用户不存在");
        // 执行逻辑删除（MyBatis-Plus 逻辑删除）
        userService.removeById(id);
        return Result.success(null, "删除成功");
    }

    /**
     * 新增用户（管理员专用）。
     * <p>
     * 管理员可以为系统添加新用户，需提供用户名和角色等必填信息。
     * 密码默认为 "123456"（BCrypt 加密），用户状态默认启用（status=1）。
     * 仅 ADMIN 角色可访问。
     * </p>
     *
     * @param request 新增用户请求体，包含用户名、角色等必填信息
     * @return 统一响应结果，包含新用户的 ID，附带"新增成功"提示
     * @throws BusinessException 如果账号已存在，抛出 DATA_ALREADY_EXIST 异常
     */
    @PostMapping("/add")
    @Operation(summary = "新增用户（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> addUser(@Valid @RequestBody UserAddRequest request) {
        // 检查账号是否已被注册
        boolean exists = userService.lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getIsDeleted, 0)
                .count() > 0;
        ThrowUtils.throwIf(exists, ResultCode.DATA_ALREADY_EXIST, "账号已存在");

        // 从请求中复制属性到 User 实体（排除 password 字段，使用默认密码）
        User user = new User();
        BeanUtil.copyProperties(request, user, "password");
        // 默认密码 "123456"（BCrypt 加密）
        user.setPassword(BCrypt.hashpw("123456"));
        // 默认启用状态
        user.setStatus(1);
        // 显式设置未删除（避免 @TableLogic 查不到新用户）
        user.setIsDeleted(0);

        // 保存用户到数据库
        userService.save(user);
        return Result.success(user.getId(), "新增成功");
    }
}
