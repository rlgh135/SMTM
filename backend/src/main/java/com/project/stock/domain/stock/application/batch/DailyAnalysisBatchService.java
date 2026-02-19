package com.project.stock.domain.stock.application.batch;

import com.project.stock.domain.stock.application.port.in.SyncStockPricesUseCase;
import com.project.stock.domain.stock.application.port.out.AiAnalysisPort;
import com.project.stock.domain.stock.application.port.out.LoadWatchlistPort;
import com.project.stock.domain.stock.application.port.out.SaveAnalysisHistoryPort;
import com.project.stock.domain.stock.domain.Stock;
import com.project.stock.domain.stock.domain.StockAnalysisHistory;
import com.project.stock.domain.stock.domain.StockAnalysisResult;
import com.project.stock.domain.stock.domain.Watchlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 일일 자동 분석 배치 서비스.
 * 평일 오후 4시에 관심 종목의 시세를 동기화하고 AI 분석을 수행합니다.
 * BATCH_ENABLED=false 환경변수로 비활성화할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.daily-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class DailyAnalysisBatchService {

    private final LoadWatchlistPort loadWatchlistPort;
    private final SyncStockPricesUseCase syncStockPricesUseCase;
    private final AiAnalysisPort aiAnalysisPort;
    private final SaveAnalysisHistoryPort saveAnalysisHistoryPort;

    /**
     * 평일 오후 4시에 실행되는 배치 작업.
     * 주식 시장 종료 후 당일 시세를 동기화하고 AI 분석을 수행합니다.
     */
    @Scheduled(cron = "${batch.daily-analysis.cron:0 0 16 * * MON-FRI}", zone = "Asia/Seoul")
    @Transactional
    public void executeDailyAnalysis() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("========== 일일 분석 배치 시작: {} ==========", startTime);

        // 주말 체크 (혹시 모를 수동 실행 대비)
        if (isWeekend()) {
            log.warn("주말에는 배치를 실행하지 않습니다. 배치 종료.");
            return;
        }

        // 오늘 이미 배치가 실행되었는지 확인
        LocalDate today = LocalDate.now();
        if (saveAnalysisHistoryPort.existsByAnalyzedDate(today)) {
            log.info("오늘({}) 이미 배치가 실행되었습니다. 배치 종료.", today);
            return;
        }

        // 활성화된 관심 종목 조회
        List<Watchlist> activeWatchlist = loadWatchlistPort.findAllActive();
        if (activeWatchlist.isEmpty()) {
            log.warn("활성화된 관심 종목이 없습니다. 배치 종료.");
            return;
        }

        log.info("분석 대상 종목: {} 개", activeWatchlist.size());

        int successCount = 0;
        int failCount = 0;

        // 각 종목에 대해 시세 동기화 및 분석 수행
        for (Watchlist watchlistItem : activeWatchlist) {
            Stock stock = watchlistItem.getStock();
            String stockCode = stock.getStockCode();

            try {
                log.info("처리 시작: {} ({})", stock.getStockName(), stockCode);

                // 1. 당일 시세 동기화 (최근 5일 업데이트 - 누락된 데이터 보정)
                int syncedCount = syncStockPricesUseCase.syncRecentStockPrices(stockCode, 5);
                log.info("시세 동기화 완료: {} 건", syncedCount);

                // 2. 이미 오늘 분석한 이력이 있는지 확인
                Optional<StockAnalysisHistory> existingHistory =
                    saveAnalysisHistoryPort.findByStockIdAndDate(stock.getId(), today);

                if (existingHistory.isPresent()) {
                    log.info("이미 분석 완료: {} - 건너뜀", stockCode);
                    successCount++;
                    continue;
                }

                // 3. AI 분석 수행
                StockAnalysisResult analysisResult = aiAnalysisPort.requestAnalysis(stock);
                log.info("AI 분석 완료: recommendation={}, confidence={}",
                    analysisResult.recommendation(), analysisResult.confidenceScore());

                // 4. 분석 이력 저장
                StockAnalysisHistory history = StockAnalysisHistory.fromAnalysisResult(
                    stock, today, analysisResult
                );
                saveAnalysisHistoryPort.save(history);
                log.info("분석 이력 저장 완료: {}", stockCode);

                successCount++;

                // API Rate Limit 대응: 종목 간 딜레이 (3초)
                Thread.sleep(3000);

            } catch (InterruptedException e) {
                log.error("배치 중단: {}", stockCode, e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("분석 실패: {} - {}", stockCode, e.getMessage(), e);
                failCount++;
                // 실패해도 다음 종목 계속 진행
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

        log.info("========== 일일 분석 배치 종료 ==========");
        log.info("총 대상: {} 개 | 성공: {} 개 | 실패: {} 개 | 소요 시간: {}초",
            activeWatchlist.size(), successCount, failCount, durationSeconds);
    }

    /**
     * 주말 여부를 확인합니다.
     */
    private boolean isWeekend() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 수동 배치 실행 (테스트용).
     * 운영 환경에서는 제거하거나 별도 권한 체크 필요.
     */
    public void executeManually() {
        log.info("수동 배치 실행 요청");
        executeDailyAnalysis();
    }
}
