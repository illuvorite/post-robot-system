package com.lu.postrobotsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lu.postrobotsystem.model.entity.User;
import com.lu.postrobotsystem.model.request.user.UserEditRequest;
import com.lu.postrobotsystem.model.request.user.UserQueryRequest;
import com.lu.postrobotsystem.model.response.user.UserLoginResponse;
import com.lu.postrobotsystem.model.response.user.UserResponse;
import com.lu.postrobotsystem.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     * <p>
     * 支持多种登录方式：用户名、邮箱、手机号三选一。
     * 校验账号状态和密码后，生成双令牌（accessToken + refreshToken）并存入 Redis。
     * </p>
     * <p>调用链路：Controller → UserService.userLogin → 查询用户 → 校验密码 → buildLoginResponse → 返回令牌</p>
     *
     * @param userAccount  用户名（与 userEmail、userPhone 三选一）
     * @param userEmail    邮箱（与 userAccount、userPhone 三选一）
     * @param userPhone    手机号（与 userAccount、userEmail 三选一）
     * @param userPassword 登录密码
     * @param request      HTTP 请求（用于获取客户端 IP）
     * @return 登录响应，包含用户信息、accessToken 和 refreshToken
     */
    UserLoginResponse userLogin(String userAccount, String userEmail, String userPhone, String userPassword, HttpServletRequest request);

    /**
     * 用户注册
     * <p>
     * 注册时需校验：账号、密码、确认密码三者均不能为空；
     * 两次密码必须一致；密码长度不少于 6 位；账号不能重复。
     * 默认角色为 OPERATOR（操作员），默认状态为启用。
     * </p>
     * <p>调用链路：Controller → UserService.userRegister → 参数校验 → 查重 → BCrypt 加密 → save → 返回用户 ID</p>
     *
     * @param userAccount   用户名
     * @param userPassword  密码（明文，内部使用 BCrypt 加密存储）
     * @param checkPassword 确认密码（需与 userPassword 一致）
     * @return 注册用户的 ID
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户注销
     * <p>
     * 从请求头中提取 accessToken，删除 Redis 中的会话记录，
     * 同时将当前 token 加入黑名单（TTL 为 token 剩余有效期），防止登出后 token 被继续使用。
     * </p>
     * <p>调用链路：Controller → UserService.userLogout → extractToken → 删除 Redis 会话 → 加入黑名单</p>
     *
     * @param request HTTP 请求，用于提取 Authorization 头中的 Bearer token
     * @return true=注销成功，false=未提供有效 token
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 刷新令牌
     * <p>
     * 使用 refreshToken 换取新的 accessToken + refreshToken（令牌轮换机制）。
     * 校验 refreshToken 的 JWT 签名和 Redis 中是否存在映射，
     * 校验通过后删除旧 refreshToken，生成新令牌。
     * </p>
     * <p>调用链路：AuthController.refresh → UserService.refreshToken → 校验 token → 查询 Redis → 校验用户 → buildLoginResponse</p>
     *
     * @param refreshToken 刷新令牌字符串
     * @return 新的登录响应，包含新 accessToken 和 refreshToken
     */
    UserLoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前登录用户
     * <p>
     * 从请求头提取 accessToken，依次进行：黑名单检查 → Redis 会话检查 → JWT 签名验证 → 用户状态验证。
     * 任何环节不通过即抛出未登录异常。
     * </p>
     * <p>调用链路：各 Controller 获取当前用户 → UserService.getLoginUser → extractToken → 黑名单校验 → Redis 会话校验 → 数据库校验 → 返回 User</p>
     *
     * @param request HTTP 请求
     * @return 当前登录的用户实体
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 将用户实体转换为用户视图对象（VO）
     * <p>转换过程中排除密码等敏感字段。</p>
     *
     * @param user 用户实体
     * @return 用户视图对象，若入参为 null 则返回 null
     */
    UserResponse getUserVO(User user);

    /**
     * 批量将用户实体列表转换为用户视图对象列表
     *
     * @param userList 用户实体列表
     * @return 用户视图对象列表，若入参为空则返回空列表
     */
    List<UserResponse> getUserVOList(List<User> userList);

    /**
     * 编辑用户信息/修改密码。
     * <p>
     * 权限校验：管理员可编辑任意用户，普通用户只能编辑自己的信息。
     * 仅当请求中提供了新值时才覆盖对应字段，避免空值覆盖已有数据。
     * 修改密码时需要验证当前密码的正确性。
     * </p>
     *
     * @param request        编辑请求体，包含要修改的字段
     * @param currentUserId  当前登录用户的 ID
     * @param isAdmin        当前用户是否为管理员
     */
    void updateUser(UserEditRequest request, Long currentUserId, boolean isAdmin);

    /**
     * 构建用户查询的 MyBatis-Plus 条件构造器
     * <p>支持按 ID、用户名、真实姓名、手机号、角色、状态等字段过滤，默认按创建时间降序排序。</p>
     *
     * @param userQueryRequest 用户查询请求对象
     * @return 查询条件包装器 {@link QueryWrapper}
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
