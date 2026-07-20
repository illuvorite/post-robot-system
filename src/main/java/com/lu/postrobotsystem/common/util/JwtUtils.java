package com.lu.postrobotsystem.common.util;

import cn.hutool.core.date.DateUtil;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT（JSON Web Token）工具类
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtils {

    /** JWT 签名密钥，用于令牌的签发和验证（HMAC 算法） */
    private String secret;

    /** 访问令牌（accessToken）的过期时间，单位：秒 */
    private long accessExpiration;

    /** 刷新令牌（refreshToken）的过期时间，单位：秒 */
    private long refreshExpiration;

    /**
     * 获取 HMAC 签名密钥（由配置中的 secret 派生）
     * <p>使用 Keys.hmacShaKeyFor 自动根据密钥长度选择 HMAC-SHA 算法。
     * 若密钥字节不足 256 位，自动补齐至 256 位以满足 jjwt 0.12.x 的安全要求。</p>
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // jjwt 0.12.x 要求 HMAC 密钥 ≥ 256 位，不足时补齐
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌（accessToken）
     * <p>
     * 令牌载荷包含：用户ID（subject）、用户名、角色、令牌类型（access）。
     * 使用 jjwt 0.12.x 安全 API 加签，过期时间由 {@link #accessExpiration} 控制。
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
                .subject(String.valueOf(userId))                  // 设置主题为用户ID
                .claim("username", username)                        // 自定义声明：用户名
                .claim("role", role.name())                         // 自定义声明：角色
                .claim("tokenType", "access")                       // 自定义声明：令牌类型
                .issuedAt(new Date())                              // 设置签发时间
                .expiration(expiryDate)                            // 设置过期时间
                .signWith(getSigningKey())                         // 使用 HMAC 算法签名（jjwt 0.12.x 新 API）
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
                .subject(String.valueOf(userId))                  // 设置主题为用户ID
                .claim("tokenType", "refresh")                      // 自定义声明：令牌类型
                .issuedAt(new Date())                              // 设置签发时间
                .expiration(expiryDate)                            // 设置过期时间
                .signWith(getSigningKey())                         // 使用 HMAC 算法签名（jjwt 0.12.x 新 API）
                .compact();                                         // 压缩为字符串
    }

    /**
     * 验证 JWT 令牌是否有效
     * <p>
     * 通过解析签名来验证令牌的完整性和真实性。
     * 包括：签名是否匹配、令牌是否过期、令牌格式是否正确。
     * 使用 jjwt 0.12.x 的 verifyWith API。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌有效返回 true，否则返回 false（过期、签名错误等均返回 false）
     */
    public boolean validateToken(String token) {
        try {
            // 使用签名密钥验证令牌（0.12.x API）
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
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
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * 从令牌中构建 Spring Security 认证对象
     */
    public Authentication getAuthentication(String token) {
        // 解析令牌获取全部声明（0.12.x API: parseSignedClaims + getPayload）
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
