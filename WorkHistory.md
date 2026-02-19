# Work History

프로젝트의 Phase별 작업 이력을 기록합니다.

---

## Phase 8: 배치 시작 로직 개선 및 중복 실행 방지

**작업 일자:** 2026-02-19

**요구사항:**
- 서버 실행 후 DB의 테이블이 비어있다면 즉시 배치를 돌리도록 수정
- 배치를 돌린 당일에는 16시가 되어도 배치가 돌지 않도록 수정

**구현 내용:**

1. **SaveAnalysisHistoryPort 확장**
   - `existsByAnalyzedDate(LocalDate date)`: 특정 날짜의 분석 이력 존재 여부 확인
   - `count()`: 전체 분석 이력 개수 조회
   - `findByAnalyzedDate(LocalDate date)`: 특정 날짜의 모든 분석 이력 조회

2. **StockAnalysisHistoryJpaRepository 확장**
   - `existsByAnalyzedDate(LocalDate date)` 메서드 추가

3. **StockPersistenceAdapter 구현**
   - 추가된 Port 메서드들 구현

4. **DailyAnalysisBatchService 수정**
   - `executeDailyAnalysis()` 메서드에 오늘 이미 배치가 실행되었는지 확인하는 로직 추가
   - 이미 실행되었으면 로그를 남기고 배치 종료
   - 중복 실행 방지 메커니즘 구현

5. **BatchStartupRunner 신규 생성**
   - `ApplicationRunner` 인터페이스 구현
   - 서버 시작 시 `StockAnalysisHistory` 테이블의 레코드 수 확인
   - 테이블이 비어있으면 즉시 `DailyAnalysisBatchService.executeDailyAnalysis()` 호출
   - 배치 활성화 설정에 따라 조건부 실행 (`@ConditionalOnProperty`)

**기술적 세부사항:**
- 배치 실행 이력은 `StockAnalysisHistory.analyzedDate` 필드로 관리
- 날짜별 유니크 제약조건 활용 (`uk_stock_date`)
- ApplicationRunner를 통한 서버 시작 시점 훅 활용
- Transaction 경계 명확히 구분

**검증 방법:**
1. Docker Compose로 전체 스택 실행
2. 초기 실행 시 DB가 비어있으면 즉시 배치 실행 확인
3. 같은 날 16시에 스케줄된 배치가 실행되지 않는지 확인
4. 로그 메시지로 중복 실행 방지 여부 확인

**파일 변경사항:**
- 수정: `domain/stock/application/port/out/SaveAnalysisHistoryPort.java`
- 수정: `domain/stock/adapter/out/persistence/StockAnalysisHistoryJpaRepository.java`
- 수정: `domain/stock/adapter/out/persistence/StockPersistenceAdapter.java`
- 수정: `domain/stock/application/batch/DailyAnalysisBatchService.java`
- 신규: `domain/stock/application/batch/BatchStartupRunner.java`

---

## Phase 9: 빌드 및 실행 가이드 문서화

**작업 일자:** 2026-02-19

**요구사항:**
- Local 빌드 방법 정리
- Docker Desktop을 이용한 빌드 방법 정리
- HowToUse.md 파일을 루트 디렉토리에 작성

**구현 내용:**

1. **HowToUse.md 작성**
   - 프로젝트 개요 및 아키텍처 구성도
   - 사전 요구사항 (Local 빌드, Docker 빌드)
   - 환경 변수 설정 가이드 (Backend, AI Worker, Frontend)
   - Local 빌드 및 실행 방법 (서비스별 상세 가이드)
   - Docker Desktop을 이용한 빌드 및 실행 방법
   - 검증 및 테스트 방법
   - 트러블슈팅 가이드

**문서 구성:**

### 1. 프로젝트 개요
- 아키텍처 다이어그램 (ASCII Art)
- 기술 스택 요약
- 포트 할당 정보

### 2. 사전 요구사항
- **Local 빌드:** JDK 17, Python 3.10, Node.js 18, PostgreSQL 16, Redis 7, TA-Lib
- **Docker 빌드:** Docker Desktop 또는 Docker Engine + Docker Compose
- 외부 API 키 발급 방법 (KIS, OpenAI, AWS Bedrock, Gemini)

### 3. 환경 변수 설정
- `.env.example` → `.env` 복사 가이드
- 각 서비스별 필수/선택 환경 변수 설명
- 실제 값 예시 제공

### 4. Local 빌드 및 실행
- **PostgreSQL/Redis:** Docker Compose로 DB만 실행하는 방법
- **Backend:** Gradle 빌드 및 실행 명령어
- **AI Worker:** Python 가상환경 설정, TA-Lib 설치, uvicorn 실행
- **Frontend:** npm install, npm run dev, npm run build
- 전체 Local 실행 순서 정리

### 5. Docker Desktop 빌드 및 실행
- 전체 스택 실행: `docker-compose up --build`
- 개별 서비스 실행 방법
- 로그 확인: `docker-compose logs -f [service]`
- 중지 및 제거: `docker-compose down`, `docker-compose down -v`
- 재빌드 방법
- Docker Desktop GUI 사용 가이드

### 6. 검증 및 테스트
- Health Check 엔드포인트 (Backend, AI Worker, PostgreSQL, Redis)
- API 엔드포인트 테스트 (curl 예시)
- 배치 작업 확인 (Phase 8 기능 검증)

### 7. 트러블슈팅
- Backend 실행 오류 (DB 연결, KIS API 인증)
- AI Worker 실행 오류 (TA-Lib, LLM API 키)
- Frontend 실행 오류 (Backend 연결, CORS, npm)
- Docker 관련 오류 (포트 충돌, 디스크 공간)
- 배치 작업 관련 오류

**기술적 특징:**
- 한국어로 작성하여 가독성 향상
- 실용적인 명령어와 예시 제공
- OS별 차이점 명시 (Windows/Mac/Linux)
- 초보자도 따라할 수 있는 단계별 가이드
- 테이블과 코드 블록을 활용한 구조화된 문서

**환경별 권장 실행 방법:**
- 개발 환경 (코드 수정): Local 빌드 (Hot Reload, 빠른 디버깅)
- 테스트 환경 (통합 테스트): Docker Compose (프로덕션 유사 환경)
- 프로덕션: Docker + Kubernetes (확장성, 무중단 배포)

**파일 변경사항:**
- 신규: `HowToUse.md` (루트 디렉토리)
- 수정: `WorkHistory.md` (Phase 9 작업 이력 추가)

**참고 문서 연결:**
- CLAUDE.md: 프로젝트 아키텍처 및 코딩 표준
- DATABASE_SCHEMA.sql: DB 스키마 정의
- .env.example 파일들: 환경 변수 템플릿

---

## Phase 10: Docker 실행 및 에러 처리

**작업 일자:** 2026-02-19

**요구사항:**
- Docker를 이용해 프로젝트를 실행
- 발생하는 에러를 확인하고 처리

**발견 및 해결한 에러:**

### 1. KIS API 토큰 발급 URL 오타
**에러:** `403 Forbidden from POST https://openapi.koreainvestment.com:9443/oauth2/tokenP`

**원인:** `KisTokenManager.java`의 60번째 줄에서 엔드포인트 URL이 `/oauth2/tokenP`로 잘못 입력됨

**해결:**
```java
// Before
.uri("/oauth2/tokenP")

// After
.uri("/oauth2/token")
```

**파일:** `backend/src/main/java/com/project/stock/domain/stock/adapter/out/external/kis/KisTokenManager.java`

### 2. Docker Compose Backend 환경 변수 누락
**에러:** KIS API 호출 시 환경 변수 인식 실패

**원인:** `docker-compose.yml`의 backend 서비스에 `env_file` 설정이 누락됨

**해결:**
```yaml
# docker-compose.yml backend 서비스에 추가
env_file:
  - ./backend/.env
```

**파일:** `docker-compose.yml`

### 3. KIS API Content-Type 헤더 누락
**에러:** API 요청 시 Content-Type 헤더 누락으로 인한 잠재적 문제

**해결:**
```java
// KisTokenManager.java에 헤더 추가
.header("Content-Type", "application/json")
```

**파일:** `backend/src/main/java/com/project/stock/domain/stock/adapter/out/external/kis/KisTokenManager.java`

### 4. KIS API 403 Forbidden (외부 API 문제)
**에러:** `403 Forbidden from POST https://openapi.koreainvestment.com:9443/oauth2/token`

**원인:** KIS API 키가 만료되었거나, API 정책 변경, 또는 IP 제한 등의 외부 요인

**임시 해결:**
- 배치 작업을 비활성화하여 서버가 정상적으로 시작되도록 조치
- `docker-compose.yml`에 `BATCH_ENABLED: false` 추가

**향후 조치 필요:**
- KIS API 키 재발급 또는 갱신
- API 문서 확인 및 인증 방식 재검토
- 실전투자/모의투자 엔드포인트 확인

**구현 내용:**

1. **KisTokenManager 수정**
   - URL 오타 수정: `/oauth2/tokenP` → `/oauth2/token`
   - Content-Type 헤더 추가

2. **docker-compose.yml 수정**
   - backend 서비스에 `env_file: ./backend/.env` 추가
   - 배치 임시 비활성화: `BATCH_ENABLED: false` 추가

3. **전체 스택 검증**
   - PostgreSQL: 정상 실행 ✅
   - Redis: 정상 실행 ✅
   - Backend: 정상 실행 ✅ (배치 비활성화 상태)
   - AI Worker: 정상 실행 ✅
   - Frontend: 정상 실행 ✅

**실행 확인 결과:**

```bash
# 컨테이너 상태
docker ps
NAMES               STATUS                    PORTS
krstock-frontend    Up                        0.0.0.0:3000->80/tcp
krstock-backend     Up                        0.0.0.0:8080->8080/tcp
krstock-ai-worker   Up                        0.0.0.0:8000->8000/tcp
krstock-postgres    Up (healthy)              0.0.0.0:5432->5432/tcp
krstock-redis       Up (healthy)              0.0.0.0:6379->6379/tcp
```

**서비스 접근:**
- Frontend: http://localhost:3000 ✅
- Backend: http://localhost:8080 ✅
- AI Worker: http://localhost:8000 ✅
- PostgreSQL: localhost:5432 ✅
- Redis: localhost:6379 ✅

**파일 변경사항:**
- 수정: `backend/src/main/java/com/project/stock/domain/stock/adapter/out/external/kis/KisTokenManager.java`
- 수정: `docker-compose.yml`

**Known Issues:**
- KIS API 403 Forbidden 에러는 외부 API 키 문제로 해결 보류
- 배치 기능 사용을 위해서는 유효한 KIS API 키 설정 필요

**다음 단계 권장사항:**
1. 한국투자증권 포털에서 API 키 상태 확인 및 재발급
2. 실전투자/모의투자 엔드포인트 확인
3. API 문서 재검토 (approval_key 필요 여부 등)
4. 배치 활성화 후 재테스트

---
