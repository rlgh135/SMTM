"""
시계열 유사도 계산 엔진.
벡터화 연산만 사용하여 성능을 보장합니다.
"""
import numpy as np


def calculate_cosine_similarity(series_a: np.ndarray, series_b: np.ndarray) -> float:
    """
    두 시계열 데이터 간의 코사인 유사도를 계산합니다.

    Args:
        series_a: 첫 번째 시계열 배열.
        series_b: 두 번째 시계열 배열.

    Returns:
        코사인 유사도 값 (-1.0 ~ 1.0).
    """
    norm_a = np.linalg.norm(series_a)
    norm_b = np.linalg.norm(series_b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(np.dot(series_a, series_b) / (norm_a * norm_b))


def calculate_dtw_distance(series_a: np.ndarray, series_b: np.ndarray) -> float:
    """
    Dynamic Time Warping 거리를 계산합니다.

    Args:
        series_a: 첫 번째 시계열 배열.
        series_b: 두 번째 시계열 배열.

    Returns:
        DTW 거리 값.
    """
    n, m = len(series_a), len(series_b)
    dtw_matrix = np.full((n + 1, m + 1), np.inf)
    dtw_matrix[0, 0] = 0.0

    for i in range(1, n + 1):
        for j in range(1, m + 1):
            cost = abs(series_a[i - 1] - series_b[j - 1])
            dtw_matrix[i, j] = cost + min(
                dtw_matrix[i - 1, j],
                dtw_matrix[i, j - 1],
                dtw_matrix[i - 1, j - 1],
            )

    return float(dtw_matrix[n, m])
