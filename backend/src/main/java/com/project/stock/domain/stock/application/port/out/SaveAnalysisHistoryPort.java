package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.StockAnalysisHistory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 분석 이력 저장 Port.
 */
public interface SaveAnalysisHistoryPort {

    /**
     * 분석 이력을 저장합니다.
     */
    StockAnalysisHistory save(StockAnalysisHistory history);

    /**
     * 특정 종목의 특정 날짜 분석 결과를 조회합니다.
     */
    Optional<StockAnalysisHistory> findByStockIdAndDate(Long stockId, LocalDate date);
}
