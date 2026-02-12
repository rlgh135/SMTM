"""
LLM API 래퍼 서비스.
OpenAI/Gemini API 호출을 관리하며, Retry 로직을 포함합니다.
"""
import logging

from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.config import settings

logger = logging.getLogger(__name__)


class LlmService:
    """LLM API 통신 서비스."""

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
    )
    async def generate_analysis(self, prompt: str) -> str:
        """
        LLM에 분석 프롬프트를 전송하고 결과를 반환합니다.

        Args:
            prompt: 분석 프롬프트 문자열.

        Returns:
            LLM 응답 텍스트.

        Raises:
            Exception: API 호출 실패 시 (3회 재시도 후).
        """
        logger.info("LLM 분석 요청 전송")
        # TODO: 실제 LLM API 호출 구현
        raise NotImplementedError("LLM 서비스가 아직 구현되지 않았습니다.")
