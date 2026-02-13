package com.project.stock.domain.stock.application.port.out;

import com.project.stock.domain.stock.domain.Watchlist;

import java.util.List;

/**
 * 관심 종목 조회 Port.
 */
public interface LoadWatchlistPort {

    /**
     * 활성화된 관심 종목 목록을 조회합니다.
     */
    List<Watchlist> findAllActive();
}
