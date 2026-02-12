package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.Stock;

import java.util.Optional;

/**
 * 주식 데이터 조회 포트 (Driven Port).
 */
public interface LoadStockPort {

    Optional<Stock> loadByCode(String stockCode);
}
