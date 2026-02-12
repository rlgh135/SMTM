package com.project.stock.domain.stock.adapter.in.web;

import com.project.stock.domain.stock.adapter.in.web.dto.StockAnalysisResponse;
import com.project.stock.domain.stock.application.port.in.AnalyzeStockUseCase;
import com.project.stock.domain.stock.domain.StockAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주식 분석 API 컨트롤러 (Driving Adapter).
 */
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final AnalyzeStockUseCase analyzeStockUseCase;

    @GetMapping("/{stockCode}/analysis")
    public ResponseEntity<StockAnalysisResponse> analyzeStock(@PathVariable String stockCode) {
        StockAnalysisResult result = analyzeStockUseCase.analyze(stockCode);
        return ResponseEntity.ok(StockAnalysisResponse.from(result));
    }
}
