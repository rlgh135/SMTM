# KR-Quant-Agent 통합 테스트 가이드

Phase 1, 2, 3 구현 완료 후 전체 시스템 통합 테스트 가이드입니다.

## 전제 조건

### 1. 환경변수 설정

**Backend (.env):**
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/krstock
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# KIS API
KIS_APP_KEY=your-kis-app-key
KIS_APP_SECRET=your-kis-app-secret
KIS_BASE_URL=https://openapi.koreainvestment.com:9443
KIS_ACCOUNT_NO=your-account-number

# AI Worker
AI_WORKER_URL=http://localhost:8000
```

**AI Worker (.env):**
```bash
# OpenAI API
OPENAI_API_KEY=sk-your-openai-api-key

# Backend API
BACKEND_API_URL=http://localhost:8080

# Analysis Parameters
DEFAULT_LOOKBACK_DAYS=120
SIMILARITY_THRESHOLD=0.85
```

### 2. 데이터베이스 준비

PostgreSQL에 `krstock` 데이터베이스 생성:
```sql
CREATE DATABASE krstock;
```

테이블 스키마:
```sql
-- Stock 테이블
CREATE TABLE stock (
  id BIGSERIAL PRIMARY KEY,
  stock_code VARCHAR(10) NOT NULL UNIQUE,
  stock_name VARCHAR(100) NOT NULL,
  market VARCHAR(10) NOT NULL,
  current_price DECIMAL(18,2),
  updated_at TIMESTAMP NOT NULL
);

-- StockPrice 테이블
CREATE TABLE stock_price (
  stock_id BIGINT NOT NULL,
  date DATE NOT NULL,
  open_price DECIMAL(18,2) NOT NULL,
  high_price DECIMAL(18,2) NOT NULL,
  low_price DECIMAL(18,2) NOT NULL,
  close_price DECIMAL(18,2) NOT NULL,
  volume BIGINT NOT NULL,
  change_rate DECIMAL(10,4),
  PRIMARY KEY (stock_id, date),
  FOREIGN KEY (stock_id) REFERENCES stock(id)
);

CREATE INDEX idx_stock_price_stock_date ON stock_price(stock_id, date);

-- 샘플 데이터 (삼성전자)
INSERT INTO stock (stock_code, stock_name, market, current_price, updated_at)
VALUES ('005930', '삼성전자', 'KOSPI', 75000, NOW());
```

## 통합 테스트 시나리오

### Scenario 1: 시세 데이터 동기화 (Backend ← KIS API)

**요청:**
```bash
POST http://localhost:8080/api/v1/stocks/005930/prices/sync/recent?days=120
```

**예상 응답:**
```json
{
  "stockCode": "005930",
  "savedCount": 120,
  "days": 120
}
```

**검증:**
```sql
SELECT COUNT(*) FROM stock_price WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930');
-- 결과: 120 (또는 실제 영업일 기준)
```

### Scenario 2: AI 분석 실행 (Backend → AI Worker → OpenAI)

**2-1. 시세 조회 (AI Worker ← Backend)**

Backend가 제공하는 API를 확인:
```bash
GET http://localhost:8080/api/v1/stocks/005930/prices?days=120
```

**예상 응답:**
```json
[
  {
    "date": "2024-01-15",
    "open": 74500,
    "high": 75800,
    "low": 74200,
    "close": 75300,
    "volume": 12345678,
    "changeRate": 1.05
  },
  ...
]
```

**2-2. AI 분석 요청 (Backend → AI Worker)**

```bash
GET http://localhost:8080/api/v1/stocks/005930/analysis
```

**처리 흐름:**
```
1. StockController.getStockAnalysis()
   ↓
2. StockService.analyze("005930")
   ↓ loadStockPort.loadByCode()
   ↓
3. AiAnalysisAdapter.requestAnalysis(stock)
   ↓ loadStockPricePort.findTopNByStockIdOrderByDateDesc(120)
   ↓ WebClient.post("/api/v1/analysis")
   ↓
4. AI Worker: POST /api/v1/analysis
   ↓
5. PatternService.analyze()
   ↓ BackendClient.get_stock_prices()
   ↓ indicators.calculate_rsi/macd/sma...
   ↓ similarity.calculate_cosine_similarity()
   ↓
6. LlmService.generate_analysis()
   ↓ OpenAI API (gpt-4o-mini)
   ↓
7. AnalysisResponse → AiAnalysisAdapter
   ↓
8. StockAnalysisResult → StockController
```

**예상 응답:**
```json
{
  "recommendation": "BUY",
  "confidenceScore": 78,
  "technicalAnalysis": "RSI 32.5로 과매도 구간 진입, MACD 히스토그램이 상승 전환 신호 감지. 현재가가 SMA(20) 대비 3.2% 하락한 상태로 단기 반등 가능성 높음.",
  "supplyAnalysis": "과거 유사 패턴 5개 분석 결과, 향후 5일간 평균 +3.1% 수익률 기록. 특히 2024-03-15 구간(유사도 0.91)에서 +4.2% 상승.",
  "riskFactors": [
    "거래량 부족: 최근 5일 평균 대비 20% 감소",
    "시장 전체 조정 가능성: KOSPI 하락세 지속"
  ]
}
```

### Scenario 3: 직접 AI Worker 호출 (디버깅용)

AI Worker를 직접 호출하여 분석 로직만 테스트:

```bash
POST http://localhost:8000/api/v1/analysis
Content-Type: application/json

{
  "stock_code": "005930",
  "lookback_days": 120
}
```

**주의:** AI Worker는 Backend에서 시세 데이터를 가져오므로, Backend가 실행 중이어야 합니다.

## 오류 처리 시나리오

### Case 1: 시세 데이터 없음

**요청:**
```bash
GET http://localhost:8080/api/v1/stocks/000000/analysis
```

**예상 응답:** `400 Bad Request`
```json
{
  "error": "분석을 위한 시세 데이터가 없습니다. 먼저 시세 동기화를 진행하세요."
}
```

### Case 2: AI Worker 다운

AI Worker를 중지한 상태에서:
```bash
GET http://localhost:8080/api/v1/stocks/005930/analysis
```

**예상 응답:** `500 Internal Server Error`
```json
{
  "error": "AI Worker 통신 오류: Connection refused"
}
```

### Case 3: OpenAI API 키 누락

AI Worker의 `.env`에서 `OPENAI_API_KEY`를 제거하고:
```bash
POST http://localhost:8000/api/v1/analysis
{
  "stock_code": "005930",
  "lookback_days": 120
}
```

**예상 응답:** `500 Internal Server Error`
```json
{
  "detail": "분석 중 오류가 발생했습니다"
}
```

## 로그 확인

### Backend 로그
```
2024-01-15 10:00:00 INFO  StockService - 주식 분석 시작: stockCode=005930
2024-01-15 10:00:01 INFO  AiAnalysisAdapter - AI 분석 요청 시작: stockCode=005930
2024-01-15 10:00:01 INFO  AiAnalysisAdapter - 시세 데이터 조회 완료: stockCode=005930, 데이터 건수=120
2024-01-15 10:00:01 INFO  AiAnalysisAdapter - AI Worker 호출: url=http://localhost:8000/api/v1/analysis, stockCode=005930
2024-01-15 10:00:05 INFO  AiAnalysisAdapter - AI Worker 응답 수신: recommendation=BUY, confidence=78
2024-01-15 10:00:05 INFO  StockService - 주식 분석 완료: stockCode=005930, recommendation=BUY
```

### AI Worker 로그
```
2024-01-15 10:00:01 INFO  analysis - 분석 요청 수신: stock_code=005930, lookback_days=120
2024-01-15 10:00:01 INFO  pattern_service - 패턴 분석 시작: stock_code=005930, lookback_days=120
2024-01-15 10:00:02 INFO  backend_client - 백엔드 API 호출: stock_code=005930, lookback_days=120
2024-01-15 10:00:02 INFO  backend_client - 백엔드 API 응답 성공: 120 건의 데이터
2024-01-15 10:00:02 INFO  pattern_service - DataFrame 생성 완료: 120 rows
2024-01-15 10:00:02 INFO  pattern_service - 기술적 지표 계산 완료
2024-01-15 10:00:03 INFO  pattern_service - 유사 패턴 탐색 완료: 5 개
2024-01-15 10:00:03 INFO  llm_service - LLM 분석 요청 전송: stock_code=005930
2024-01-15 10:00:05 INFO  llm_service - LLM 분석 완료: recommendation=BUY, confidence=78
2024-01-15 10:00:05 INFO  analysis - 분석 완료: stock_code=005930, recommendation=BUY
```

## 성능 벤치마크

**목표:**
- 시세 동기화 (120일): < 5초
- AI 분석 (OHLCV → 결과): < 10초
  - 백엔드 시세 조회: < 500ms
  - 지표 계산: < 1초
  - 유사 패턴 탐색: < 2초
  - LLM 호출: < 5초

**측정:**
```bash
time curl -X POST http://localhost:8080/api/v1/stocks/005930/prices/sync/recent?days=120
time curl http://localhost:8080/api/v1/stocks/005930/analysis
```

## Docker Compose 통합 테스트

```bash
# 전체 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f backend
docker-compose logs -f ai-worker

# 테스트 실행
curl http://localhost:8080/api/v1/stocks/005930/analysis

# 서비스 종료
docker-compose down
```
