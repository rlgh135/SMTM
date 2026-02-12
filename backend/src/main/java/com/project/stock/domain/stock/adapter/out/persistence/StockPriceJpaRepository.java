package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.domain.StockPrice;
import com.project.stock.domain.stock.domain.StockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * StockPrice JPA Repository.
 */
public interface StockPriceJpaRepository extends JpaRepository<StockPrice, StockPriceId> {

    /**
     * 특정 종목의 기간별 시세 데이터를 조회합니다.
     */
    @Query("SELECT sp FROM StockPrice sp WHERE sp.stock.id = :stockId AND sp.id.date BETWEEN :startDate AND :endDate ORDER BY sp.id.date DESC")
    List<StockPrice> findByStockIdAndDateBetween(
        @Param("stockId") Long stockId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 종목의 최근 N일 시세 데이터를 조회합니다.
     */
    @Query("SELECT sp FROM StockPrice sp WHERE sp.stock.id = :stockId ORDER BY sp.id.date DESC LIMIT :limit")
    List<StockPrice> findTopNByStockIdOrderByDateDesc(
        @Param("stockId") Long stockId,
        @Param("limit") int limit
    );
}
