package com.project.stock.domain.stock.adapter.out.external.kis;

import com.project.stock.common.config.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 한국투자증권 OpenAPI 클라이언트.
 * WebClient를 사용하여 주식 시세 데이터를 조회합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KisClient {

    private static final String TR_ID_DAILY_PRICE = "FHKST03010100"; // 국내주식 기간별 시세 조회
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final WebClient.Builder webClientBuilder;

    /**
     * 특정 종목의 일별 시세 데이터를 조회합니다.
     *
     * @param stockCode 종목코드 (6자리)
     * @param startDate 시작일
     * @param endDate   종료일
     * @return OHLCV 응답 데이터
     */
    public KisOhlcvResponse fetchDailyPrices(String stockCode, LocalDate startDate, LocalDate endDate) {
        String accessToken = tokenManager.getAccessToken();

        WebClient webClient = webClientBuilder
            .baseUrl(kisProperties.getBaseUrl())
            .build();

        String startDateStr = startDate.format(DATE_FORMATTER);
        String endDateStr = endDate.format(DATE_FORMATTER);

        log.info("KIS API 일별 시세 조회: 종목={}, 기간={} ~ {}", stockCode, startDateStr, endDateStr);

        try {
            KisOhlcvResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J") // 시장 구분 (J: 주식)
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .queryParam("FID_INPUT_DATE_1", startDateStr)
                    .queryParam("FID_INPUT_DATE_2", endDateStr)
                    .queryParam("FID_PERIOD_DIV_CODE", "D") // 기간 구분 (D: 일)
                    .queryParam("FID_ORG_ADJ_PRC", "0") // 수정주가 여부 (0: 원주가, 1: 수정주가)
                    .build())
                .header("content-type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", TR_ID_DAILY_PRICE)
                .retrieve()
                .bodyToMono(KisOhlcvResponse.class)
                .block();

            if (response == null) {
                log.error("KIS API 응답이 null입니다");
                throw new RuntimeException("KIS API 응답 오류");
            }

            if (!"0".equals(response.resultCode())) {
                log.error("KIS API 에러: 코드={}, 메시지={}", response.messageCode(), response.message());
                throw new RuntimeException("KIS API 에러: " + response.message());
            }

            log.info("KIS API 조회 성공: {} 건의 데이터", response.output1() != null ? response.output1().size() : 0);
            return response;

        } catch (WebClientResponseException e) {
            log.error("KIS API 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("KIS API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 종목의 최근 N일 시세 데이터를 조회합니다.
     *
     * @param stockCode 종목코드
     * @param days      조회할 일수
     * @return OHLCV 응답 데이터
     */
    public KisOhlcvResponse fetchRecentDailyPrices(String stockCode, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return fetchDailyPrices(stockCode, startDate, endDate);
    }
}
