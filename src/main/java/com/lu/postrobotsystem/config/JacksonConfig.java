package com.lu.postrobotsystem.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置
 * <p>
 * 将 Long 类型统一序列化为字符串，解决 Snowflake ID（64 位）
 * 在前端 JavaScript 中精度丢失的问题。
 * JS 的 Number 只能精确表示 2^53 以内的整数，而 Snowflake ID 可达 2^63。
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Long 和 long 都序列化为字符串，前端可通过字符串完整保留精度
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
