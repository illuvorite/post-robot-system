package com.lu.postrobotsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域资源共享（CORS）配置类
 * <p>
 * 实现 {@link WebMvcConfigurer} 接口，重写 {@link #addCorsMappings} 方法，
 * 配置 Spring MVC 的跨域访问策略，允许前端开发环境（localhost）
 * 调用后端 API 接口，解决浏览器的同源策略限制问题。
 * </p>
 *
 * <p><b>配置策略：</b>
 * <ul>
 *   <li>允许所有路径（/**）的跨域请求</li>
 *   <li>允许携带 Cookie（credentials = true）</li>
 *   <li>仅允许本地开发域名（http://localhost:*）</li>
 *   <li>支持 RESTful 的常用 HTTP 方法</li>
 *   <li>允许所有请求头和响应头</li>
 * </ul>
 * </p>
 *
 * <p><b>安全说明：</b>
 * 生产部署时建议将 {@code allowedOriginPatterns} 调整为具体的正式域名，
 * 避免暴露过宽的跨域策略。
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置跨域访问规则
     * <p>
     * 注册全局 CORS 配置，对所有路径生效。
     * 当前仅允许来自 localhost 的请求，满足前后端分离开发场景。
     * </p>
     *
     * @param registry CORS 注册表，用于添加跨域映射
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求路径
        registry.addMapping("/**")
                // 允许发送 Cookie（如 SessionID、JWT 等认证信息）
                .allowCredentials(true)
                // 允许本地开发域名和端口（仅开发环境）
                .allowedOriginPatterns("http://localhost:*")
                // 允许的 HTTP 方法（符合 RESTful 规范）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // 允许所有请求头
                .allowedHeaders("*")
                // 暴露所有响应头给前端
                .exposedHeaders("*");
    }
}
