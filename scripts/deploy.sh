#!/usr/bin/env bash
#
# SKALA-FUND 백엔드 블루/그린 배포 (INF-04)
#
# 순서
#   1. 비활성 슬롯(blue <-> green)을 새 이미지로 기동
#   2. /actuator/health 를 최대 HEALTH_TIMEOUT 초 동안 폴링
#   3. 통과 -> nginx.conf 의 upstream 을 새 슬롯으로 교체
#   4. docker exec nginx nginx -s reload  (커넥션을 끊지 않고 설정만 갈아끼운다)
#   5. 기존 슬롯 중지·제거
#   6. 헬스체크 실패 -> 새 슬롯만 제거하고 기존 슬롯 유지 (롤백). nginx 는 건드리지 않는다.
#
# 사용법
#   ./scripts/deploy.sh                        # .env 의 BACKEND_IMAGE_TAG 로 배포
#   ./scripts/deploy.sh <BACKEND_IMAGE_TAG>    # 태그를 지정해 배포
#
# 현재 활성 슬롯은 .deploy-state 에 기록한다. 다음 배포가 이 파일을 읽어 반대편을 고른다.

set -Eeuo pipefail

# ---------------------------------------------------------------------------
# 경로와 기본값
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_FILE="${PROJECT_DIR}/docker-compose.yml"
ENV_FILE="${PROJECT_DIR}/.env"
STATE_FILE="${PROJECT_DIR}/.deploy-state"

NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"
NGINX_CONF_PATH="${NGINX_CONF_PATH:-/etc/nginx/conf.d/default.conf}"

# 헬스체크 총 대기 시간(초). 첫 기동에서는 Flyway 마이그레이션이 먼저 돌기 때문에
# 60초로는 부족할 수 있다.
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-120}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-3}"

log()  { printf '[deploy %s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { printf '[deploy %s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

compose() {
    docker compose --project-directory "${PROJECT_DIR}" -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" "$@"
}

# ---------------------------------------------------------------------------
# 사전 점검
# ---------------------------------------------------------------------------
[ -f "${COMPOSE_FILE}" ] || fail "compose 파일이 없습니다: ${COMPOSE_FILE}"
[ -f "${ENV_FILE}" ]     || fail ".env 가 없습니다: ${ENV_FILE} (.env.example 을 복사해 채우세요)"

if ! docker info > /dev/null 2>&1; then
    fail "Docker 데몬에 접근할 수 없습니다."
fi

if [ "${1:-}" != "" ]; then
    # 셸 환경변수가 --env-file 의 값보다 우선한다 (compose 변수 치환 규칙).
    export BACKEND_IMAGE_TAG="$1"
    log "백엔드 이미지 태그를 인자로 지정: ${BACKEND_IMAGE_TAG}"
fi

# ---------------------------------------------------------------------------
# 활성/비활성 슬롯 결정
# ---------------------------------------------------------------------------
CURRENT="blue"
if [ -f "${STATE_FILE}" ]; then
    CURRENT="$(tr -d '[:space:]' < "${STATE_FILE}")"
fi

case "${CURRENT}" in
    blue)  TARGET="green" ;;
    green) TARGET="blue"  ;;
    *)
        log "경고: .deploy-state 값이 이상합니다('${CURRENT}'). blue 가 활성이라고 가정합니다."
        CURRENT="blue"
        TARGET="green"
        ;;
esac

CURRENT_SVC="app-${CURRENT}"
TARGET_SVC="app-${TARGET}"

log "현재 활성=${CURRENT_SVC} / 배포 대상=${TARGET_SVC}"

# ---------------------------------------------------------------------------
# 롤백 경로
#   신규(TARGET) 컨테이너만 지우고 기존(CURRENT)은 그대로 둔다.
#   nginx 는 아직 CURRENT 를 가리키고 있으므로 서비스는 끊기지 않는다.
# ---------------------------------------------------------------------------
rollback() {
    local reason="$1"
    log "롤백: ${reason}"
    log "실패 원인 파악용 로그 (${TARGET_SVC} 마지막 80줄)"
    docker logs --tail 80 "${TARGET_SVC}" 2>&1 | sed 's/^/    | /' || true

    log "${TARGET_SVC} 를 제거합니다. ${CURRENT_SVC} 는 그대로 유지됩니다."
    compose stop "${TARGET_SVC}" > /dev/null 2>&1 || true
    compose rm -f "${TARGET_SVC}" > /dev/null 2>&1 || true

    # 상태 파일은 손대지 않는다. 다음 배포도 같은 슬롯을 대상으로 재시도한다.
    fail "배포 실패. 기존 ${CURRENT_SVC} 로 서비스가 계속됩니다."
}

# ---------------------------------------------------------------------------
# 1. 새 이미지 받아서 비활성 슬롯 기동
# ---------------------------------------------------------------------------
log "이미지 pull"
compose pull "${TARGET_SVC}" || fail "이미지 pull 실패. ECR 로그인 상태와 BACKEND_IMAGE_TAG 를 확인하세요."

log "${TARGET_SVC} 기동 (기존 컨테이너가 남아 있으면 새로 만든다)"
compose up -d --force-recreate --no-deps "${TARGET_SVC}" || rollback "${TARGET_SVC} 기동 실패"

# ---------------------------------------------------------------------------
# 2. 헬스체크 폴링
#    nginx 를 거치지 않고 앱 네트워크 안에서 직접 때린다.
#    (아직 upstream 이 CURRENT 를 가리키고 있으므로 nginx 로는 확인할 수 없다)
# ---------------------------------------------------------------------------
log "헬스체크 시작 (최대 ${HEALTH_TIMEOUT}초)"
elapsed=0
healthy=0

while [ "${elapsed}" -lt "${HEALTH_TIMEOUT}" ]; do
    # 컨테이너가 죽어버렸으면 더 기다릴 이유가 없다.
    if ! docker ps --format '{{.Names}}' | grep -qx "${TARGET_SVC}"; then
        rollback "${TARGET_SVC} 컨테이너가 실행 중이 아닙니다 (기동 직후 종료)."
    fi

    if docker exec "${TARGET_SVC}" curl -fsS "http://localhost:8080/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
        healthy=1
        break
    fi

    sleep "${HEALTH_INTERVAL}"
    elapsed=$((elapsed + HEALTH_INTERVAL))
    log "  대기 중... ${elapsed}/${HEALTH_TIMEOUT}초"
done

[ "${healthy}" -eq 1 ] || rollback "헬스체크가 ${HEALTH_TIMEOUT}초 안에 UP 이 되지 않았습니다."
log "헬스체크 통과: ${TARGET_SVC} UP"

# ---------------------------------------------------------------------------
# 3. nginx upstream 교체
#    frontend/nginx.conf 의 `server app-blue:8080;` 줄을 대상 슬롯으로 치환한다.
# ---------------------------------------------------------------------------
docker ps --format '{{.Names}}' | grep -qx "${NGINX_CONTAINER}" \
    || rollback "nginx 컨테이너(${NGINX_CONTAINER})가 실행 중이 아닙니다."

log "nginx upstream 을 ${TARGET_SVC} 로 교체"
# 양방향 치환을 한 줄에 넣어 현재 값이 blue 든 green 이든 대상 슬롯으로 맞춘다.
# (구분자로 # 을 쓴 이유: 값에 / 가 없고, | 를 쓰면 sed 대체 구문과 헷갈린다)
docker exec "${NGINX_CONTAINER}" \
    sed -i "s#app-blue:8080#${TARGET_SVC}:8080#g; s#app-green:8080#${TARGET_SVC}:8080#g" "${NGINX_CONF_PATH}" \
    || rollback "nginx.conf 치환 실패"

# 치환이 실제로 됐는지 확인한다. sed 는 매칭이 없어도 성공(0)을 반환한다.
docker exec "${NGINX_CONTAINER}" grep -q "server ${TARGET_SVC}:8080;" "${NGINX_CONF_PATH}" \
    || rollback "nginx.conf 에 upstream 줄이 반영되지 않았습니다. nginx.conf 의 upstream 형식을 확인하세요."

# 4. 문법 검증 후 무중단 reload
log "nginx 설정 문법 검증"
docker exec "${NGINX_CONTAINER}" nginx -t || rollback "nginx -t 실패 (설정이 적용되지 않았습니다)"

log "nginx reload"
docker exec "${NGINX_CONTAINER}" nginx -s reload || rollback "nginx reload 실패"

# nginx 가 새 upstream 으로 실제 응답하는지 최종 확인한다.
sleep 2
if ! docker exec "${NGINX_CONTAINER}" curl -fsS "http://localhost/api/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    log "경고: nginx 경유 헬스체크가 실패했습니다. 이미 upstream 이 교체된 상태이므로"
    log "      기존 슬롯(${CURRENT_SVC})은 제거하지 않고 남겨 둡니다. 수동 확인이 필요합니다."
    log "      되돌리려면: docker exec ${NGINX_CONTAINER} sed -i 's|${TARGET_SVC}:8080|${CURRENT_SVC}:8080|' ${NGINX_CONF_PATH} && docker exec ${NGINX_CONTAINER} nginx -s reload"
    exit 1
fi

# ---------------------------------------------------------------------------
# 5. 상태 기록 후 기존 슬롯 정리
#    상태 파일을 먼저 쓴다. 이 시점 이후로는 트래픽이 TARGET 으로 가고 있으므로
#    정리 단계에서 스크립트가 죽더라도 다음 배포가 올바른 슬롯을 고를 수 있어야 한다.
# ---------------------------------------------------------------------------
printf '%s\n' "${TARGET}" > "${STATE_FILE}"
log ".deploy-state = ${TARGET}"

log "기존 ${CURRENT_SVC} 중지·제거"
compose stop "${CURRENT_SVC}" > /dev/null 2>&1 || true
compose rm -f "${CURRENT_SVC}" > /dev/null 2>&1 || true

# 이전 태그 이미지가 계속 쌓이면 EC2 디스크가 찬다.
log "미사용 이미지 정리"
docker image prune -f > /dev/null 2>&1 || true

log "배포 완료. 활성 슬롯 = ${TARGET_SVC}"
