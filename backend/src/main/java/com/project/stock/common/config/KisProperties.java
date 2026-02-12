package com.project.stock.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 한국투자증권 API 설정.
 */
@Configuration
@ConfigurationProperties(prefix = "kis")
@Getter
@Setter
public class KisProperties {

    /**
     * API App Key.
     */
    private String appKey;

    /**
     * API App Secret.
     */
    private String appSecret;

    /**
     * API Base URL.
     */
    private String baseUrl;

    /**
     * 계좌번호.
     */
    private String accountNo;
}
