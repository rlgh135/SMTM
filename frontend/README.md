# KR-Quant-Agent Frontend

React + TypeScript + Vite 기반의 한국 주식 AI 분석 플랫폼 프론트엔드입니다.

## 기술 스택

- **React 18.3** - UI 라이브러리
- **TypeScript 5.4** - 타입 안정성
- **Vite 5.3** - 빌드 도구 (HMR)
- **React Router 6.23** - 라우팅
- **TanStack Query 5.40** - 서버 상태 관리
- **Zustand 4.5** - 클라이언트 상태 관리
- **Axios 1.7** - HTTP 클라이언트
- **Recharts 2.12** - 차트 라이브러리
- **Tailwind CSS 3.4** - 스타일링

## 프로젝트 구조 (FSD: Feature-Sliced Design)

```
src/
├── app/                          # 애플리케이션 계층
│   ├── App.tsx                   # 라우트 정의
│   ├── main.tsx                  # 진입점 (QueryClient, Router Provider)
│   └── index.css                 # 글로벌 스타일
│
├── pages/                        # 페이지 컴포넌트
│   └── StockAnalysis.tsx         # 주식 분석 페이지 (메인)
│
├── widgets/                      # 독립적인 UI 블록
│   └── stock-chart/
│       ├── CandlestickChart.tsx  # 캔들스틱 차트 + MA 오버레이
│       └── index.ts
│
├── features/                     # 사용자 액션 (추후 확장)
│
├── entities/stock/               # Stock 도메인
│   ├── api/
│   │   └── stockApi.ts           # API 함수 (getStockAnalysis, getStockPrices)
│   ├── model/
│   │   ├── types.ts              # TypeScript 인터페이스
│   │   └── hooks.ts              # TanStack Query 커스텀 훅
│   └── ui/                       # 도메인 전용 UI (추후 확장)
│
└── shared/                       # 재사용 가능한 코드
    ├── api/
    │   └── axios.ts              # Axios 싱글톤 (인터셉터)
    └── ui/
        ├── Badge.tsx             # Buy/Sell/Hold 뱃지
        ├── Card.tsx              # 카드 컨테이너
        ├── Spinner.tsx           # 로딩 스피너
        ├── ErrorMessage.tsx      # 에러 메시지
        └── index.ts
```

## 주요 기능

### 1. StockAnalysis 페이지 (3단 레이아웃)

**상단: 종목 정보**
- 종목 코드
- 현재가
- 등락률 (▲/▼)

**중단: 캔들스틱 차트**
- 120일 OHLCV 데이터
- 이동평균선 오버레이 (MA5, MA20, MA60)
- 인터랙티브 툴팁 (Recharts)

**하단: AI 분석 리포트**
- 투자 추천 (BUY/SELL/HOLD 뱃지)
- 신뢰도 점수 (프로그레스 바)
- 기술적 분석 (RSI, MACD, 이동평균)
- 패턴 분석 (유사 패턴 수익률)
- 리스크 요인 목록

### 2. 커스텀 훅 (TanStack Query)

```typescript
// useStockAnalysis: AI 분석 결과 조회
const { data, isLoading, error } = useStockAnalysis('005930');

// useStockPrices: 시세 데이터 조회 + MA 계산
const { data } = useStockPrices('005930', 120);
```

**특징:**
- 자동 캐싱 (5분 staleTime)
- 에러 핸들링 (1회 재시도)
- 로딩 상태 관리
- 이동평균 자동 계산 (5, 20, 60일)

### 3. CandlestickChart 컴포넌트

**구현 방식:**
- Recharts `ComposedChart` 사용
- `Bar` + `shape` prop으로 캔들 바디/심지 렌더링
- `Line`으로 이동평균선 오버레이

**기능:**
- 자동 Y축 범위 계산 (여유 10%)
- 날짜 포맷팅 (MM/DD)
- 상승 캔들(녹색) / 하락 캔들(적색)
- 커스텀 툴팁 (시가/고가/저가/종가/거래량/MA)

## 설치 및 실행

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 환경변수 설정

`.env.example`을 복사하여 `.env` 생성:

```bash
cp .env.example .env
```

`.env` 파일:
```
VITE_API_URL=http://localhost:8080/api/v1
```

### 3. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 http://localhost:3000 접속

### 4. 프로덕션 빌드

```bash
npm run build
npm run preview
```

## API 통신 구조

### Axios 인터셉터 (`shared/api/axios.ts`)

```typescript
// Request Interceptor: Bearer 토큰 주입
config.headers.Authorization = `Bearer ${token}`;

// Response Interceptor: 401 자동 로그아웃
if (error.response?.status === 401) {
  localStorage.removeItem('accessToken');
  window.location.href = '/login';
}
```

### API 엔드포인트

**시세 조회:**
```
GET /stocks/{code}/prices?days=120
→ StockPrice[]
```

**AI 분석:**
```
GET /stocks/{code}/analysis
→ StockAnalysis { recommendation, confidenceScore, ... }
```

## 스타일링 (Tailwind CSS)

### 컬러 팔레트

- **Buy (매수):** `green-600`, `green-100`
- **Sell (매도):** `red-600`, `red-100`
- **Hold (보유):** `yellow-600`, `yellow-100`
- **차트 MA5:** `amber-600`
- **차트 MA20:** `blue-600`
- **차트 MA60:** `purple-600`

### 반응형 디자인

- Tailwind의 `sm:`, `md:`, `lg:` breakpoints 활용
- `ResponsiveContainer` (Recharts)로 차트 자동 리사이즈

## 개발 가이드

### 새 페이지 추가

1. `src/pages/NewPage.tsx` 생성
2. `src/app/App.tsx`에 라우트 추가:
   ```tsx
   <Route path="/new" element={<NewPage />} />
   ```

### 새 API 추가

1. `entities/stock/api/stockApi.ts`에 함수 작성
2. `entities/stock/model/hooks.ts`에 커스텀 훅 작성
3. 컴포넌트에서 훅 사용

### 새 UI 컴포넌트 추가

1. `shared/ui/Component.tsx` 생성
2. `shared/ui/index.ts`에서 export
3. 다른 컴포넌트에서 import

## 트러블슈팅

### CORS 오류

백엔드에서 CORS 설정 필요:

```java
@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
```

### Recharts 타입 오류

`vite-env.d.ts`에 추가:

```typescript
/// <reference types="recharts" />
```

### Axios 401 무한 루프

인터셉터에서 로그인 페이지 제외:

```typescript
if (error.config.url !== '/auth/login') {
  // redirect to login
}
```

## 성능 최적화

1. **코드 스플리팅:** React.lazy()로 페이지 지연 로딩
2. **메모이제이션:** React.memo()로 불필요한 리렌더링 방지
3. **Query 캐싱:** TanStack Query의 staleTime 활용
4. **번들 크기:** Recharts 대신 Lightweight Charts 고려 (필요시)

## 라이센스

MIT
