package com.lu.postrobotsystem.adapter.postal.config;

import com.lu.postrobotsystem.adapter.postal.spi.PostalApi;
import com.lu.postrobotsystem.adapter.postal.spi.PostalApiMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 邮政 API 适配层配置
 * <p>
 * 装配 PostalApi bean（根据 postal.mock.enabled 自动切换 Mock/HTTP 实现），
 * 以及全局 RestTemplate（供 HTTP 客户端使用）。
 * </p>
 */
@Configuration
public class PostalApiConfig {

    /**
     * 配置 RestTemplate（供 PostalApiHttpClient 使用）
     */
    @Bean
    public RestTemplate postalRestTemplate() {
        return new RestTemplate();
    }

    /**
     * 确保 PostalApi bean 始终存在。
     * 当 postal.mock.enabled=false 时，Spring 容器中应存在 PostalApiHttpClient；
     * 此兜底 bean 仅在无其他实现时生效。
     */
    @Bean
    @ConditionalOnMissingBean(PostalApi.class)
    public PostalApi defaultPostalApi(PostalApiMock mock) {
        // 如果条件不满足导致无 PostalApi bean 可用，回退到 Mock
        return mock;
    }
}
