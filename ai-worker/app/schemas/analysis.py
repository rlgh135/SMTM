"""
분석 관련 Pydantic 스키마 (DTO).
"""
from enum import Enum

from pydantic import BaseModel, Field


class Recommendation(str, Enum):
    """투자 추천 유형."""
    BUY = "BUY"
    SELL = "SELL"
    HOLD = "HOLD"


class AnalysisRequest(BaseModel):
    """분석 요청 DTO."""

    stock_code: str = Field(..., description="종목 코드", examples=["005930"])
    lookback_days: int = Field(default=120, description="분석 기간(일)")


class AnalysisResponse(BaseModel):
    """분석 응답 DTO."""

    recommendation: Recommendation = Field(..., description="투자 추천")
    confidence_score: int = Field(..., ge=0, le=100, description="신뢰도 점수")
    technical_analysis: str = Field(..., description="기술적 분석 요약")
    supply_analysis: str = Field(..., description="수급 분석 요약")
    risk_factors: list[str] = Field(default_factory=list, description="리스크 요인")
