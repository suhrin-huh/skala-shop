# skala-shop

SKALA-FUND — 예약형 All-or-Nothing 크라우드펀딩 서비스. 백엔드는 Spring Boot(H2 인메모리, `dev` 프로파일 고정), 프론트는 React + Vite, 대표 이미지는 S3에 저장한다.

## 사전 준비

- Java 21
- Node.js 18 이상
- (선택) Docker Desktop — 백엔드를 컨테이너로 띄우려면 필요
- AWS S3 버킷 1개 + 아래 권한을 가진 IAM 자격증명
  - 버킷의 `images/*` 에 대한 `s3:PutObject`
  - `images/*` 에 대한 퍼블릭 `s3:GetObject` 버킷 정책 (업로드한 이미지를 브라우저가 URL로 직접 보기 때문)

## 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env`를 열어 값을 채운다.

| 변수 | 설명 |
|---|---|
| `JWT_SECRET` | HS256 서명 키. `openssl rand -base64 48`로 생성한다. 하이픈 등 Base64가 아닌 문자가 섞이면 기동 시점에 바로 죽는다. |
| `AWS_REGION` | S3 버킷 리전 (예: `us-east-1`) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | 위 IAM 자격증명 |
| `AWS_S3_BUCKET` | S3 버킷명 |

**중요**: `AWS_S3_BUCKET`은 기본값이 없다. 이 값이 실제 프로세스 환경변수로 채워지지 않은 채 백엔드를 띄우면 `S3FileStorageService` 빈을 만드는 시점에

```
Could not resolve placeholder 'AWS_S3_BUCKET' in value "${AWS_S3_BUCKET}"
```

에러로 즉시 죽는다. IDE의 "실행" 버튼처럼 `.env`를 자동으로 읽지 않는 방식으로 띄우면 그대로 재현되니, 아래 두 방법 중 하나로 **`.env`가 실제 환경변수로 주입된 상태**에서 실행한다.

## 2. 백엔드 실행

### 방법 A. Docker (권장)

`docker-compose.yml`이 `.env`를 `env_file`로 자동으로 읽는다.

```bash
docker compose up -d --build
docker compose logs -f backend   # 기동 로그 확인
docker compose down              # 종료. H2 인메모리라 별도 -v 없이도 데이터는 항상 초기화된다
```

### 방법 B. 로컬 Gradle

`.env`를 셸에 직접 소싱한 뒤 실행한다.

```bash
# bash / git bash
cd backend
set -a; source ../.env; set +a
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

```powershell
# PowerShell
cd backend
Get-Content ..\.env | Where-Object { $_ -match '=' -and $_ -notmatch '^\s*#' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item "Env:$name" $value
}
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\gradlew.bat bootRun
```

IDE(IntelliJ 등)에서 실행하려면 Run Configuration의 환경변수 목록에 `.env`의 5개 값을 직접 등록해야 한다.

두 방법 다 `http://localhost:8080`에서 뜬다. `curl http://localhost:8080/actuator/health`로 기동을 확인할 수 있다.

## 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

`http://localhost:3000`. `vite.config.js`의 dev 프록시가 `/api`, `/images` 요청을 `http://localhost:8080`으로 넘기므로 백엔드가 먼저 떠 있어야 한다.

## 4. 확인

- DB는 H2 인메모리(`dev` 프로파일)라 **백엔드를 재시작할 때마다 스키마와 데이터가 초기화**되고, 그때마다 카테고리·계정·프로젝트 목업 데이터가 자동으로 채워진다(`MockDataInitializer` → `HarnessSeedService`).
  - 로그인 가능한 목업 계정: `creator@skala.com` / `test@skala.com`, 공통 비밀번호 `skala123!`
- H2 콘솔: `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:skalafund`, 사용자 `sa`, 비밀번호 없음
- API 문서(Swagger): `http://localhost:8080/swagger-ui/index.html`
