package com.project.stock.domain.stock.application.port.in;

import java.time.LocalDate;

/**
 * 주식 시세 동기화 Use Case.
 */
public interface SyncStockPricesUseCase {

    /**
     * 특정 종목의 시세 데이터를 외부 API에서 조회하여 DB에 저장합니다.
     *
     * @param stockCode 종목코드
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 저장된 데이터 건수
     */
    int syncStockPrices(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 종목의 최근 N일 시세 데이터를 동기화합니다.
     *
     * @param stockCode 종목코드
     * @param days      일수
     * @return 저장된 데이터 건수
     */
    int syncRecentStockPrices(String stockCode, int days);
}
