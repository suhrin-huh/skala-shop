---
name: domain-rules
description: SKALA-FUND 의 확정 도메인 규칙 — 예약형 All-or-Nothing 펀딩, 포인트 흐름, 사용 가능 포인트 공식, 상태 Enum 전이표, Soft Delete 노출 규칙, 동시성 계약. 후원·취소·정산·포인트·프로젝트 상태·삭제에 닿는 코드를 읽거나 쓰기 전에 반드시 로드한다. 백엔드 로직뿐 아니라 프론트의 상태 배지 문구와 포인트 표시도 이 규칙에 묶여 있다.
---

# SKALA-FUND 도메인 규칙

이 문서는 **확정 사항**입니다. 여기 적힌 값과 다르게 구현되어 있으면 그것이 버그입니다.

---

## 0. 한 문장 요약

> **후원해도 포인트는 줄지 않는다. 마감일에 목표를 달성한 프로젝트만 결제된다.**

이것을 놓치면 이 도메인의 거의 모든 것을 잘못 구현하게 됩니다.

---

## 1. 절대 만들면 안 되는 것

### ❌ 환급(REFUND) 로직

이 서비스에 환급 개념은 **존재하지 않습니다.** 애초에 차감된 적이 없기 때문입니다.

- 후원 취소 → 포인트 되돌려주기? **아니다.** 예약 해제만 한다.
- 펀딩 실패 → 후원자에게 환불? **아니다.** 결제가 일어난 적이 없다.
- `PledgeStatus.REFUNDED`, `refund()`, `RefundService` 같은 것을 만들고 있다면 이미 잘못된 길이다.

### ❌ 후원 시점 포인트 차감

`customer.point` 는 **정산 배치에서만** 줄어듭니다. 후원·취소 경로에서 `point` 를 건드리면 안 됩니다.

### ❌ 엔티티 전역 soft delete 필터

`@SQLRestriction("deleted_at is null")` / `@Where` 를 `Project` 에 붙이면 편해 보이지만, **마이페이지 후원 내역에서 삭제된 프로젝트가 통째로 사라집니다.** 후원자 입장에선 자기 후원 기록이 증발한 것처럼 보입니다. Repository 쿼리마다 조건을 명시하세요.

---

## 2. 펀딩 생명주기

```
[SCHEDULED] --startDate 도래--> [ONGOING] --endDate 경과--> 정산 배치
   후원 불가                      후원 가능                    │
                                            모금액 >= target ──┼─> [SUCCESS] 후원자 차감 + 창작자 지급
                                            모금액 <  target ──┴─> [FAILED]  아무 정산도 없음
```

`ProjectStatus`

| 값 | 의미 | 후원 가능 | 수정 가능 |
|---|---|---|---|
| `SCHEDULED` | 등록 완료, 시작일 전 | ❌ | ✅ |
| `ONGOING` | 진행 중 | ✅ | ✅ |
| `SUCCESS` | 마감 + 목표 달성 (결제 완료) | ❌ | ❌ |
| `FAILED` | 마감 + 목표 미달 (결제 없음) | ❌ | ❌ |

---

## 3. 포인트 흐름 (핵심 표)

| 시점 | 후원자 `point` | 후원자 `reservedPoint` | 창작자 `point` | `Pledge.status` |
|---|---|---|---|---|
| 회원가입 | **1,000,000 지급** | 0 | - | - |
| 후원하기 | **변화 없음** | **+amount** | - | `PLEDGED` |
| 후원 취소 (마감 전) | **변화 없음** | **−amount** | - | `CANCELLED` |
| 마감 후 **성공** | **−amount** | **−amount** | **+모금액** | `CONFIRMED` |
| 마감 후 **실패** | 변화 없음 | **−amount** | 변화 없음 | `FAILED` |

`PledgeStatus`

| 값 | 전이 시점 | 취소 가능 | `deliveryStatus` |
|---|---|---|---|
| `PLEDGED` | 후원하기 | ✅ (마감 전) | `null` |
| `CANCELLED` | 사용자 취소 / 프로젝트 삭제 | ❌ | `null` |
| `CONFIRMED` | 정산 배치 (성공) | ❌ | `ORDER_COMPLETED` 부여 |
| `FAILED` | 정산 배치 (실패) | ❌ | `null` |

`DeliveryStatus` — `CONFIRMED` 에만 값이 있다. 역행 불가.

```
ORDER_COMPLETED → SHIPPING → DELIVERED
```

별도 `Reward` 엔티티는 없습니다. 이 상태로 리워드 이행을 판단합니다.

---

## 4. 사용 가능 포인트

포인트가 후원 시점에 줄지 않으므로, 잔액을 초과해 여러 프로젝트에 후원하는 문제가 생깁니다. 100만 보유자가 50만원 후원을 10건 하면 마감일에 500만이 필요해집니다.

```
사용 가능 포인트 = customer.point − customer.reservedPoint
```

**이 저장소는 비정규화를 정본으로 확정했습니다.** (원본 기획서의 "매 조회 시 SUM" 방식에서 의도적으로 벗어난 결정입니다. 되돌리지 마세요.)

| 컬럼 | 갱신 경로 |
|---|---|
| `Customer.reservedPoint` | `reservePoint()` / `releaseReservedPoint()` / `confirmDeduction()` |
| `Project.currentAmount`, `pledgeCount` | `addPledgeAmount()` / `removePledgeAmount()` |
| `Project.searchTitle` | 생성·수정 시 제목에서 파생 (공백 제거 + 소문자) |

**후원 검증은 `point` 가 아니라 `getAvailablePoint()` 로 합니다.** 이 규칙이 지켜지면 마감 시점에 잔액 부족이 발생할 수 없습니다. 그래도 정산 배치에는 방어 로직을 두고, 도달 시 `log.error` 를 남깁니다 (도달하면 그 자체가 상위 결함의 증거입니다).

---

## 5. 후원 / 취소 절차

### 후원하기 — 하나의 `@Transactional`

1. 프로젝트 조회: `deletedAt IS NULL`
2. `status == ONGOING` 검증
3. 마감일 경과 여부 재검증
4. 후원 금액 ≥ 1,000원 검증
5. **`Customer` 를 비관적 락으로 조회** (`findByIdWithLock`) — 동시 요청을 직렬화
6. `availablePoint >= amount` 검증 (부족 시 `INSUFFICIENT_AVAILABLE_POINT`)
7. `customer.reservePoint(amount)` + `project.addPledgeAmount(amount)`
8. `Pledge(PLEDGED, deliveryStatus=null)` 생성 — **`customer.point` 는 그대로**

### 후원 취소

1. 본인 후원인지 검증 (아니면 403)
2. `status == PLEDGED` 검증
3. 프로젝트 마감일 이전인지 검증
4. `Customer` 비관적 락 조회 → `releaseReservedPoint(amount)` + `project.removePledgeAmount(amount)`
5. `pledge.cancel()` — **포인트 조작 없음**

---

## 6. 정산 배치

대상: `status == ONGOING` && `endDate < 오늘` && `deletedAt IS NULL`
**프로젝트 단위로 트랜잭션 분리** — 한 건 실패가 전체를 롤백하면 안 됩니다.

```
모금액 = SUM(PLEDGED pledge.amount)

IF 모금액 >= targetAmount:
    project.status = SUCCESS
    각 Pledge (customer id 오름차순, 비관적 락):
        IF customer.point >= pledge.amount:
            customer.confirmDeduction(amount)   # point −, reservedPoint −
            pledge.confirm()                    # CONFIRMED + ORDER_COMPLETED
        ELSE:
            pledge.markFailed()
            customer.releaseReservedPoint(amount)
            log.error(...)                      # 도달하면 상위 검증이 뚫린 것
    creator.addPoint(실제 결제된 금액 합계)
ELSE:
    project.status = FAILED
    각 Pledge: releaseReservedPoint(amount) + markFailed()   # 포인트 조작 없음
```

- **멱등성 장치는 `status == ONGOING` 조건**입니다. 재실행해도 두 번 정산되지 않습니다.
- 실행 결과(대상 건수, 성공/실패, 총 정산액)를 반드시 로깅합니다. 돈이 움직이는 지점입니다.

---

## 7. Soft Delete

`Project` 는 물리 삭제하지 않습니다. `deletedAt` (nullable `LocalDateTime`) 으로 표현합니다.

**삭제 절차 (하나의 `@Transactional`)**

1. 요청자 == `project.creator` 검증 (아니면 403)
2. 해당 프로젝트의 `PLEDGED` 후원을 전부 `CANCELLED` 로 전환 + 각 후원자의 `reservedPoint` 해제
3. `project.softDelete()`

**조회 시 노출 규칙 — 여기가 함정입니다**

| 화면 | 삭제된 프로젝트 |
|---|---|
| 프로젝트 목록 / 검색 / 인기 | ❌ `deletedAt IS NULL` 필터 |
| 프로젝트 상세 | ❌ 404 |
| 찜 목록 | ❌ 필터 |
| 최근 본 항목 (`?ids=`) | ❌ **조용히 제외** (404 로 전체 실패시키지 말 것) |
| **마이페이지 후원 내역** | ✅ **노출** + `projectDeleted: true` 플래그, 상세 링크 비활성 |
| 정산 배치 대상 | ❌ 제외 |

---

## 8. 프로젝트 수정

- 요청자 ID ≠ `project.creator.id` → 403
- `SUCCESS` / `FAILED` (이미 마감) → 수정 불가
- `deletedAt != null` → 수정 불가
- **후원자가 있어도 수정 가능**합니다. 다만 목표 금액·마감일 변경은 이미 후원한 사람에게 영향이 가므로 **변경 이력 로그를 남깁니다.**

---

## 9. 엔티티 제약

| 엔티티 | 제약 |
|---|---|
| `Customer` | email 유니크·정규식, nickname 2~10자, password 8자 이상 + 특수문자(BCrypt), point ≥ 0, 가입 시 1,000,000 |
| `Category` | name 유니크, 20종 고정 |
| `Project` | title 5~50자, description 20자 이상, targetAmount ≥ 100,000, **endDate ≥ startDate + 7일** |
| `Pledge` | amount ≥ 1,000 |
| `ProjectLike` | (customer, project) 복합 유니크 |
| `RefreshToken` | token 유니크 |

**카테고리 20종 (순서 고정)**

```
디자인 문구 / 푸드 / 출판 / 영화·비디오 / 보드게임·TRPG /
캐릭터·굿즈 / 향수·뷰티 / 디자인·일러스트 / 공연 / 홈·리빙 /
의류 / 문화·예술 / 웹툰·만화 / 테크·가전 / 잡화 /
사진 / 웹툰 리소스 / 반려동물 / 주얼리 / 음악
```

> `웹툰·만화` = 완성된 작품, `웹툰 리소스` = 웹툰 제작용 배경·소재 파일. **별개 카테고리이므로 통합 금지.**

---

## 10. 프론트 표기 규칙 (도메인에 묶인 문구)

후원 모달과 마이페이지 문구는 임의로 바꾸면 사용자가 결제 시점을 오해합니다.

- 후원 모달: **"보유 포인트"와 "사용 가능 포인트"를 나란히** 표시하고, 검증은 사용 가능 포인트 기준.
- 후원 모달 필수 문구: **"지금 결제되지 않으며, 펀딩 성공 시 마감일에 결제됩니다"**
- 마이페이지 후원 상태 표기
  - `PLEDGED` → "결제 예약됨 (마감일 결제 예정)" + 취소 버튼
  - `CANCELLED` → "취소됨"
  - `CONFIRMED` → "결제 완료" + 배송 상태
  - `FAILED` → "펀딩 무산 (미결제)"
  - `projectDeleted: true` → "삭제된 프로젝트" 배지 + 상세 링크 비활성화
- 포인트 영역은 **3개 값을 모두** 표시: `보유 / 예약 / 사용 가능`
- 회원가입 성공 시 "100만 포인트 지급" 안내 모달.

---

## 11. 동시성 계약

- 락 대상은 **`Customer` 행**입니다. 여러 요청이 같은 회원의 사용 가능 포인트를 동시에 읽고 초과 후원하는 것을 막는 게 목적입니다.
- 여러 행을 잠글 때는 **항상 id 오름차순**으로 (데드락 방지).
- 완료 조건: 동일 계정 동시 후원 시 `reservedPoint <= point` 가 항상 유지됨을 테스트로 검증. → `harness-verify` 스킬 참조.
