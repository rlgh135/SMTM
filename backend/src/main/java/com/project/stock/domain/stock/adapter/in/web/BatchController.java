package com.project.stock.domain.stock.adapter.in.web;

import com.project.stock.domain.stock.application.batch.DailyAnalysisBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 배치 작업 수동 실행 컨트롤러.
 * BATCH_ENABLED=false 시 빈 자체가 등록되지 않습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.daily-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class BatchController {

    private final DailyAnalysisBatchService dailyAnalysisBatchService;

    /**
     * 일일 분석 배치를 수동으로 실행합니다.
     *
     * @return 실행 결과 메시지
     */
    @PostMapping("/daily-analysis")
    public ResponseEntity<Map<String, String>> executeDailyAnalysis() {
        log.info("일일 분석 배치 수동 실행 요청");

        // 비동기 실행 (요청은 즉시 반환)
        new Thread(() -> {
            try {
                dailyAnalysisBatchService.executeManually();
            } catch (Exception e) {
                log.error("배치 실행 중 오류 발생", e);
            }
        }).start();

        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "일일 분석 배치가 백그라운드에서 실행되었습니다. 로그를 확인하세요."
        ));
    }
}
