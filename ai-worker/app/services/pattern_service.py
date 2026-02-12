"""
차트 패턴 분석 서비스.
프랙탈 패턴 매칭과 기술적 지표를 기반으로 분석을 수행합니다.
"""
import logging
from typing import Any

import numpy as np
import pandas as pd

from app.clients.backend_client import BackendClient
from app.core.config import settings
from app.engine.indicators import (
    calculate_bollinger_bands,
    calculate_ema,
    calculate_macd,
    calculate_rsi,
    calculate_sma,
)
from app.engine.similarity import calculate_cosine_similarity
from app.schemas.analysis import AnalysisRequest
from app.services.llm_service import LlmService

logger = logging.getLogger(__name__)


class PatternService:
    """차트 패턴 분석 오케스트레이터."""

    def __init__(self):
        self.backend_client = BackendClient()
        self.llm_service = LlmService()

    async def analyze(self, request: AnalysisRequest) -> dict[str, Any]:
        """
        종목의 차트 패턴을 분석합니다.

        Args:
            request: 분석 요청 데이터.

        Returns:
            분석 결과 딕셔너리 (기술적 지표, 유사 패턴 등).
        """
        logger.info(
            "패턴 분석 시작: stock_code=%s, lookback_days=%d",
            request.stock_code,
            request.lookback_days,
        )

        # 1. 백엔드에서 OHLCV 데이터 조회
        price_data = await self.backend_client.get_stock_prices(
            request.stock_code, request.lookback_days
        )

        if not price_data or len(price_data) < 30:
            logger.warning("데이터가 부족합니다: %d 건", len(price_data) if price_data else 0)
            raise ValueError("분석에 필요한 최소 데이터(30일)가 부족합니다")

        # 2. DataFrame 변환
        df = self._convert_to_dataframe(price_data)
        logger.info("DataFrame 생성 완료: %d rows", len(df))

        # 3. 기술적 지표 계산
        indicators = self._calculate_indicators(df)
        logger.info("기술적 지표 계산 완료")

        # 4. 유사 패턴 탐색 (최근 20일 기준)
        similar_patterns = self._find_similar_patterns(df, window_size=20, top_k=5)
        logger.info("유사 패턴 탐색 완료: %d 개", len(similar_patterns))

        # 5. 결과 통합
        analysis_result = {
            "stock_code": request.stock_code,
            "current_price": float(df["close"].iloc[-1]),
            "indicators": indicators,
            "similar_patterns": similar_patterns,
            "data_points": len(df),
        }

        logger.info("패턴 분석 완료: stock_code=%s", request.stock_code)
        return analysis_result

    def _convert_to_dataframe(self, price_data: list[dict[str, Any]]) -> pd.DataFrame:
        """
        백엔드 API 응답을 pandas DataFrame으로 변환합니다.

        Args:
            price_data: 시세 데이터 리스트.

        Returns:
            OHLCV DataFrame (date 기준 정렬).
        """
        df = pd.DataFrame(price_data)

        # 컬럼명 확인 및 타입 변환
        df["date"] = pd.to_datetime(df["date"])
        df["open"] = df["open"].astype(float)
        df["high"] = df["high"].astype(float)
        df["low"] = df["low"].astype(float)
        df["close"] = df["close"].astype(float)
        df["volume"] = df["volume"].astype(int)

        # 날짜 기준 오름차순 정렬 (오래된 데이터 → 최신 데이터)
        df = df.sort_values("date").reset_index(drop=True)

        return df

    def _calculate_indicators(self, df: pd.DataFrame) -> dict[str, Any]:
        """
        기술적 지표를 계산합니다.

        Args:
            df: OHLCV DataFrame.

        Returns:
            지표값 딕셔너리.
        """
        close = df["close"]

        # RSI
        rsi = calculate_rsi(close, period=14)
        rsi_current = float(rsi.iloc[-1]) if not pd.isna(rsi.iloc[-1]) else None

        # MACD
        macd_line, signal_line, histogram = calculate_macd(close)
        macd_current = float(macd_line.iloc[-1]) if not pd.isna(macd_line.iloc[-1]) else None
        signal_current = (
            float(signal_line.iloc[-1]) if not pd.isna(signal_line.iloc[-1]) else None
        )
        histogram_current = (
            float(histogram.iloc[-1]) if not pd.isna(histogram.iloc[-1]) else None
        )

        # 이동평균
        sma_20 = calculate_sma(close, period=20)
        sma_60 = calculate_sma(close, period=60)
        ema_12 = calculate_ema(close, period=12)

        sma_20_current = float(sma_20.iloc[-1]) if not pd.isna(sma_20.iloc[-1]) else None
        sma_60_current = float(sma_60.iloc[-1]) if not pd.isna(sma_60.iloc[-1]) else None
        ema_12_current = float(ema_12.iloc[-1]) if not pd.isna(ema_12.iloc[-1]) else None

        # 볼린저 밴드
        upper_band, middle_band, lower_band = calculate_bollinger_bands(close, period=20)
        upper_current = (
            float(upper_band.iloc[-1]) if not pd.isna(upper_band.iloc[-1]) else None
        )
        middle_current = (
            float(middle_band.iloc[-1]) if not pd.isna(middle_band.iloc[-1]) else None
        )
        lower_current = (
            float(lower_band.iloc[-1]) if not pd.isna(lower_band.iloc[-1]) else None
        )

        # 현재가 대비 위치
        current_price = float(close.iloc[-1])
        price_vs_sma20 = (
            ((current_price - sma_20_current) / sma_20_current * 100)
            if sma_20_current
            else None
        )
        price_vs_sma60 = (
            ((current_price - sma_60_current) / sma_60_current * 100)
            if sma_60_current
            else None
        )

        return {
            "rsi": rsi_current,
            "macd": macd_current,
            "macd_signal": signal_current,
            "macd_histogram": histogram_current,
            "sma_20": sma_20_current,
            "sma_60": sma_60_current,
            "ema_12": ema_12_current,
            "bollinger_upper": upper_current,
            "bollinger_middle": middle_current,
            "bollinger_lower": lower_current,
            "price_vs_sma20_pct": price_vs_sma20,
            "price_vs_sma60_pct": price_vs_sma60,
        }

    def _find_similar_patterns(
        self, df: pd.DataFrame, window_size: int = 20, top_k: int = 5
    ) -> list[dict[str, Any]]:
        """
        과거 데이터에서 최근 패턴과 유사한 구간을 찾습니다.

        Args:
            df: OHLCV DataFrame.
            window_size: 비교할 윈도우 크기 (일).
            top_k: 반환할 상위 유사 패턴 개수.

        Returns:
            유사도 상위 패턴 리스트.
        """
        if len(df) < window_size * 2:
            logger.warning("유사 패턴 탐색을 위한 데이터 부족")
            return []

        # 최근 window_size 일의 종가 패턴 (정규화)
        recent_pattern = df["close"].iloc[-window_size:].values
        recent_pattern_normalized = self._normalize_series(recent_pattern)

        # 과거 구간들과 비교
        similarities = []
        max_start_idx = len(df) - window_size * 2  # 최근 패턴과 겹치지 않도록

        for i in range(0, max_start_idx):
            historical_pattern = df["close"].iloc[i : i + window_size].values
            if len(historical_pattern) < window_size:
                continue

            historical_pattern_normalized = self._normalize_series(historical_pattern)

            # 코사인 유사도 계산
            similarity = calculate_cosine_similarity(
                recent_pattern_normalized, historical_pattern_normalized
            )

            # 유사도가 임계값 이상인 경우만 저장
            if similarity >= settings.SIMILARITY_THRESHOLD:
                # 패턴 이후 5일간의 수익률 계산 (미래 수익률)
                future_start_idx = i + window_size
                future_end_idx = min(future_start_idx + 5, len(df))

                if future_end_idx > future_start_idx:
                    future_return = (
                        (df["close"].iloc[future_end_idx - 1] - df["close"].iloc[i + window_size - 1])
                        / df["close"].iloc[i + window_size - 1]
                        * 100
                    )
                else:
                    future_return = 0.0

                similarities.append(
                    {
                        "start_date": str(df["date"].iloc[i].date()),
                        "end_date": str(df["date"].iloc[i + window_size - 1].date()),
                        "similarity": round(similarity, 4),
                        "future_return_pct": round(float(future_return), 2),
                    }
                )

        # 유사도 기준 내림차순 정렬 후 상위 K개 반환
        similarities.sort(key=lambda x: x["similarity"], reverse=True)
        return similarities[:top_k]

    @staticmethod
    def _normalize_series(series: np.ndarray) -> np.ndarray:
        """
        시계열 데이터를 정규화합니다 (Min-Max Scaling).

        Args:
            series: 시계열 배열.

        Returns:
            정규화된 배열.
        """
        min_val = np.min(series)
        max_val = np.max(series)
        if max_val - min_val == 0:
            return np.zeros_like(series, dtype=float)
        return (series - min_val) / (max_val - min_val)
