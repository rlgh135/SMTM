package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.application.port.out.LoadStockPort;
import com.project.stock.domain.stock.domain.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 주식 영속성 어댑터 (Driven Adapter).
 */
@Component
@RequiredArgsConstructor
class StockPersistenceAdapter implements LoadStockPort {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Optional<Stock> loadByCode(String stockCode) {
        return stockJpaRepository.findByStockCode(stockCode);
    }
}
