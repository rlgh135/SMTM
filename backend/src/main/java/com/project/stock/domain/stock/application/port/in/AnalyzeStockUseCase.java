package com.project.stock.domain.stock.application.port.in;

import com.project.stock.domain.stock.domain.StockAnalysisResult;

/**
 * 주식 분석 유스케이스 인터페이스 (Driving Port).
 */
public interface AnalyzeStockUseCase {

    StockAnalysisResult analyze(String stockCode);
}
