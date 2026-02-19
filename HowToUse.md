# KR-Quant-Agent 빌드 및 실행 가이드

**한국 주식 시장 AI 기반 예측 및 에이전트 리포팅 플랫폼**의 빌드 및 실행 방법을 안내합니다.

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [사전 요구사항](#2-사전-요구사항)
3. [환경 변수 설정](#3-환경-변수-설정)
4. [Local 빌드 및 실행](#4-local-빌드-및-실행)
5. [Docker Desktop을 이용한 빌드 및 실행](#5-docker-desktop을-이용한-빌드-및-실행)
6. [검증 및 테스트](#6-검증-및-테스트)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. 프로젝트 개요

이 프로젝트는 **Hexagonal Architecture**, **Clean Architecture**, **FSD(Feature-Sliced Design)** 패턴을 적용한 엔터프라이즈급 핀테크 애플리케이션입니다.

### 아키텍처 구성

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Frontend        │────▶│  Backend         │────▶│  AI Worker       │
│  (React + Vite)  │     │  (Spring Boot)   │     │  (FastAPI)       │
│  Port: 3000      │     │  Port: 8080      │     │  Port: 8000      │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                 │                         │
                         ┌───────┴────────┐               │
                         ▼                ▼               ▼
                  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
                  │ PostgreSQL  │  │   Redis     │  │  LLM APIs   │
                  │ Port: 5432  │  │ Port: 6379  │  │ (OpenAI 등) │
                  └─────────────┘  └─────────────┘  └─────────────┘
```

### 기술 스택

- **Backend:** Java 17, Spring Boot 3.x, Spring Data JPA, QueryDSL, WebClient
- **AI Worker:** Python 3.10, FastAPI, Pandas, NumPy, TA-Lib, Scikit-learn
- **Frontend:** React 18, TypeScript, Vite, Tailwind CSS, Zustand, TanStack Query, Axios
- **Database:** PostgreSQL 16, Redis 7
- **Infrastructure:** Docker, Docker Compose

---

## 2. 사전 요구사항

### 2.1 Local 빌드 시 필요

#### **공통**
- Git
- 터미널(Bash, Zsh, PowerShell 등)

#### **Backend (Spring Boot)**
- **JDK 17 이상** (Eclipse Temurin, OpenJDK 권장)
  ```bash
  java -version  # 17 이상 확인
  ```

#### **AI Worker (FastAPI)**
- **Python 3.10 이상**
  ```bash
  python --version  # 3.10 이상 확인
  ```
- **TA-Lib C 라이브러리** (OS별 설치 방법은 아래 참조)

#### **Frontend (React)**
- **Node.js 18 이상** 및 npm
  ```bash
  node -v  # 18 이상 확인
  npm -v
  ```

#### **Database**
- **PostgreSQL 16 이상**
- **Redis 7 이상**

### 2.2 Docker 빌드 시 필요

- **Docker Desktop** (Windows/Mac) 또는 **Docker Engine + Docker Compose** (Linux)
  ```bash
  docker --version         # 20.10 이상 권장
  docker-compose --version # 2.0 이상 권장
  ```

### 2.3 외부 API 키

- **한국투자증권 Open API** (KIS_APP_KEY, KIS_APP_SECRET)
  - 발급: https://apiportal.koreainvestment.com
- **LLM API 키** (선택적, 아래 중 하나 이상)
  - OpenAI API Key (GPT-4/GPT-3.5)
  - AWS Bedrock (Claude 3.5 Sonnet)
  - Google Gemini API Key

---

## 3. 환경 변수 설정

각 서비스별로 `.env.example`을 복사하여 `.env` 파일을 생성하고, 실제 값을 입력합니다.

### 3.1 Backend (.env)

```bash
cd backend
cp .env.example .env
```

**backend/.env 예시:**
```ini
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/krstock
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# 한국투자증권 API
KIS_APP_KEY=your-actual-app-key
KIS_APP_SECRET=your-actual-app-secret
KIS_BASE_URL=https://openapi.koreainvestment.com:9443
KIS_ACCOUNT_NO=your-account-number

# AI Worker
AI_WORKER_URL=http://localhost:8000

# 배치 작업
BATCH_ENABLED=true
BATCH_CRON=0 0 16 * * MON-FRI

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### 3.2 AI Worker (.env)

```bash
cd ai-worker
cp .env.example .env
```

**ai-worker/.env 예시:**
```ini
# LLM API (아래 중 하나 이상 설정)
OPENAI_API_KEY=sk-your-actual-openai-api-key

# AWS Bedrock (선택)
AWS_ACCESS_KEY_ID=your-aws-key
AWS_SECRET_ACCESS_KEY=your-aws-secret
AWS_REGION=us-east-1

# Google Gemini (선택)
GEMINI_API_KEY=your-gemini-api-key

# 백엔드 연결
BACKEND_API_URL=http://localhost:8080

# 분석 파라미터
DEFAULT_LOOKBACK_DAYS=120
SIMILARITY_THRESHOLD=0.85
```

### 3.3 Frontend (.env)

```bash
cd frontend
cp .env.example .env
```

**frontend/.env 예시:**
```ini
VITE_API_URL=http://localhost:8080/api/v1
```

---

## 4. Local 빌드 및 실행

### 4.1 PostgreSQL 및 Redis 설정

#### **Option 1: Docker Compose로 DB만 실행 (권장)**

```bash
# 프로젝트 루트에서
docker-compose up -d postgres redis

# 확인
docker ps  # postgres, redis 컨테이너 실행 확인
```

#### **Option 2: Local 설치 (수동)**

**PostgreSQL:**
```bash
# 데이터베이스 생성
psql -U postgres
CREATE DATABASE krstock;
\q

# 스키마 초기화 (DATABASE_SCHEMA.sql 실행)
psql -U postgres -d krstock -f DATABASE_SCHEMA.sql
```

**Redis:**
```bash
# Linux/Mac
redis-server

# Windows (WSL 권장)
redis-server.exe
```

---

### 4.2 Backend (Spring Boot) 빌드 및 실행

```bash
cd backend

# Gradle Wrapper를 이용한 빌드 (테스트 제외)
./gradlew clean build -x test

# JAR 파일 실행 (환경변수는 .env 파일 또는 export로 설정)
java -jar build/libs/*.jar

# 또는 Gradle로 바로 실행
./gradlew bootRun
```

**실행 확인:**
```bash
curl http://localhost:8080/actuator/health
# 응답: {"status":"UP"}
```

**Windows에서 실행 시:**
```powershell
# gradlew.bat 사용
.\gradlew.bat clean build -x test
.\gradlew.bat bootRun
```

---

### 4.3 AI Worker (FastAPI) 빌드 및 실행

#### **TA-Lib C 라이브러리 설치 (필수)**

**Mac (Homebrew):**
```bash
brew install ta-lib
```

**Ubuntu/Debian:**
```bash
wget http://prdownloads.sourceforge.net/ta-lib/ta-lib-0.4.0-src.tar.gz
tar -xzf ta-lib-0.4.0-src.tar.gz
cd ta-lib/
./configure --prefix=/usr
make
sudo make install
```

**Windows:**
- TA-Lib 바이너리를 직접 설치하거나 Docker 사용 권장
- 참고: https://github.com/TA-Lib/ta-lib-python

#### **Python 가상환경 및 의존성 설치**

```bash
cd ai-worker

# 가상환경 생성
python -m venv venv

# 가상환경 활성화
# Mac/Linux:
source venv/bin/activate
# Windows (PowerShell):
.\venv\Scripts\Activate.ps1

# NumPy 먼저 설치 (TA-Lib 빌드 의존성)
pip install numpy==1.26.4

# TA-Lib 설치
pip install TA-Lib==0.4.28

# 나머지 의존성 설치
pip install -r requirements.txt
```

#### **실행**

```bash
# .env 파일이 있는 디렉토리에서 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

**실행 확인:**
```bash
curl http://localhost:8000/health
# 응답: {"status":"healthy"}
```

---

### 4.4 Frontend (React) 빌드 및 실행

```bash
cd frontend

# 의존성 설치
npm install

# 개발 모드 실행 (Hot Reload)
npm run dev

# 프로덕션 빌드
npm run build

# 프로덕션 빌드 미리보기
npm run preview
```

**실행 확인:**
- 브라우저에서 http://localhost:3000 접속
- Vite 개발 서버가 기본적으로 5173 포트를 사용할 수 있음 (콘솔 확인)

---

### 4.5 전체 Local 실행 순서

```bash
# 1. DB 실행 (Docker Compose)
docker-compose up -d postgres redis

# 2. Backend 실행
cd backend
./gradlew bootRun &

# 3. AI Worker 실행
cd ../ai-worker
source venv/bin/activate  # 가상환경 활성화
uvicorn app.main:app --host 0.0.0.0 --port 8000 &

# 4. Frontend 실행
cd ../frontend
npm run dev
```

**실행 확인:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/actuator/health
- AI Worker: http://localhost:8000/health
- PostgreSQL: localhost:5432
- Redis: localhost:6379

---

## 5. Docker Desktop을 이용한 빌드 및 실행

Docker Compose를 사용하면 **모든 서비스를 한 번에** 빌드 및 실행할 수 있습니다.

### 5.1 전체 스택 실행 (권장)

#### **초기 실행 (빌드 포함)**

```bash
# 프로젝트 루트에서
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

#### **로그 확인**

```bash
# 전체 서비스 로그
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f backend
docker-compose logs -f ai-worker
docker-compose logs -f frontend
docker-compose logs -f postgres
docker-compose logs -f redis
```

#### **컨테이너 상태 확인**

```bash
docker-compose ps

# 또는
docker ps
```

**기대 출력:**
```
NAME                   STATUS         PORTS
krstock-backend        Up 2 minutes   0.0.0.0:8080->8080/tcp
krstock-ai-worker      Up 2 minutes   0.0.0.0:8000->8000/tcp
krstock-frontend       Up 2 minutes   0.0.0.0:3000->80/tcp
krstock-postgres       Up 2 minutes   0.0.0.0:5432->5432/tcp
krstock-redis          Up 2 minutes   0.0.0.0:6379->6379/tcp
```

---

### 5.2 개별 서비스 실행

```bash
# DB만 실행
docker-compose up -d postgres redis

# Backend만 빌드 및 실행
docker-compose up --build backend

# AI Worker만 실행
docker-compose up -d ai-worker
```

---

### 5.3 서비스 중지 및 제거

#### **중지 (컨테이너만 중지, 볼륨 유지)**

```bash
docker-compose stop
```

#### **제거 (컨테이너 및 네트워크 제거, 볼륨 유지)**

```bash
docker-compose down
```

#### **완전 제거 (컨테이너, 네트워크, 볼륨 모두 제거 - DB 데이터 삭제)**

```bash
docker-compose down -v

# 또는 볼륨만 따로 제거
docker volume rm krstock_postgres_data krstock_redis_data
```

---

### 5.4 재빌드 (소스 코드 변경 후)

```bash
# 특정 서비스만 재빌드
docker-compose up -d --build backend

# 전체 재빌드
docker-compose up -d --build

# 캐시 없이 완전히 새로 빌드
docker-compose build --no-cache
docker-compose up -d
```

---

### 5.5 Docker Desktop GUI 사용

#### **Windows/Mac 사용자:**

1. **Docker Desktop 실행**
2. **Containers 탭 이동**
3. **krstock-xxx 컨테이너 그룹 확인**
4. **각 컨테이너 클릭 → Logs, Inspect, Stats 확인**
5. **중지/재시작:** 컨테이너 우클릭 → Stop/Restart

#### **이미지 관리:**

- **Images 탭:** 빌드된 이미지 확인 및 삭제
- **Volumes 탭:** 데이터 볼륨 확인 (postgres_data, redis_data)

---

## 6. 검증 및 테스트

### 6.1 Health Check

#### **Backend**
```bash
curl http://localhost:8080/actuator/health
# 응답: {"status":"UP"}
```

#### **AI Worker**
```bash
curl http://localhost:8000/health
# 응답: {"status":"healthy"}
```

#### **PostgreSQL**
```bash
docker exec -it krstock-postgres psql -U postgres -d krstock -c "SELECT version();"
```

#### **Redis**
```bash
docker exec -it krstock-redis redis-cli ping
# 응답: PONG
```

---

### 6.2 API 엔드포인트 테스트

#### **Backend REST API**

```bash
# 종목 조회 (예시 - 실제 엔드포인트는 코드 확인)
curl http://localhost:8080/api/v1/stocks/005930

# 배치 수동 실행 (테스트용)
curl -X POST http://localhost:8080/api/v1/batch/daily-analysis
```

#### **AI Worker API**

```bash
# 분석 요청 (예시 - 실제 스키마는 코드 확인)
curl -X POST http://localhost:8000/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"stock_code":"005930"}'
```

---

### 6.3 배치 작업 확인

#### **서버 시작 시 즉시 배치 실행 (Phase 8 기능)**

```bash
# 1. DB 초기화 (기존 데이터 삭제)
docker-compose down -v

# 2. 전체 재시작
docker-compose up -d --build

# 3. Backend 로그 확인
docker logs -f krstock-backend

# 기대되는 로그:
# "========== 서버 시작 - DB 상태 확인 =========="
# "분석 이력 테이블 레코드 수: 0"
# "분석 이력 테이블이 비어있습니다. 즉시 배치를 실행합니다."
# "========== 일일 분석 배치 시작 =========="
```

#### **정기 스케줄 확인 (평일 16시 KST)**

- 배치를 돌린 당일에는 16시가 되어도 중복 실행되지 않음
- 로그: "오늘(YYYY-MM-DD) 이미 배치가 실행되었습니다. 배치 종료."

---

## 7. 트러블슈팅

### 7.1 Backend 실행 오류

#### **문제: "Cannot connect to database"**

**원인:** PostgreSQL이 실행되지 않았거나 연결 정보가 잘못됨

**해결:**
```bash
# PostgreSQL 컨테이너 상태 확인
docker ps | grep postgres

# 로그 확인
docker logs krstock-postgres

# 재시작
docker-compose restart postgres
```

#### **문제: "KIS API authentication failed"**

**원인:** `.env` 파일의 KIS API 키가 유효하지 않음

**해결:**
- `backend/.env` 파일의 `KIS_APP_KEY`, `KIS_APP_SECRET` 재확인
- 한국투자증권 포털에서 키 재발급

---

### 7.2 AI Worker 실행 오류

#### **문제: "TA-Lib not found"**

**원인:** TA-Lib C 라이브러리가 설치되지 않음

**해결:**
- Docker 사용 시: 자동 설치되므로 `docker-compose up --build ai-worker`
- Local 실행 시: 섹션 4.3 참조하여 OS별 TA-Lib 설치

#### **문제: "OPENAI_API_KEY not set"**

**원인:** LLM API 키가 설정되지 않음

**해결:**
- `ai-worker/.env` 파일 생성 및 API 키 입력
- OpenAI, Bedrock, Gemini 중 최소 하나 설정

---

### 7.3 Frontend 실행 오류

#### **문제: "Failed to fetch"**

**원인:** Backend API가 실행되지 않았거나 CORS 설정 오류

**해결:**
```bash
# Backend 상태 확인
curl http://localhost:8080/actuator/health

# CORS 설정 확인 (backend/.env)
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

#### **문제: "npm install 실패"**

**원인:** Node.js 버전 불일치

**해결:**
```bash
# Node.js 버전 확인
node -v  # 18 이상 필요

# nvm 사용 시
nvm install 20
nvm use 20
```

---

### 7.4 Docker 관련 오류

#### **문제: "port is already allocated"**

**원인:** 이미 해당 포트를 사용 중인 프로세스가 있음

**해결:**
```bash
# Windows (PowerShell)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :8080
kill -9 <PID>

# 또는 docker-compose.yml에서 포트 변경
ports:
  - "8081:8080"  # 호스트 포트 변경
```

#### **문제: "no space left on device"**

**원인:** Docker 디스크 공간 부족

**해결:**
```bash
# 미사용 컨테이너, 이미지, 볼륨 정리
docker system prune -a --volumes

# Docker Desktop 설정에서 Disk Image Size 증가
```

---

### 7.5 배치 작업 관련

#### **문제: "배치가 실행되지 않음"**

**확인 사항:**
1. `backend/.env`의 `BATCH_ENABLED=true` 설정
2. 관심 종목(Watchlist) 테이블에 활성화된 종목이 있는지 확인
3. 주말에는 실행되지 않음 (평일만 실행)

**해결:**
```bash
# 수동 배치 실행 (테스트용)
curl -X POST http://localhost:8080/api/v1/batch/daily-analysis

# 로그 확인
docker logs -f krstock-backend | grep "배치"
```

---

## 📌 추가 참고사항

### 환경별 권장 실행 방법

| 환경 | 권장 방법 | 이유 |
|------|-----------|------|
| **개발 (코드 수정)** | Local 빌드 | Hot Reload, 빠른 디버깅 |
| **테스트 (통합)** | Docker Compose | 프로덕션 환경과 유사 |
| **프로덕션** | Docker + Kubernetes | 확장성, 무중단 배포 |

### 성능 최적화

- **Backend JVM 옵션 (프로덕션):**
  ```bash
  java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar
  ```

- **AI Worker Workers 수 조정:**
  ```bash
  uvicorn app.main:app --workers 4 --host 0.0.0.0 --port 8000
  ```

### 모니터링

- **Backend:** Spring Actuator → http://localhost:8080/actuator
- **AI Worker:** FastAPI Docs → http://localhost:8000/docs
- **Database:** pgAdmin, DBeaver 등 GUI 도구 사용

---

## 🔗 관련 문서

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 아키텍처 및 코딩 표준
- [WorkHistory.md](./WorkHistory.md) - Phase별 작업 이력
- [DATABASE_SCHEMA.sql](./DATABASE_SCHEMA.sql) - DB 스키마 정의

---

**작성일:** 2026-02-19
**버전:** 1.0.0
**문의:** Phase별 작업 내용은 WorkHistory.md 참조
