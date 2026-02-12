package com.project.stock.domain.stock.application.port.in;

import com.project.stock.domain.stock.domain.StockPrice;

import java.time.LocalDate;
import java.util.List;

/**
 * 주식 시세 조회 Use Case.
 */
public interface GetStockPricesUseCase {

    /**
     * 특정 종목의 기간별 시세 데이터를 조회합니다.
     */
    List<StockPrice> getStockPrices(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 종목의 최근 N일 시세 데이터를 조회합니다.
     */
    List<StockPrice> getRecentStockPrices(String stockCode, int days);
}
