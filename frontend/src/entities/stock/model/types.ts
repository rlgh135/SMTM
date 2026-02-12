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
