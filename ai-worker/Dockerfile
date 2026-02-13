# ============================================================
# Python 3.10 (Debian Slim - TA-Lib C 라이브러리 컴파일 필요)
# ============================================================
FROM python:3.10-slim

WORKDIR /app

# TA-Lib C 라이브러리 컴파일에 필요한 시스템 의존성 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# TA-Lib C 라이브러리 소스 다운로드, 컴파일, 설치
# Python TA-Lib 패키지는 이 C 라이브러리에 의존함
WORKDIR /tmp
RUN wget -q https://sourceforge.net/projects/ta-lib/files/ta-lib/0.4.0/ta-lib-0.4.0-src.tar.gz \
    && tar -xzf ta-lib-0.4.0-src.tar.gz \
    && cd ta-lib \
    && ./configure --prefix=/usr \
    && make \
    && make install \
    && cd /tmp \
    && rm -rf ta-lib ta-lib-0.4.0-src.tar.gz

WORKDIR /app

# Python 의존성 설치 (TA-Lib C 라이브러리 설치 이후에 실행해야 함)
COPY requirements.txt ./

# 1단계: numpy를 먼저 고정 버전으로 설치
# (pip가 TA-Lib 빌드 격리 환경에 NumPy 2.x를 설치하면 PyArray_Descr API 불일치 발생)
RUN pip install --no-cache-dir numpy==1.26.4

# 2단계: TA-Lib 설치
# --no-build-isolation: 위에서 설치한 numpy 1.26.4를 빌드 환경에서 그대로 사용
# CFLAGS: GCC 14의 -Wincompatible-pointer-types 에러를 경고로 강등
RUN CFLAGS="-Wno-incompatible-pointer-types" pip install --no-cache-dir --no-build-isolation TA-Lib==0.4.28

# 3단계: 나머지 의존성 설치
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 복사
COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
