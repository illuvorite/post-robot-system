package com.lu.postrobotsystem.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
 * <p>
 * 负责处理用户管理相关的 HTTP 请求，包括用户查询、个人信息编辑、密码修改、用户删除和新增用户。
 * 该控制器是系统用户模块的管理入口，与 {@link UserService} 协作完成业务逻辑。
 * 区分普通用户和管理员权限：
 * <ul>
 *   <li>普通用户可以查询和编辑个人信息</li>
 *   <li>管理员可以分页查询用户列表、删除用户和新增用户</li>
 * </ul>
 * </p>
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
     * 当前登录用户可以修改自己的真实姓名、手机号、邮箱和密码。
     * 权限控制：只能修改自己的信息，不能修改他人信息。
     * 修改密码时需要验证当前密码的正确性，新密码长度不能少于 6 位。
     * 密码使用 BCrypt 加密存储。
     * </p>
     *
     * @param request        编辑请求体，可包含 id、realName、phone、email、password、newPassword
     * @param authentication 当前认证信息，用于获取当前用户 ID
     * @return 统一响应结果，附带"更新成功"提示
     * @throws BusinessException 如果不是修改自己的信息，抛出 FORBIDDEN 异常
     * @throws RuntimeException  如果当前密码错误或新密码格式不符合要求，抛出 PARAM_ERROR 异常
     */
    @PutMapping("/edit")
    @Operation(summary = "编辑个人信息/修改密码")
    public Result<Void> updateUser(@Valid @RequestBody UserEditRequest request,
                                   Authentication authentication) {
        // 从认证信息中提取当前用户 ID
        Long currentUserId = Long.parseLong(authentication.getName());
        ThrowUtils.throwIf(ObjectUtil.isNull(currentUserId), PARAM_ERROR, "当前用户ID不能为空");

        // 确定目标用户 ID：request 中未指定则默认为当前用户
        Long targetId = ObjectUtil.defaultIfNull(request.getId(), currentUserId);
        // 权限校验：只能编辑自己的信息
        ThrowUtils.throwIf(!currentUserId.equals(targetId), ResultCode.FORBIDDEN, "无权限修改其他用户信息");

        // 查询目标用户实体
        User user = userService.getById(targetId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), PARAM_ERROR, "用户不存在");

        // 更新基本信息：真实姓名、手机号、邮箱（仅当请求中提供了新值时覆盖）
        if (StrUtil.isNotBlank(request.getRealName())) user.setRealName(request.getRealName());
        if (StrUtil.isNotBlank(request.getPhone())) user.setPhone(request.getPhone());
        if (StrUtil.isNotBlank(request.getEmail())) user.setEmail(request.getEmail());

        // 处理密码修改逻辑
        if (StrUtil.isNotBlank(request.getNewPassword())) {
            // 验证当前密码不能为空
            ThrowUtils.throwIf(StrUtil.isBlank(request.getPassword()), PARAM_ERROR, "当前密码不能为空");
            // 验证当前密码是否正确（使用 BCrypt 校验）
            ThrowUtils.throwIf(!BCrypt.checkpw(request.getPassword(), user.getPassword()), PARAM_ERROR, "当前密码错误");
            // 验证新密码长度是否符合要求
            ThrowUtils.throwIf(request.getNewPassword().length() < 6, PARAM_ERROR, "新密码长度不能少于6位");
            // BCrypt 加密新密码并更新
            user.setPassword(BCrypt.hashpw(request.getNewPassword()));
        }

        // 持久化更新到数据库
        userService.updateById(user);
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
     * 管理员可以为系统添加新用户。
     * 请求中只需提供用户名等基本信息，密码默认初始化为 "123456"（BCrypt 加密），
     * 用户状态默认设置为启用（status=1）。
     * 仅 ADMIN 角色可访问。
     * </p>
     *
     * @param request 新增用户请求体，包含用户名等基本信息
     * @return 统一响应结果，包含新用户的 ID，附带"新增成功"提示
     * @throws RuntimeException 如果请求为空或用户名为空，抛出 PARAM_ERROR 异常
     */
    @PostMapping("/add")
    @Operation(summary = "新增用户（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> addUser(@Valid @RequestBody UserAddRequest request) {
        // 前置校验：请求体和用户名不能为空
        ThrowUtils.throwIf(ObjectUtil.isNull(request), PARAM_ERROR, "新增请求不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getUsername()), PARAM_ERROR, "账号不能为空");

        // 从请求中复制属性到 User 实体（排除 password 字段，使用默认密码）
        User user = new User();
        BeanUtil.copyProperties(request, user, "password");
        // 设置默认密码 "123456"（BCrypt 加密）
        user.setPassword(BCrypt.hashpw("123456"));
        // 默认启用状态
        user.setStatus(1);

        // 保存用户到数据库
        userService.save(user);
        return Result.success(user.getId(), "新增成功");
    }
}
