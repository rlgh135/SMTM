package com.project.stock.domain.stock.application.service;

import com.project.stock.domain.stock.adapter.out.external.kis.KisClient;
import com.project.stock.domain.stock.adapter.out.external.kis.KisOhlcvResponse;
import com.project.stock.domain.stock.application.port.in.AnalyzeStockUseCase;
import com.project.stock.domain.stock.application.port.in.GetStockPricesUseCase;
import com.project.stock.domain.stock.application.port.in.SyncStockPricesUseCase;
import com.project.stock.domain.stock.application.port.out.AiAnalysisPort;
import com.project.stock.domain.stock.application.port.out.LoadStockPort;
import com.project.stock.domain.stock.application.port.out.LoadStockPricePort;
import com.project.stock.domain.stock.application.port.out.SaveStockPricePort;
import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisResult;
import com.project.stock.domain.stock.domain.StockPrice;
import com.project.stock.domain.stock.domain.StockPriceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 주식 분석 및 시세 동기화 유스케이스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StockService implements AnalyzeStockUseCase, SyncStockPricesUseCase, GetStockPricesUseCase {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LoadStockPort loadStockPort;
    private final AiAnalysisPort aiAnalysisPort;
    private final LoadStockPricePort loadStockPricePort;
    private final SaveStockPricePort saveStockPricePort;
    private final KisClient kisClient;

    @Override
    public StockAnalysisResult analyze(String stockCode) {
        log.info("주식 분석 시작: stockCode={}", stockCode);

        Stock stock = loadStockPort.loadByCode(stockCode)
                .orElseThrow(() -> {
                    log.error("종목을 찾을 수 없습니다: stockCode={}", stockCode);
                    return new IllegalArgumentException("존재하지 않는 종목 코드: " + stockCode);
                });

        StockAnalysisResult result = aiAnalysisPort.requestAnalysis(stock);
        log.info("주식 분석 완료: stockCode={}, recommendation={}", stockCode, result.recommendation());

        return result;
    }

    @Override
    @Transactional
    public int syncStockPrices(String stockCode, LocalDate startDate, LocalDate endDate) {
        log.info("시세 동기화 시작: stockCode={}, 기간={} ~ {}", stockCode, startDate, endDate);

        // 종목 정보 조회
        Stock stock = loadStockPort.loadByCode(stockCode)
            .orElseThrow(() -> {
                log.error("종목을 찾을 수 없습니다: stockCode={}", stockCode);
                return new IllegalArgumentException("존재하지 않는 종목 코드: " + stockCode);
            });

        // KIS API에서 시세 데이터 조회
        KisOhlcvResponse response = kisClient.fetchDailyPrices(stockCode, startDate, endDate);

        if (response.output1() == null || response.output1().isEmpty()) {
            log.warn("조회된 시세 데이터가 없습니다: stockCode={}", stockCode);
            return 0;
        }

        // DTO를 엔티티로 변환 및 저장
        List<StockPrice> stockPrices = new ArrayList<>();
        for (KisOhlcvResponse.DailyPrice dailyPrice : response.output1()) {
            LocalDate date = LocalDate.parse(dailyPrice.businessDate(), DATE_FORMATTER);
            StockPriceId priceId = new StockPriceId(stock.getId(), date);

            // 중복 체크: 이미 존재하면 업데이트, 없으면 생성
            StockPrice stockPrice = loadStockPricePort.findById(priceId)
                .orElseGet(() -> StockPrice.builder()
                    .stock(stock)
                    .date(date)
                    .openPrice(new BigDecimal(dailyPrice.openPrice()))
                    .highPrice(new BigDecimal(dailyPrice.highPrice()))
                    .lowPrice(new BigDecimal(dailyPrice.lowPrice()))
                    .closePrice(new BigDecimal(dailyPrice.closePrice()))
                    .volume(Long.parseLong(dailyPrice.volume()))
                    .changeRate(new BigDecimal(dailyPrice.changeRate()))
                    .build());

            // 기존 데이터 업데이트
            if (loadStockPricePort.findById(priceId).isPresent()) {
                stockPrice.updatePriceData(
                    new BigDecimal(dailyPrice.openPrice()),
                    new BigDecimal(dailyPrice.highPrice()),
                    new BigDecimal(dailyPrice.lowPrice()),
                    new BigDecimal(dailyPrice.closePrice()),
                    Long.parseLong(dailyPrice.volume()),
                    new BigDecimal(dailyPrice.changeRate())
                );
            }

            stockPrices.add(stockPrice);
        }

        // 일괄 저장
        List<StockPrice> saved = saveStockPricePort.saveAll(stockPrices);
        log.info("시세 동기화 완료: stockCode={}, 저장 건수={}", stockCode, saved.size());

        return saved.size();
    }

    @Override
    @Transactional
    public int syncRecentStockPrices(String stockCode, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return syncStockPrices(stockCode, startDate, endDate);
    }

    @Override
    public List<StockPrice> getStockPrices(String stockCode, LocalDate startDate, LocalDate endDate) {
        log.info("시세 조회 요청: stockCode={}, 기간={} ~ {}", stockCode, startDate, endDate);

        Stock stock = loadStockPort.loadByCode(stockCode)
            .orElseThrow(() -> {
                log.error("종목을 찾을 수 없습니다: stockCode={}", stockCode);
                return new IllegalArgumentException("존재하지 않는 종목 코드: " + stockCode);
            });

        List<StockPrice> prices = loadStockPricePort.findByStockIdAndDateBetween(
            stock.getId(),
            startDate,
            endDate
        );

        log.info("시세 조회 완료: stockCode={}, 조회 건수={}", stockCode, prices.size());
        return prices;
    }

    @Override
    public List<StockPrice> getRecentStockPrices(String stockCode, int days) {
        log.info("최근 시세 조회 요청: stockCode={}, days={}", stockCode, days);

        Stock stock = loadStockPort.loadByCode(stockCode)
            .orElseThrow(() -> {
                log.error("종목을 찾을 수 없습니다: stockCode={}", stockCode);
                return new IllegalArgumentException("존재하지 않는 종목 코드: " + stockCode);
            });

        List<StockPrice> prices = loadStockPricePort.findTopNByStockIdOrderByDateDesc(
            stock.getId(),
            days
        );

        log.info("최근 시세 조회 완료: stockCode={}, 조회 건수={}", stockCode, prices.size());
        return prices;
    }
}
