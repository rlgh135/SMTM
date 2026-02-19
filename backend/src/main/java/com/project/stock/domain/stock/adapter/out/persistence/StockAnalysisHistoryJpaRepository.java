package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.domain.StockAnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * StockAnalysisHistory JPA Repository.
 */
public interface StockAnalysisHistoryJpaRepository extends JpaRepository<StockAnalysisHistory, Long> {

    /**
     * 특정 종목의 특정 날짜 분석 결과를 조회합니다.
     */
    @Query("SELECT h FROM StockAnalysisHistory h WHERE h.stock.id = :stockId AND h.analyzedDate = :date")
    Optional<StockAnalysisHistory> findByStockIdAndAnalyzedDate(
        @Param("stockId") Long stockId,
        @Param("date") LocalDate date
    );

    /**
     * 특정 종목의 최근 N개 분석 이력을 조회합니다.
     */
    @Query("SELECT h FROM StockAnalysisHistory h WHERE h.stock.id = :stockId ORDER BY h.analyzedDate DESC LIMIT :limit")
    List<StockAnalysisHistory> findTopNByStockIdOrderByDateDesc(
        @Param("stockId") Long stockId,
        @Param("limit") int limit
    );

    /**
     * 특정 날짜의 모든 분석 결과를 조회합니다.
     */
    List<StockAnalysisHistory> findByAnalyzedDate(LocalDate date);

    /**
     * 특정 날짜의 분석 이력이 존재하는지 확인합니다.
     */
    boolean existsByAnalyzedDate(LocalDate date);
}
