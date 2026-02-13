package com.project.stock.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정.
 * @Scheduled 어노테이션을 활성화합니다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
