package com.project.stock.domain.stock.domain;

import lombok.Builder;

import java.util.List;

/**
 * AI 주식 분석 결과 도메인 모델.
 */
@Builder
public record StockAnalysisResult(
        Recommendation recommendation,
        int confidenceScore,
        String technicalAnalysis,
        String supplyAnalysis,
        List<String> riskFactors
) {

    public enum Recommendation {
        BUY, SELL, HOLD
    }
}
