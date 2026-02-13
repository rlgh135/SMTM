# KR-Quant-Agent 배치 작업 가이드

Phase 5에서 구현된 일일 자동 분석 배치 작업에 대한 가이드입니다.

## 개요

**DailyAnalysisBatchService**는 평일 오후 4시(한국 시간)에 자동으로 실행되어 관심 종목의 시세를 동기화하고 AI 분석을 수행합니다.

### 배치 실행 시간

- **스케줄:** 평일 월~금, 오후 4시 (16:00 KST)
- **타임존:** Asia/Seoul
- **Cron 표현식:** `0 0 16 * * MON-FRI`

**왜 오후 4시?**
- 한국 주식 시장 종료: 오후 3시 30분
- KIS API 당일 시세 제공: 오후 3시 35분 이후
- 여유 시간 확보: 25분 버퍼

## 배치 작업 흐름

```
1. 배치 시작 (평일 16:00)
   ↓
2. 주말 체크 (만약 주말이면 즉시 종료)
   ↓
3. 활성화된 관심 종목 조회 (watchlist 테이블)
   ↓
4. 각 종목에 대해 순차 처리:
   ├─ a. 최근 5일 시세 동기화 (KIS API)
   ├─ b. 오늘 이미 분석했는지 확인 (중복 방지)
   ├─ c. AI 분석 수행 (Backend → AI Worker → OpenAI)
   ├─ d. 분석 결과 저장 (stock_analysis_history 테이블)
   └─ e. 3초 대기 (Rate Limit 대응)
   ↓
5. 배치 종료 (성공/실패 통계 로깅)
```

## 데이터베이스 스키마

### 1. Watchlist (관심 종목)

```sql
CREATE TABLE watchlist (
  id BIGSERIAL PRIMARY KEY,
  stock_id BIGINT NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT true,
  priority INTEGER NOT NULL DEFAULT 999,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

**컬럼 설명:**
- `stock_id`: 종목 ID (stock 테이블 FK)
- `is_active`: 활성화 여부 (true인 종목만 배치 대상)
- `priority`: 우선순위 (낮을수록 먼저 처리)

### 2. StockAnalysisHistory (분석 이력)

```sql
CREATE TABLE stock_analysis_history (
  id BIGSERIAL PRIMARY KEY,
  stock_id BIGINT NOT NULL,
  analyzed_date DATE NOT NULL,
  recommendation VARCHAR(10) NOT NULL,  -- BUY/SELL/HOLD
  confidence_score INTEGER NOT NULL,    -- 0-100
  technical_analysis TEXT,
  supply_analysis TEXT,
  created_at TIMESTAMP NOT NULL,
  UNIQUE (stock_id, analyzed_date)
);
```

**특징:**
- `UNIQUE (stock_id, analyzed_date)`: 하루에 한 번만 분석 저장
- 리스크 요인은 별도 테이블 (`stock_analysis_risk_factors`)

## 배치 설정

### application.yml

```yaml
batch:
  daily-analysis:
    cron: ${BATCH_CRON:0 0 16 * * MON-FRI}
    enabled: ${BATCH_ENABLED:true}
```

### 환경변수 (.env)

```bash
# 배치 활성화 여부
BATCH_ENABLED=true

# Cron 표현식 (커스터마이징 가능)
BATCH_CRON=0 0 16 * * MON-FRI
```

**Cron 표현식 예시:**
- `0 0 16 * * MON-FRI` - 평일 오후 4시 (기본값)
- `0 30 15 * * MON-FRI` - 평일 오후 3시 30분
- `0 0 9 * * MON-FRI` - 평일 오전 9시 (장 시작 전)
- `0 0 * * * *` - 매시간 정각 (테스트용)

## 관심 종목 관리

### 종목 추가

```sql
INSERT INTO watchlist (stock_id, is_active, priority, created_at, updated_at)
VALUES (
  (SELECT id FROM stock WHERE stock_code = '005930'),
  true,
  1,
  NOW(),
  NOW()
);
```

### 종목 활성화/비활성화

```sql
-- 비활성화 (배치 대상에서 제외)
UPDATE watchlist
SET is_active = false, updated_at = NOW()
WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930');

-- 다시 활성화
UPDATE watchlist
SET is_active = true, updated_at = NOW()
WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930');
```

### 우선순위 변경

```sql
-- 우선순위를 1로 설정 (가장 먼저 처리)
UPDATE watchlist
SET priority = 1, updated_at = NOW()
WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930');
```

## 수동 배치 실행

### REST API

개발/테스트 환경에서 배치를 즉시 실행할 수 있습니다.

```bash
POST http://localhost:8080/api/v1/batch/daily-analysis
```

**응답:**
```json
{
  "status": "started",
  "message": "일일 분석 배치가 백그라운드에서 실행되었습니다. 로그를 확인하세요."
}
```

**주의사항:**
- 비동기 실행되므로 요청은 즉시 반환됩니다
- 실제 배치 진행 상황은 로그로 확인해야 합니다
- **운영 환경에서는 보안을 위해 엔드포인트 제거 권장**

### 로그 확인

```bash
# Backend 로그 (Spring Boot)
tail -f logs/application.log | grep "일일 분석 배치"

# Docker Compose
docker-compose logs -f backend | grep "일일 분석 배치"
```

**로그 예시:**
```
2024-01-15 16:00:00 INFO  - ========== 일일 분석 배치 시작: 2024-01-15T16:00:00 ==========
2024-01-15 16:00:00 INFO  - 분석 대상 종목: 2 개
2024-01-15 16:00:00 INFO  - 처리 시작: 삼성전자 (005930)
2024-01-15 16:00:01 INFO  - 시세 동기화 완료: 5 건
2024-01-15 16:00:05 INFO  - AI 분석 완료: recommendation=BUY, confidence=78
2024-01-15 16:00:05 INFO  - 분석 이력 저장 완료: 005930
2024-01-15 16:00:08 INFO  - 처리 시작: SK하이닉스 (000660)
...
2024-01-15 16:00:20 INFO  - ========== 일일 분석 배치 종료 ==========
2024-01-15 16:00:20 INFO  - 총 대상: 2 개 | 성공: 2 개 | 실패: 0 개 | 소요 시간: 20초
```

## 에러 처리

### 1. 시세 동기화 실패

**원인:**
- KIS API 토큰 만료
- KIS API Rate Limit 초과
- 네트워크 오류

**대응:**
- 해당 종목은 실패로 기록하고 다음 종목 계속 처리
- 에러 로그 기록: `log.error("분석 실패: {}", stockCode, e)`

### 2. AI 분석 실패

**원인:**
- AI Worker 다운
- OpenAI API 키 오류
- 시세 데이터 부족 (최소 30일 필요)

**대응:**
- 해당 종목은 실패로 기록하고 다음 종목 계속 처리
- 다음 배치 실행 시 재시도

### 3. 중복 분석 방지

**상황:** 배치가 하루에 여러 번 실행되는 경우

**대응:**
```java
Optional<StockAnalysisHistory> existingHistory =
    saveAnalysisHistoryPort.findByStockIdAndDate(stock.getId(), today);

if (existingHistory.isPresent()) {
    log.info("이미 분석 완료: {} - 건너뜀", stockCode);
    continue;
}
```

## 성능 최적화

### Rate Limit 대응

KIS API 및 OpenAI API는 초당 호출 제한이 있습니다.

**현재 설정:**
- 종목 간 3초 대기 (`Thread.sleep(3000)`)
- 20개 종목 기준: 약 1분 소요

**개선 방안:**
1. **병렬 처리:** CompletableFuture로 동시 N개 처리
2. **우선순위 큐:** 중요 종목 먼저 처리
3. **배치 시간 분산:** 오전/오후 두 번 나누어 실행

### 메모리 최적화

**대용량 처리 시:**
```java
// Batch Size 설정
@Transactional
public void processBatch(List<Watchlist> batch) {
    // 100개씩 처리
}

// 전체 목록을 100개씩 나누어 처리
List<Watchlist> allWatchlist = loadWatchlistPort.findAllActive();
List<List<Watchlist>> batches = partition(allWatchlist, 100);

for (List<Watchlist> batch : batches) {
    processBatch(batch);
    entityManager.clear(); // 메모리 해제
}
```

## 분석 이력 조회

### 최근 분석 결과 조회

```sql
SELECT
  s.stock_code,
  s.stock_name,
  h.analyzed_date,
  h.recommendation,
  h.confidence_score
FROM stock_analysis_history h
JOIN stock s ON h.stock_id = s.id
WHERE h.analyzed_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY h.analyzed_date DESC, h.confidence_score DESC;
```

### 특정 종목의 분석 트렌드

```sql
SELECT
  analyzed_date,
  recommendation,
  confidence_score,
  technical_analysis
FROM stock_analysis_history
WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930')
ORDER BY analyzed_date DESC
LIMIT 30;
```

### 매수 추천 종목 (신뢰도 70% 이상)

```sql
SELECT
  s.stock_code,
  s.stock_name,
  h.recommendation,
  h.confidence_score,
  h.technical_analysis
FROM stock_analysis_history h
JOIN stock s ON h.stock_id = s.id
WHERE h.analyzed_date = CURRENT_DATE
  AND h.recommendation = 'BUY'
  AND h.confidence_score >= 70
ORDER BY h.confidence_score DESC;
```

## 모니터링

### 배치 성공률

```sql
SELECT
  analyzed_date,
  COUNT(*) as total_analyzed,
  COUNT(*) FILTER (WHERE recommendation = 'BUY') as buy_count,
  COUNT(*) FILTER (WHERE recommendation = 'SELL') as sell_count,
  COUNT(*) FILTER (WHERE recommendation = 'HOLD') as hold_count,
  ROUND(AVG(confidence_score), 2) as avg_confidence
FROM stock_analysis_history
WHERE analyzed_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY analyzed_date
ORDER BY analyzed_date DESC;
```

### 배치 실행 로그 통계

Spring Boot Actuator를 사용하여 메트릭 수집 가능:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

## 운영 체크리스트

### 배치 시작 전

- [ ] Watchlist에 종목이 추가되어 있는가?
- [ ] KIS API 키가 유효한가?
- [ ] OpenAI API 키가 유효한가?
- [ ] Redis가 실행 중인가?
- [ ] AI Worker가 실행 중인가?

### 배치 실행 후

- [ ] 로그에서 배치 종료 메시지 확인
- [ ] 성공/실패 통계 확인
- [ ] `stock_analysis_history` 테이블에 당일 데이터 확인
- [ ] 실패한 종목이 있다면 원인 분석

### 주간 점검

- [ ] 최근 7일 배치 성공률 확인
- [ ] 평균 신뢰도 점수 확인
- [ ] 추천 분포 (BUY/SELL/HOLD) 확인
- [ ] 디스크 사용량 확인 (분석 이력 증가)

## 트러블슈팅

### Q1. 배치가 실행되지 않아요

**확인 사항:**
1. `@EnableScheduling`이 활성화되어 있는가?
2. `BATCH_ENABLED=true`로 설정되어 있는가?
3. Cron 표현식이 올바른가?
4. 타임존이 `Asia/Seoul`인가?

### Q2. 모든 종목이 실패해요

**확인 사항:**
1. AI Worker가 실행 중인가? (`http://localhost:8000/docs`)
2. Backend ↔ AI Worker 통신이 되는가?
3. OpenAI API 키가 유효한가?
4. 시세 데이터가 충분한가? (최소 30일)

### Q3. 배치 실행 시간을 변경하고 싶어요

`.env` 파일:
```bash
BATCH_CRON=0 30 15 * * MON-FRI  # 오후 3시 30분으로 변경
```

재시작 후 적용됩니다.

## 확장 아이디어

1. **알림 기능:** 매수 추천 종목이 나오면 이메일/슬랙 알림
2. **백테스팅:** 과거 추천 결과와 실제 수익률 비교
3. **자동 매매:** 특정 조건 충족 시 자동 주문 (주의 필요!)
4. **대시보드:** Grafana로 배치 실행 현황 시각화
5. **멀티 워치리스트:** 사용자별 관심 종목 관리

## 참고 자료

- [Spring @Scheduled Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Cron Expression Generator](https://crontab.guru/)
- [KIS API Documentation](https://apiportal.koreainvestment.com/)
