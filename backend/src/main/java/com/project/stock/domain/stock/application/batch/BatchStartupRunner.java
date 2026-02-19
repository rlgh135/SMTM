package com.project.stock.domain.stock.application.batch;

import com.project.stock.domain.stock.application.port.out.SaveAnalysisHistoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 DB 상태를 확인하고 필요시 배치를 즉시 실행하는 Runner.
 * 분석 이력 테이블이 비어있으면 즉시 배치를 실행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.daily-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class BatchStartupRunner implements ApplicationRunner {

    private final SaveAnalysisHistoryPort saveAnalysisHistoryPort;
    private final DailyAnalysisBatchService dailyAnalysisBatchService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 서버 시작 - DB 상태 확인 ==========");

        long analysisHistoryCount = saveAnalysisHistoryPort.count();
        log.info("분석 이력 테이블 레코드 수: {}", analysisHistoryCount);

        if (analysisHistoryCount == 0) {
            log.info("분석 이력 테이블이 비어있습니다. 즉시 배치를 실행합니다.");
            try {
                dailyAnalysisBatchService.executeDailyAnalysis();
                log.info("초기 배치 실행 완료");
            } catch (Exception e) {
                log.error("초기 배치 실행 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            log.info("분석 이력이 존재합니다. 정기 스케줄에 따라 배치가 실행됩니다.");
        }
    }
}
