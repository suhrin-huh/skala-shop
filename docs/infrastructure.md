# SKALA-FUND 인프라 운영 문서

EC2 **1대**에 Docker Compose 로 전부 올린다. Kubernetes / ECS / Fargate 를 쓰지 않는다.

```
                    ┌──────────────── EC2 1대 ────────────────┐
                    │                                          │
사용자 ──80/443──►  │  nginx 컨테이너                          │
                    │   ├─ /        → React 정적 파일           │
                    │   ├─ /api/    → proxy_pass backend        │
                    │   └─ upstream backend → blue 또는 green   │
                    │                                          │
                    │  app-blue (8080)    app-green (8080)      │
                    │   ※ 둘 중 하나만 떠 있다                 │
                    └────────────┬─────────────────────────────┘
                                 │
                          RDS MySQL 8 (프라이빗 서브넷)
```

프론트와 API 가 **같은 오리진**이다. 그래서 백엔드에 CORS 설정이 없고, 필요하지도 않다.
`localhost:3000` 에서 개발할 때만 Vite dev server 의 proxy 가 그 역할을 한다.

---

## 1. 파일 지도

| 경로 | 역할 |
|---|---|
| `backend/Dockerfile` | 백엔드 멀티 스테이지 빌드 (JDK 21 build → JRE 21 runtime) |
| `frontend/Dockerfile` | React 빌드 → nginx:alpine 서빙 |
| `frontend/nginx.conf` | 리버스 프록시 + SPA 폴백. **이미지 안에 구워진다** |
| `docker-compose.yml` | 운영 구성 (nginx / app-blue / app-green) |
| `docker-compose.local.yml` | 로컬 통합 환경 (backend + MySQL) |
| `scripts/deploy.sh` | 백엔드 블루/그린 배포 + 롤백 |
| `scripts/deploy-frontend.sh` | Nginx 컨테이너 재생성 |
| `backend/src/main/resources/db/migration/` | Flyway 마이그레이션 |
| `.env.example` | 환경변수 키 템플릿 (값 없음) |
| `.github/workflows/ci.yml` | PR·main push CI |
| `.github/workflows/cd-backend.yml` | 백엔드 CD (`paths: backend/**`) |
| `.github/workflows/cd-frontend.yml` | 프론트 CD (`paths: frontend/**`) |

---

## 2. AWS 프로비저닝 순서

> 아래는 **사용자가 콘솔에서 직접 수행**한다. 과금이 발생하는 작업이라 자동화하지 않았다.
> 순서에 의존성이 있다. VPC → 보안 그룹 → RDS → EC2 순서를 지킬 것.

### 2-1. VPC / 서브넷
1. 기본 VPC 를 써도 된다. 새로 만든다면 퍼블릭 서브넷 1개 + 프라이빗 서브넷 **2개**(RDS 는 서로 다른 AZ 의 서브넷 2개를 요구한다).
2. 퍼블릭 서브넷에 인터넷 게이트웨이를 연결한다.

### 2-2. 보안 그룹 (2개)
| 이름 | 인바운드 | 비고 |
|---|---|---|
| `skala-ec2-sg` | 80 (0.0.0.0/0), 443 (0.0.0.0/0), 22 (**내 IP 만**) | 22 를 전체 개방하지 말 것 |
| `skala-rds-sg` | 3306 — **소스를 `skala-ec2-sg` 로 지정** | CIDR 이 아니라 보안 그룹을 소스로 |

RDS 인바운드에 `0.0.0.0/0` 을 넣지 않는다. DB 비밀번호 하나에 전부가 걸리게 된다.

### 2-3. RDS (MySQL 8.x)
1. 엔진 MySQL 8.0, 퍼블릭 액세스 **아니요**, 프라이빗 서브넷 그룹, 보안 그룹 `skala-rds-sg`.
2. **파라미터 그룹**을 새로 만들어 적용한다 (기본 그룹은 수정할 수 없다).
   - `time_zone` = `Asia/Seoul`
   - `character_set_server` = `utf8mb4`
   - `collation_server` = `utf8mb4_unicode_ci`
   - 이 셋을 빼먹으면 카테고리명의 가운뎃점(`·`)과 한글이 `????` 로 저장된다.
3. 초기 DB 이름을 지정한다 (예: `skalafund`). 지정하지 않으면 스키마가 만들어지지 않아
   Flyway 가 붙을 대상이 없다.
4. 엔드포인트·포트·마스터 계정명·비밀번호를 기록해 둔다 → `.env` 의 `DB_*`.

### 2-4. ECR (리포지토리 2개)
```
skala-fund-backend
skala-fund-frontend
```
- 프라이빗 리포지토리로 만든다.
- 수명 주기 정책을 걸어 둔다: "태그 없는 이미지 1일 후 만료", "최근 10개만 유지".
  없으면 커밋마다 이미지가 쌓여 스토리지 비용이 계속 늘어난다.
- 레지스트리 URI(`<계정ID>.dkr.ecr.<리전>.amazonaws.com`)를 기록 → `.env` 의 `ECR_REGISTRY`.

### 2-5. S3 (이미지 저장)
1. 버킷 생성 (예: `skala-fund-images-prod`). 리전은 EC2/RDS 와 같게.
2. 퍼블릭 읽기가 필요하면 **버킷 정책으로 GetObject 만** 허용한다. ACL 을 켜지 않는다.
3. 버킷명 기록 → `.env` 의 `AWS_S3_BUCKET`.

### 2-6. EC2
1. Amazon Linux 2023 또는 Ubuntu 22.04, t3.small 이상 (t2.micro 는 Gradle/Java 힙에 빠듯하다).
2. 보안 그룹 `skala-ec2-sg`, 키 페어 생성 후 `.pem` 보관.
3. **Elastic IP 할당 후 연결.** 안 하면 인스턴스를 중지·시작할 때마다 IP 가 바뀌어 DNS 가 깨진다.
4. IAM 인스턴스 프로파일을 붙인다 (권장). 정책: `AmazonEC2ContainerRegistryReadOnly` +
   S3 버킷에 대한 `s3:PutObject`/`s3:GetObject`.
   인스턴스 프로파일을 붙이면 `.env` 의 `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` 를 비워도 된다.
   장기 액세스 키를 디스크에 두는 것보다 안전하다.
5. 초기 설정 (SSH 접속 후):
   ```bash
   # Docker
   sudo dnf install -y docker            # Ubuntu: sudo apt-get install -y docker.io
   sudo systemctl enable --now docker
   sudo usermod -aG docker $USER         # 재로그인 필요

   # Docker Compose v2 플러그인
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   docker compose version

   # AWS CLI (ECR 로그인에 필요)
   sudo dnf install -y awscli git

   # 배포 디렉터리
   sudo mkdir -p /opt/skala-fund && sudo chown $USER:$USER /opt/skala-fund
   git clone <저장소 URL> /opt/skala-fund
   cd /opt/skala-fund
   cp .env.example .env && chmod 600 .env   # 값을 채운다
   chmod +x scripts/*.sh
   ```

### 2-7. 도메인 / HTTPS
1. Route 53 (또는 외부 등록기관)에서 A 레코드 → EC2 Elastic IP.
2. DNS 전파 확인 후 certbot 으로 인증서 발급:
   ```bash
   sudo dnf install -y certbot
   sudo certbot certonly --standalone -d example.com -d www.example.com
   ```
   `--standalone` 은 80 포트를 쓰므로 발급 중에는 nginx 컨테이너를 잠깐 내린다.
3. `docker-compose.yml` 의 nginx 서비스가 `/etc/letsencrypt` 를 읽기 전용으로 마운트한다.
   `frontend/nginx.conf` 에 443 `server` 블록을 추가하고 `ssl_certificate` 경로를 지정한 뒤 재배포한다.
4. 자동 갱신:
   ```bash
   sudo crontab -e
   0 3 * * 1 certbot renew --quiet --deploy-hook "docker exec nginx nginx -s reload"
   ```

---

## 3. 시크릿 등록 체크리스트

### 3-1. EC2 `/opt/skala-fund/.env`
`.env.example` 을 복사해 아래를 채운다. **`chmod 600 .env` 를 반드시 실행한다.**

- [ ] `ECR_REGISTRY` — `<계정ID>.dkr.ecr.<리전>.amazonaws.com`
- [ ] `BACKEND_REPOSITORY` / `FRONTEND_REPOSITORY`
- [ ] `BACKEND_IMAGE_TAG` / `FRONTEND_IMAGE_TAG` — 최초엔 `latest`, 이후 CD 가 커밋 SHA 로 갱신
- [ ] `DB_HOST` — RDS 엔드포인트
- [ ] `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`
- [ ] `JWT_SECRET` — `openssl rand -base64 48` 로 새로 만든다
- [ ] `AWS_REGION` / `AWS_S3_BUCKET`
- [ ] `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` — IAM 인스턴스 프로파일을 붙였다면 비워 둔다

> `JWT_SECRET` 은 `backend/src/main/resources/application.yml` 의 개발용 값을 그대로 쓰면 안 된다.
> 그 값은 저장소에 그대로 들어 있어 누구나 토큰을 위조할 수 있다.
> `application-prod.yml` 은 `jwt.secret: ${JWT_SECRET}` 에 기본값을 두지 않았다.
> 주입을 빠뜨리면 애플리케이션이 조용히 개발용 키로 뜨지 않고 **기동 자체가 실패한다.** 의도된 동작이다.

### 3-2. GitHub Actions Secrets
`Settings → Secrets and variables → Actions → New repository secret`

| Secret | 값 |
|---|---|
| `AWS_REGION` | 예: `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID` | CI 전용 IAM 사용자의 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | 위 사용자의 시크릿 키 |
| `ECR_REGISTRY` | `<계정ID>.dkr.ecr.<리전>.amazonaws.com` |
| `BACKEND_ECR_REPOSITORY` | `skala-fund-backend` |
| `FRONTEND_ECR_REPOSITORY` | `skala-fund-frontend` |
| `EC2_HOST` | Elastic IP 또는 도메인 |
| `EC2_USER` | `ec2-user` (Amazon Linux) 또는 `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 **전체 내용** (`-----BEGIN ...` 줄 포함) |
| `EC2_SSH_PORT` | 22 (기본값이면 생략 가능) |
| `EC2_DEPLOY_PATH` | `/opt/skala-fund` |

CI 용 IAM 사용자 정책은 ECR 푸시 권한만 준다 (`AmazonEC2ContainerRegistryPowerUser`).
`AdministratorAccess` 를 붙이지 않는다. 키가 유출되면 계정 전체가 넘어간다.

---

## 4. 최초 배포 (한 번만)

```bash
cd /opt/skala-fund

# 1) ECR 로그인
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

# 2) blue 슬롯과 nginx 만 띄운다. green 은 다음 배포 때 쓴다.
#    compose up 을 인자 없이 실행하면 blue/green 이 둘 다 떠 버린다.
docker compose up -d nginx app-blue

# 3) 활성 슬롯을 기록한다. 이 파일이 없으면 다음 배포가 blue 를 활성으로 가정한다.
echo blue > .deploy-state

# 4) 확인
curl -s http://localhost/api/actuator/health   # {"status":"UP"}
docker compose ps
```

첫 기동에서 Flyway 가 `V1__init_schema.sql` → `V2__insert_categories.sql` 을 순서대로 적용하고
`flyway_schema_history` 테이블을 만든다. 로그에서 확인한다:

```bash
docker logs app-blue | grep -i flyway
# Successfully applied 2 migrations to schema `skalafund`
```

---

## 5. 평상시 배포

**자동** — `main` 에 머지하면 끝난다.
- `backend/**` 변경 → `cd-backend.yml` → ECR 푸시 → SSH → `scripts/deploy.sh`
- `frontend/**` 변경 → `cd-frontend.yml` → ECR 푸시 → SSH → `scripts/deploy-frontend.sh`
- 둘 다 `concurrency: deploy-prod` 그룹이라 동시에 실행되지 않고 줄을 선다.

**수동**
```bash
cd /opt/skala-fund
./scripts/deploy.sh <이미지태그>            # 백엔드
./scripts/deploy-frontend.sh <이미지태그>   # 프론트
```

### 5-1. 백엔드 블루/그린 동작
1. `.deploy-state` 를 읽어 비활성 슬롯을 고른다 (blue ↔ green).
2. 새 이미지를 pull 하고 비활성 슬롯을 기동한다.
3. 컨테이너 **안에서** `curl localhost:8080/actuator/health` 를 최대 120초 폴링한다.
   nginx 를 거치지 않는 이유: 이 시점에 upstream 은 아직 기존 슬롯을 가리키고 있다.
4. 통과하면 `docker exec nginx sed -i` 로 upstream 줄을 바꾸고 `nginx -t` → `nginx -s reload`.
   reload 는 기존 워커가 처리 중인 요청을 마칠 때까지 기다리므로 커넥션이 끊기지 않는다.
5. `.deploy-state` 를 새 슬롯으로 갱신한 뒤 기존 슬롯을 중지·제거하고 `docker image prune -f`.

### 5-2. 프론트 배포에서 주의할 점
`nginx.conf` 는 프론트 **이미지 안에 구워져 있고**, 그 기본값은 `server app-blue:8080;` 이다.
활성 슬롯이 green 인 상태에서 nginx 컨테이너를 재생성하면 upstream 이 app-blue 로 되돌아가
죽은 컨테이너를 가리켜 **502** 가 난다.
`scripts/deploy-frontend.sh` 는 재생성 직후 `.deploy-state` 를 읽어 upstream 을 다시 맞춘다.
nginx 를 수동으로 재생성했다면 같은 작업을 직접 해야 한다:

```bash
ACTIVE=$(cat .deploy-state)
docker exec nginx sed -i "s#app-blue:8080#app-${ACTIVE}:8080#g; s#app-green:8080#app-${ACTIVE}:8080#g" \
  /etc/nginx/conf.d/default.conf
docker exec nginx nginx -s reload
```

---

## 6. 롤백

### 6-1. 배포 중 자동 롤백
`scripts/deploy.sh` 가 헬스체크에 실패하면 **신규 컨테이너만** 제거하고 기존 슬롯을 유지한다.
nginx upstream 은 손대지 않으므로 사용자 입장에서는 아무 일도 일어나지 않는다.
실패 원인을 보려면 스크립트 출력의 마지막 80줄 로그를 확인한다.

### 6-2. 배포 후 수동 롤백 (이전 버전으로 되돌리기)
```bash
cd /opt/skala-fund

# 되돌릴 태그 확인 (ECR 콘솔 또는 아래 명령)
aws ecr describe-images --repository-name skala-fund-backend \
  --query 'sort_by(imageDetails,&imagePushedAt)[-5:].[imageTags[0],imagePushedAt]' --output table

# 이전 태그로 다시 배포한다. 블루/그린이 그대로 동작하므로 이것이 곧 롤백이다.
./scripts/deploy.sh <이전_커밋SHA_태그>
```

### 6-3. DB 마이그레이션 롤백
Flyway 커뮤니티 버전에는 undo 가 없다. **되돌리는 마이그레이션(V3, V4 …)을 새로 추가한다.**
이미 적용된 `V1`/`V2` 파일을 수정하면 체크섬이 어긋나 다음 기동에서
`Migration checksum mismatch` 로 실패한다.

컬럼을 지우는 변경이라면 애플리케이션 롤백만으로는 복구되지 않는다.
스키마를 바꾸는 배포 전에는 RDS 스냅샷을 먼저 찍는다.

---

## 7. 모니터링 / 로깅

### 7-1. Actuator 노출 범위
`application-prod.yml`:
- `management.endpoints.web.exposure.include: health,info` — 이 둘만 연다.
  `env`, `beans`, `configprops`, `heapdump` 는 설정값과 내부 구조를 그대로 뱉는다.
- `management.endpoint.health.show-details: never` — 상태 문자열만 내려준다.
  `always` 면 DB 종류·검증 쿼리 같은 내부 정보가 인터넷에 노출된다.

외부 확인 경로: `https://<도메인>/api/actuator/health`
(백엔드 Actuator 는 `/actuator/health` 에 있고 `/api` 프리픽스가 없다. nginx 가 매핑해 준다.)

### 7-2. 컨테이너 로그
`json-file` 드라이버 + 로테이션이 compose 에 걸려 있다 (앱 20MB × 5, nginx 20MB × 3).
로테이션이 없으면 로그가 EC2 디스크를 채워 컨테이너가 아니라 **호스트가** 먼저 죽는다.

```bash
docker logs -f app-blue                    # 실시간
docker logs --since 1h app-blue            # 최근 1시간
docker compose ps                          # 어느 슬롯이 떠 있는지
cat /opt/skala-fund/.deploy-state          # 활성 슬롯
```

### 7-3. 정산 배치 로그 ★ 돈이 움직이는 지점

`SettlementScheduler` 는 매일 **00:10**, `ProjectStatusScheduler` 는 **00:05** 에 돈다.
두 로거는 `application-prod.yml` 에서 INFO 로 고정돼 있어 root 레벨을 올려도 기록이 남는다.

```bash
ACTIVE=$(cat /opt/skala-fund/.deploy-state)

# 오늘 정산 결과 (시작/종료 요약 한 쌍이 반드시 보여야 한다)
docker logs --since 24h "app-${ACTIVE}" | grep '\[배치\]'
```

정상이라면 이런 쌍이 나온다:
```
[배치] 정산 시작 - 대상 N건
[배치] 정산 종료 - 대상 N건, 성공 N건, 실패 0건
```

**확인 포인트**
- 시작 로그만 있고 종료 로그가 없다 → 배치가 도중에 죽었다. 정산이 일부만 반영됐을 수 있다.
- `실패` 가 0 이 아니다 → 바로 위 `[배치] 정산 실패 - projectId=...` 스택트레이스를 본다.
  프로젝트 단위로 트랜잭션이 분리돼 있어 실패한 건만 롤백된다. 나머지는 정상 반영된 상태다.
- 배치 로그가 아예 없다 → 배포 시각이 00:05~00:10 과 겹쳤을 가능성이 크다.
  블루/그린 전환 중에 기존 컨테이너가 내려가면 그날 배치가 실행되지 않는다.
  **자정 전후에는 배포하지 않는다.**

> 로그는 컨테이너에 붙어 있다. 배포로 컨테이너가 제거되면 그 로그도 사라진다.
> 정산 기록을 오래 보관해야 한다면 CloudWatch Logs 에이전트를 붙이거나
> 아래처럼 매일 호스트로 떠 두는 것을 권한다.
> ```bash
> # crontab -e  (매일 01:00)
> 0 1 * * * docker logs --since 24h "app-$(cat /opt/skala-fund/.deploy-state)" 2>&1 \
>           | grep '\[배치\]' >> /var/log/skala-settlement.log
> ```

### 7-4. 디스크 관리
배포마다 새 이미지가 쌓인다. 방치하면 EC2 루트 볼륨이 찬다.
배포 스크립트가 끝에 `docker image prune -f` 를 돌리지만, 주 단위로 한 번 더 청소한다.

```bash
# crontab -e
0 4 * * 0 docker image prune -af --filter "until=168h"
0 4 * * 0 docker builder prune -af

# 사용량 확인
df -h /
docker system df
```

---

## 8. 로컬 통합 환경 (INF-10)

```bash
# 백엔드 + MySQL 8
docker compose -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.local.yml logs -f backend

# 프론트는 Vite dev server 로 따로 (vite.config.js 의 proxy 가 /api 를 8080 으로 넘긴다)
cd frontend && npm run dev        # http://localhost:3000

# 정리 (-v 를 붙이면 DB 볼륨까지 지워져 Flyway 가 처음부터 다시 돈다)
docker compose -f docker-compose.local.yml down -v
```

프로파일은 `dev,local` 이다.
- `dev` : `LocalFileStorageService`, `/images/**` 매핑, 하네스 시뮬레이터 컨트롤러를 살린다.
- `local`: 뒤에 오므로 dev 의 H2 설정을 덮어쓰고 **MySQL 8 + Flyway + `ddl-auto: validate`** 로 만든다.

**이 환경의 목적은 prod 와 같은 조건에서 스키마를 검증하는 것이다.**
마이그레이션이 엔티티와 어긋나면 backend 컨테이너가 `Schema-validation` 오류로 죽는다.
그게 정상 동작이고, prod 첫 배포에서 터질 문제를 여기서 잡는 것이다.

스키마를 직접 들여다보려면 `localhost:3307` 로 붙는다 (`skala` / `skala`).

---

## 9. 엔티티를 바꿨을 때

`ddl-auto: validate` 라 스키마 변경은 **반드시** 마이그레이션을 동반해야 한다.

1. `backend/src/main/resources/db/migration/V3__<설명>.sql` 을 새로 만든다.
   **`V1`, `V2` 를 수정하지 않는다.** 체크섬이 어긋나 기동이 실패한다.
2. 로컬에서 검증한다:
   ```bash
   docker compose -f docker-compose.local.yml down -v      # 스키마 초기화
   docker compose -f docker-compose.local.yml up --build    # 기동에 성공하면 일치한다
   ```
3. 컬럼 추가는 `NOT NULL` 이면 기본값을 함께 준다. 기존 행이 있으면 그냥은 못 붙는다.
4. 블루/그린 특성상 **배포 중 잠깐 신·구 버전이 같은 DB 를 함께 본다.**
   컬럼 삭제·이름 변경은 두 번에 나눠 배포한다.
   (1차: 새 컬럼 추가 + 양쪽 쓰기 → 2차: 옛 컬럼 제거)

---

## 10. 자주 겪는 문제

| 증상 | 원인 | 조치 |
|---|---|---|
| 앱이 `Schema-validation` 으로 기동 실패 | 마이그레이션과 엔티티 불일치 | 9장 참조. 로컬에서 재현 후 V3 추가 |
| `/projects/3` 새로고침 시 404 | nginx SPA 폴백 누락 | `nginx.conf` 의 `try_files $uri $uri/ /index.html` 확인 |
| 이미지 업로드가 413 | `client_max_body_size` 기본값(1M) | `nginx.conf` 에 `10M` 이 있는지 확인 |
| 배포 후 502 | upstream 이 죽은 슬롯을 가리킴 | 5-2 절의 수동 복구 명령 실행 |
| nginx 가 기동 실패 (`host not found in upstream`) | upstream 이 가리키는 앱 컨테이너가 없음 | `docker compose up -d app-$(cat .deploy-state)` 먼저 |
| 카테고리명이 `???` | RDS 파라미터 그룹의 charset 미설정 | 2-3 절. DB 를 다시 만들어야 할 수도 있다 |
| CD 가 `denied: not authorized` | ECR 로그인 실패 / IAM 권한 부족 | GitHub Secrets 와 IAM 정책 확인 |
| CD SSH 단계에서 `permission denied` | `EC2_SSH_KEY` 가 잘림 | `-----BEGIN`/`-----END` 줄까지 전부 넣었는지 확인 |
