# ============================================================
# 스테이지 1: 빌드 (Node.js)
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 파일만 먼저 복사하여 Docker 캐시 레이어 활용
COPY package.json package-lock.json* ./
RUN npm install

# 소스 코드 복사
COPY . .

# Vite 빌드 시점에 API URL 주입 (브라우저에서 실행되므로 localhost 기준)
ARG VITE_API_URL=http://localhost:8080/api/v1
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# ============================================================
# 스테이지 2: 서빙 (Nginx Alpine)
# ============================================================
FROM nginx:alpine AS runner

# SPA 라우팅을 위한 Nginx 설정 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드 결과물 복사
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
