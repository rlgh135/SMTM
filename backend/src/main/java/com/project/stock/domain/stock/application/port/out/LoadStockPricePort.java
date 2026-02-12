package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.StockPrice;
import com.project.stock.domain.stock.domain.StockPriceId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 주식 시세 조회 Port.
 */
public interface LoadStockPricePort {

    /**
     * ID로 시세 데이터를 조회합니다.
     */
    Optional<StockPrice> findById(StockPriceId id);

    /**
     * 특정 종목의 기간별 시세 데이터를 조회합니다.
     */
    List<StockPrice> findByStockIdAndDateBetween(Long stockId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 종목의 최근 N일 시세 데이터를 조회합니다.
     */
    List<StockPrice> findTopNByStockIdOrderByDateDesc(Long stockId, int limit);
}
