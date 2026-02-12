package com.project.stock.domain.stock.adapter.out.external.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI Worker 분석 응답 DTO.
 */
public record AiAnalysisResponse(
    @JsonProperty("recommendation")
    String recommendation,  // BUY, SELL, HOLD

    @JsonProperty("confidence_score")
    Integer confidenceScore,

    @JsonProperty("technical_analysis")
    String technicalAnalysis,

    @JsonProperty("supply_analysis")
    String supplyAnalysis,

    @JsonProperty("risk_factors")
    List<String> riskFactors
) {
}
