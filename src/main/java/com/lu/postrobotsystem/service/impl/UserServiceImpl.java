package com.lu.postrobotsystem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.common.util.JwtUtils;
import com.lu.postrobotsystem.constant.RedisKeyConstants;
import com.lu.postrobotsystem.exception.ResultCode;
import com.lu.postrobotsystem.exception.ThrowUtils;
import com.lu.postrobotsystem.mapper.UserMapper;
import com.lu.postrobotsystem.model.entity.User;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import com.lu.postrobotsystem.model.request.user.UserQueryRequest;
import com.lu.postrobotsystem.model.response.user.UserLoginResponse;
import com.lu.postrobotsystem.model.response.user.UserResponse;
import com.lu.postrobotsystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lu.postrobotsystem.exception.ResultCode.PARAM_ERROR;

/**
 * 用户服务实现类
 * <p>
 * 实现用户认证与管理的全部业务逻辑。系统采用 <b>JWT 双令牌 + Redis 缓存</b>的认证架构：
 * </p>
 *
 * <p>
 * <b>认证架构说明：</b><br>
 * <ul>
 *   <li><b>accessToken（短期令牌）：</b>有效期较短（由 JwtUtils 配置），用于接口鉴权。存储在 Redis 会话中，支持服务端主动失效。</li>
 *   <li><b>refreshToken（刷新令牌）：</b>有效期较长，用于无感刷新 accessToken。采用令牌轮换机制，每次刷新旧令牌立即失效。</li>
 *   <li><b>黑名单机制：</b>用户注销时，accessToken 会被加入 Redis 黑名单，在剩余有效期内不可用。</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>核心业务流程与方法调用关系：</b><br>
 * <pre>
 * ┌─ 登录 ──────────────────────────────────────────────────┐
 * │ userLogin()                                              │
 * │   ├─ 多条件查询用户（用户名 / 邮箱 / 手机号）             │
 * │   ├─ BCrypt 校验密码                                      │
 * │   └─ buildLoginResponse()                                │
 * │        ├─ jwtUtils.generateAccessToken()                 │
 * │        ├─ jwtUtils.generateRefreshToken()                │
 * │        ├─ 存 Redis：LOGIN_TOKEN_KEY + accessToken → 会话  │
 * │        └─ 存 Redis：LOGIN_REFRESH_KEY + refreshToken → 用户ID│
 * │                                                          │
 * ├─ 注册 ────────────────────────────────────────────────────┤
 * │ userRegister()                                           │
 * │   ├─ 参数校验（密码一致性、长度等）                       │
 * │   ├─ 账号查重                                            │
 * │   └─ BCrypt 加密 → save                                  │
 * │                                                          │
 * ├─ 注销 ────────────────────────────────────────────────────┤
 * │ userLogout()                                             │
 * │   ├─ 删除 Redis 会话                                     │
 * │   └─ 将 accessToken 加入黑名单（TTL = 剩余有效期）         │
 * │                                                          │
 * ├─ 令牌刷新 ────────────────────────────────────────────────┤
 * │ refreshToken()                                           │
 * │   ├─ 校验 refreshToken 签名                               │
 * │   ├─ 校验 Redis 映射                                      │
 * │   ├─ 删除旧 refreshToken                                  │
 * │   └─ buildLoginResponse() 生成新双令牌                    │
 * │                                                          │
 * └─ 获取当前用户 ────────────────────────────────────────────┤
 *   getLoginUser()                                           │
 *     ├─ extractToken() 从请求头提取 Bearer token             │
 *     ├─ 检查黑名单                                           │
 *     ├─ 检查 Redis 会话                                      │
 *     └─ 检查用户状态（存在、未删除、未停用）                  │
 * </pre>
 * </p>
 *
 * @see UserService
 * @see UserMapper
 * @see JwtUtils
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 登录 ====================

    /**
     * 用户登录
     * <p>
     * 业务流程：<br>
     * 1. 校验参数（账号/邮箱/手机号至少一个不为空，密码不为空）<br>
     * 2. 根据登录方式构造查询条件（用户名 / 邮箱 / 手机号三选一）<br>
     * 3. 查询用户并校验：用户是否存在、账号是否停用<br>
     * 4. BCrypt 校验密码<br>
     * 5. 调用 {@link #buildLoginResponse(User, String)} 生成双令牌并存储 Redis 会话
     * </p>
     * <p>调用链路：AuthController.login → this.userLogin → 查询用户 → 校验密码 → buildLoginResponse → 返回令牌</p>
     *
     * @param userAccount  用户名
     * @param userEmail    邮箱
     * @param userPhone    手机号
     * @param userPassword 登录密码
     * @param request      HTTP 请求（用于记录客户端 IP）
     * @return 登录响应，包含用户信息和双令牌
     */
    @Override
    public UserLoginResponse userLogin(String userAccount, String userEmail, String userPhone,
                                       String userPassword, HttpServletRequest request) {
        // === 参数校验：账号/邮箱/手机号至少提供一个 ===
        ThrowUtils.throwIf(StrUtil.isBlank(userAccount) && StrUtil.isBlank(userEmail) && StrUtil.isBlank(userPhone),
                PARAM_ERROR, "账号/邮箱/手机号不能都为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userPassword), PARAM_ERROR, "密码不能为空");

        // === 构造查询条件：按优先级匹配（账号 → 邮箱 → 手机号） ===
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0);
        if (StrUtil.isNotBlank(userAccount)) {
            wrapper.eq(User::getUsername, userAccount);
        } else if (StrUtil.isNotBlank(userEmail)) {
            wrapper.eq(User::getEmail, userEmail);
        } else if (StrUtil.isNotBlank(userPhone)) {
            wrapper.eq(User::getPhone, userPhone);
        }

        // === 查询用户 ===
        User user = baseMapper.selectOne(wrapper);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), PARAM_ERROR, "账号/邮箱/手机号不存在");
        ThrowUtils.throwIf(ObjectUtil.equal(user.getStatus(), 0), PARAM_ERROR, "账号已停用");

        // === BCrypt 校验密码 ===
        boolean passwordMatch = BCrypt.checkpw(userPassword, user.getPassword());
        ThrowUtils.throwIf(!passwordMatch, PARAM_ERROR, "密码错误");

        // === 生成双令牌并返回 ===
        return buildLoginResponse(user, getClientIp(request));
    }

    // ==================== 注册 ====================

    /**
     * 用户注册
     * <p>
     * 业务流程：<br>
     * 1. 校验参数（账号、密码、确认密码均不为空）<br>
     * 2. 校验两次密码一致<br>
     * 3. 校验密码长度 >= 6 位<br>
     * 4. 检查账号是否已存在<br>
     * 5. BCrypt 加密密码 → 创建用户（默认角色 OPERATOR，默认状态启用）→ save
     * </p>
     * <p>调用链路：AuthController.register → this.userRegister → 参数校验 → 查重 → 加密 → save → 返回 ID</p>
     *
     * @param userAccount   用户名
     * @param userPassword  密码（明文）
     * @param checkPassword 确认密码
     * @return 注册用户的 ID
     */
    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // === 参数基础校验 ===
        ThrowUtils.throwIf(StrUtil.isBlank(userAccount), PARAM_ERROR, "账号不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userPassword), PARAM_ERROR, "密码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(checkPassword), PARAM_ERROR, "确认密码不能为空");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), PARAM_ERROR, "两次密码不一致");
        ThrowUtils.throwIf(userPassword.length() < 6, PARAM_ERROR, "密码长度不能少于6位");

        // === 检查账号是否已被注册 ===
        boolean exists = baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, userAccount)
                        .eq(User::getIsDeleted, 0)
        ) > 0;
        ThrowUtils.throwIf(exists, PARAM_ERROR, "账号已存在");

        // === 创建用户（BCrypt 加密存储密码，默认角色为操作员） ===
        User user = new User()
                .setUsername(userAccount)
                .setPassword(BCrypt.hashpw(userPassword))    // BCrypt 加密（不可逆）
                .setRole(UserRoleEnum.OPERATOR)              // 默认角色：操作员
                .setStatus(1);                                // 默认状态：启用
        save(user);
        return user.getId();
    }

    // ==================== 注销 ====================

    /**
     * 用户注销
     * <p>
     * 业务流程：<br>
     * 1. 从请求头提取 Bearer token<br>
     * 2. 删除 Redis 中的登录会话<br>
     * 3. 将 token 加入 Redis 黑名单（TTL = token 剩余有效期），防止登出后被冒用
     * </p>
     * <p>调用链路：AuthController.logout → this.userLogout → extractToken → 删除 Redis 会话 → 加入黑名单</p>
     *
     * @param request HTTP 请求
     * @return true=注销成功，false=未提供有效 token
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            return false;  // 未携带 token，无需注销
        }

        // === 步骤 1：从 Redis 删除 accessToken 会话 ===
        stringRedisTemplate.delete(RedisKeyConstants.LOGIN_TOKEN_KEY + token);

        // === 步骤 2：将 token 加入黑名单，TTL 设为 token 的剩余有效期 ===
        try {
            long remainingTtl = jwtUtils.getAccessExpiration();  // 默认剩余有效期
            if (jwtUtils.validateToken(token)) {
                // 若 token 仍未过期，计算精确的剩余有效期
                long exp = jwtUtils.getClaims(token).getExpiration().getTime();
                remainingTtl = Math.max(1, (exp - System.currentTimeMillis()) / 1000);
            }
            // 将 token 加入黑名单（"1" 为占位值，无实际含义）
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.LOGIN_BLACKLIST_KEY + token, "1",
                    remainingTtl, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // token 已过期，无需加入黑名单（过期 token 本身已不可用）
        }

        return true;
    }

    // ==================== 令牌刷新 ====================

    /**
     * 刷新令牌（令牌轮换）
     * <p>
     * 业务流程：<br>
     * 1. 校验 refreshToken 不能为空<br>
     * 2. 校验 refreshToken 的 JWT 签名是否有效<br>
     * 3. 校验 Redis 中是否存在该 refreshToken → userId 的映射<br>
     * 4. 查询用户并校验状态<br>
     * 5. 删除旧的 refreshToken 映射<br>
     * 6. 调用 {@link #buildLoginResponse(User, String)} 生成全新的双令牌
     * </p>
     * <p>每次刷新都会轮换令牌（旧 refreshToken 作废），增强安全性。</p>
     * <p>调用链路：AuthController.refresh → this.refreshToken → 校验 → 查 Redis → 查用户 → buildLoginResponse</p>
     *
     * @param refreshToken 刷新令牌字符串
     * @return 新的登录响应，包含新 accessToken 和 refreshToken
     */
    @Override
    public UserLoginResponse refreshToken(String refreshToken) {
        // === 步骤 1：参数校验 ===
        ThrowUtils.throwIf(StrUtil.isBlank(refreshToken), ResultCode.NOT_LOGIN_ERROR, "刷新令牌不能为空");

        // === 步骤 2：验证 refreshToken 的 JWT 签名 ===
        ThrowUtils.throwIf(!jwtUtils.validateToken(refreshToken), ResultCode.NOT_LOGIN_ERROR, "刷新令牌无效或已过期");

        // === 步骤 3：验证 Redis 中是否存在该 refreshToken 的映射 ===
        String userIdStr = stringRedisTemplate.opsForValue().get(
                RedisKeyConstants.LOGIN_REFRESH_KEY + refreshToken);
        ThrowUtils.throwIf(StrUtil.isBlank(userIdStr), ResultCode.NOT_LOGIN_ERROR, "刷新令牌已失效，请重新登录");

        // === 步骤 4：查询用户并校验状态 ===
        Long userId = Long.parseLong(userIdStr);
        User user = getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), PARAM_ERROR, "用户不存在");
        ThrowUtils.throwIf(ObjectUtil.equal(user.getStatus(), 0), PARAM_ERROR, "账号已停用");

        // === 步骤 5：删除旧的 refreshToken 映射（令牌轮换） ===
        stringRedisTemplate.delete(RedisKeyConstants.LOGIN_REFRESH_KEY + refreshToken);

        // === 步骤 6：生成全新的双令牌 ===
        return buildLoginResponse(user, null);
    }

    // ==================== 当前用户获取 ====================

    /**
     * 获取当前登录用户
     * <p>
     * 多层验证机制：<br>
     * 1. 从请求头提取 Bearer token<br>
     * 2. 检查 token 是否在黑名单中（已注销的 token）<br>
     * 3. 检查 Redis 中是否存在该 token 的登录会话（会话是否有效）<br>
     * 4. 从 Redis 会话中获取 userId，查询用户信息<br>
     * 5. 验证用户状态（存在、未删除、未停用）
     * </p>
     * <p>调用链路：各需要鉴权的 Controller → this.getLoginUser → extractToken → 黑名单检查 → Redis 会话检查 → 用户状态检查 → 返回 User</p>
     *
     * @param request HTTP 请求
     * @return 当前登录的用户实体
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // === 步骤 1：从请求头提取 token ===
        String token = extractToken(request);
        ThrowUtils.throwIf(StrUtil.isBlank(token), ResultCode.NOT_LOGIN_ERROR, "未登录");

        // === 步骤 2：检查是否在黑名单中 ===
        Boolean isBlacklisted = stringRedisTemplate.hasKey(RedisKeyConstants.LOGIN_BLACKLIST_KEY + token);
        ThrowUtils.throwIf(Boolean.TRUE.equals(isBlacklisted), ResultCode.NOT_LOGIN_ERROR, "Token 已被注销");

        // === 步骤 3：检查 Redis 会话（是否已过期或被踢下线） ===
        String sessionJson = stringRedisTemplate.opsForValue().get(RedisKeyConstants.LOGIN_TOKEN_KEY + token);
        ThrowUtils.throwIf(StrUtil.isBlank(sessionJson), ResultCode.NOT_LOGIN_ERROR, "登录已过期，请重新登录");

        // === 步骤 4：从 Redis 会话中解析出 userId ===
        Map<String, Object> sessionMap = JSONUtil.parseObj(sessionJson);
        Long userId = Long.valueOf((String) sessionMap.get("userId"));

        // === 步骤 5：查询用户并校验状态 ===
        User user = getById(userId);
        ThrowUtils.throwIf(ObjectUtil.isNull(user), ResultCode.NOT_LOGIN_ERROR, "用户不存在");
        ThrowUtils.throwIf(ObjectUtil.equal(user.getIsDeleted(), 1), ResultCode.NOT_LOGIN_ERROR, "用户已注销");
        ThrowUtils.throwIf(ObjectUtil.equal(user.getStatus(), 0), ResultCode.NOT_LOGIN_ERROR, "账号已停用");

        return user;
    }

    // ==================== VO 转换 ====================

    /**
     * 将用户实体转换为用户视图对象（排除密码等敏感字段）
     *
     * @param user 用户实体
     * @return 用户视图对象，若入参为 null 则返回 null
     */
    @Override
    public UserResponse getUserVO(User user) {
        if (ObjectUtil.isNull(user)) return null;
        UserResponse vo = new UserResponse();
        // 拷贝属性，排除 "password" 字段（敏感信息不返回前端）
        BeanUtil.copyProperties(user, vo, "password");
        return vo;
    }

    /**
     * 批量将用户实体列表转换为用户视图对象列表
     * <p>遍历调用 {@link #getUserVO(User)} 进行逐个转换。</p>
     *
     * @param userList 用户实体列表
     * @return 用户视图对象列表，若入参为空则返回空列表
     */
    @Override
    public List<UserResponse> getUserVOList(List<User> userList) {
        if (ObjectUtil.isEmpty(userList)) return Collections.emptyList();
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    // ==================== 查询条件构造 ====================

    /**
     * 构建用户查询的 MyBatis-Plus 条件构造器
     * <p>
     * 支持按 ID、用户名、真实姓名、手机号、角色、状态等字段过滤。
     * 默认排除已删除记录（is_deleted = 0），按创建时间降序排列。
     * </p>
     *
     * @param query 用户查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest query) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 默认只查询未删除记录
        wrapper.eq("is_deleted", 0);
        if (ObjectUtil.isNotNull(query)) {
            // ID 精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getId()), "id", query.getId());
            // 用户名模糊搜索
            wrapper.like(StrUtil.isNotBlank(query.getUserName()), "username", query.getUserName());
            // 真实姓名模糊搜索
            wrapper.like(StrUtil.isNotBlank(query.getRealName()), "real_name", query.getRealName());
            // 手机号模糊搜索
            wrapper.like(StrUtil.isNotBlank(query.getPhone()), "phone", query.getPhone());
            // 角色精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getRole()), "role", query.getRole());
            // 状态精确匹配
            wrapper.eq(ObjectUtil.isNotNull(query.getStatus()), "status", query.getStatus());
        }
        // 默认按创建时间降序排序
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建登录响应（生成双令牌 + 存储 Redis 会话）
     * <p>
     * 业务流程：<br>
     * 1. 生成 accessToken（短期有效，用于接口鉴权）<br>
     * 2. 生成 refreshToken（长期有效，用于令牌刷新）<br>
     * 3. 将 accessToken 会话信息存入 Redis（含 userId、username、role、loginIp、loginTime）<br>
     * 4. 将 refreshToken → userId 映射存入 Redis（用于刷新时验证）<br>
     * 5. 组装 UserLoginResponse 返回
     * </p>
     *
     * @param user   登录用户实体
     * @param loginIp 客户端 IP 地址（用于审计日志）
     * @return 包含双令牌和用户信息的登录响应
     */
    private UserLoginResponse buildLoginResponse(User user, String loginIp) {
        // === 生成 JWT 双令牌 ===
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // === 构建 accessToken 的 Redis 会话信息 ===
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("userId", String.valueOf(user.getId()));
        sessionMap.put("username", user.getUsername());
        sessionMap.put("role", user.getRole().name());
        sessionMap.put("loginIp", ObjectUtil.defaultIfNull(loginIp, "unknown"));
        sessionMap.put("loginTime", System.currentTimeMillis());

        // === 存储 accessToken 会话到 Redis（TTL = accessToken 有效期） ===
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.LOGIN_TOKEN_KEY + accessToken,
                JSONUtil.toJsonStr(sessionMap),
                jwtUtils.getAccessExpiration(), TimeUnit.SECONDS);

        // === 存储 refreshToken → userId 映射到 Redis（TTL = refreshToken 有效期） ===
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.LOGIN_REFRESH_KEY + refreshToken,
                String.valueOf(user.getId()),
                jwtUtils.getRefreshExpiration(), TimeUnit.SECONDS);

        // === 组装响应 ===
        UserLoginResponse response = new UserLoginResponse();
        BeanUtil.copyProperties(user, response, "password");  // 排除密码
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    /**
     * 从 HTTP 请求头中提取 Bearer token
     * <p>从 Authorization 头中提取 "Bearer xxx" 格式的 token 字符串（去除 "Bearer " 前缀）。</p>
     *
     * @param request HTTP 请求
     * @return token 字符串，若不存在或格式不对则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(bearer) && bearer.startsWith("Bearer ")) {
            return StrUtil.subAfter(bearer, "Bearer ", true);
        }
        return null;
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 依次尝试以下请求头（适配反向代理场景）：<br>
     * 1. X-Forwarded-For<br>
     * 2. Proxy-Client-IP<br>
     * 3. WL-Proxy-Client-IP<br>
     * 4. request.getRemoteAddr()（直连 IP）
     * </p>
     * <p>若 X-Forwarded-For 包含多个 IP（逗号分隔），取第一个（客户端原始 IP）。</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址，无法获取时返回 "unknown"
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        // 依次尝试各代理头
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();

        // 取第一个 IP（X-Forwarded-For 可能包含多个代理 IP，第一个为客户端 IP）
        if (StrUtil.isNotBlank(ip) && ip.contains(","))
            ip = StrUtil.subBefore(ip, ",", true);

        return ObjectUtil.defaultIfNull(ip, "unknown");
    }
}
