package com.project.stock.domain.stock.adapter.out.external.kis;

import com.project.stock.common.config.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * KIS API Access Token 관리 서비스.
 * Redis에 토큰을 캐싱하고 자동으로 갱신합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KisTokenManager {

    private static final String REDIS_KEY_TOKEN = "kis:access_token";
    private static final Duration TOKEN_EXPIRY_BUFFER = Duration.ofMinutes(5);

    private final KisProperties kisProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    /**
     * Access Token을 반환합니다.
     * Redis 캐시에서 조회하고, 없거나 만료된 경우 새로 발급합니다.
     */
    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_TOKEN);

        if (cachedToken != null && !cachedToken.isEmpty()) {
            log.debug("Redis에서 캐시된 토큰 사용");
            return cachedToken;
        }

        log.info("토큰이 없거나 만료됨. 새 토큰 발급 요청");
        return issueNewToken();
    }

    /**
     * KIS API로부터 새로운 Access Token을 발급받습니다.
     */
    private String issueNewToken() {
        WebClient webClient = webClientBuilder
            .baseUrl(kisProperties.getBaseUrl())
            .build();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", kisProperties.getAppKey());
        requestBody.put("appsecret", kisProperties.getAppSecret());

        KisTokenResponse response = webClient.post()
            .uri("/oauth2/token")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(KisTokenResponse.class)
            .block();

        if (response == null || response.accessToken() == null) {
            log.error("토큰 발급 실패: 응답이 null입니다");
            throw new RuntimeException("KIS API 토큰 발급 실패");
        }

        String accessToken = response.accessToken();
        long expirySeconds = response.expiresIn() != null ? response.expiresIn() : 86400; // 기본 24시간

        // Redis에 캐싱 (만료 시간보다 5분 일찍 만료 설정)
        Duration cacheDuration = Duration.ofSeconds(expirySeconds).minus(TOKEN_EXPIRY_BUFFER);
        redisTemplate.opsForValue().set(REDIS_KEY_TOKEN, accessToken, cacheDuration);

        log.info("새 토큰 발급 완료. 만료: {}초 후", expirySeconds);
        return accessToken;
    }

    /**
     * 수동으로 토큰을 갱신합니다.
     */
    public String refreshToken() {
        log.info("수동 토큰 갱신 요청");
        redisTemplate.delete(REDIS_KEY_TOKEN);
        return issueNewToken();
    }
}
