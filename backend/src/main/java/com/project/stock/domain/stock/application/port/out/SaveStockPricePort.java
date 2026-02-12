package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.StockPrice;

import java.util.List;

/**
 * 주식 시세 저장 Port.
 */
public interface SaveStockPricePort {

    /**
     * 시세 데이터를 저장합니다.
     */
    StockPrice save(StockPrice stockPrice);

    /**
     * 시세 데이터를 일괄 저장합니다.
     */
    List<StockPrice> saveAll(List<StockPrice> stockPrices);
}
