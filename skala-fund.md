# SKALA-FUND 작업 정의서

## 0. 프로젝트 컨텍스트

| 항목 | 내용 |
|---|---|
| 서비스명 | SKALA-FUND (마이크로 크라우드 펀딩 플랫폼) |
| 펀딩 모델 | **예약형 All-or-Nothing** — 마감 시점에만 결제, 목표 미달 시 결제 자체가 발생하지 않음 |
| 백엔드 | **Java 21 (LTS)**, Spring Boot 3.x, Spring Data JPA, Gradle, JWT |
| DB | H2 (dev, in-memory) / **AWS RDS MySQL 8.x** (prod) |
| 프론트엔드 | React(Vite), Pure CSS, react-icons, Axios |
| 배포 구조 | **EC2 1대 + Docker Compose (Nginx 1 + Spring Boot 2 컨테이너, 블루/그린)** |
| 서빙 방식 | **Nginx가 React 정적 파일 서빙 + `/api` 리버스 프록시 → 동일 오리진** |

### 확정된 정책 요약 (AI는 이 값을 임의로 바꾸지 말 것)

| 항목 | 확정 내용 |
|---|---|
| 패키지 구조 | **계층형** (`controller / service / repository / domain / dto / aop / batch / config / common`) |
| 카테고리 | **별도 테이블(`Category` 엔티티)** |
| Refresh Token | **DB 저장** (`RefreshToken` 엔티티) |
| 모금액 집계 | **매 조회 시 SUM 쿼리**. 개선 지점에 `// TODO` 주석 필수 |
| 인기순 정렬 | **후원액 합계 내림차순 상위 5개** |
| 동시성 제어 | **정합성 최우선** → 비관적 락(`PESSIMISTIC_WRITE`) |
| 초기 포인트 | **회원가입 시 1,000,000 포인트 자동 지급** |
| 포인트 차감 시점 | **마감 시 일괄 차감** (후원 시점에는 차감하지 않음) |
| 이미지 저장 | dev: 로컬 파일시스템 + 정적 리소스 매핑 / prod: S3 |
| 관리자 권한 | **이번 범위 제외** (Role 없음) |
| 프로젝트 수정 | **창작자 본인이면 후원 유무와 무관하게 가능** |
| 프로젝트 삭제 | **Soft Delete** (`deletedAt` 컬럼, 물리 삭제 금지) |
| CORS | **불필요** (동일 오리진). 로컬 개발 환경에서만 Vite proxy로 우회 |

---

## 1. 핵심 도메인 규칙 (가장 먼저 읽을 것)

### 1-1. 펀딩 생명주기

```
[SCHEDULED 예정] --startDate 도래--> [ONGOING 진행중] --endDate 경과--> 정산 배치
                                                                         │
                                        모금액 >= targetAmount ──────────┤
                                                                         ├─> [SUCCESS]
                                        모금액 <  targetAmount ──────────┘     후원자 차감 + 창작자 지급
                                                                         └─> [FAILED]
                                                                               아무 정산도 하지 않음
```

### 1-2. 포인트 흐름

**핵심 원칙: 후원해도 포인트는 줄지 않는다. 마감일에 성공한 프로젝트만 결제된다.**

| 시점 | 후원자 `point` | 창작자 `point` | `Pledge.status` |
|---|---|---|---|
| 후원하기 | 변화 없음 | - | `PLEDGED` |
| 후원 취소 (마감 전) | 변화 없음 | - | `CANCELLED` |
| 마감 후 **성공** | **차감** | **모금액만큼 증가** | `CONFIRMED` |
| 마감 후 **실패** | 변화 없음 | 변화 없음 | `FAILED` |

> 환급(REFUND) 개념이 존재하지 않습니다. 애초에 차감된 적이 없기 때문입니다.
> AI가 환급 로직을 만들려 하면 잘못된 것입니다.

### 1-3. 사용 가능 포인트 (Available Point)

포인트가 후원 시점에 줄지 않으므로, **잔액을 초과해 여러 프로젝트에 후원하는 문제**가 발생합니다.
100만 포인트 보유자가 50만원짜리 후원을 10건 하면 마감일에 500만원이 필요해집니다.

```
사용 가능 포인트 = Customer.point − SUM(내 Pledge 중 status = PLEDGED 인 amount)
```

- **후원 시 검증은 `point`가 아니라 이 값으로 합니다.**
- DB 컬럼이 아니라 조회 시 계산합니다.
- 이 규칙이 지켜지면 마감 시점에 잔액 부족이 발생할 수 없습니다 (포인트가 줄어드는 유일한 경로가 정산 배치이므로).
- 그래도 정산 배치에는 **잔액 부족 방어 로직**을 두고, 발생 시 ERROR 로그를 남깁니다.

```java
// TODO(성능): 사용 가능 포인트를 매번 PLEDGED 후원액 SUM으로 계산한다.
//  후원 건수가 늘면 마이페이지/후원 API마다 집계 비용이 발생하므로,
//  Customer에 reservedPoint 컬럼을 두고 후원/취소 시 증감시키는 방식으로 개선 필요.
```

### 1-4. 상태 Enum 정의

**`ProjectStatus`**

| 값 | 의미 |
|---|---|
| `SCHEDULED` | 등록 완료, 시작일 전 (후원 불가) |
| `ONGOING` | 진행 중 (후원 가능) |
| `SUCCESS` | 마감 + 목표 달성 (결제 완료) |
| `FAILED` | 마감 + 목표 미달 (결제 없음) |

**`PledgeStatus`**

| 값 | 의미 | 전이 시점 | 포인트 영향 |
|---|---|---|---|
| `PLEDGED` | 후원 예약 (결제 전) | 후원하기 | 없음 (사용 가능 포인트에서만 차감) |
| `CANCELLED` | 사용자 직접 취소 / 프로젝트 삭제 | 마감 전 | 없음 |
| `CONFIRMED` | 펀딩 성공, 결제 완료 | 정산 배치 | **후원자 차감** |
| `FAILED` | 펀딩 무산, 결제 없음 | 정산 배치 | 없음 |

**`DeliveryStatus`** — `CONFIRMED` 상태에만 값이 존재

| 값 | 의미 |
|---|---|
| `ORDER_COMPLETED` | 주문 완료 (정산 배치가 부여하는 초기값) |
| `SHIPPING` | 배송중 |
| `DELIVERED` | 배송완료 |

> 별도 `Reward` 엔티티 없이 이 상태로 리워드 이행을 판단합니다.

### 1-5. Soft Delete 규칙 ★ v4 신규

`Project`는 물리 삭제하지 않습니다. `deletedAt` (nullable, `LocalDateTime`) 컬럼으로 삭제를 표현합니다.

**삭제 시 처리 (하나의 `@Transactional`)**
```
1. 요청자 == project.creator 검증 (아니면 403)
2. 해당 프로젝트의 PLEDGED 후원을 전부 CANCELLED 로 변경
   (포인트 차감이 없었으므로 환급 처리는 불필요)
3. project.deletedAt = now()
```

**조회 시 처리 — 여기가 함정입니다**

- Hibernate의 `@SQLRestriction("deleted_at is null")` 을 엔티티에 붙이면 **모든 조회에 전역 적용**됩니다. 편해 보이지만, **마이페이지 후원 내역에서 삭제된 프로젝트가 통째로 안 보이게 되는** 문제가 생깁니다. 후원자 입장에선 자기 후원 기록이 사라진 것처럼 보이죠.
- 따라서 **`@SQLRestriction` 을 쓰지 말고, Repository 쿼리마다 조건을 명시**합니다.

| 화면 | 삭제된 프로젝트 노출 |
|---|---|
| 프로젝트 목록 / 검색 / 인기 | ❌ `deletedAt IS NULL` 필터 |
| 프로젝트 상세 | ❌ 404 처리 |
| 찜 목록 | ❌ 필터 |
| 최근 본 항목 (`?ids=`) | ❌ 조용히 제외 (404로 전체 실패시키지 말 것) |
| **마이페이지 후원 내역** | ✅ **노출하되 "삭제된 프로젝트" 배지 표시**, 상세 링크는 비활성 |
| 정산 배치 대상 | ❌ 제외 |

---

## 2. 백엔드 (BE)

### Phase 1 — 프로젝트 기반

**BE-01. 프로젝트 초기 세팅**
- Gradle + Spring Boot 3.x (**Java 21**)
- 의존성: `web`, `data-jpa`, `validation`, `security`, `actuator`, `lombok`, `h2`, `mysql-connector-j`, `jjwt`, `flyway-core`, `flyway-mysql`, `aws-sdk-s3`
- **계층형 패키지 구조 확정**
  ```
  com.skala.fund
  ├── controller
  ├── service
  ├── repository
  ├── domain              # 엔티티, Enum
  ├── dto                 # request / response (record)
  ├── aop                 # LoggingAspect, ExecutionTimeAspect        → BE-19
  ├── batch               # ProjectStatusScheduler, SettlementScheduler → BE-16, BE-17
  ├── config              # Security, Web, Jpa, Scheduler, Storage 설정
  └── common
      ├── response        # ApiResponse 공통 포맷
      ├── exception       # 커스텀 예외, GlobalExceptionHandler, ErrorCode
      └── util
  ```
- **AOP와 배치는 최상위로 분리합니다.** AOP는 특정 계층에 속하지 않는 횡단 관심사(cross-cutting concern)이고, 배치는 HTTP 요청 없이 도는 별도 진입점이라 `common`에 묻어두면 나중에 찾기 어려워집니다
- `config`에는 **스케줄러를 켜는 설정**(`@EnableScheduling`)만 두고, **실제 배치 로직**은 `batch`에 둡니다. 둘을 섞으면 배치가 늘어날 때 `config`가 잡동사니 서랍이 됩니다

**BE-02. 프로파일 분리 설정**
- `application.yml` / `application-dev.yml` / `application-prod.yml`
- dev: H2 in-memory, `ddl-auto: create-drop`, H2 Console, Actuator `*`, SQL 로그 on, 이미지 = 로컬 폴더
- prod: RDS MySQL, `ddl-auto: validate`, Actuator `health,info`, 이미지 = S3
- 민감정보는 환경변수 주입(`${DB_PASSWORD}`), 하드코딩 금지

**BE-03. 공통 응답·예외 처리**
- 응답 포맷: `{ success, data, error: { code, message } }`
- `@RestControllerAdvice` + 에러 코드 Enum
- 도메인 예외: `InsufficientAvailablePointException`, `ProjectNotOngoingException`, `NotProjectOwnerException`, `PledgeNotCancellableException`, `ProjectDeletedException`

### Phase 2 — 도메인 모델

**BE-04. 엔티티 및 연관관계 매핑**

| 엔티티 | 필드 | 제약 |
|---|---|---|
| `Customer` | id(PK), email, nickname, password, point | email 유니크·정규식, nickname 2~10자, password 8자 이상+특수문자(BCrypt), point ≥ 0, **가입 시 1,000,000** |
| `Category` | id(PK), name, displayOrder | name 유니크 |
| `Project` | id(PK), creator(FK), category(FK), title, description, mainImage, targetAmount, startDate, endDate, status, **deletedAt** | title 5~50자, description 20자 이상, mainImage URL, targetAmount ≥ 100,000, **endDate ≥ startDate + 7일**, deletedAt nullable |
| `Pledge` | id(PK), customer(FK), project(FK), amount, status, deliveryStatus | amount ≥ 1,000 |
| `ProjectLike` | id(PK), customer(FK), project(FK) | (customer, project) 복합 유니크 |
| `RefreshToken` | id(PK), customer(FK), token, expiresAt | token 유니크 |

- 공통 `BaseTimeEntity`(createdAt, updatedAt) + `@EnableJpaAuditing`
- 단방향 `@ManyToOne(fetch = LAZY)` 기본
- 엔티티에 `@Setter` 금지 → `confirm()`, `cancel()`, `markFailed()`, `softDelete()` 등 의미 있는 메서드로만 상태 변경
- **`@SQLRestriction` / `@Where` 사용 금지** (1-5 참고)

**BE-05. 카테고리 초기 데이터**
- dev: `data.sql` / prod: Flyway 마이그레이션
- 20개: 디자인 문구, 푸드, 출판, 영화·비디오, 보드게임·TRPG, 캐릭터·굿즈, 향수·뷰티, 디자인·일러스트, 공연, 홈·리빙, 의류, 문화·예술, 웹툰·만화, 테크·가전, 잡화, 사진, **웹툰 리소스**, 반려동물, 주얼리, 음악
- ※ `웹툰·만화`는 완성된 작품, `웹툰 리소스`는 웹툰 제작용 배경·소재 파일. **별개 카테고리이므로 통합 금지**
- `GET /api/categories` 제공

**BE-06. Repository 계층**
- 엔티티별 `JpaRepository`, 목록은 `Pageable`
- 조회 전용에 `@Transactional(readOnly = true)`
- **모든 프로젝트 조회 쿼리에 `deletedAt IS NULL` 조건 명시** (예외: 마이페이지 후원 내역)
- 필수 집계 쿼리 2종
  - 프로젝트별 모금액: `SUM(amount) WHERE project_id IN (...) AND status IN (PLEDGED, CONFIRMED) GROUP BY project_id`
  - 회원별 예약액: `SUM(amount) WHERE customer_id = ? AND status = PLEDGED`

### Phase 3 — 인증/인가

**BE-07. Spring Security + JWT 설정**
- `SecurityFilterChain`, 세션 `STATELESS`, CSRF 비활성화
- `JwtAuthenticationFilter` → `SecurityContext` 등록
- `PasswordEncoder`(BCrypt)
- **CORS 설정 불필요** — Nginx가 프론트와 API를 같은 오리진에서 서빙합니다
  - 단, **로컬 개발 시에는 Vite dev server(5173)와 백엔드(8080)가 다른 포트**이므로 프론트의 `vite.config.js`에 proxy를 설정해 우회합니다 (FE-01 참고). 백엔드에 CORS를 여는 것보다 이 방식이 배포 환경과 동일해서 안전합니다
- **쿠키 설정: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/auth`**
  - 동일 오리진이므로 `SameSite=None`이 필요 없습니다. Lax가 CSRF 방어에 더 유리합니다
- Role 없음 — 인증 여부만 판단, 소유자 검증은 서비스 계층에서

**BE-08. 인증 API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입. 중복 검사, 암호화, **포인트 1,000,000 지급** |
| POST | `/api/auth/login` | Access Token(3일) 본문, Refresh Token(2주) 쿠키, **DB 저장** |
| POST | `/api/auth/refresh` | 쿠키 토큰을 **DB와 대조 검증** 후 재발급 |
| POST | `/api/auth/logout` | DB Refresh Token 삭제 + 쿠키 만료 |

- (권장) refresh 시 토큰 회전(Rotation)
- 만료 토큰 정리 배치 또는 조회 시 만료 검사

### Phase 4 — 핵심 비즈니스 로직

**BE-09. 프로젝트 API**

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/projects` | - | 목록. 카테고리 + 키워드 검색 + 정렬 + 페이징 |
| GET | `/api/projects/popular` | - | **인기 5개** (후원액 합계 내림차순) |
| GET | `/api/projects/{id}` | - | 상세 (모금액, 달성률, 후원자 수) |
| POST | `/api/projects` | 필요 | 등록 (이미지 업로드 포함) |
| PUT | `/api/projects/{id}` | 필요 | **수정 — 창작자 본인만** |
| DELETE | `/api/projects/{id}` | 필요 | **Soft Delete — 창작자 본인만** |

**수정 규칙 (후원 유무와 무관하게 전면 허용)**
- 요청자 ID ≠ `project.creator.id` → 403
- `SUCCESS`/`FAILED` 상태(이미 마감)는 수정 불가
- `deletedAt != null` 이면 수정 불가
- 목표 금액·마감일 변경은 이미 후원한 사람에게 영향이 가므로 **변경 이력 로그를 남길 것**

**삭제 규칙 (Soft Delete)** — 상세 처리는 1-5 참고
- 물리 삭제(`DELETE FROM project`) 금지
- 삭제 시 PLEDGED 후원을 CANCELLED로 정리
- 이미 삭제된 프로젝트에 대한 재삭제 요청은 멱등 처리 또는 404

**BE-10. 모금액 집계**
- 현재 모금액 = `PLEDGED` + `CONFIRMED` 상태 `Pledge.amount` 합계 (`CANCELLED`, `FAILED` 제외)
- 목록 조회 시 프로젝트마다 개별 SUM을 돌리면 N+1이므로, **ID 목록으로 한 번에 GROUP BY 집계** 후 Map 매핑
- 코드 주석 필수
  ```java
  // TODO(성능): 조회할 때마다 Pledge를 SUM으로 집계한다.
  //  후원 데이터가 늘면 목록 조회 비용이 선형 증가하므로,
  //  Project에 currentAmount 비정규화 컬럼을 두고 후원/취소 시 갱신하도록 개선 필요.
  ```

**BE-11. 검색 기능**
- 규칙: **키워드 공백 제거** + **제목 공백 제거** 후 포함(LIKE) 검사
  ```sql
  SELECT * FROM project
  WHERE deleted_at IS NULL
    AND REPLACE(title, ' ', '') LIKE CONCAT('%', :keyword, '%')
  ```
- `:keyword`는 애플리케이션에서 이미 공백 제거된 값
- 대소문자 무시 (MySQL 기본 collation)
- 주석 필수
  ```java
  // TODO(성능): WHERE 절의 REPLACE() 때문에 인덱스를 타지 못한다(Full Scan).
  //  공백 제거본을 searchTitle 컬럼으로 저장해 인덱스를 걸거나 Full-Text Search 도입 검토.
  ```

**BE-12. 후원(Pledge) API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/pledges` | 후원하기 |
| POST | `/api/pledges/{id}/cancel` | 후원 취소 |

**후원하기** — 하나의 `@Transactional`

1. 프로젝트 조회 후 **`deletedAt IS NULL`** && `status == ONGOING` 검증
2. 마감일 경과 여부 재검증
3. 후원 금액 ≥ 1,000원 검증
4. **`Customer`를 비관적 락으로 조회** (`@Lock(PESSIMISTIC_WRITE)`)
   → 동시 요청을 직렬화시켜 사용 가능 포인트 계산의 정합성을 보장
5. **사용 가능 포인트 계산** = `customer.point − SUM(내 PLEDGED 후원액)`
6. 사용 가능 포인트 ≥ 후원 금액 검증 (부족 시 `InsufficientAvailablePointException`)
7. `Pledge` 생성 (`status = PLEDGED`, `deliveryStatus = null`)
   → **`customer.point`는 변경하지 않음**

**후원 취소**

1. 본인 후원인지 검증 (아니면 403)
2. `status == PLEDGED` 검증
3. 프로젝트 마감일 이전인지 검증
4. `status = CANCELLED`
   → **포인트 조작 없음.** 사용 가능 포인트가 자동으로 회복됨

동시성 주의사항
- 락 대상은 **`Customer` 행**입니다. 여러 요청이 같은 회원의 사용 가능 포인트를 동시에 읽고 초과 후원하는 것을 막는 게 목적입니다
- 데드락 방지를 위해 여러 행을 잠글 때는 **항상 id 오름차순**으로
- 락 타임아웃(`jakarta.persistence.lock.timeout`) 설정 권장
- **완료 조건:** 동일 계정으로 동시에 후원 요청 시 `SUM(PLEDGED) ≤ point` 가 항상 유지됨을 테스트로 검증

**BE-13. 찜(ProjectLike) API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/projects/{id}/like` | 찜 등록 |
| DELETE | `/api/projects/{id}/like` | 찜 해제 |
| GET | `/api/users/me/likes` | 내 찜 목록 (삭제된 프로젝트 제외, 페이징) |

**BE-14. 마이페이지 API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/users/me` | 프로필 + **보유 포인트 + 예약액 + 사용 가능 포인트** |
| GET | `/api/users/me/pledges` | 내 후원 내역. **삭제된 프로젝트도 포함**하고 응답에 `projectDeleted: true` 플래그 제공 |
| GET | `/api/users/me/projects` | 내가 등록한 프로젝트 (삭제분 제외) |
| GET | `/api/projects?ids=1,2,3` | 최근 본 펀딩용 일괄 조회 (없거나 삭제된 ID는 무시) |

**BE-15. 이미지 업로드**
- `FileStorageService` 인터페이스로 추상화, 프로파일별 구현체 분리
  ```java
  public interface FileStorageService {
      String store(MultipartFile file);  // 접근 가능한 URL 반환
  }
  ```
  - `LocalFileStorageService` (`@Profile("dev")`) — `./uploads` 에 UUID 파일명 저장
  - `S3FileStorageService` (`@Profile("prod")`) — S3 업로드 후 객체 URL 반환
- dev 정적 리소스 매핑
  ```java
  @Configuration
  public class WebConfig implements WebMvcConfigurer {
      @Override
      public void addResourceHandlers(ResourceHandlerRegistry registry) {
          // /images/** 요청을 로컬 uploads 폴더로 연결한다.
          registry.addResourceHandler("/images/**")
                  .addResourceLocations("file:./uploads/");
      }
  }
  ```
- 확장자 화이트리스트(jpg/png/webp), 최대 크기 제한, 파일명 UUID 변환 (경로 조작·중복 방지)

### Phase 5 — 배치

**BE-16. 프로젝트 상태 전이 배치**
- `@EnableScheduling` + `@Scheduled` (매일 00:05)
- `SCHEDULED` && `deletedAt IS NULL` 중 `startDate <= 오늘` → `ONGOING`

**BE-17. 정산 배치**
- `@Scheduled` (매일 00:10)
- 대상: `status == ONGOING` && `endDate < 오늘` && **`deletedAt IS NULL`**
- **프로젝트 단위로 트랜잭션 분리** (한 건 실패가 전체를 롤백하지 않도록)

```
프로젝트별로:
  모금액 = SUM(PLEDGED 상태 Pledge.amount)

  IF 모금액 >= targetAmount:
      project.status = SUCCESS
      각 Pledge 에 대해 (customer id 오름차순, 비관적 락):
          IF customer.point >= pledge.amount:
              customer.point -= pledge.amount     // 여기서 최초로 결제 발생
              pledge.status = CONFIRMED
              pledge.deliveryStatus = ORDER_COMPLETED
          ELSE:
              // 사용 가능 포인트 검증이 정상 동작했다면 도달할 수 없는 분기
              pledge.status = FAILED
              log.error("정산 잔액 부족 - pledgeId={}, customerId={}", ...)
      creator.point += 실제 결제된 금액 합계

  ELSE:
      project.status = FAILED
      각 Pledge: status = FAILED      // 포인트 조작 없음
```

- 정산 후 **포인트 총량 보존** 검증: 후원자 차감 합계 == 창작자 증가액
- 재실행 시 중복 정산 방지 → `status == ONGOING` 조건이 멱등성 장치
- 실행 결과(대상 건수, 성공/실패, 총 정산액) 로깅 필수
- **완료 조건:** 성공/실패 시나리오 각각에 대해 포인트 총량이 보존됨을 테스트로 검증

**BE-18. 배송 상태 변경 API**

| 메서드 | 경로 | 설명 |
|---|---|---|
| PATCH | `/api/pledges/{id}/delivery` | 배송 상태 변경 — **해당 프로젝트 창작자만** |

- `ORDER_COMPLETED → SHIPPING → DELIVERED` 순서 역행 불가
- `CONFIRMED` 상태 후원에만 허용

### Phase 6 — 공통 관심사 & 품질

**BE-19. AOP 적용**
- `@Aspect` + `@within(RestController)` 포인트컷
- Controller 실행 시간 측정 + 요청/응답 로깅
- **비밀번호·토큰 마스킹 필수**
- 완료 조건: `[POST /api/pledges] 실행시간: 42ms` 형태 로그

**BE-20. 테스트 코드**
- Service 단위 테스트(Mockito) — 후원 검증, 취소, 정산 배치, soft delete
- **동시성 테스트** — `ExecutorService`로 동시 후원 요청 후 `SUM(PLEDGED) ≤ point` 확인
- Controller 통합 테스트(`@SpringBootTest` + `MockMvc`)
- 인증 흐름 통합 테스트

**BE-21. API 문서화**
- springdoc-openapi (Swagger UI) — 프론트 병렬 개발을 위해 사실상 필수

---

## 3. 프론트엔드 (FE)

**FE-01. 프로젝트 세팅 ★ v4 변경**
- Vite + React, `react-router-dom`, `axios`, `react-icons`
- **Pure CSS** — 프레임워크 미사용. 컴포넌트별 `.css` 또는 CSS Modules
- **API 베이스 URL은 상대 경로 `/api`** — 배포 시 Nginx가 같은 오리진에서 프록시하므로 절대 URL이 필요 없습니다
- **로컬 개발용 Vite proxy 설정** (배포 환경과 동일한 경로 구조를 유지하기 위함)
  ```js
  // vite.config.js
  export default defineConfig({
    server: {
      proxy: {
        // 개발 서버에서 /api 요청을 로컬 백엔드로 넘긴다.
        // 이렇게 하면 프론트 코드가 배포 환경과 똑같이 '/api'만 호출하면 된다.
        '/api': { target: 'http://localhost:8080', changeOrigin: true },
        '/images': { target: 'http://localhost:8080', changeOrigin: true },
      },
    },
  });
  ```
- 디렉터리: `pages / components / api / hooks / contexts / utils / styles`

**FE-02. 인증 상태 및 토큰 관리 (최대 난이도)**
- Access Token은 **React 메모리(Context)** 에만 보관. LocalStorage 금지
- Refresh Token은 Http-Only 쿠키 → JS 접근 안 함
- axios 인스턴스: `baseURL: '/api'`, `withCredentials: true`
- **Request Interceptor:** 메모리 토큰을 `Authorization: Bearer` 헤더에 주입
- **Response Interceptor:** 401 → `/api/auth/refresh` → 성공 시 **원 요청 재시도**, 실패 시 로그인 이동
- **동시 401 대응:** 진행 중인 refresh Promise를 공유하는 **대기 큐** 필수
- **새로고침 복원:** 앱 마운트 시 refresh 1회 호출

**FE-03. 공통 레이아웃 및 라우팅**
- Header(로고, **검색 입력**, 로그인/마이페이지), Footer
- `PrivateRoute` — `/projects/new`, `/projects/:id/edit`, `/mypage` 차단
- 404 페이지 (삭제된 프로젝트 접근 시에도 사용)

**FE-04. 메인 페이지 `/`**
- **`home-sample.html` 참고**하여 레이아웃 구성
- **인기 프로젝트 5개 섹션** (`GET /api/projects/popular`)
- 카테고리 바로가기

**FE-05. 인증 페이지 `/login`, `/signup`**
- 클라이언트 검증을 서버 규칙과 동일하게 (이메일 정규식 / 닉네임 2~10자 / 비밀번호 8자+특수문자)
- **회원가입 성공 시 "100만 포인트 지급" 안내 모달** — 아이콘 + 짧은 문구 + 확인 버튼의 가벼운 톤

**FE-06. 프로젝트 목록 `/projects`**
- 카테고리 필터(`GET /api/categories`), 정렬 선택, 페이징
- **키워드 검색** (프론트에서도 trim)
- 카드: 썸네일, 제목, 달성률 프로그레스 바, 남은 기간, **상태 배지**
- 필터·검색어·페이지를 URL 쿼리스트링과 동기화

**FE-07. 프로젝트 등록 `/projects/new`**
- 폼: 제목, 설명, 카테고리, 목표 금액, **시작일**, 마감일, 대표 이미지
- **이미지 파일 업로드**(`multipart/form-data`) + 미리보기
- 시작일은 오늘 이전 선택 불가, 마감일은 시작일 +7일 이전 선택 불가

**FE-08. 프로젝트 상세 `/projects/:id`**
- 정보 + 달성률 + 남은 기간 + 후원자 수 + 상태 배지
- **후원 모달**
  - "보유 포인트"와 **"사용 가능 포인트"를 나란히 표시**, 검증은 사용 가능 포인트 기준
  - **"지금 결제되지 않으며, 펀딩 성공 시 마감일에 결제됩니다"** 안내 문구 필수
- 후원 버튼은 `status == ONGOING` 일 때만 활성화
- 찜 버튼 (낙관적 업데이트)
- **창작자 본인이면 수정/삭제 버튼 노출**, 삭제는 확인 모달 (후원자가 있으면 인원수 명시)
- 진입 시 LocalStorage "최근 본 펀딩" 기록

**FE-09. 프로젝트 수정 `/projects/:id/edit`**
- 등록 폼 재사용, 기존 값 프리필
- 창작자 본인이 아니면 접근 차단
- 후원자가 있는 프로젝트 수정 시 경고 문구 노출

**FE-10. 마이페이지 `/mypage`**
- 탭: 후원 내역 / 찜 목록 / 최근 본 항목 / 내가 등록한 프로젝트
- **포인트 영역** — `보유 1,000,000 P / 예약 600,000 P / 사용 가능 400,000 P` 3개 값 표시
- **후원 내역 상태 표기**
  - `PLEDGED` → "결제 예약됨 (마감일 결제 예정)" + 취소 버튼
  - `CANCELLED` → "취소됨"
  - `CONFIRMED` → "결제 완료" + 배송 상태
  - `FAILED` → "펀딩 무산 (미결제)"
  - **`projectDeleted: true` → "삭제된 프로젝트" 배지 + 상세 링크 비활성화**
- **내가 등록한 프로젝트 탭에서 후원자별 배송 상태 변경** (BE-18)

**FE-11. 최근 본 펀딩 (LocalStorage)**
- 커스텀 훅 `useRecentlyViewed()`
- 큐 방식, **최대 10개** FIFO. 중복 진입 시 제거 후 맨 앞 재삽입
- **ID만 저장** → `GET /api/projects?ids=...` 로 최신 정보 재조회
- **삭제된 프로젝트 ID는 조용히 걸러낼 것** (soft delete가 있으므로 실제로 발생합니다)

**FE-12. 스켈레톤 UI**
- 스피너 대신 **실제 레이아웃과 동일한 스켈레톤** 노출
- 적용: 프로젝트 목록 카드, 상세, 마이페이지 각 탭
- 레이아웃 시프트(CLS) 없도록 실제 컴포넌트와 크기 일치
- 공통 `<Skeleton />` + CSS shimmer 애니메이션

**FE-13. 에러 및 빈 상태 처리**
- API 실패 시 토스트 안내
- Empty State (검색 결과 없음, 삭제된 프로젝트 접근)

---

## 4. 인프라 (INF) ★ v4 전면 개편

> **EC2 1대에 모든 것이 올라갑니다.** Nginx가 React 정적 파일을 서빙하면서 `/api` 요청만
> Spring Boot 컨테이너로 넘깁니다. 프론트와 API가 같은 오리진이므로 CORS가 필요 없습니다.

```
                    ┌──────────────── EC2 1대 ────────────────┐
                    │                                          │
사용자 ──443──►  │  Nginx 컨테이너                          │
                    │   ├─ /        → React 정적 파일           │
                    │   ├─ /api/    → proxy_pass backend        │
                    │   └─ upstream backend → blue 또는 green   │
                    │                                          │
                    │  app-blue (8081)    app-green (8082)      │
                    └────────────┬─────────────────────────────┘
                                 │
                          RDS MySQL (프라이빗 서브넷)
```

**INF-01. 백엔드 Dockerfile**
- 멀티 스테이지 빌드 (build: `gradlew build` → run: JRE 슬림)
- `SPRING_PROFILES_ACTIVE=prod` 환경변수 주입
- `/actuator/health` 를 컨테이너 헬스체크로 등록

**INF-02. 프론트엔드 Dockerfile + Nginx 설정**
- 멀티 스테이지 빌드
  ```dockerfile
  # 1단계: React 빌드
  FROM node:20-alpine AS build
  WORKDIR /app
  COPY package*.json ./
  RUN npm ci
  COPY . .
  RUN npm run build          # → /app/dist 생성

  # 2단계: Nginx가 빌드 결과물을 서빙
  FROM nginx:alpine
  COPY --from=build /app/dist /usr/share/nginx/html
  COPY nginx.conf /etc/nginx/conf.d/default.conf
  ```
- `nginx.conf` 핵심 구성
  ```nginx
  upstream backend {
      server app-blue:8080;    # 배포 스크립트가 이 줄을 blue/green으로 교체
  }

  server {
      listen 80;
      client_max_body_size 10M;          # 이미지 업로드 대비

      location /api/ {
          proxy_pass http://backend;
          proxy_set_header Host              $host;
          proxy_set_header X-Real-IP         $remote_addr;
          proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }

      location / {
          root /usr/share/nginx/html;
          # SPA 라우팅: 존재하지 않는 경로는 index.html로 넘겨 React Router가 처리하게 한다.
          # 이 설정이 없으면 /projects/3 새로고침 시 404가 뜬다.
          try_files $uri $uri/ /index.html;
      }
  }
  ```
- HTTPS는 Nginx에서 종단 처리 (Let's Encrypt + certbot)

**INF-03. Docker Compose 구성**
```yaml
services:
  nginx:        # 80, 443 개방. 프론트 정적 파일 + 리버스 프록시
  app-blue:     # 8080 (내부), 활성 컨테이너
  app-green:    # 8080 (내부), 대기 컨테이너
```
- 세 컨테이너를 같은 Docker 네트워크에 두어 서비스명으로 통신
- 앱 컨테이너 포트는 호스트에 노출하지 않음 (Nginx만 외부 개방)

**INF-04. 블루/그린 배포 스크립트**
```
1. 비활성 컨테이너(예: green)를 새 이미지로 기동
2. /actuator/health 폴링하여 기동 완료 대기 (최대 N초)
3. 헬스체크 통과 → nginx.conf 의 upstream 을 green 으로 교체
4. docker exec nginx nginx -s reload   (커넥션 끊김 없이 설정 적용)
5. 기존 blue 컨테이너 중지·제거
6. 헬스체크 실패 시 → green 만 제거하고 blue 유지 (롤백)
```
- **프론트엔드는 블루/그린이 필요 없습니다.** 정적 파일이므로 Nginx 이미지를 새로 올리고 재기동하면 됩니다 (수 초 내 완료)
- 활성 컨테이너 상태를 파일(예: `.deploy-state`)에 기록해 다음 배포 시 참조

**INF-05. AWS 리소스 프로비저닝**
- **EC2** — Amazon Linux 또는 Ubuntu, Docker·Docker Compose 설치, Elastic IP 할당
- **RDS MySQL 8.x** — 프라이빗 서브넷, 파라미터 그룹 timezone(Asia/Seoul)·charset(utf8mb4)
- **ECR** — 백엔드/프론트엔드 이미지 리포지토리 2개
- **S3** — prod 이미지 저장 버킷 (BE-15 연동)
- 보안 그룹: RDS 인바운드는 EC2 보안 그룹에서만, EC2는 80/443/22만 개방
- 도메인: Route 53 또는 외부 등록기관 → Elastic IP 연결

**INF-06. 시크릿 관리**
- DB 비밀번호, JWT 시크릿, AWS 자격증명, S3 버킷명
- GitHub Actions Secrets + EC2의 `.env` 파일 (Compose가 참조)
- `application-prod.yml` 평문 저장 금지

**INF-07. DB 마이그레이션 (Flyway)**
- prod가 `ddl-auto: validate` 이므로 **필수**. 없으면 첫 배포에서 `Schema-validation` 오류로 기동 실패
- `V1__init_schema.sql`, `V2__insert_categories.sql`
- 카테고리 20건 초기 데이터 포함

**INF-08. GitHub Actions CI**
- 트리거: PR 및 `main` push
- 백엔드: JDK 21 → Gradle 캐시 → `./gradlew build test`
- 프론트: Node 20 → `npm ci` → `npm run build` (빌드 실패 조기 감지)
- 테스트 실패 시 머지 차단

**INF-09. GitHub Actions CD**
- 트리거: `main` 머지
- 백엔드/프론트 이미지 각각 빌드 → ECR 푸시
- **SSH(appleboy/ssh-action)로 EC2 접속 → 배포 스크립트 실행**
  - 백엔드 변경 시 → INF-04 블루/그린 스크립트
  - 프론트 변경 시 → Nginx 컨테이너 재생성
- 변경된 쪽만 배포하도록 경로 필터(`paths:`) 적용 권장

**INF-10. 로컬 통합 환경**
- 별도 `docker-compose.local.yml` — backend + MySQL 컨테이너
- 프론트는 Vite dev server로 실행하고 proxy로 연결 (FE-01)

**INF-11. 모니터링 및 로깅**
- prod Actuator `health`, `info` 개방 (Nginx `/api/actuator/health` 경유)
- 컨테이너 로그 수집 (CloudWatch Logs 또는 로컬 파일 + logrotate)
- **정산 배치 실행 로그는 별도 확인 경로 확보** (돈이 움직이는 지점이므로)
- EC2 디스크 사용량 모니터링 (Docker 이미지가 쌓이므로 `docker image prune` 정기 실행)

---

## 5. 권장 작업 순서

```
BE-01 → BE-02 → BE-03 → BE-04 → BE-05 → BE-06 → BE-07 → BE-08
                                                            │
                                        (API 스펙 확정 시점) ├──────────────┐
                                                            ▼              ▼
                          BE-09 → BE-10 → BE-11 → BE-12 → BE-13 → BE-14 → BE-15
                                                            │              │
                                                            ▼              ▼
                                              BE-16 → BE-17 → BE-18   FE-01 → FE-02 → FE-03
                                                            │              │
                                                            ▼              ▼
                                                    BE-19 → BE-20 → BE-21  FE-04 ~ FE-13

INF-01 → INF-02 → INF-03 → INF-10 → INF-07 → INF-05 → INF-06 → INF-08 → INF-09 → INF-04 → INF-11
```

**난이도 상위 3개 — 시간을 넉넉히 잡을 것**
1. **BE-12 + BE-17** 사용 가능 포인트 검증과 정산 배치 (둘이 한 쌍입니다. 검증이 뚫리면 정산에서 터집니다)
2. **FE-02** Axios 인터셉터 토큰 갱신 + 동시 401 대기 큐
3. **INF-04** Nginx 블루/그린 스위칭 스크립트

---

## 부록: 원본 기획서 오탈자

- 3장 Dev 환경: `Actuator 모든 엔드포인트 개방.ㄴ` → 오타
- 6장 스켈레톤 UI: "질낮은 사용자 경험을 제공하는 것을 완벽히 방지하기 위해" → "로딩 중 사용자 경험 저하를 방지하기 위해" 정도로 정리 권장