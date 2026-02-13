package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.domain.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Watchlist JPA Repository.
 */
public interface WatchlistJpaRepository extends JpaRepository<Watchlist, Long> {

    /**
     * 활성화된 관심 종목을 우선순위 순으로 조회합니다.
     */
    @Query("SELECT w FROM Watchlist w WHERE w.isActive = true ORDER BY w.priority ASC, w.stock.stockCode ASC")
    List<Watchlist> findAllActiveOrderByPriority();
}
