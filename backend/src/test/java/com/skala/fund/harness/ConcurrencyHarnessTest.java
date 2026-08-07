package com.skala.fund.harness;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.service.PledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 후원 하네스.
 *
 * 검증 불변식
 * - I1: customer.reservedPoint == SUM(그 회원의 PLEDGED pledge.amount)
 * - I2: customer.reservedPoint <= customer.point
 * - I3/I4: project.currentAmount / pledgeCount == 실제 SUM / COUNT
 * - I7: 후원 경로에서 customer.point 는 변하지 않는다
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(HarnessFixture.class)
class ConcurrencyHarnessTest {

    private static final long INITIAL_POINT = 1_000_000L;

    @Autowired
    private PledgeService pledgeService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PledgeRepository pledgeRepository;

    @Autowired
    private HarnessFixture fixture;

    private Long customerId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        fixture.clearAll();
        Category category = fixture.createCategory("테크·가전");
        Customer creator = fixture.createCustomer("creator@test.com", "창작자", 0L);
        Customer supporter = fixture.createCustomer("supporter@test.com", "후원자", INITIAL_POINT);
        Project project = fixture.createOngoingProject(creator, category, "동시성 검증 프로젝트", 500_000L);

        customerId = supporter.getId();
        projectId = project.getId();
    }

    @Test
    @DisplayName("[하네스] 동시 후원 50건 중 잔액이 허용하는 건수만 성공하고 예약 포인트가 보유액을 넘지 않는다")
    void concurrentPledgesNeverExceedAvailablePoint() throws InterruptedException {
        int threadCount = 50;
        long pledgeAmount = 400_000L; // 1,000,000 P 로는 최대 2건만 성공할 수 있다

        ConcurrentResult result = runConcurrentPledges(threadCount, pledgeAmount);

        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failure()).isEqualTo(threadCount - 2);

        // 실패가 "잔액 부족"이 아닌 다른 이유(NPE 등)로 났다면 이 테스트는 아무것도 검증하지 못한 것이다.
        assertThat(result.unexpectedErrors()).isEmpty();

        assertInvariants(800_000L);
    }

    @Test
    @DisplayName("[하네스] 스레드 수를 100으로 올려도 동일한 결과가 나온다")
    void concurrencyResultIsStableUnderHigherContention() throws InterruptedException {
        ConcurrentResult result = runConcurrentPledges(100, 300_000L);

        // 1,000,000 / 300,000 = 3건
        assertThat(result.success()).isEqualTo(3);
        assertThat(result.unexpectedErrors()).isEmpty();

        assertInvariants(900_000L);
    }

    @Test
    @DisplayName("[하네스] 사용 가능 포인트와 후원 금액이 정확히 같으면 후원이 허용된다")
    void pledgeIsAllowedWhenAmountEqualsAvailablePoint() {
        pledgeService.createPledge(customerId, projectId, INITIAL_POINT);

        assertInvariants(INITIAL_POINT);
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.getAvailablePoint()).isZero();
    }

    @Test
    @DisplayName("[하네스] 동시 후원과 취소가 섞여도 예약 포인트 정합성이 유지된다")
    void concurrentPledgeAndCancelKeepsReservedPointConsistent() throws InterruptedException {
        // 취소 대상이 될 후원을 먼저 만들어둔다.
        List<Long> pledgeIds = List.of(
                pledgeService.createPledge(customerId, projectId, 100_000L).getId(),
                pledgeService.createPledge(customerId, projectId, 100_000L).getId(),
                pledgeService.createPledge(customerId, projectId, 100_000L).getId());

        int pledgeThreads = 20;
        int totalThreads = pledgeThreads + pledgeIds.size();

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch ready = new CountDownLatch(totalThreads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(totalThreads);

        for (int i = 0; i < pledgeThreads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    pledgeService.createPledge(customerId, projectId, 100_000L);
                } catch (Exception ignored) {
                    // 잔액을 넘긴 요청이 실패하는 것은 정상이다.
                } finally {
                    done.countDown();
                }
            });
        }
        for (Long pledgeId : pledgeIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    pledgeService.cancelPledge(customerId, pledgeId);
                } catch (Exception ignored) {
                    // 동시 취소 경합으로 실패할 수 있다.
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        // 성공/실패 건수는 스케줄링에 따라 달라지지만, 비정규화 값과 실제 SUM 은 항상 같아야 한다.
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        long actualReserved = pledgeRepository.sumReservedAmountByCustomer(customerId);

        assertThat(customer.getReservedPoint()).isEqualTo(actualReserved);          // I1
        assertThat(customer.getReservedPoint()).isLessThanOrEqualTo(customer.getPoint()); // I2
        assertThat(customer.getPoint()).isEqualTo(INITIAL_POINT);                   // I7
        assertProjectDenormalizationMatches();
    }

    private ConcurrentResult runConcurrentPledges(int threadCount, long amount) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        ConcurrentLinkedQueue<String> unexpected = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 전원이 여기서 함께 풀린다. 이 신호가 없으면 순차 실행될 수 있다.
                    pledgeService.createPledge(customerId, projectId, amount);
                    success.incrementAndGet();
                } catch (CustomException e) {
                    failure.incrementAndGet();
                    if (e.getErrorCode() != ErrorCode.INSUFFICIENT_AVAILABLE_POINT) {
                        unexpected.add(e.getErrorCode().name());
                    }
                } catch (Exception e) {
                    failure.incrementAndGet();
                    unexpected.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        return new ConcurrentResult(success.get(), failure.get(), List.copyOf(unexpected));
    }

    /** DB 를 다시 읽어서 검증한다. 영속성 컨텍스트에 남은 엔티티를 보면 갱신 전 값을 볼 수 있다. */
    private void assertInvariants(long expectedReserved) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        assertThat(customer.getReservedPoint()).isEqualTo(expectedReserved);
        assertThat(customer.getReservedPoint())
                .isEqualTo(pledgeRepository.sumReservedAmountByCustomer(customerId));      // I1
        assertThat(customer.getReservedPoint()).isLessThanOrEqualTo(customer.getPoint());  // I2
        assertThat(customer.getPoint()).isEqualTo(INITIAL_POINT);                          // I7
        assertThat(customer.getAvailablePoint()).isEqualTo(INITIAL_POINT - expectedReserved);

        assertProjectDenormalizationMatches();
    }

    private void assertProjectDenormalizationMatches() {
        Project project = projectRepository.findById(projectId).orElseThrow();
        assertThat(project.getCurrentAmount())
                .isEqualTo(pledgeRepository.sumActiveAmountByProject(projectId));   // I3
        assertThat(project.getPledgeCount())
                .isEqualTo(pledgeRepository.countActiveByProject(projectId));       // I4
    }

    private record ConcurrentResult(int success, int failure, List<String> unexpectedErrors) {}
}
