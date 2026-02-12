package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisResult;

/**
 * AI 분석 요청 포트 (Driven Port).
 */
public interface AiAnalysisPort {

    StockAnalysisResult requestAnalysis(Stock stock);
}
