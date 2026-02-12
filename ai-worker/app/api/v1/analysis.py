"""
주식 분석 API 엔드포인트.
"""
from fastapi import APIRouter

from app.schemas.analysis import AnalysisRequest, AnalysisResponse
from app.services.pattern_service import PatternService

router = APIRouter()
pattern_service = PatternService()


@router.post("/", response_model=AnalysisResponse)
async def analyze_stock(request: AnalysisRequest) -> AnalysisResponse:
    """
    주식 종목 기술적 분석을 수행합니다.

    Args:
        request: 분석 요청 데이터 (종목 코드, OHLCV 등).

    Returns:
        AI 기반 분석 결과.
    """
    return await pattern_service.analyze(request)
