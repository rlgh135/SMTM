"""
LLM API 래퍼 서비스.
OpenAI, AWS Bedrock(Claude 3.5 Sonnet), Google Gemini API 호출을 관리하며,
Retry 로직을 포함합니다.
"""
import asyncio
import json
import logging
from typing import Any

from fastapi import HTTPException
from openai import AsyncOpenAI
from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.config import settings
from app.schemas.analysis import ModelProvider, Recommendation

logger = logging.getLogger(__name__)

# AWS Bedrock 모델 ID
_BEDROCK_MODEL_ID = "anthropic.claude-3-5-sonnet-20240620-v1:0"
# Google Gemini 모델명
_GEMINI_MODEL_NAME = "gemini-1.5-pro"


class LlmService:
    """멀티 프로바이더 LLM API 통신 서비스."""

    def __init__(self) -> None:
        # OpenAI 클라이언트는 항상 초기화 (API 키 미설정 시 호출 단계에서 오류 발생)
        self._openai_client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    async def generate_analysis(
        self,
        analysis_data: dict[str, Any],
        model_provider: ModelProvider = ModelProvider.OPENAI,
    ) -> dict[str, Any]:
        """
        지정된 LLM 프로바이더에 분석 데이터를 전송하고 투자 추천을 받습니다.

        Args:
            analysis_data: 패턴 분석 결과 딕셔너리.
            model_provider: 사용할 LLM 프로바이더.

        Returns:
            LLM 분석 결과 (recommendation, confidence_score, technical_analysis,
            supply_analysis, risk_factors).

        Raises:
            HTTPException(503): 요청한 프로바이더의 자격증명이 서버에 설정되지 않은 경우.
            HTTPException(500): API 호출이 3회 재시도 후에도 실패한 경우.
        """
        logger.info(
            "LLM 분석 요청 전송: stock_code=%s, provider=%s",
            analysis_data.get("stock_code"),
            model_provider.value,
        )

        # 자격증명 사전 검증 (retry 대상이 아니므로 외부에서 수행)
        self._validate_provider_credentials(model_provider)

        # 실제 API 호출은 retry 데코레이터가 적용된 내부 메서드에 위임
        return await self._call_with_retry(analysis_data, model_provider)

    # ------------------------------------------------------------------
    # Retry 래퍼
    # ------------------------------------------------------------------

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
    )
    async def _call_with_retry(
        self,
        analysis_data: dict[str, Any],
        model_provider: ModelProvider,
    ) -> dict[str, Any]:
        """
        프로바이더 별 API 호출 메서드를 실행합니다. (Tenacity Retry 적용)

        Args:
            analysis_data: 패턴 분석 결과 딕셔너리.
            model_provider: 사용할 LLM 프로바이더.

        Returns:
            검증된 LLM 분석 결과 딕셔너리.
        """
        system_prompt = self._get_system_prompt()
        user_prompt = self._build_prompt(analysis_data)

        try:
            if model_provider == ModelProvider.OPENAI:
                result_text = await self._call_openai(system_prompt, user_prompt)
            elif model_provider == ModelProvider.BEDROCK:
                result_text = await self._call_bedrock(system_prompt, user_prompt)
            elif model_provider == ModelProvider.GEMINI:
                result_text = await self._call_gemini(system_prompt, user_prompt)
            else:
                raise ValueError(f"지원하지 않는 프로바이더: {model_provider}")

            result = json.loads(result_text)
            self._validate_response(result)

            logger.info(
                "LLM 분석 완료: provider=%s, recommendation=%s, confidence=%d",
                model_provider.value,
                result["recommendation"],
                result["confidence_score"],
            )
            return result

        except json.JSONDecodeError as e:
            logger.error(
                "LLM 응답 JSON 파싱 실패 (provider=%s): %s", model_provider.value, str(e)
            )
            raise ValueError(f"LLM 응답 파싱 오류: {str(e)}") from e
        except HTTPException:
            # 자격증명 오류는 재시도 없이 즉시 전파
            raise
        except Exception as e:
            logger.error("LLM API 호출 실패 (provider=%s): %s", model_provider.value, str(e))
            raise

    # ------------------------------------------------------------------
    # 프로바이더 별 호출 메서드
    # ------------------------------------------------------------------

    async def _call_openai(self, system_prompt: str, user_prompt: str) -> str:
        """
        OpenAI Chat Completions API를 호출합니다.

        Args:
            system_prompt: 시스템 역할 프롬프트.
            user_prompt: 사용자 분석 요청 프롬프트.

        Returns:
            LLM 응답 텍스트 (JSON 문자열).
        """
        response = await self._openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            response_format={"type": "json_object"},
            temperature=0.3,
            max_tokens=1500,
        )
        result_text = response.choices[0].message.content
        if not result_text:
            raise ValueError("OpenAI 응답이 비어있습니다")
        return result_text

    async def _call_bedrock(self, system_prompt: str, user_prompt: str) -> str:
        """
        AWS Bedrock의 Claude 3.5 Sonnet 모델을 호출합니다.
        boto3는 동기 라이브러리이므로 별도 스레드에서 실행합니다.

        Args:
            system_prompt: 시스템 역할 프롬프트.
            user_prompt: 사용자 분석 요청 프롬프트.

        Returns:
            LLM 응답 텍스트 (JSON 문자열).
        """
        import boto3  # 런타임 임포트 (미설치 환경 대비)

        def _invoke() -> str:
            client = boto3.client(
                service_name="bedrock-runtime",
                region_name=settings.AWS_REGION,
                aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
                aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            )
            body = json.dumps(
                {
                    "anthropic_version": "bedrock-2023-05-31",
                    "max_tokens": 1500,
                    "temperature": 0.3,
                    "system": system_prompt,
                    "messages": [{"role": "user", "content": user_prompt}],
                }
            )
            response = client.invoke_model(modelId=_BEDROCK_MODEL_ID, body=body)
            response_body = json.loads(response["body"].read())
            return response_body["content"][0]["text"]

        # boto3 동기 호출을 비동기 컨텍스트에서 안전하게 실행
        result_text = await asyncio.to_thread(_invoke)
        if not result_text:
            raise ValueError("Bedrock 응답이 비어있습니다")
        return result_text

    async def _call_gemini(self, system_prompt: str, user_prompt: str) -> str:
        """
        Google Gemini 1.5 Pro 모델을 호출합니다.

        Args:
            system_prompt: 시스템 역할 프롬프트.
            user_prompt: 사용자 분석 요청 프롬프트.

        Returns:
            LLM 응답 텍스트 (JSON 문자열).
        """
        import google.generativeai as genai  # 런타임 임포트 (미설치 환경 대비)

        genai.configure(api_key=settings.GEMINI_API_KEY)
        model = genai.GenerativeModel(
            model_name=_GEMINI_MODEL_NAME,
            system_instruction=system_prompt,
            generation_config=genai.GenerationConfig(
                temperature=0.3,
                max_output_tokens=1500,
                response_mime_type="application/json",
            ),
        )
        response = await model.generate_content_async(user_prompt)
        result_text = response.text
        if not result_text:
            raise ValueError("Gemini 응답이 비어있습니다")
        return result_text

    # ------------------------------------------------------------------
    # 자격증명 검증
    # ------------------------------------------------------------------

    @staticmethod
    def _validate_provider_credentials(model_provider: ModelProvider) -> None:
        """
        요청한 프로바이더의 자격증명이 서버에 설정되어 있는지 검증합니다.
        서버 시작 시가 아닌, 런타임에 검증합니다.

        Args:
            model_provider: 사용 요청된 LLM 프로바이더.

        Raises:
            HTTPException(503): 필요한 자격증명이 설정되지 않은 경우.
        """
        if model_provider == ModelProvider.OPENAI and not settings.OPENAI_API_KEY:
            raise HTTPException(
                status_code=503,
                detail="OPENAI_API_KEY가 서버에 설정되어 있지 않습니다",
            )

        if model_provider == ModelProvider.BEDROCK:
            missing = [
                name
                for name, value in [
                    ("AWS_ACCESS_KEY_ID", settings.AWS_ACCESS_KEY_ID),
                    ("AWS_SECRET_ACCESS_KEY", settings.AWS_SECRET_ACCESS_KEY),
                ]
                if not value
            ]
            if missing:
                raise HTTPException(
                    status_code=503,
                    detail=(
                        f"AWS Bedrock 자격증명이 서버에 설정되어 있지 않습니다: "
                        f"{', '.join(missing)}"
                    ),
                )

        if model_provider == ModelProvider.GEMINI and not settings.GEMINI_API_KEY:
            raise HTTPException(
                status_code=503,
                detail="GEMINI_API_KEY가 서버에 설정되어 있지 않습니다",
            )

    # ------------------------------------------------------------------
    # 프롬프트 구성
    # ------------------------------------------------------------------

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

        Args:
            analysis_data: 패턴 분석 결과 딕셔너리.

        Returns:
            완성된 사용자 프롬프트 문자열.
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
            pattern_summary += (
                f"총 {len(similar_patterns)}개의 유사 패턴 발견 "
                f"(유사도 {settings.SIMILARITY_THRESHOLD} 이상)\n\n"
            )
            for idx, pattern in enumerate(similar_patterns, 1):
                pattern_summary += f"{idx}. 기간: {pattern['start_date']} ~ {pattern['end_date']}\n"
                pattern_summary += f"   - 유사도: {pattern['similarity']}\n"
                pattern_summary += (
                    f"   - 향후 5일 수익률: {pattern['future_return_pct']:+.2f}%\n\n"
                )

            avg_return = sum(p["future_return_pct"] for p in similar_patterns) / len(
                similar_patterns
            )
            pattern_summary += f"**평균 향후 수익률:** {avg_return:+.2f}%\n"
        else:
            pattern_summary = "**유사 패턴:** 임계값 이상의 유사 패턴이 발견되지 않았습니다.\n"

        return f"""
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

    # ------------------------------------------------------------------
    # 응답 검증
    # ------------------------------------------------------------------

    @staticmethod
    def _validate_response(result: dict[str, Any]) -> None:
        """
        LLM 응답의 유효성을 검증합니다.

        Args:
            result: JSON 파싱된 LLM 응답 딕셔너리.

        Raises:
            ValueError: 필수 필드 누락 또는 유효하지 않은 값인 경우.
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

        if result["recommendation"] not in [e.value for e in Recommendation]:
            raise ValueError(f"유효하지 않은 recommendation: {result['recommendation']}")

        if not isinstance(result["confidence_score"], int) or not (
            0 <= result["confidence_score"] <= 100
        ):
            raise ValueError(f"유효하지 않은 confidence_score: {result['confidence_score']}")

        if not isinstance(result["risk_factors"], list):
            raise ValueError("risk_factors는 리스트여야 합니다")
