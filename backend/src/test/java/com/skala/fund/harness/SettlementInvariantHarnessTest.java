package com.skala.fund.harness;

import com.skala.fund.batch.SettlementScheduler;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.DeliveryStatus;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.service.PledgeService;
import com.skala.fund.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정산 불변식 하네스.
 *
 * - I5: 정산 전후 시스템 전체 포인트 총합이 불변 (후원자 차감 합 == 창작자 증가 합)
 * - I6: 정산 배치를 두 번 돌려도 결과가 같다 (멱등)
 * - 실패 정산에서는 어떤 포인트도 움직이지 않는다
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(HarnessFixture.class)
class SettlementInvariantHarnessTest {

    private static final long SUPPORTER_POINT = 1_000_000L;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementScheduler settlementScheduler;

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

    private Customer creator;
    private Customer supporter;
    private Category category;

    @BeforeEach
    void setUp() {
        fixture.clearAll();
        category = fixture.createCategory("디자인 문구");
        creator = fixture.createCustomer("creator@test.com", "창작자", 0L);
        supporter = fixture.createCustomer("supporter@test.com", "후원자", SUPPORTER_POINT);
    }

    @Test
    @DisplayName("[하네스] 펀딩 성공 정산 시 후원자 차감액과 창작자 증가액이 일치하고 총량이 보존된다")
    void successfulSettlementPreservesTotalPoints() {
        Project project = fixture.createOngoingProject(creator, category, "성공 예정 펀딩", 500_000L);
        pledgeService.createPledge(supporter.getId(), project.getId(), 600_000L);
        fixture.closeProject(project.getId());

        long before = totalSystemPoints();
        settlementService.settleSingleProject(project.getId());
        long after = totalSystemPoints();

        assertThat(after).isEqualTo(before); // I5

        Project settled = projectRepository.findById(project.getId()).orElseThrow();
        Customer updatedSupporter = customerRepository.findById(supporter.getId()).orElseThrow();
        Customer updatedCreator = customerRepository.findById(creator.getId()).orElseThrow();

        assertThat(settled.getStatus()).isEqualTo(ProjectStatus.SUCCESS);
        assertThat(updatedSupporter.getPoint()).isEqualTo(400_000L);
        assertThat(updatedSupporter.getReservedPoint()).isZero();
        assertThat(updatedCreator.getPoint()).isEqualTo(600_000L);

        List<Pledge> pledges = pledgeRepository.findByProject(settled);
        assertThat(pledges).allSatisfy(pledge -> {
            assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.CONFIRMED);
            assertThat(pledge.getDeliveryStatus()).isEqualTo(DeliveryStatus.ORDER_COMPLETED);
        });
    }

    @Test
    @DisplayName("[하네스] 펀딩 무산 정산 시 예약만 해제되고 어떤 포인트도 움직이지 않는다")
    void failedSettlementMovesNoPoints() {
        Project project = fixture.createOngoingProject(creator, category, "실패 예정 펀딩", 500_000L);
        pledgeService.createPledge(supporter.getId(), project.getId(), 200_000L);
        fixture.closeProject(project.getId());

        long before = totalSystemPoints();
        settlementService.settleSingleProject(project.getId());
        long after = totalSystemPoints();

        assertThat(after).isEqualTo(before);

        Project settled = projectRepository.findById(project.getId()).orElseThrow();
        Customer updatedSupporter = customerRepository.findById(supporter.getId()).orElseThrow();
        Customer updatedCreator = customerRepository.findById(creator.getId()).orElseThrow();

        assertThat(settled.getStatus()).isEqualTo(ProjectStatus.FAILED);
        // 환급 개념이 없다. 애초에 차감된 적이 없으므로 보유 포인트는 그대로다.
        assertThat(updatedSupporter.getPoint()).isEqualTo(SUPPORTER_POINT);
        assertThat(updatedSupporter.getReservedPoint()).isZero();
        assertThat(updatedCreator.getPoint()).isZero();

        assertThat(pledgeRepository.findByProject(settled))
                .allSatisfy(pledge -> assertThat(pledge.getStatus()).isEqualTo(PledgeStatus.FAILED));
    }

    @Test
    @DisplayName("[하네스] 모금액이 목표액과 정확히 같으면 성공으로 정산된다")
    void settlementSucceedsWhenAmountEqualsTarget() {
        Project project = fixture.createOngoingProject(creator, category, "경계값 펀딩", 500_000L);
        pledgeService.createPledge(supporter.getId(), project.getId(), 500_000L);
        fixture.closeProject(project.getId());

        settlementService.settleSingleProject(project.getId());

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectStatus.SUCCESS);
    }

    @Test
    @DisplayName("[하네스] 정산 배치를 두 번 실행해도 두 번째는 아무것도 바꾸지 않는다")
    void settlementBatchIsIdempotent() {
        Project project = fixture.createOngoingProject(creator, category, "멱등성 검증 펀딩", 500_000L);
        pledgeService.createPledge(supporter.getId(), project.getId(), 600_000L);
        fixture.closeProject(project.getId());

        int firstRun = settlementScheduler.run();
        long afterFirst = totalSystemPoints();

        int secondRun = settlementScheduler.run();
        long afterSecond = totalSystemPoints();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero(); // 이미 SUCCESS 라 대상에서 빠진다
        assertThat(afterSecond).isEqualTo(afterFirst); // I6
        assertThat(customerRepository.findById(creator.getId()).orElseThrow().getPoint())
                .isEqualTo(600_000L);
    }

    @Test
    @DisplayName("[하네스] 삭제된 프로젝트는 정산 배치 대상에서 제외된다")
    void deletedProjectIsExcludedFromSettlement() {
        Project project = fixture.createOngoingProject(creator, category, "삭제될 펀딩", 500_000L);
        pledgeService.createPledge(supporter.getId(), project.getId(), 600_000L);
        fixture.closeProject(project.getId());

        // 창작자가 마감 후 삭제한 상황을 가정한다.
        projectRepository.findById(project.getId()).ifPresent(p -> {
            p.softDelete();
            projectRepository.save(p);
        });

        assertThat(settlementService.findSettlementTargetIds()).doesNotContain(project.getId());
    }

    private long totalSystemPoints() {
        return customerRepository.findAll().stream().mapToLong(Customer::getPoint).sum();
    }
}
