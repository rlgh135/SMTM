import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { StockPriceWithMA } from '@/entities/stock/model/types';

interface CandlestickChartProps {
  data: StockPriceWithMA[];
  height?: number;
}

interface CandleData extends StockPriceWithMA {
  candleColor: string;
  candleRange: [number, number];
}

export function CandlestickChart({ data, height = 400 }: CandlestickChartProps) {
  // 캔들 데이터 가공
  const chartData: CandleData[] = data.map((item) => ({
    ...item,
    candleColor: item.close >= item.open ? '#16a34a' : '#dc2626', // green : red
    candleRange: [
      Math.min(item.open, item.close),
      Math.max(item.open, item.close),
    ] as [number, number],
  }));

  // Y축 범위 계산 (여유 공간 10%)
  const allPrices = data.flatMap((d) => [d.low, d.high]);
  const minPrice = Math.min(...allPrices) * 0.95;
  const maxPrice = Math.max(...allPrices) * 1.05;

  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis
          dataKey="date"
          tickFormatter={(value) => {
            const date = new Date(value);
            return `${date.getMonth() + 1}/${date.getDate()}`;
          }}
          tick={{ fontSize: 12 }}
          stroke="#6b7280"
        />
        <YAxis
          domain={[minPrice, maxPrice]}
          tick={{ fontSize: 12 }}
          stroke="#6b7280"
          tickFormatter={(value) => value.toLocaleString()}
        />
        <Tooltip content={<CustomTooltip />} />
        <Legend wrapperStyle={{ fontSize: 12 }} />

        {/* High-Low 라인 (심지) */}
        <Bar
          dataKey="high"
          fill="transparent"
          shape={(props: any) => {
            const { x, y, width, payload } = props;
            const centerX = x + width / 2;
            const highY = y;
            const lowY =
              y +
              ((payload.high - payload.low) / (maxPrice - minPrice)) *
                height;

            return (
              <line
                x1={centerX}
                y1={highY}
                x2={centerX}
                y2={lowY}
                stroke="#6b7280"
                strokeWidth={1}
              />
            );
          }}
        />

        {/* Candlestick Body */}
        <Bar
          dataKey="candleRange"
          fill="#000"
          shape={(props: any) => {
            const { x, y, width, height, payload } = props;
            return (
              <rect
                x={x}
                y={y}
                width={width}
                height={height || 1}
                fill={payload.candleColor}
                stroke={payload.candleColor}
                strokeWidth={1}
              />
            );
          }}
        />

        {/* 이동평균선 */}
        <Line
          type="monotone"
          dataKey="ma5"
          stroke="#f59e0b"
          strokeWidth={1.5}
          dot={false}
          name="MA5"
          connectNulls
        />
        <Line
          type="monotone"
          dataKey="ma20"
          stroke="#3b82f6"
          strokeWidth={1.5}
          dot={false}
          name="MA20"
          connectNulls
        />
        <Line
          type="monotone"
          dataKey="ma60"
          stroke="#8b5cf6"
          strokeWidth={1.5}
          dot={false}
          name="MA60"
          connectNulls
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

function CustomTooltip({ active, payload }: any) {
  if (!active || !payload || payload.length === 0) return null;

  const data = payload[0].payload as StockPriceWithMA;

  return (
    <div className="bg-white border border-gray-300 rounded-lg shadow-lg p-3 text-sm">
      <p className="font-semibold text-gray-900 mb-2">{data.date}</p>
      <div className="space-y-1 text-gray-700">
        <p>
          시가: <span className="font-medium">{data.open.toLocaleString()}</span>
        </p>
        <p>
          고가: <span className="font-medium text-red-600">{data.high.toLocaleString()}</span>
        </p>
        <p>
          저가: <span className="font-medium text-blue-600">{data.low.toLocaleString()}</span>
        </p>
        <p>
          종가: <span className="font-medium">{data.close.toLocaleString()}</span>
        </p>
        <p>
          거래량:{' '}
          <span className="font-medium">{data.volume.toLocaleString()}</span>
        </p>
        {data.ma5 && (
          <p className="text-amber-600">
            MA5: <span className="font-medium">{data.ma5.toFixed(0)}</span>
          </p>
        )}
        {data.ma20 && (
          <p className="text-blue-600">
            MA20: <span className="font-medium">{data.ma20.toFixed(0)}</span>
          </p>
        )}
        {data.ma60 && (
          <p className="text-purple-600">
            MA60: <span className="font-medium">{data.ma60.toFixed(0)}</span>
          </p>
        )}
      </div>
    </div>
  );
}
