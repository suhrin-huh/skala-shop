# SKALA-FUND 하네스 엔지니어링 기반 구축 및 서비스 고도화 실행 계획

이 계획서는 [design-sheet.md](file:///c:/Users/MIN/Desktop/programming/skala-shop/design-sheet.md)와 [skala-fund.md](file:///c:/Users/MIN/Desktop/programming/skala-shop/skala-fund.md) 명세를 바탕으로, **하네스 엔지니어링(Harness Engineering)** 체계를 선제적으로 구축하고 **고도화된 비정규화 데이터 모델** 및 **완전한 풀스택 시스템**을 개발하기 위한 전략을 다룹니다.

---

## User Review Required

> [!IMPORTANT]
> **고도화된 데이터 모델 적용 (초기 반영)**
> 1. **사용 가능 포인트 비정규화 (`Customer.reservedPoint`)**: 기존의 매번 `SUM(PLEDGED)` 집계 방식 대신 `Customer` 엔티티에 `reservedPoint` 비정규화 필드를 두고 DB 비관적 락으로 즉시 동기화합니다. 사용 가능 포인트 = `point - reservedPoint`.
> 2. **프로젝트 모금액 및 후원자 수 비정규화 (`Project.currentAmount`, `Project.pledgeCount`)**: 목록/상세 조회 시 N+1 `SUM` 집계 대신 비정규화 컬럼으로 즉시 관리합니다.
> 3. **검색 고도화 (`Project.searchTitle`)**: 공백을 제거한 검색 전용 타이틀 컬럼 및 인덱싱 처리로 쿼리 성능을 극대화합니다.
> 4. **프로젝트 Soft Delete (`Project.deletedAt`)**: 삭제 후에도 기존 후원 이력 및 마이페이지 참조 정합성을 보장합니다.

> [!NOTE]
> **하네스 엔지니어링 (옵션 A) 적용**
> 핵심 비즈니스 로직(동시 후원, 가상 시간 배치 정산, 포인트 보존 법칙, 디자인 토큰 및 UI 인터셉터)을 자동으로 시뮬레이션하고 검증할 수 있는 **하네스 모듈(Harness System)**을 가장 먼저 구축한 뒤 개발 및 고도화를 진행합니다.

---

## Open Questions

질문 사항이 완료되었으며, 사용자의 지침에 따라 이하의 구체적 플랜으로 바로 진행합니다.

---

## Proposed Changes

### 1. Harness Engineering Architecture (하네스 체계)

#### [NEW] `backend/src/test/java/com/skala/fund/harness/`
- **`ConcurrencyHarnessTest.java`**: `ExecutorService` (10~100 스레드)를 이용해 동일 사용자 동시 후원 및 잔액 초과 후원 실패/보유 포인트 보존을 자동 검증.
- **`SettlementInvariantHarnessTest.java`**: 정산 성공/실패 시 `후원자 차감 포인트 합계 == 창작자 증가 포인트 합계` (보존 법칙)을 자동 검증.
- **`TimeTravelBatchSimulator.java`**: 원하는 날짜/시각으로 배치 상태 전이(`SCHEDULED` $\rightarrow$ `ONGOING` $\rightarrow$ `SUCCESS`/`FAILED`)를 강제 트리거하고 검증하는 테스트 및 내장 컨트롤러 하네스.

#### [NEW] `backend/src/main/java/com/skala/fund/harness/controller/HarnessSimulatorController.java`
- 개발/테스트 전용 REST 컨트롤러 (`@Profile({"dev", "local"})`):
  - `POST /api/harness/batch/run-status-transition`: 프로젝트 상태 전이 배치 즉시 트리거.
  - `POST /api/harness/batch/run-settlement`: 정산 배치 즉시 트리거.
  - `POST /api/harness/seed`: 다양한 상태의 샘플 테스트 데이터(창작자, 펀딩 프로젝트 20개, 후원 내역) 일괄 세딩.

---

### 2. Backend (Spring Boot 3.x, Java 17, Gradle, JPA)

#### [NEW] [build.gradle](file:///c:/Users/MIN/Desktop/programming/skala-shop/backend/build.gradle)
- Spring Boot 3.x, Spring Data JPA, Spring Security, JWT (`jjwt`), H2 / MySQL, Flyway, Validation, Actuator 설정.

#### [NEW] Domain Layer & Enums
- **`Customer`**: `id`, `email`, `nickname`, `password`, `point` (보유 포인트), `reservedPoint` (예약 중인 포인트), `getAvailablePoint()`.
- **`Project`**: `id`, `creator`, `category`, `title`, `searchTitle`, `description`, `mainImage`, `targetAmount`, `currentAmount`, `pledgeCount`, `startDate`, `endDate`, `status`, `deletedAt`.
- **`Pledge`**: `id`, `customer`, `project`, `amount`, `status` (`PLEDGED`, `CANCELLED`, `CONFIRMED`, `FAILED`), `deliveryStatus` (`ORDER_COMPLETED`, `SHIPPING`, `DELIVERED`).
- **`Category`**: `id`, `name`, `displayOrder` (20개 카테고리 명세 준수).
- **`ProjectLike`**, **`RefreshToken`**.

#### [NEW] Service & Repository Layer
- **`CustomerRepository`**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` 조회 구현.
- **`PledgeService`**:
  - `pledge()`: 비관적 락으로 Customer 조회 $\rightarrow$ `availablePoint >= amount` 검증 $\rightarrow$ `customer.reservedPoint += amount` & `project.currentAmount += amount` & `project.pledgeCount++` 비정규화 즉시 업데이트 후 `Pledge(PLEDGED)` 생성.
  - `cancelPledge()`: `customer.reservedPoint -= amount` & `project.currentAmount -= amount` & `project.pledgeCount--` 업데이트 후 `Pledge(CANCELLED)`.
- **`SettlementBatchService`**:
  - 마감일 지난 `ONGOING` 프로젝트 정산 처리.
  - 목표 달성 시: `PLEDGED` $\rightarrow$ `CONFIRMED`, `customer.point -= amount`, `customer.reservedPoint -= amount`, 창작자 `point += totalConfirmedAmount`.
  - 목표 미달 시: `PLEDGED` $\rightarrow$ `FAILED`, `customer.reservedPoint -= amount`.
  - 포인트 총량 보존 락 및 트랜잭션 분리.

#### [NEW] Controller Layer
- `/api/auth/*` (회원가입/로그인/Refresh/로그아웃)
- `/api/projects/*` (목록/인기5/상세/등록/수정/삭제/찜)
- `/api/pledges/*` (후원/취소/배송상태)
- `/api/users/me/*` (마이페이지 프로필/후원/찜/내프로젝트)
- `/api/categories` (카테고리 20종)

---

### 3. Frontend (Vite + React, Pure CSS)

#### [NEW] [frontend/package.json](file:///c:/Users/MIN/Desktop/programming/skala-shop/frontend/package.json)
- React, React Router DOM, Axios, React Icons, Vite.

#### [NEW] Design System & CSS Tokens ([design-sheet.md](file:///c:/Users/MIN/Desktop/programming/skala-shop/design-sheet.md) 준수)
- `frontend/src/styles/tokens.css`: `--color-primary` (`#ff5757`), `--color-violet` (`#7a4df5`), `--color-ink` (`#212124`), `--font-family` (`Pretendard Variable`), 4px 스페이싱 토큰 등.
- `frontend/src/styles/global.css`: 리셋, Pretendard 폰트 페이스, 스크롤바, 공통 유틸리티 클래스.

#### [NEW] Core FE Components & Pages
- **`Header` / `NavBar` / `UtilityBar`**: 2단 헤더, 카테고리 탭, 3px 잉크 언더라인, "N" 뱃지, "창작자센터" 아웃라인 버튼.
- **`CategoryStrip`**: 72px 대형 라운드 타일 8열, "N" 뱃지 호버 효과.
- **`HeroBanner`**: 16:7 배너, 텍스트 그라디언트 스크림, 우하단 알약 카운터 & 셰브론 컨트롤.
- **`RankingSidebar`**: 356px 우측 고정 영역, 시그니처 24px 코랄 랭크 뱃지.
- **`ProjectCard` / `ProjectGrid`**: 4:3 썸네일, 하트 버튼 스케일 팝, 코랄 달성률 수치, 3px 진행바, `badge-creator` (Violet #7a4df5), `badge-urgent` (Coral Soft).
- **`PledgeModal`**: 보유 포인트 vs 사용 가능 포인트 대조 카드, 결제 예약 안내 문구.
- **`AuthContext` & Axios Interceptor**: 메모리 Access Token, Http-Only Refresh Token, 동시 401 대기 큐 처리.
- **`Pages`**: 메인(`/`), 프로젝트목록(`/projects`), 상세(`/projects/:id`), 등록/수정(`/projects/new`, `/projects/:id/edit`), 마이페이지(`/mypage`), 인증(`/login`, `/signup`).

---

## Verification Plan

### Automated Tests (하네스 테스트)
1. **백엔드 동시성 & 포인트 보존 하네스**:
   - `ConcurrencyHarnessTest`: 50개 스레드로 동시에 50만원씩 후원 시 100만원 잔액에서 2건만 성공하고 `reservedPoint = 100만`, `availablePoint = 0`이 정확히 유지되는지 검증.
   - `SettlementInvariantHarnessTest`: 정산 배치 후 시스템 내 전체 포인트 합계(후원자+창작자)가 변함없음을 자동 검증.
2. **단위 및 통합 테스트**:
   - `./gradlew test` 로 전체 API 및 서비스 비즈니스 로직 100% 통과 검증.

### Manual & Visual Verification (인프라 & UI 하네스 검증)
1. **하네스 시뮬레이터 API 검증**:
   - `/api/harness/seed`로 20개 프로젝트 시뮬레이션 데이터 생성.
   - `/api/harness/batch/run-settlement` 호출하여 마감 프로젝트 정산 결과 및 모금액/포인트 즉시 갱신 확인.
2. **UI & 디자인 시트 준수 검증**:
   - 데스크톱/태블릿/모바일 반응형 레이아웃 스크롤 및 브라우저 렌더링 확인.
   - 코랄(`#ff5757`) & 바이올렛(`#7a4df5`) 이중 액센트 가이드 적용 확인.
