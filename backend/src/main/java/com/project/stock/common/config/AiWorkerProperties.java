package com.project.stock.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI Worker 설정.
 */
@Configuration
@ConfigurationProperties(prefix = "ai-worker")
@Getter
@Setter
public class AiWorkerProperties {

    /**
     * AI Worker Base URL.
     */
    private String baseUrl;
}
