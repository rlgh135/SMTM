"""
기술적 지표 계산 엔진.
TA-Lib 래퍼 및 커스텀 지표를 제공합니다.
"""
import numpy as np
import pandas as pd


def calculate_rsi(close: pd.Series, period: int = 14) -> pd.Series:
    """
    RSI(Relative Strength Index)를 계산합니다.

    Args:
        close: 종가 시계열.
        period: 계산 기간 (기본값: 14).

    Returns:
        RSI 값 시계열.
    """
    delta = close.diff()
    gain = delta.where(delta > 0, 0.0)
    loss = (-delta).where(delta < 0, 0.0)

    avg_gain = gain.rolling(window=period, min_periods=period).mean()
    avg_loss = loss.rolling(window=period, min_periods=period).mean()

    rs = avg_gain / avg_loss.replace(0, np.nan)
    return 100.0 - (100.0 / (1.0 + rs))


def calculate_macd(
    close: pd.Series,
    fast_period: int = 12,
    slow_period: int = 26,
    signal_period: int = 9,
) -> tuple[pd.Series, pd.Series, pd.Series]:
    """
    MACD(Moving Average Convergence Divergence)를 계산합니다.

    Args:
        close: 종가 시계열.
        fast_period: 빠른 EMA 기간.
        slow_period: 느린 EMA 기간.
        signal_period: 시그널 라인 기간.

    Returns:
        (MACD 라인, 시그널 라인, 히스토그램) 튜플.
    """
    ema_fast = close.ewm(span=fast_period, adjust=False).mean()
    ema_slow = close.ewm(span=slow_period, adjust=False).mean()

    macd_line = ema_fast - ema_slow
    signal_line = macd_line.ewm(span=signal_period, adjust=False).mean()
    histogram = macd_line - signal_line

    return macd_line, signal_line, histogram


def calculate_sma(close: pd.Series, period: int = 20) -> pd.Series:
    """
    SMA(Simple Moving Average)를 계산합니다.

    Args:
        close: 종가 시계열.
        period: 이동평균 기간.

    Returns:
        이동평균 시계열.
    """
    return close.rolling(window=period, min_periods=period).mean()


def calculate_ema(close: pd.Series, period: int = 20) -> pd.Series:
    """
    EMA(Exponential Moving Average)를 계산합니다.

    Args:
        close: 종가 시계열.
        period: 이동평균 기간.

    Returns:
        지수 이동평균 시계열.
    """
    return close.ewm(span=period, adjust=False).mean()


def calculate_bollinger_bands(
    close: pd.Series, period: int = 20, num_std: float = 2.0
) -> tuple[pd.Series, pd.Series, pd.Series]:
    """
    볼린저 밴드를 계산합니다.

    Args:
        close: 종가 시계열.
        period: 이동평균 기간.
        num_std: 표준편차 배수.

    Returns:
        (상단 밴드, 중간 밴드, 하단 밴드) 튜플.
    """
    middle_band = calculate_sma(close, period)
    std = close.rolling(window=period, min_periods=period).std()
    upper_band = middle_band + (std * num_std)
    lower_band = middle_band - (std * num_std)

    return upper_band, middle_band, lower_band
