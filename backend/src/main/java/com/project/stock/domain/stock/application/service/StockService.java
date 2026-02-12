package com.project.stock.domain.stock.application.service;

import com.project.stock.domain.stock.application.port.in.AnalyzeStockUseCase;
import com.project.stock.domain.stock.application.port.out.AiAnalysisPort;
import com.project.stock.domain.stock.application.port.out.LoadStockPort;
import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주식 분석 유스케이스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StockService implements AnalyzeStockUseCase {

    private final LoadStockPort loadStockPort;
    private final AiAnalysisPort aiAnalysisPort;

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
}
