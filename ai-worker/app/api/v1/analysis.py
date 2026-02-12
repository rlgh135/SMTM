"""
주식 분석 API 엔드포인트.
"""
import logging

from fastapi import APIRouter, HTTPException

from app.schemas.analysis import AnalysisRequest, AnalysisResponse
from app.services.pattern_service import PatternService

logger = logging.getLogger(__name__)

router = APIRouter()
pattern_service = PatternService()


@router.post("/", response_model=AnalysisResponse)
async def analyze_stock(request: AnalysisRequest) -> AnalysisResponse:
    """
    주식 종목 기술적 분석을 수행합니다.

    Args:
        request: 분석 요청 데이터 (종목 코드, 분석 기간).

    Returns:
        AI 기반 분석 결과 (추천, 신뢰도, 기술적 분석, 수급 분석, 리스크).

    Raises:
        HTTPException: 분석 실패 시.
    """
    try:
        logger.info(
            "분석 요청 수신: stock_code=%s, lookback_days=%d",
            request.stock_code,
            request.lookback_days,
        )

        # 1. 패턴 분석 수행 (기술적 지표 + 유사 패턴)
        pattern_result = await pattern_service.analyze(request)

        # 2. LLM 분석 수행 (투자 추천 생성)
        llm_result = await pattern_service.llm_service.generate_analysis(pattern_result)

        # 3. 최종 응답 구성
        response = AnalysisResponse(
            recommendation=llm_result["recommendation"],
            confidence_score=llm_result["confidence_score"],
            technical_analysis=llm_result["technical_analysis"],
            supply_analysis=llm_result["supply_analysis"],
            risk_factors=llm_result["risk_factors"],
        )

        logger.info(
            "분석 완료: stock_code=%s, recommendation=%s",
            request.stock_code,
            response.recommendation,
        )

        return response

    except ValueError as e:
        logger.error("분석 데이터 오류: %s", str(e))
        raise HTTPException(status_code=400, detail=str(e)) from e
    except RuntimeError as e:
        logger.error("백엔드 통신 오류: %s", str(e))
        raise HTTPException(status_code=502, detail=str(e)) from e
    except Exception as e:
        logger.error("분석 중 예외 발생: %s", str(e), exc_info=True)
        raise HTTPException(status_code=500, detail="분석 중 오류가 발생했습니다") from e
