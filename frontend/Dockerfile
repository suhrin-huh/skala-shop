# SKALA-FUND 프론트 이미지 (React 정적 파일 + Nginx 리버스 프록시)
# 빌드 컨텍스트는 frontend/ 디렉터리다.
#   docker build -t skala-fund-frontend:latest ./frontend

# ---------------------------------------------------------------------------
# 1단계: React 빌드
# ---------------------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# 매니페스트만 먼저 복사해 npm ci 레이어를 소스와 분리한다.
# src/ 만 바뀐 빌드에서는 node_modules 를 다시 설치하지 않는다.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------------------------------------------------------------------------
# 2단계: Nginx 서빙
# ---------------------------------------------------------------------------
FROM nginx:alpine AS runtime

RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -fsS http://localhost/ -o /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
