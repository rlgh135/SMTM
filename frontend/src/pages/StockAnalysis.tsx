import { useState } from 'react';
import { useStockAnalysis, useStockPrices } from '@/entities/stock/model/hooks';
import { CandlestickChart } from '@/widgets/stock-chart';
import { Badge, Card, LoadingSpinner, ErrorMessage } from '@/shared/ui';
import type { Recommendation } from '@/entities/stock/model/types';

export function StockAnalysisPage() {
  const [stockCode, setStockCode] = useState('005930'); // 기본값: 삼성전자

  const {
    data: analysisData,
    isLoading: isAnalysisLoading,
    error: analysisError,
    refetch: refetchAnalysis,
  } = useStockAnalysis(stockCode);

  const {
    data: pricesData,
    isLoading: isPricesLoading,
    error: pricesError,
    refetch: refetchPrices,
  } = useStockPrices(stockCode, 120);

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const code = formData.get('stockCode') as string;
    if (code.trim()) {
      setStockCode(code.trim());
    }
  };

  const handleRetry = () => {
    refetchAnalysis();
    refetchPrices();
  };

  const isLoading = isAnalysisLoading || isPricesLoading;
  const hasError = analysisError || pricesError;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-gray-900">
              KR-Quant-Agent
            </h1>
            <form onSubmit={handleSearch} className="flex gap-2">
              <input
                type="text"
                name="stockCode"
                defaultValue={stockCode}
                placeholder="종목 코드 입력 (예: 005930)"
                className="px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <button
                type="submit"
                className="px-6 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors"
              >
                분석
              </button>
            </form>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {isLoading && <LoadingSpinner message="AI 분석 중..." />}

        {hasError && (
          <ErrorMessage
            title="분석 실패"
            message={
              analysisError?.message ||
              pricesError?.message ||
              '데이터를 불러오는 중 오류가 발생했습니다.'
            }
            onRetry={handleRetry}
          />
        )}

        {!isLoading && !hasError && analysisData && pricesData && (
          <div className="space-y-6">
            {/* 상단: 종목 정보 */}
            <StockInfoSection
              stockCode={stockCode}
              currentPrice={pricesData[pricesData.length - 1]?.close || 0}
              changeRate={pricesData[pricesData.length - 1]?.changeRate || 0}
            />

            {/* 중단: 차트 */}
            <Card title="주가 차트 (120일)">
              <CandlestickChart data={pricesData} height={500} />
            </Card>

            {/* 하단: AI 분석 리포트 */}
            <AnalysisReportSection analysis={analysisData} />
          </div>
        )}
      </main>
    </div>
  );
}

interface StockInfoSectionProps {
  stockCode: string;
  currentPrice: number;
  changeRate: number;
}

function StockInfoSection({
  stockCode,
  currentPrice,
  changeRate,
}: StockInfoSectionProps) {
  const isPositive = changeRate >= 0;

  return (
    <Card>
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-900">
            {stockCode}
          </h2>
          <p className="text-sm text-gray-500 mt-1">종목 코드</p>
        </div>
        <div className="text-right">
          <p className="text-3xl font-bold text-gray-900">
            {currentPrice.toLocaleString()} 원
          </p>
          <p
            className={`text-lg font-semibold mt-1 ${
              isPositive ? 'text-red-600' : 'text-blue-600'
            }`}
          >
            {isPositive ? '▲' : '▼'} {Math.abs(changeRate).toFixed(2)}%
          </p>
        </div>
      </div>
    </Card>
  );
}

interface AnalysisReportSectionProps {
  analysis: {
    recommendation: Recommendation;
    confidenceScore: number;
    technicalAnalysis: string;
    supplyAnalysis: string;
    riskFactors: string[];
  };
}

function AnalysisReportSection({ analysis }: AnalysisReportSectionProps) {
  const badgeVariant =
    analysis.recommendation === 'BUY'
      ? 'buy'
      : analysis.recommendation === 'SELL'
        ? 'sell'
        : 'hold';

  const recommendationText = {
    BUY: '매수',
    SELL: '매도',
    HOLD: '보유',
  }[analysis.recommendation];

  return (
    <Card title="AI 분석 리포트">
      <div className="space-y-6">
        {/* 추천 및 신뢰도 */}
        <div className="flex items-center gap-4">
          <div>
            <p className="text-sm text-gray-500 mb-2">투자 추천</p>
            <Badge variant={badgeVariant} size="lg">
              {recommendationText}
            </Badge>
          </div>
          <div className="flex-1">
            <p className="text-sm text-gray-500 mb-2">신뢰도</p>
            <div className="flex items-center gap-3">
              <div className="flex-1 bg-gray-200 rounded-full h-3">
                <div
                  className={`h-3 rounded-full transition-all ${
                    analysis.confidenceScore >= 70
                      ? 'bg-green-600'
                      : analysis.confidenceScore >= 40
                        ? 'bg-yellow-500'
                        : 'bg-red-600'
                  }`}
                  style={{ width: `${analysis.confidenceScore}%` }}
                />
              </div>
              <span className="text-lg font-bold text-gray-900 min-w-[3rem]">
                {analysis.confidenceScore}%
              </span>
            </div>
          </div>
        </div>

        {/* 기술적 분석 */}
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">
            기술적 분석
          </h3>
          <p className="text-gray-700 leading-relaxed whitespace-pre-line">
            {analysis.technicalAnalysis}
          </p>
        </div>

        {/* 수급 분석 */}
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">
            패턴 분석
          </h3>
          <p className="text-gray-700 leading-relaxed whitespace-pre-line">
            {analysis.supplyAnalysis}
          </p>
        </div>

        {/* 리스크 요인 */}
        {analysis.riskFactors.length > 0 && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              리스크 요인
            </h3>
            <ul className="space-y-2">
              {analysis.riskFactors.map((risk, index) => (
                <li key={index} className="flex items-start gap-2">
                  <span className="text-red-600 mt-1">⚠</span>
                  <span className="text-gray-700">{risk}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* 면책 조항 */}
        <div className="mt-6 p-4 bg-gray-100 border border-gray-300 rounded-md">
          <p className="text-xs text-gray-600">
            ※ 본 분석은 AI 기반의 참고 자료이며, 투자 권유가 아닙니다. 투자에
            대한 최종 책임은 투자자 본인에게 있습니다.
          </p>
        </div>
      </div>
    </Card>
  );
}
