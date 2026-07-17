package com.lu.postrobotsystem.config;

import cn.hutool.core.util.StrUtil;
import com.lu.postrobotsystem.common.util.JwtUtils;
import com.lu.postrobotsystem.constant.RedisKeyConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，提供令牌的验证、解析和认证对象构建 */
    private final JwtUtils jwtUtils;

    /** Redis 模板，用于检查令牌黑名单和活跃会话状态 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 执行过滤器核心逻辑
     * <p>
     * 对每个 HTTP 请求执行 JWT 认证流程：
     * <ol>
     *   <li>从请求的 Authorization 头或自定义 token 头中提取 JWT</li>
     *   <li>验证令牌签名和有效期</li>
     *   <li>检查令牌是否在黑名单中（已登出）</li>
     *   <li>检查令牌对应的会话是否在 Redis 中有效</li>
     *   <li>通过验证后构建 Authentication 并设置到 SecurityContext</li>
     * </ol>
     * 未通过认证的请求会在后续的过滤器链中被 {@link SecurityConfig} 拦截（返回 401）。
     * 通过认证的请求可以在 Controller 中通过 {@code SecurityContextHolder} 获取用户信息。
     * </p>
     *
     * @param request      HTTP 请求对象
     * @param response     HTTP 响应对象
     * @param filterChain  过滤器链，用于继续执行后续过滤器
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 第一步：从请求头中提取 JWT 令牌
        String token = extractToken(request);

        // 第二步：验证令牌是否存在且格式有效
        if (StrUtil.isNotBlank(token) && jwtUtils.validateToken(token)) {
            // 第三步：检查该令牌是否在 Redis 黑名单中（用户已登出）
            Boolean isBlacklisted = stringRedisTemplate.hasKey(
                    RedisKeyConstants.LOGIN_BLACKLIST_KEY + token);

            // 第四步：检查 Redis 中是否存在该令牌对应的有效会话
            Boolean hasSession = stringRedisTemplate.hasKey(
                    RedisKeyConstants.LOGIN_TOKEN_KEY + token);

            // 第五步：仅当令牌不在黑名单中且会话有效时，才设置认证信息
            if (Boolean.FALSE.equals(isBlacklisted) && Boolean.TRUE.equals(hasSession)) {
                // 从令牌中构建 Spring Security 认证对象
                Authentication authentication = jwtUtils.getAuthentication(token);
                // 将认证信息设置到安全上下文，后续请求可通过 SecurityContextHolder 获取
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 第六步：继续执行过滤器链（无论是否认证成功，都不在此处拦截）
        // 未认证的请求将在 SecurityConfig 的 authorizeHttpRequests 配置中被拦截
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求中提取 JWT 令牌
     * <p>
     * 支持两种提取方式（按优先级）：
     * <ol>
     *   <li>标准方式：从 {@code Authorization} 头中提取 {@code Bearer <token>}</li>
     *   <li>兼容方式：从自定义请求头 {@code token} 中直接提取（适配 Knife4j 等 API 文档工具）</li>
     * </ol>
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 提取到的 JWT 令牌字符串，如果未找到则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // 方式一：从 Authorization: Bearer <token> 中提取（标准的 OAuth2/JWT 方式）
        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            // 截取 "Bearer " 后面的 token 部分
            return StrUtil.subAfter(authHeader, "Bearer ", true);
        }
        // 方式二：从自定义的 "token" 请求头中直接提取（兼容 Knife4j 全局参数方式）
        String tokenHeader = request.getHeader("token");
        if (StrUtil.isNotBlank(tokenHeader)) {
            return tokenHeader;
        }
        // 两种方式都未找到，返回 null
        return null;
    }
}
