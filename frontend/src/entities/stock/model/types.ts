export interface StockData {
  stockCode: string;
  stockName: string;
  market: 'KOSPI' | 'KOSDAQ';
  currentPrice: number;
  updatedAt: string;
}

export type Recommendation = 'BUY' | 'SELL' | 'HOLD';

export interface StockAnalysis {
  recommendation: Recommendation;
  confidenceScore: number;
  technicalAnalysis: string;
  supplyAnalysis: string;
  riskFactors: string[];
}

export interface StockPrice {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  changeRate: number;
}

export interface StockPriceWithMA extends StockPrice {
  ma5?: number;
  ma20?: number;
  ma60?: number;
}
