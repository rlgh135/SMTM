-- KR-Quant-Agent Database Schema
-- PostgreSQL 16

-- =====================================================
-- 1. Stock 테이블 (종목 정보)
-- =====================================================
CREATE TABLE stock (
  id BIGSERIAL PRIMARY KEY,
  stock_code VARCHAR(10) NOT NULL UNIQUE,
  stock_name VARCHAR(100) NOT NULL,
  market VARCHAR(10) NOT NULL,
  current_price DECIMAL(18,2),
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_stock_code ON stock(stock_code);

COMMENT ON TABLE stock IS '주식 종목 정보';
COMMENT ON COLUMN stock.stock_code IS '종목 코드 (6자리)';
COMMENT ON COLUMN stock.market IS '시장 구분 (KOSPI/KOSDAQ)';

-- =====================================================
-- 2. StockPrice 테이블 (일별 시세)
-- =====================================================
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
  CONSTRAINT fk_stock_price_stock FOREIGN KEY (stock_id) REFERENCES stock(id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_price_stock_date ON stock_price(stock_id, date);

COMMENT ON TABLE stock_price IS '주식 일별 시세 (OHLCV)';
COMMENT ON COLUMN stock_price.change_rate IS '전일 대비율 (%)';

-- =====================================================
-- 3. Watchlist 테이블 (관심 종목)
-- =====================================================
CREATE TABLE watchlist (
  id BIGSERIAL PRIMARY KEY,
  stock_id BIGINT NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT true,
  priority INTEGER NOT NULL DEFAULT 999,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_watchlist_stock FOREIGN KEY (stock_id) REFERENCES stock(id) ON DELETE CASCADE,
  CONSTRAINT uk_watchlist_stock UNIQUE (stock_id)
);

CREATE INDEX idx_watchlist_active_priority ON watchlist(is_active, priority);

COMMENT ON TABLE watchlist IS '관심 종목 목록 (배치 작업 대상)';
COMMENT ON COLUMN watchlist.priority IS '우선순위 (낮을수록 먼저 처리)';

-- =====================================================
-- 4. StockAnalysisHistory 테이블 (분석 이력)
-- =====================================================
CREATE TABLE stock_analysis_history (
  id BIGSERIAL PRIMARY KEY,
  stock_id BIGINT NOT NULL,
  analyzed_date DATE NOT NULL,
  recommendation VARCHAR(10) NOT NULL,
  confidence_score INTEGER NOT NULL,
  technical_analysis TEXT,
  supply_analysis TEXT,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_analysis_history_stock FOREIGN KEY (stock_id) REFERENCES stock(id) ON DELETE CASCADE,
  CONSTRAINT uk_stock_date UNIQUE (stock_id, analyzed_date)
);

CREATE INDEX idx_stock_analysis_date ON stock_analysis_history(stock_id, analyzed_date);

COMMENT ON TABLE stock_analysis_history IS 'AI 분석 이력 (일별 저장)';
COMMENT ON COLUMN stock_analysis_history.recommendation IS '투자 추천 (BUY/SELL/HOLD)';
COMMENT ON COLUMN stock_analysis_history.confidence_score IS '신뢰도 점수 (0-100)';

-- =====================================================
-- 5. StockAnalysisRiskFactors 테이블 (리스크 요인)
-- =====================================================
CREATE TABLE stock_analysis_risk_factors (
  analysis_id BIGINT NOT NULL,
  risk_factor VARCHAR(500) NOT NULL,
  CONSTRAINT fk_risk_factors_analysis FOREIGN KEY (analysis_id) REFERENCES stock_analysis_history(id) ON DELETE CASCADE
);

CREATE INDEX idx_risk_factors_analysis ON stock_analysis_risk_factors(analysis_id);

COMMENT ON TABLE stock_analysis_risk_factors IS 'AI 분석 리스크 요인 (다대다)';

-- =====================================================
-- 샘플 데이터 삽입
-- =====================================================

-- Stock 샘플 데이터
INSERT INTO stock (stock_code, stock_name, market, current_price, updated_at)
VALUES
  ('005930', '삼성전자', 'KOSPI', 75000, NOW()),
  ('000660', 'SK하이닉스', 'KOSPI', 145000, NOW()),
  ('035720', '카카오', 'KOSPI', 48500, NOW()),
  ('035420', 'NAVER', 'KOSPI', 215000, NOW()),
  ('051910', 'LG화학', 'KOSPI', 385000, NOW());

-- Watchlist 샘플 데이터 (삼성전자, SK하이닉스만 활성화)
INSERT INTO watchlist (stock_id, is_active, priority, created_at, updated_at)
VALUES
  ((SELECT id FROM stock WHERE stock_code = '005930'), true, 1, NOW(), NOW()),
  ((SELECT id FROM stock WHERE stock_code = '000660'), true, 2, NOW(), NOW()),
  ((SELECT id FROM stock WHERE stock_code = '035720'), false, 3, NOW(), NOW());

-- =====================================================
-- 유용한 쿼리
-- =====================================================

-- 1. 특정 종목의 최근 30일 시세 조회
-- SELECT * FROM stock_price
-- WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930')
-- ORDER BY date DESC
-- LIMIT 30;

-- 2. 오늘 분석된 종목 목록
-- SELECT s.stock_code, s.stock_name, h.recommendation, h.confidence_score
-- FROM stock_analysis_history h
-- JOIN stock s ON h.stock_id = s.id
-- WHERE h.analyzed_date = CURRENT_DATE
-- ORDER BY h.confidence_score DESC;

-- 3. 활성화된 관심 종목 목록
-- SELECT s.stock_code, s.stock_name, w.priority
-- FROM watchlist w
-- JOIN stock s ON w.stock_id = s.id
-- WHERE w.is_active = true
-- ORDER BY w.priority;

-- 4. 특정 종목의 분석 이력 (최근 10개)
-- SELECT analyzed_date, recommendation, confidence_score
-- FROM stock_analysis_history
-- WHERE stock_id = (SELECT id FROM stock WHERE stock_code = '005930')
-- ORDER BY analyzed_date DESC
-- LIMIT 10;
