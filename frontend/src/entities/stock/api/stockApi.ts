import { axiosInstance } from '@/shared/api/axios';
import type { StockData, StockAnalysis } from '../model/types';

export const getStockDetail = async (code: string): Promise<StockData> => {
  const { data } = await axiosInstance.get<StockData>(`/stocks/${code}`);
  return data;
};

export const getStockAnalysis = async (code: string): Promise<StockAnalysis> => {
  const { data } = await axiosInstance.get<StockAnalysis>(
    `/stocks/${code}/analysis`,
  );
  return data;
};
