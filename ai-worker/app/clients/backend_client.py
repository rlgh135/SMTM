"""
백엔드 API 클라이언트.
Spring Boot 백엔드로부터 주식 데이터를 가져옵니다.
"""
import logging
from typing import Any

import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)


class BackendClient:
    """백엔드 API와 통신하는 클라이언트."""

    def __init__(self):
        self.base_url = settings.BACKEND_API_URL
        self.timeout = 30.0

    async def get_stock_prices(
        self, stock_code: str, lookback_days: int
    ) -> list[dict[str, Any]]:
        """
        특정 종목의 최근 시세 데이터를 조회합니다.

        Args:
            stock_code: 종목 코드.
            lookback_days: 조회할 일수.

        Returns:
            시세 데이터 리스트 (date, open, high, low, close, volume).
        """
        url = f"{self.base_url}/api/v1/stocks/{stock_code}/prices"
        params = {"days": lookback_days}

        logger.info(
            "백엔드 API 호출: stock_code=%s, lookback_days=%d", stock_code, lookback_days
        )

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                data = response.json()

                logger.info("백엔드 API 응답 성공: %d 건의 데이터", len(data))
                return data

        except httpx.HTTPStatusError as e:
            logger.error(
                "백엔드 API 오류: status=%d, body=%s",
                e.response.status_code,
                e.response.text,
            )
            raise RuntimeError(f"백엔드 API 오류: {e.response.status_code}") from e
        except Exception as e:
            logger.error("백엔드 API 호출 실패: %s", str(e))
            raise RuntimeError(f"백엔드 API 호출 실패: {str(e)}") from e
