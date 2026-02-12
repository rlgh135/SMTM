import { useQuery } from '@tanstack/react-query';
import { getStockAnalysis, getStockPrices } from '../api/stockApi';
import type { StockAnalysis, StockPrice, StockPriceWithMA } from './types';

/**
 * 주식 분석 데이터를 가져오는 커스텀 훅.
 */
export function useStockAnalysis(stockCode: string) {
  return useQuery<StockAnalysis, Error>({
    queryKey: ['stockAnalysis', stockCode],
    queryFn: () => getStockAnalysis(stockCode),
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000, // 5분
    retry: 1,
  });
}

/**
 * 주식 시세 데이터를 가져오고 이동평균을 계산하는 커스텀 훅.
 */
export function useStockPrices(stockCode: string, days: number = 120) {
  return useQuery<StockPriceWithMA[], Error>({
    queryKey: ['stockPrices', stockCode, days],
    queryFn: async () => {
      const prices = await getStockPrices(stockCode, days);
      return calculateMovingAverages(prices);
    },
    enabled: !!stockCode,
    staleTime: 5 * 60 * 1000, // 5분
    retry: 1,
  });
}

/**
 * 시세 데이터에 이동평균을 계산하여 추가합니다.
 */
function calculateMovingAverages(prices: StockPrice[]): StockPriceWithMA[] {
  // 날짜 오름차순 정렬 (오래된 데이터 → 최신 데이터)
  const sorted = [...prices].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
  );

  return sorted.map((price, index) => {
    const pricesWithMA: StockPriceWithMA = { ...price };

    // MA5 계산
    if (index >= 4) {
      const sum5 = sorted.slice(index - 4, index + 1).reduce((acc, p) => acc + p.close, 0);
      pricesWithMA.ma5 = sum5 / 5;
    }

    // MA20 계산
    if (index >= 19) {
      const sum20 = sorted.slice(index - 19, index + 1).reduce((acc, p) => acc + p.close, 0);
      pricesWithMA.ma20 = sum20 / 20;
    }

    // MA60 계산
    if (index >= 59) {
      const sum60 = sorted.slice(index - 59, index + 1).reduce((acc, p) => acc + p.close, 0);
      pricesWithMA.ma60 = sum60 / 60;
    }

    return pricesWithMA;
  });
}
