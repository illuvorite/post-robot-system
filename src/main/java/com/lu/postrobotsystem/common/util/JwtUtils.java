package com.lu.postrobotsystem.common.util;

import cn.hutool.core.date.DateUtil;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * JWT（JSON Web Token）工具类
 * <p>
 * 负责 JWT 令牌的生成、验证和解析，以及 Spring Security 认证信息的构建。
 * 令牌分为 accessToken（访问令牌）和 refreshToken（刷新令牌）两种类型，
 * 分别用于接口鉴权和令牌续期。
 * </p>
 *
 * <p><b>配置属性（来自 application.yml 的 jwt 前缀）：</b>
 * <ul>
 *   <li>{@link #secret} -- JWT 签名密钥，用于 HS512 算法加签</li>
 *   <li>{@link #accessExpiration} -- 访问令牌过期时间（秒）</li>
 *   <li>{@link #refreshExpiration} -- 刷新令牌过期时间（秒）</li>
 * </ul>
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>{@link #generateAccessToken} / {@link #generateRefreshToken} -- 由 {@code AuthController} 在登录/刷新时调用</li>
 *   <li>{@link #validateToken} / {@link #getAuthentication} -- 由 {@link com.lu.postrobotsystem.config.JwtAuthenticationFilter} 在请求过滤时调用</li>
 *   <li>{@link #getUserId} / {@link #getUsername} / {@link #getRole} -- 由业务 Service 层从 token 中提取用户信息</li>
 * </ul>
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtils {

    /** JWT 签名密钥，用于令牌的签发和验证（HS512 算法） */
    private String secret;

    /** 访问令牌（accessToken）的过期时间，单位：秒 */
    private long accessExpiration;

    /** 刷新令牌（refreshToken）的过期时间，单位：秒 */
    private long refreshExpiration;

    /**
     * 生成访问令牌（accessToken）
     * <p>
     * 令牌载荷包含：用户ID（subject）、用户名、角色、令牌类型（access）。
     * 使用 HS512 算法加签，过期时间由 {@link #accessExpiration} 控制。
     * </p>
     *
     * @param userId   用户ID，作为令牌主体（subject）
     * @param username 用户名，存入 claim 供后续快速获取
     * @param role     用户角色枚举，用于后续的权限控制
     * @return 签发的 JWT 访问令牌字符串
     */
    public String generateAccessToken(Long userId, String username, UserRoleEnum role) {
        // 计算过期时间：当前时间 + accessExpiration 秒
        Date expiryDate = DateUtil.offsetSecond(new Date(), (int) accessExpiration);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))                  // 设置主题为用户ID
                .claim("username", username)                        // 自定义声明：用户名
                .claim("role", role.name())                         // 自定义声明：角色
                .claim("tokenType", "access")                       // 自定义声明：令牌类型
                .setIssuedAt(new Date())                            // 设置签发时间
                .setExpiration(expiryDate)                          // 设置过期时间
                .signWith(SignatureAlgorithm.HS512, secret)         // 使用 HS512 算法签名
                .compact();                                         // 压缩为字符串
    }

    /**
     * 生成刷新令牌（refreshToken）
     * <p>
     * 刷新令牌仅包含用户ID和令牌类型标记，不包含角色等敏感信息。
     * 过期时间由 {@link #refreshExpiration} 控制，通常比访问令牌长。
     * 客户端在访问令牌过期后使用刷新令牌获取新的访问令牌。
     * </p>
     *
     * @param userId 用户ID，作为令牌主体（subject）
     * @return 签发的 JWT 刷新令牌字符串
     */
    public String generateRefreshToken(Long userId) {
        // 计算过期时间：当前时间 + refreshExpiration 秒
        Date expiryDate = DateUtil.offsetSecond(new Date(), (int) refreshExpiration);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))                  // 设置主题为用户ID
                .claim("tokenType", "refresh")                      // 自定义声明：令牌类型
                .setIssuedAt(new Date())                            // 设置签发时间
                .setExpiration(expiryDate)                          // 设置过期时间
                .signWith(SignatureAlgorithm.HS512, secret)         // 使用 HS512 算法签名
                .compact();                                         // 压缩为字符串
    }

    /**
     * 验证 JWT 令牌是否有效
     * <p>
     * 通过解析签名来验证令牌的完整性和真实性。
     * 包括：签名是否匹配、令牌是否过期、令牌格式是否正确。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌有效返回 true，否则返回 false（过期、签名错误等均返回 false）
     */
    public boolean validateToken(String token) {
        try {
            // 使用签名密钥解析令牌，若抛出异常则说明验证失败
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // 令牌过期、签名不匹配、格式错误等均视为无效
            return false;
        }
    }

    /**
     * 获取 JWT 令牌中的 Claims（载荷）
     * <p>
     * Claims 包含了令牌的所有声明信息，如用户ID、用户名、角色等。
     * 注意：此方法不验证令牌的有效性，调用前应先调用 {@link #validateToken}。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return Claims 对象，包含令牌载荷中的所有声明信息
     */
    public Claims getClaims(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 从令牌中提取用户ID
     * <p>
     * 用户ID存储在令牌的 subject（主题）字段中，
     * 通过 {@link #getClaims} 获取 Claims 后再提取 subject。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 用户ID（Long 类型）
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 从令牌中提取用户名
     * <p>
     * 用户名存储在自定义 claim "username" 中，
     * 通过 {@link #getClaims} 获取 Claims 后再提取。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 用户名字符串
     */
    public String getUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    /**
     * 从令牌中提取用户角色
     * <p>
     * 角色名称存储在自定义 claim "role" 中，
     * 通过 {@link #getClaims} 获取 Claims 后转换为 {@link UserRoleEnum} 枚举。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 用户角色枚举 {@link UserRoleEnum}
     */
    public UserRoleEnum getRole(String token) {
        return UserRoleEnum.getEnumByValue(getClaims(token).get("role", String.class));
    }

    /**
     * 判断令牌是否为访问令牌（accessToken）
     * <p>
     * 通过检查自定义声明 "tokenType" 的值是否为 "access" 来判断。
     * 用于区分访问令牌和刷新令牌。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 如果是访问令牌返回 true，否则返回 false
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("tokenType"));
    }

    /**
     * 从令牌中构建 Spring Security 认证对象
     * <p>
     * 将 JWT 令牌中的用户信息和角色转换为 Spring Security 的
     * {@link Authentication} 接口实现，以便设置到安全上下文中。
     * </p>
     *
     * <p><b>转换过程：</b>
     * <ol>
     *   <li>从令牌 Claims 中提取用户ID和角色</li>
     *   <li>创建角色相关的 {@link SimpleGrantedAuthority} 列表</li>
     *   <li>构建 {@link org.springframework.security.core.userdetails.User} 对象</li>
     *   <li>返回 {@link UsernamePasswordAuthenticationToken} 对象</li>
     * </ol>
     * </p>
     *
     * <p><b>调用关系：</b>
     * 此方法由 {@link com.lu.postrobotsystem.config.JwtAuthenticationFilter#doFilterInternal} 调用，
     * 将认证结果设置到 {@code SecurityContextHolder} 中。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return Spring Security 认证对象，包含用户身份和权限信息
     */
    public Authentication getAuthentication(String token) {
        // 解析令牌获取全部声明
        Claims claims = getClaims(token);
        // 提取用户ID
        Long userId = Long.parseLong(claims.getSubject());
        // 提取角色名称
        String role = claims.get("role", String.class);

        // 构建权限列表（Spring Security 格式：ROLE_xxx）
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        // 构建 UserDetails 主体对象
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        String.valueOf(userId), "", authorities
                );

        // 返回认证令牌（包含主体、凭证和权限信息）
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}
