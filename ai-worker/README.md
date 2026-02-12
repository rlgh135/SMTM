# KR-Quant-Agent AI Worker

한국 주식 시장(KOSPI/KOSDAQ) 분석을 위한 FastAPI 기반 AI 마이크로서비스입니다.
차트 패턴 분석, 기술적 지표 계산, LLM 기반 투자 리포트 생성을 담당합니다.

## 기술 스택

- Python 3.10+
- FastAPI 0.111
- Pydantic v2
- Pandas / NumPy / Scikit-learn / TA-Lib
- OpenAI SDK
- Tenacity (재시도 로직)

## 사전 요구사항

- **Python 3.10 이상** 설치
- **TA-Lib C 라이브러리** 설치 (Python 래퍼 설치 전 필수)

### TA-Lib 설치

TA-Lib Python 패키지는 네이티브 C 라이브러리에 의존합니다. OS별로 사전 설치가 필요합니다.

**Windows:**

[TA-Lib 비공식 빌드](https://github.com/cgohlke/talib-build/releases)에서 본인의 Python 버전에 맞는 `.whl` 파일을 다운로드한 후 설치합니다.

```bash
pip install TA_Lib‑0.4.28‑cp310‑cp310‑win_amd64.whl
```

**macOS:**

```bash
brew install ta-lib
```

**Linux (Ubuntu/Debian):**

```bash
sudo apt-get update
sudo apt-get install -y build-essential wget
wget http://prdownloads.sourceforge.net/ta-lib/ta-lib-0.4.0-src.tar.gz
tar -xzf ta-lib-0.4.0-src.tar.gz
cd ta-lib/
./configure --prefix=/usr
make
sudo make install
```

## 가상 환경 설정 및 실행

### 1. 가상 환경 생성

```bash
cd ai-worker
python -m venv .venv
```

### 2. 가상 환경 활성화

**Windows (PowerShell):**

```powershell
.\.venv\Scripts\Activate.ps1
```

**Windows (CMD):**

```cmd
.\.venv\Scripts\activate.bat
```

**Windows (Git Bash / MINGW):**

```bash
source .venv/Scripts/activate
```

**macOS / Linux:**

```bash
source .venv/bin/activate
```

### 3. 의존성 설치

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 4. 환경변수 설정

`.env.example` 파일을 복사하여 `.env` 파일을 생성하고 값을 채워넣습니다.

```bash
cp .env.example .env
```

필수 항목인 `OPENAI_API_KEY`를 반드시 설정해주세요.

### 5. 서버 실행

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

`--reload` 옵션은 개발 환경에서 코드 변경 시 자동으로 서버를 재시작합니다.
프로덕션 환경에서는 `--reload` 옵션을 제거하세요.

### 6. API 문서 확인

서버 실행 후 브라우저에서 접속합니다.

- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc

## 프로젝트 구조

```
ai-worker/
├── app/
│   ├── main.py              # FastAPI 앱 정의 및 생명주기 관리
│   ├── core/
│   │   └── config.py         # 환경변수 기반 설정 (pydantic-settings)
│   ├── api/v1/
│   │   ├── router.py         # API v1 라우터
│   │   └── analysis.py       # 분석 엔드포인트
│   ├── schemas/
│   │   └── analysis.py       # Pydantic 요청/응답 스키마
│   ├── services/
│   │   ├── pattern_service.py # 차트 패턴 분석 오케스트레이터
│   │   └── llm_service.py     # LLM API 래퍼 (Tenacity 재시도)
│   └── engine/
│       ├── indicators.py      # 기술적 지표 계산 (RSI, MACD)
│       └── similarity.py      # 유사도 계산 (코사인, DTW)
├── requirements.txt
├── .env.example
└── README.md
```

## Docker 실행

프로젝트 루트에서 Docker Compose로 전체 서비스를 실행할 수도 있습니다.

```bash
cd ..  # 프로젝트 루트로 이동
docker-compose up ai-worker
```
