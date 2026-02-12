package com.project.stock.domain.stock.adapter.in.web.dto;

import com.project.stock.domain.stock.domain.StockAnalysisResult;

import java.util.List;

/**
 * 주식 분석 응답 DTO.
 */
public record StockAnalysisResponse(
        String recommendation,
        int confidenceScore,
        String technicalAnalysis,
        String supplyAnalysis,
        List<String> riskFactors
) {

    public static StockAnalysisResponse from(StockAnalysisResult result) {
        return new StockAnalysisResponse(
                result.recommendation().name(),
                result.confidenceScore(),
                result.technicalAnalysis(),
                result.supplyAnalysis(),
                result.riskFactors()
        );
    }
}
