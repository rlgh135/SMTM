"""
애플리케이션 설정 모듈.
환경변수 기반의 설정 관리.
"""
from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 전역 설정."""

    PROJECT_NAME: str = "KR-Quant-Agent AI Worker"
    VERSION: str = "0.1.0"

    # OpenAI 설정
    OPENAI_API_KEY: str = ""
    BACKEND_API_URL: str = "http://localhost:8080"

    # AWS Bedrock 설정 (선택적 - 미설정 시 서버 정상 기동)
    AWS_ACCESS_KEY_ID: Optional[str] = None
    AWS_SECRET_ACCESS_KEY: Optional[str] = None
    AWS_REGION: str = "us-east-1"

    # Google Gemini 설정 (선택적 - 미설정 시 서버 정상 기동)
    GEMINI_API_KEY: Optional[str] = None

    # 분석 파라미터
    DEFAULT_LOOKBACK_DAYS: int = 120
    SIMILARITY_THRESHOLD: float = 0.85

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
    }


settings = Settings()
