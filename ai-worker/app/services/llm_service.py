"""
LLM API 래퍼 서비스.
OpenAI/Gemini API 호출을 관리하며, Retry 로직을 포함합니다.
"""
import json
import logging
from typing import Any

from openai import AsyncOpenAI
from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.config import settings
from app.schemas.analysis import Recommendation

logger = logging.getLogger(__name__)


class LlmService:
    """LLM API 통신 서비스."""

    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = "gpt-4o-mini"  # 비용 효율적인 모델

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
    )
    async def generate_analysis(
        self, analysis_data: dict[str, Any]
    ) -> dict[str, Any]:
        """
        LLM에 분석 데이터를 전송하고 투자 추천을 받습니다.

        Args:
            analysis_data: 패턴 분석 결과 딕셔너리.

        Returns:
            LLM 분석 결과 (recommendation, confidence_score, technical_analysis, supply_analysis, risk_factors).

        Raises:
            Exception: API 호출 실패 시 (3회 재시도 후).
        """
        logger.info("LLM 분석 요청 전송: stock_code=%s", analysis_data.get("stock_code"))

        # 프롬프트 구성
        prompt = self._build_prompt(analysis_data)

        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": self._get_system_prompt(),
                    },
                    {
                        "role": "user",
                        "content": prompt,
                    },
                ],
                response_format={"type": "json_object"},
                temperature=0.3,  # 일관성 있는 분석을 위해 낮은 온도
                max_tokens=1500,
            )

            result_text = response.choices[0].message.content
            if not result_text:
                raise ValueError("LLM 응답이 비어있습니다")

            # JSON 파싱
            result = json.loads(result_text)

            # 응답 검증
            self._validate_response(result)

            logger.info(
                "LLM 분석 완료: recommendation=%s, confidence=%d",
                result["recommendation"],
                result["confidence_score"],
            )

            return result

        except json.JSONDecodeError as e:
            logger.error("LLM 응답 JSON 파싱 실패: %s", str(e))
            raise ValueError(f"LLM 응답 파싱 오류: {str(e)}") from e
        except Exception as e:
            logger.error("LLM API 호출 실패: %s", str(e))
            raise

    def _get_system_prompt(self) -> str:
        """
        시스템 프롬프트를 반환합니다.
        LLM에게 "냉철한 월스트리트 펀드매니저" 페르소나를 부여합니다.
        """
        return """당신은 한국 주식 시장(KOSPI/KOSDAQ)을 전문으로 하는 베테랑 퀀트 애널리스트입니다.
기술적 지표와 프랙탈 패턴 분석을 기반으로 냉철하고 객관적인 투자 의견을 제시합니다.

**중요 규칙:**
1. 절대로 모호한 표현("좋아 보입니다", "나쁘지 않습니다")을 사용하지 마세요.
2. 구체적인 수치와 근거를 바탕으로 명확한 결론을 도출하세요.
3. 리스크 요인을 반드시 언급하세요.
4. 응답은 반드시 JSON 형식으로만 제공하세요.

**응답 형식 (JSON):**
{
  "recommendation": "BUY" | "SELL" | "HOLD",
  "confidence_score": 0-100 정수,
  "technical_analysis": "기술적 지표 분석 요약 (RSI, MACD, 이동평균 등)",
  "supply_analysis": "유사 패턴 분석 요약 (과거 유사 구간의 향후 수익률 패턴)",
  "risk_factors": ["리스크 요인 1", "리스크 요인 2", ...]
}"""

    def _build_prompt(self, analysis_data: dict[str, Any]) -> str:
        """
        분석 데이터를 기반으로 LLM 프롬프트를 구성합니다.
        """
        stock_code = analysis_data["stock_code"]
        current_price = analysis_data["current_price"]
        indicators = analysis_data["indicators"]
        similar_patterns = analysis_data["similar_patterns"]

        # 기술적 지표 요약
        tech_summary = f"""
**현재가:** {current_price:,.0f}원

**RSI(14):** {indicators.get('rsi', 'N/A')}
**MACD:** {indicators.get('macd', 'N/A')} (시그널: {indicators.get('macd_signal', 'N/A')}, 히스토그램: {indicators.get('macd_histogram', 'N/A')})
**이동평균:**
- SMA(20): {indicators.get('sma_20', 'N/A')} (현재가 대비: {indicators.get('price_vs_sma20_pct', 'N/A')}%)
- SMA(60): {indicators.get('sma_60', 'N/A')} (현재가 대비: {indicators.get('price_vs_sma60_pct', 'N/A')}%)
- EMA(12): {indicators.get('ema_12', 'N/A')}

**볼린저 밴드(20, 2σ):**
- 상단: {indicators.get('bollinger_upper', 'N/A')}
- 중간: {indicators.get('bollinger_middle', 'N/A')}
- 하단: {indicators.get('bollinger_lower', 'N/A')}
"""

        # 유사 패턴 요약
        if similar_patterns:
            pattern_summary = "**유사 패턴 분석 (최근 20일 기준):**\n"
            pattern_summary += f"총 {len(similar_patterns)}개의 유사 패턴 발견 (유사도 {settings.SIMILARITY_THRESHOLD} 이상)\n\n"

            for idx, pattern in enumerate(similar_patterns, 1):
                pattern_summary += f"{idx}. 기간: {pattern['start_date']} ~ {pattern['end_date']}\n"
                pattern_summary += f"   - 유사도: {pattern['similarity']}\n"
                pattern_summary += f"   - 향후 5일 수익률: {pattern['future_return_pct']:+.2f}%\n\n"

            # 평균 수익률 계산
            avg_return = sum(p["future_return_pct"] for p in similar_patterns) / len(
                similar_patterns
            )
            pattern_summary += f"**평균 향후 수익률:** {avg_return:+.2f}%\n"
        else:
            pattern_summary = "**유사 패턴:** 임계값 이상의 유사 패턴이 발견되지 않았습니다.\n"

        prompt = f"""
종목 코드: {stock_code}

{tech_summary}

{pattern_summary}

위 데이터를 종합하여 투자 의견을 제시해주세요.
- RSI 과매수/과매도 여부
- MACD 신호 (골든크로스/데드크로스)
- 이동평균선 배열 및 현재가 위치
- 볼린저 밴드 돌파 여부
- 유사 패턴의 향후 수익률 경향

JSON 형식으로만 응답하세요.
"""
        return prompt

    @staticmethod
    def _validate_response(result: dict[str, Any]) -> None:
        """
        LLM 응답의 유효성을 검증합니다.
        """
        required_fields = [
            "recommendation",
            "confidence_score",
            "technical_analysis",
            "supply_analysis",
            "risk_factors",
        ]

        for field in required_fields:
            if field not in result:
                raise ValueError(f"LLM 응답에 필수 필드 누락: {field}")

        # recommendation 검증
        if result["recommendation"] not in [e.value for e in Recommendation]:
            raise ValueError(f"유효하지 않은 recommendation: {result['recommendation']}")

        # confidence_score 검증
        if not isinstance(result["confidence_score"], int) or not (
            0 <= result["confidence_score"] <= 100
        ):
            raise ValueError(f"유효하지 않은 confidence_score: {result['confidence_score']}")

        # risk_factors 검증
        if not isinstance(result["risk_factors"], list):
            raise ValueError("risk_factors는 리스트여야 합니다")
