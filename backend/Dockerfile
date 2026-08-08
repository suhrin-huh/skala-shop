# SKALA-FUND 백엔드 이미지
# 빌드 컨텍스트는 backend/ 디렉터리다.
#   docker build -t skala-fund-backend:latest ./backend

# ---------------------------------------------------------------------------
# 1단계: 빌드
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 의존성 캐시 레이어를 소스와 분리한다.
# 래퍼와 빌드 스크립트만 먼저 복사하면, src/ 만 바뀐 빌드에서는 아래 RUN 레이어가
# 캐시에 적중해 의존성을 다시 받지 않는다. build.gradle 이 바뀔 때만 다시 받는다.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew \
    # 의존성만 미리 내려받아 레이어로 굳힌다.
    # 이 단계의 실패로 빌드를 멈추지는 않는다(캐시 워밍이 목적이며, 실제 해결 실패는 다음 build 에서 드러난다).
    && ./gradlew --no-daemon dependencies --refresh-dependencies > /dev/null 2>&1 || true

# 여기부터가 소스. 위 레이어는 재사용된다.
COPY src src

# 테스트는 CI(GitHub Actions)에서 이미 돌았다. 이미지 빌드에서 또 돌리면
# 배포 시간만 늘고, 컨테이너 안에는 테스트용 DB 도 없다.
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------------------------------------------------------------------------
# 2단계: 실행 (JRE 슬림)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# 헬스체크와 진단에 필요한 최소 도구만 넣는다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=prod
# 컨테이너 메모리 한도를 힙 계산에 반영한다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

# root 로 돌리지 않는다.
RUN groupadd --system skala && useradd --system --gid skala --create-home skala
WORKDIR /app

# 로컬 통합 환경(docker-compose.local.yml)이 여기에 볼륨을 붙인다.
# 디렉터리를 미리 만들어 두지 않으면 Docker 가 root 소유로 생성해 skala 유저가 쓰지 못한다.
RUN mkdir -p /app/uploads && chown -R skala:skala /app

COPY --from=build --chown=skala:skala /workspace/build/libs/*.jar app.jar

USER skala
EXPOSE 8080

# 컨테이너 헬스체크. 배포 스크립트(scripts/deploy.sh)도 같은 엔드포인트를 폴링한다.
# start-period 를 넉넉히 잡는 이유: 첫 기동에서 Flyway 마이그레이션이 먼저 돌기 때문이다.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
