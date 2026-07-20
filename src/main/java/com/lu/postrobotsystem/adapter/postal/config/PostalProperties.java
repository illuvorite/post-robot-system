package com.lu.postrobotsystem.adapter.postal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮政系统对接配置属性
 * <p>
 * 绑定 application.yml 中 postal.* 前缀的所有配置项，
 * 替代现有 PostalApiClient 中的散落 @Value 注入，
 * 统一管理邮政接口的地址、超时、重试、签名、机构等信息。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "postal")
public class PostalProperties {

    /** Mock 模式开关 */
    private Mock mock = new Mock();

    /** API 连接配置 */
    private Api api = new Api();

    /** 平台配置 */
    private Platform platform = new Platform();

    @Data
    public static class Mock {
        /** 是否启用 Mock 模式（默认 true） */
        private boolean enabled = true;
        /** Mock 模拟的支付等待毫秒数（模拟支付处理延迟） */
        private long paymentDelayMs = 5000;
    }

    @Data
    public static class Api {
        /** 邮政接口基础 URL */
        private String baseUrl = "https://yyqduat.11185.cn/ptfwApi/getMessage";
        /** 连接超时（毫秒） */
        private int connectTimeout = 5000;
        /** 读取超时（毫秒） */
        private int readTimeout = 10000;
        /** 失败重试次数 */
        private int retryCount = 3;
        /** 签名密钥 */
        private String signSecret = "dcff43b0fea660914aAFHElVnrq8";
    }

    @Data
    public static class Platform {
        /** 平台编码 */
        private String code = "POSTR";
        /** 机构编码 */
        private String orgCode = "AHFY01";
        /** 台席代码 */
        private String deskCode = "ROBOT01";
        /** 源系统 ID */
        private String srcSysId = "POSTR";
        /** 目标系统 ID */
        private String dstSysId = "XYDYYQDXT";
    }
}
