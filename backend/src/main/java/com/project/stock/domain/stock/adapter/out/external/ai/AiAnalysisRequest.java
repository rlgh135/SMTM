package com.project.stock.domain.stock.adapter.out.external.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI Worker 분석 요청 DTO.
 */
public record AiAnalysisRequest(
    @JsonProperty("stock_code")
    String stockCode,

    @JsonProperty("lookback_days")
    Integer lookbackDays
) {
    public AiAnalysisRequest(String stockCode) {
        this(stockCode, 120);
    }
}
