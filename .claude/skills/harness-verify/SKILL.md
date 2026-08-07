---
name: harness-verify
description: SKALA-FUND 하네스 실행·검증 절차 — 동시성 테스트, 정산 불변식, 포인트 총량 보존, 시뮬레이터 API(seed/배치 트리거) 사용법과 판정 기준. 후원·취소·정산·포인트·비정규화 컬럼에 닿는 코드를 바꾼 뒤, 또는 "검증해줘 / 테스트 돌려줘 / 하네스 실행" 요청을 받았을 때 로드한다.
---

# 하네스 검증 절차

이 프로젝트는 **하네스 엔지니어링**을 씁니다. 기능이 맞는지를 사람 눈이 아니라 자동 하네스로 판정합니다.

먼저 `domain-rules` 스킬로 무엇이 참이어야 하는지 확정한 뒤 이 절차를 실행하세요.

---

## 1. 언제 돌리는가

| 변경한 것 | 돌려야 할 것 |
|---|---|
| `PledgeService` | 동시성 하네스 + 전체 테스트 |
| `SettlementBatchService` / `batch/**` | 정산 불변식 하네스 + 전체 테스트 |
| `Customer` / `Project` / `Pledge` 엔티티 | **하네스 전체** |
| Repository 쿼리 | 전체 테스트 |
| Controller / DTO | 전체 테스트 |
| 프론트엔드만 | `npm run build` (하네스 불필요) |

애매하면 전체를 돌립니다. 판단을 아끼는 것보다 30초 더 기다리는 게 쌉니다.

---

## 2. 실행 명령

```bash
# 하네스만
cd backend && ./gradlew test --tests 'com.skala.fund.harness.*'

# 전체
cd backend && ./gradlew test

# 빌드 포함 (완료 보고 전 필수)
cd backend && ./gradlew build
```

Windows 에서도 `./gradlew` 를 씁니다 (Bash 툴). PowerShell 이면 `.\gradlew.bat`.

> `gradle` 명령을 직접 쓰지 마세요. 저장소에 풀려 있는 배포판은 gitignore 대상이고, Wrapper 만이 정본입니다.

---

## 3. 검증해야 할 불변식

| # | 불변식 | 확인 방법 |
|---|---|---|
| I1 | `customer.reservedPoint == SUM(그 회원의 PLEDGED amount)` | 하네스에서 SUM 쿼리로 대조 |
| I2 | `customer.reservedPoint <= customer.point` | 동시 후원 후 단언 |
| I3 | `project.currentAmount == SUM(PLEDGED+CONFIRMED amount)` | 후원/취소 후 대조 |
| I4 | `project.pledgeCount == COUNT(PLEDGED+CONFIRMED)` | 후원/취소 후 대조 |
| I5 | 정산 전후 **시스템 전체 포인트 총합 불변** | 전 회원 `point` 합계를 정산 전후로 비교 |
| I6 | 정산 배치 재실행 시 결과 동일 (멱등) | 배치를 2회 호출 후 상태 비교 |
| I7 | 후원/취소 경로에서 `customer.point` 불변 | 후원 전후 `point` 단언 |
| I8 | soft delete 노출 규칙 | 목록/상세/찜에서 제외, 마이페이지 후원 내역에는 노출 |

**비정규화 컬럼을 정본으로 쓰는 프로젝트이므로, I1/I3/I4 (비정규화 값 == 실제 SUM) 가 이 시스템의 생명선입니다.** 여기가 어긋나면 화면의 달성률과 포인트가 전부 거짓말이 됩니다.

---

## 4. 동시성 하네스 작성 규칙

`ExecutorService` 만 쓰면 스레드가 순차 실행되어 아무것도 검증하지 못할 수 있습니다. 반드시 **동시 진입을 강제**하세요.

```java
int threads = 50;
ExecutorService pool = Executors.newFixedThreadPool(threads);
CountDownLatch ready = new CountDownLatch(threads);   // 전원 대기 완료 신호
CountDownLatch start = new CountDownLatch(1);         // 동시 출발 신호
CountDownLatch done  = new CountDownLatch(threads);

for (int i = 0; i < threads; i++) {
    pool.submit(() -> {
        ready.countDown();
        try {
            start.await();              // 여기서 전원이 함께 풀린다
            pledgeService.createPledge(customerId, projectId, 500_000L);
            success.incrementAndGet();
        } catch (Exception e) {
            failure.incrementAndGet();  // 초과 후원은 실패하는 것이 정상이다
        } finally {
            done.countDown();
        }
    });
}
ready.await();
start.countDown();
done.await();
```

### 판정
- 잔액 100만 / 후원 50만 / 50스레드 → **성공 정확히 2건**, 실패 48건
- `reservedPoint == 1,000,000`, `getAvailablePoint() == 0`
- `point == 1,000,000` (후원으로는 안 줄어든다 — I7)

### 흔한 함정
- **테스트에 `@Transactional` 을 붙이면 안 됩니다.** 테스트 트랜잭션이 스레드마다 분리되지 않아 락이 의미를 잃고, 롤백 때문에 최종 상태도 확인할 수 없습니다. 대신 테스트 후 수동 정리를 하세요.
- 스레드 안에서 던진 예외는 삼켜집니다. `AtomicInteger` 로 성공/실패를 세고 단언하세요.
- 실패 건수만 세고 **"왜 실패했는지"를 확인하지 않으면** 잔액 부족이 아니라 NPE 로 실패해도 통과합니다. 예외 타입까지 단언하세요.
- 검증은 반드시 **DB 를 다시 읽어서** 합니다. 영속성 컨텍스트에 남은 엔티티를 보면 갱신 전 값을 볼 수 있습니다.

---

## 5. 정산 하네스 작성 규칙

```java
long before = customerRepository.findAll().stream().mapToLong(Customer::getPoint).sum();
settlementBatchService.runSettlementBatch();
long after  = customerRepository.findAll().stream().mapToLong(Customer::getPoint).sum();
assertThat(after).isEqualTo(before);   // I5: 포인트는 이동할 뿐 생성/소멸하지 않는다
```

### 필수 시나리오
| 시나리오 | 기대 결과 |
|---|---|
| 목표 달성 | `SUCCESS`, 모든 PLEDGED → `CONFIRMED` + `ORDER_COMPLETED`, 창작자 `point` += 모금액, I5 성립 |
| 목표 미달 | `FAILED`, 모든 PLEDGED → `FAILED`, **어떤 `point` 도 변하지 않음** |
| 모금액 == 목표액 | `SUCCESS` (`>=` 이므로 경계 포함) |
| 삭제된 프로젝트 | 정산 대상에서 제외 |
| 배치 2회 실행 | 두 번째 실행은 변화 없음 (I6) |

정산은 프로젝트 단위 트랜잭션이므로, 한 프로젝트를 일부러 실패시켜도 다른 프로젝트 정산이 롤백되지 않아야 합니다.

---

## 6. 시뮬레이터 API (수동 검증)

`@Profile({"dev","local"})` 전용입니다. **prod 에서 노출되면 안 됩니다.**

```bash
# 애플리케이션 기동 (dev)
cd backend && ./gradlew bootRun

# 샘플 데이터 세딩 (카테고리 20종 + 계정 + 프로젝트)
curl -X POST http://localhost:8080/api/harness/seed

# 상태 전이 배치 즉시 실행 (SCHEDULED → ONGOING)
curl -X POST http://localhost:8080/api/harness/batch/run-status-transition

# 정산 배치 즉시 실행 (마감 프로젝트 정산)
curl -X POST http://localhost:8080/api/harness/batch/run-settlement
```

배치는 원래 매일 00:05 / 00:10 에 돌기 때문에, 이 트리거 없이는 하루를 기다려야 검증할 수 있습니다.

H2 콘솔로 실제 값을 대조할 수 있습니다: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:skalafund`)

```sql
-- I1 대조
SELECT c.id, c.reserved_point,
       (SELECT COALESCE(SUM(p.amount),0) FROM pledge p
         WHERE p.customer_id = c.id AND p.status = 'PLEDGED') AS actual
FROM customer c;

-- I3/I4 대조
SELECT pr.id, pr.current_amount, pr.pledge_count,
       (SELECT COALESCE(SUM(p.amount),0) FROM pledge p
         WHERE p.project_id = pr.id AND p.status IN ('PLEDGED','CONFIRMED')) AS actual_amount,
       (SELECT COUNT(*) FROM pledge p
         WHERE p.project_id = pr.id AND p.status IN ('PLEDGED','CONFIRMED')) AS actual_count
FROM project pr;
```

---

## 7. 판정과 보고

**합격 기준: 실행한 명령의 실제 출력이 통과를 보여줄 것.** 그 외의 근거는 인정하지 않습니다.

- 돌리지 않은 테스트를 통과했다고 쓰지 않습니다.
- 실패를 "환경 문제 같다"로 넘기지 않습니다. 재현 조건을 좁혀서 보고합니다.
- 우연히 통과했을 가능성이 있으면(예: 스레드가 실제로 동시 진입했는지 불확실) 조건을 강화해 다시 돌립니다.

### 보고 형식

```
## 하네스 판정: 합격 | 불합격

실행: ./gradlew test --tests 'com.skala.fund.harness.*'
결과: N개 통과 / M개 실패

| # | 불변식 | 결과 |
|---|---|---|
| I1 | reservedPoint == SUM(PLEDGED) | ✅ |
| ... |

### 발견된 결함
- 증상 / 재현 조건 / 위반 불변식 / 담당 파트
```
