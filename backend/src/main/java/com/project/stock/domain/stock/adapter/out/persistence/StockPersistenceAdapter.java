package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.application.port.out.LoadStockPort;
import com.project.stock.domain.stock.application.port.out.LoadStockPricePort;
import com.project.stock.domain.stock.application.port.out.LoadWatchlistPort;
import com.project.stock.domain.stock.application.port.out.SaveAnalysisHistoryPort;
import com.project.stock.domain.stock.application.port.out.SaveStockPricePort;
import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisHistory;
import com.project.stock.domain.stock.domain.StockPrice;
import com.project.stock.domain.stock.domain.StockPriceId;
import com.project.stock.domain.stock.domain.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 주식 영속성 어댑터 (Driven Adapter).
 * Stock, StockPrice, StockAnalysisHistory, Watchlist 데이터 접근을 담당.
 */
@Component
@RequiredArgsConstructor
class StockPersistenceAdapter implements LoadStockPort, LoadStockPricePort, SaveStockPricePort, SaveAnalysisHistoryPort, LoadWatchlistPort {

    private final StockJpaRepository stockJpaRepository;
    private final StockPriceJpaRepository stockPriceJpaRepository;
    private final StockAnalysisHistoryJpaRepository analysisHistoryJpaRepository;
    private final WatchlistJpaRepository watchlistJpaRepository;

    @Override
    public Optional<Stock> loadByCode(String stockCode) {
        return stockJpaRepository.findByStockCode(stockCode);
    }

    @Override
    public Optional<StockPrice> findById(StockPriceId id) {
        return stockPriceJpaRepository.findById(id);
    }

    @Override
    public List<StockPrice> findByStockIdAndDateBetween(Long stockId, LocalDate startDate, LocalDate endDate) {
        return stockPriceJpaRepository.findByStockIdAndDateBetween(stockId, startDate, endDate);
    }

    @Override
    public List<StockPrice> findTopNByStockIdOrderByDateDesc(Long stockId, int limit) {
        return stockPriceJpaRepository.findTopNByStockIdOrderByDateDesc(stockId, limit);
    }

    @Override
    public StockPrice save(StockPrice stockPrice) {
        return stockPriceJpaRepository.save(stockPrice);
    }

    @Override
    public List<StockPrice> saveAll(List<StockPrice> stockPrices) {
        return stockPriceJpaRepository.saveAll(stockPrices);
    }

    @Override
    public StockAnalysisHistory save(StockAnalysisHistory history) {
        return analysisHistoryJpaRepository.save(history);
    }

    @Override
    public Optional<StockAnalysisHistory> findByStockIdAndDate(Long stockId, LocalDate date) {
        return analysisHistoryJpaRepository.findByStockIdAndAnalyzedDate(stockId, date);
    }

    @Override
    public List<Watchlist> findAllActive() {
        return watchlistJpaRepository.findAllActiveOrderByPriority();
    }

    @Override
    public boolean existsByAnalyzedDate(LocalDate date) {
        return analysisHistoryJpaRepository.existsByAnalyzedDate(date);
    }

    @Override
    public long count() {
        return analysisHistoryJpaRepository.count();
    }

    @Override
    public List<StockAnalysisHistory> findByAnalyzedDate(LocalDate date) {
        return analysisHistoryJpaRepository.findByAnalyzedDate(date);
    }
}
