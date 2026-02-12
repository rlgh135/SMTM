package com.project.stock.domain.stock.adapter.in.web;

import com.project.stock.domain.stock.adapter.in.web.dto.StockPriceResponse;
import com.project.stock.domain.stock.adapter.in.web.dto.SyncStockPricesRequest;
import com.project.stock.domain.stock.application.port.in.GetStockPricesUseCase;
import com.project.stock.domain.stock.application.port.in.SyncStockPricesUseCase;
import com.project.stock.domain.stock.domain.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 주식 시세 조회 및 동기화 컨트롤러.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/prices")
@RequiredArgsConstructor
public class StockPriceController {

    private final SyncStockPricesUseCase syncStockPricesUseCase;
    private final GetStockPricesUseCase getStockPricesUseCase;

    /**
     * 특정 종목의 시세 데이터를 동기화합니다.
     *
     * @param stockCode 종목코드
     * @param request   동기화 요청
     * @return 저장된 데이터 건수
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncStockPrices(
        @PathVariable String stockCode,
        @RequestBody SyncStockPricesRequest request
    ) {
        log.info("시세 동기화 요청: stockCode={}, startDate={}, endDate={}",
            stockCode, request.startDate(), request.endDate());

        int savedCount = syncStockPricesUseCase.syncStockPrices(
            stockCode,
            request.startDate(),
            request.endDate()
        );

        return ResponseEntity.ok(Map.of(
            "stockCode", stockCode,
            "savedCount", savedCount,
            "startDate", request.startDate(),
            "endDate", request.endDate()
        ));
    }

    /**
     * 특정 종목의 최근 N일 시세 데이터를 동기화합니다.
     *
     * @param stockCode 종목코드
     * @param days      조회할 일수 (기본값: 120일)
     * @return 저장된 데이터 건수
     */
    @PostMapping("/sync/recent")
    public ResponseEntity<Map<String, Object>> syncRecentStockPrices(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "120") int days
    ) {
        log.info("최근 시세 동기화 요청: stockCode={}, days={}", stockCode, days);

        int savedCount = syncStockPricesUseCase.syncRecentStockPrices(stockCode, days);

        return ResponseEntity.ok(Map.of(
            "stockCode", stockCode,
            "savedCount", savedCount,
            "days", days
        ));
    }

    /**
     * 특정 종목의 최근 N일 시세 데이터를 조회합니다.
     *
     * @param stockCode 종목코드
     * @param days      조회할 일수 (기본값: 120일)
     * @return 시세 데이터 리스트
     */
    @GetMapping
    public ResponseEntity<List<StockPriceResponse>> getStockPrices(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "120") int days
    ) {
        log.info("시세 조회 요청: stockCode={}, days={}", stockCode, days);

        List<StockPrice> prices = getStockPricesUseCase.getRecentStockPrices(stockCode, days);

        List<StockPriceResponse> response = prices.stream()
            .map(price -> new StockPriceResponse(
                price.getId().getDate(),
                price.getOpenPrice(),
                price.getHighPrice(),
                price.getLowPrice(),
                price.getClosePrice(),
                price.getVolume(),
                price.getChangeRate()
            ))
            .toList();

        log.info("시세 조회 완료: stockCode={}, 조회 건수={}", stockCode, response.size());
        return ResponseEntity.ok(response);
    }
}
