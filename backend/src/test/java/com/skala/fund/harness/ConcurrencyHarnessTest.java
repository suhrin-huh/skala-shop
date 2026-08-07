package com.skala.fund.harness;

import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CategoryRepository;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.service.PledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class ConcurrencyHarnessTest {

    @Autowired
    private PledgeService pledgeService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PledgeRepository pledgeRepository;

    private Long testCustomerId;
    private Long testProjectId;

    @BeforeEach
    void setUp() {
        pledgeRepository.deleteAll();
        projectRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(new Category("테크·가전", 1));

        Customer creator = customerRepository.save(Customer.builder()
                .email("creator@test.com")
                .nickname("창작자")
                .password("password123!")
                .point(0L)
                .build());

        // 보유 포인트 1,000,000 P 인 후원자
        Customer supporter = customerRepository.save(Customer.builder()
                .email("supporter@test.com")
                .nickname("후원자")
                .password("password123!")
                .point(1_000_000L)
                .build());
        testCustomerId = supporter.getId();

        Project project = projectRepository.save(Project.builder()
                .creator(creator)
                .category(category)
                .title("동시성 검증 프로젝트")
                .description("동시성 테스트용 설명입니다.")
                .mainImage("http://localhost/image.png")
                .targetAmount(500_000L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .status(ProjectStatus.ONGOING)
                .build());
        testProjectId = project.getId();
    }

    @Test
    @DisplayName("[하네스 검증] 10개 동시 후원 요청 시 사용 가능 포인트를 초과하지 않는 정합성 검증")
    void testConcurrentPledgingIntegrity() throws InterruptedException {
        int threadCount = 10;
        long pledgeAmount = 400_000L; // 1,000,000 P로 최대 2건만 성공 가능 (총 800,000 P 예약)

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pledgeService.createPledge(testCustomerId, testProjectId, pledgeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Customer updatedCustomer = customerRepository.findById(testCustomerId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(8);
        assertThat(updatedCustomer.getReservedPoint()).isEqualTo(800_000L);
        assertThat(updatedCustomer.getAvailablePoint()).isEqualTo(200_000L);
        assertThat(updatedCustomer.getPoint()).isEqualTo(1_000_000L);
    }
}
