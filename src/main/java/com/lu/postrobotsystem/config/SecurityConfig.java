package com.lu.postrobotsystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，用于从请求中提取和验证 JWT 令牌 */
    private final JwtAuthenticationFilter jwtFilter;

    /**
     * 配置 Spring Security 的安全过滤器链
     * <p>
     * 定义全局安全策略，包括 CSRF 保护、会话管理、请求授权规则
     * 以及自定义过滤器的注册位置。
     * </p>
     *
     * @param http HttpSecurity 对象，用于链式配置安全策略
     * @return 构建完成的 SecurityFilterChain 实例
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF 保护（RESTful API 使用 Token 认证，无需 CSRF）
            .csrf(csrf -> csrf.disable())
            // 设置会话管理策略为无状态（STATELESS），不创建也不使用 HTTP Session
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置 HTTP 请求的授权规则
            .authorizeHttpRequests(auth -> auth
                    // 允许所有以 /auth/ 开头的请求（登录、注册等）
                    .requestMatchers("/auth/**").permitAll()
                    // 允许所有以 /health/ 开头的请求（健康检查）
                    .requestMatchers("/health/**").permitAll()
                    // 允许支付回调（仅 POST，邮政系统异步通知，无需 JWT 认证）
                    .requestMatchers(HttpMethod.POST, "/payment/callback").permitAll()
                    // 允许所有 Swagger / Knife4j 相关请求（API 文档）
                    .requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/doc.html",
                            "/webjars/**"
                    ).permitAll()
                    // 除上述白名单外，所有其他请求都需要进行认证
                    .anyRequest().authenticated()
            )
            // 在 Spring Security 内置的 UsernamePasswordAuthenticationFilter 之前
            // 注册自定义的 JWT 认证过滤器
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用默认的登出功能（因为是无状态 API，登出由 Redis 黑名单机制实现）
            .logout(logout -> logout.disable());

        return http.build();
    }

    /**
     * 创建密码编码器 Bean
     * <p>
     * 使用 BCrypt 强哈希算法对用户密码进行编码。
     * BCrypt 内置了盐值（Salt），每次加密结果不同，即使相同密码也会产生不同密文，
     * 可以有效防御彩虹表攻击。
     * </p>
     *
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>用户注册时：使用此编码器加密密码后存储到数据库</li>
     *   <li>用户登录时：使用此编码器验证明文密码与数据库中密文是否匹配</li>
     *   <li>管理员初始化时：{@link AdminInitializer} 使用 Hutool 的 BCrypt 工具加密</li>
     * </ul>
     * </p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
