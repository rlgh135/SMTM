package com.project.stock.domain.stock.adapter.out.external.ai;

import com.project.stock.common.config.AiWorkerProperties;
import com.project.stock.domain.stock.application.port.out.AiAnalysisPort;
import com.project.stock.domain.stock.application.port.out.LoadStockPricePort;
import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisResult;
import com.project.stock.domain.stock.domain.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;

/**
 * AI Worker 통신 어댑터 (Driven Adapter).
 * AI Worker에 분석 요청을 전송하고 결과를 수신합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisAdapter implements AiAnalysisPort {

    private static final int DEFAULT_LOOKBACK_DAYS = 120;

    private final AiWorkerProperties aiWorkerProperties;
    private final LoadStockPricePort loadStockPricePort;
    private final WebClient.Builder webClientBuilder;

    @Override
    public StockAnalysisResult requestAnalysis(Stock stock) {
        log.info("AI 분석 요청 시작: stockCode={}", stock.getStockCode());

        // 1. 최근 120일 시세 데이터 조회
        List<StockPrice> recentPrices = loadStockPricePort.findTopNByStockIdOrderByDateDesc(
            stock.getId(),
            DEFAULT_LOOKBACK_DAYS
        );

        if (recentPrices.isEmpty()) {
            log.warn("시세 데이터가 없습니다: stockCode={}", stock.getStockCode());
            throw new IllegalStateException("분석을 위한 시세 데이터가 없습니다. 먼저 시세 동기화를 진행하세요.");
        }

        log.info("시세 데이터 조회 완료: stockCode={}, 데이터 건수={}", stock.getStockCode(), recentPrices.size());

        // 2. AI Worker에 분석 요청
        AiAnalysisRequest request = new AiAnalysisRequest(
            stock.getStockCode(),
            DEFAULT_LOOKBACK_DAYS
        );

        AiAnalysisResponse response = callAiWorker(request);

        // 3. 응답을 도메인 객체로 변환
        StockAnalysisResult result = mapToAnalysisResult(response);

        log.info("AI 분석 완료: stockCode={}, recommendation={}",
            stock.getStockCode(), result.recommendation());

        return result;
    }

    private AiAnalysisResponse callAiWorker(AiAnalysisRequest request) {
        WebClient webClient = webClientBuilder
            .baseUrl(aiWorkerProperties.getBaseUrl())
            .build();

        log.info("AI Worker 호출: url={}/api/v1/analysis, stockCode={}",
            aiWorkerProperties.getBaseUrl(), request.stockCode());

        try {
            AiAnalysisResponse response = webClient.post()
                .uri("/api/v1/analysis")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiAnalysisResponse.class)
                .block();

            if (response == null) {
                log.error("AI Worker 응답이 null입니다");
                throw new RuntimeException("AI Worker 응답 오류");
            }

            log.info("AI Worker 응답 수신: recommendation={}, confidence={}",
                response.recommendation(), response.confidenceScore());

            return response;

        } catch (WebClientResponseException e) {
            log.error("AI Worker 호출 실패: status={}, body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI Worker 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI Worker 통신 오류: {}", e.getMessage(), e);
            throw new RuntimeException("AI Worker 통신 오류: " + e.getMessage(), e);
        }
    }

    private StockAnalysisResult mapToAnalysisResult(AiAnalysisResponse response) {
        // Recommendation enum 변환
        StockAnalysisResult.Recommendation recommendation;
        try {
            recommendation = StockAnalysisResult.Recommendation.valueOf(response.recommendation());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 recommendation: {}, HOLD로 변환", response.recommendation());
            recommendation = StockAnalysisResult.Recommendation.HOLD;
        }

        return StockAnalysisResult.builder()
            .recommendation(recommendation)
            .confidenceScore(response.confidenceScore())
            .technicalAnalysis(response.technicalAnalysis())
            .supplyAnalysis(response.supplyAnalysis())
            .riskFactors(response.riskFactors())
            .build();
    }
}
