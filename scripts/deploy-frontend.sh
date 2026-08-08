#!/usr/bin/env bash
#
# SKALA-FUND 프론트 배포 (INF-04)
#
# 프론트는 정적 파일이라 블루/그린이 필요 없다. Nginx 이미지를 새로 받아 컨테이너를
# 재생성하면 끝난다 (수 초). 다만 아래 한 가지를 반드시 처리해야 한다.
#
#   ★ nginx.conf 는 이미지 안에 구워져 있고, 그 기본값은 `server app-blue:8080;` 이다.
#     현재 활성 슬롯이 green 인 상태에서 nginx 컨테이너를 재생성하면 upstream 이
#     app-blue 로 되돌아가 죽은 컨테이너를 가리킨다 (502).
#     그래서 재생성 직후 .deploy-state 를 읽어 upstream 을 다시 맞춰 준다.
#
# 사용법
#   ./scripts/deploy-frontend.sh
#   ./scripts/deploy-frontend.sh <FRONTEND_IMAGE_TAG>

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_FILE="${PROJECT_DIR}/docker-compose.yml"
ENV_FILE="${PROJECT_DIR}/.env"
STATE_FILE="${PROJECT_DIR}/.deploy-state"

NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"
NGINX_CONF_PATH="${NGINX_CONF_PATH:-/etc/nginx/conf.d/default.conf}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-3}"

log()  { printf '[deploy-fe %s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { printf '[deploy-fe %s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

compose() {
    docker compose --project-directory "${PROJECT_DIR}" -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" "$@"
}

[ -f "${COMPOSE_FILE}" ] || fail "compose 파일이 없습니다: ${COMPOSE_FILE}"
[ -f "${ENV_FILE}" ]     || fail ".env 가 없습니다: ${ENV_FILE}"
docker info > /dev/null 2>&1 || fail "Docker 데몬에 접근할 수 없습니다."

if [ "${1:-}" != "" ]; then
    export FRONTEND_IMAGE_TAG="$1"
    log "프론트 이미지 태그를 인자로 지정: ${FRONTEND_IMAGE_TAG}"
fi

# 현재 활성 백엔드 슬롯. 파일이 없으면 blue 로 본다 (nginx.conf 기본값과 같다).
ACTIVE="blue"
if [ -f "${STATE_FILE}" ]; then
    ACTIVE="$(tr -d '[:space:]' < "${STATE_FILE}")"
fi
case "${ACTIVE}" in
    blue|green) ;;
    *) log "경고: .deploy-state 값이 이상합니다('${ACTIVE}'). blue 로 간주합니다."; ACTIVE="blue" ;;
esac
ACTIVE_SVC="app-${ACTIVE}"
log "활성 백엔드 슬롯 = ${ACTIVE_SVC}"

log "이미지 pull"
compose pull nginx || fail "이미지 pull 실패. ECR 로그인 상태와 FRONTEND_IMAGE_TAG 를 확인하세요."

log "nginx 컨테이너 재생성"
compose up -d --force-recreate --no-deps nginx || fail "nginx 재생성 실패"

# upstream 을 활성 슬롯으로 되돌린다 (이미지에 구워진 기본값은 app-blue).
log "upstream 을 ${ACTIVE_SVC} 로 맞춤"
docker exec "${NGINX_CONTAINER}" \
    sed -i "s#app-blue:8080#${ACTIVE_SVC}:8080#g; s#app-green:8080#${ACTIVE_SVC}:8080#g" "${NGINX_CONF_PATH}" \
    || fail "nginx.conf 치환 실패"

docker exec "${NGINX_CONTAINER}" nginx -t || fail "nginx -t 실패"
docker exec "${NGINX_CONTAINER}" nginx -s reload || fail "nginx reload 실패"

# 정적 파일과 API 프록시가 모두 살아 있는지 확인한다.
log "헬스체크 (최대 ${HEALTH_TIMEOUT}초)"
elapsed=0
while [ "${elapsed}" -lt "${HEALTH_TIMEOUT}" ]; do
    if docker exec "${NGINX_CONTAINER}" curl -fsS http://localhost/ -o /dev/null 2>/dev/null \
       && docker exec "${NGINX_CONTAINER}" curl -fsS http://localhost/api/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        log "배포 완료. 정적 서빙 + API 프록시 정상."
        docker image prune -f > /dev/null 2>&1 || true
        exit 0
    fi
    sleep "${HEALTH_INTERVAL}"
    elapsed=$((elapsed + HEALTH_INTERVAL))
    log "  대기 중... ${elapsed}/${HEALTH_TIMEOUT}초"
done

fail "헬스체크 실패. 'docker logs ${NGINX_CONTAINER}' 와 백엔드 슬롯(${ACTIVE_SVC}) 상태를 확인하세요."
