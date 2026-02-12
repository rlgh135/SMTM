"""
KR-Quant-Agent AI Worker 메인 모듈.
FastAPI 기반의 AI 분석 마이크로서비스.
"""
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from app.api.v1.router import api_router
from app.core.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """애플리케이션 생명주기 관리."""
    # 시작 시 초기화 로직
    yield
    # 종료 시 정리 로직


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    lifespan=lifespan,
)

app.include_router(api_router, prefix="/api/v1")
