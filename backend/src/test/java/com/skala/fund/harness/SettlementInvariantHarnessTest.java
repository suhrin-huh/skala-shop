package com.skala.fund.harness;

import com.skala.fund.domain.*;
import com.skala.fund.repository.*;
import com.skala.fund.service.PledgeService;
import com.skala.fund.service.SettlementBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class SettlementInvariantHarnessTest {

    @Autowired
    private SettlementBatchService settlementBatchService;

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

    private Customer creator;
    private Customer supporter;
    private Project successProject;
    private Project failProject;

    @BeforeEach
    void setUp() {
        pledgeRepository.deleteAll();
        projectRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(new Category("디자인 문구", 1));

        creator = customerRepository.save(Customer.builder()
                .email("creator@test.com")
                .nickname("창작자")
                .password("password123!")
                .point(0L)
                .build());

        supporter = customerRepository.save(Customer.builder()
                .email("supporter@test.com")
                .nickname("후원자")
                .password("password123!")
                .point(1_000_000L)
                .build());

        // 마감일이 어제인 성공 대상 프로젝트 (목표액 50만)
        successProject = projectRepository.save(Project.builder()
                .creator(creator)
                .category(category)
                .title("성공 예정 펀딩")
                .description("설명")
                .mainImage("http://localhost/image.png")
                .targetAmount(500_000L)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .status(ProjectStatus.ONGOING)
                .build());

        // 마감일이 어제인 실패 대상 프로젝트 (목표액 50만)
        failProject = projectRepository.save(Project.builder()
                .creator(creator)
                .category(category)
                .title("실패 예정 펀딩")
                .description("설명")
                .mainImage("http://localhost/image.png")
                .targetAmount(500_000L)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .status(ProjectStatus.ONGOING)
                .build());
    }

    @Test
    @DisplayName("[하네스 검증] 펀딩 성공 정산 시 포인트 차감/지급 일치 및 총량 보존 법칙 검증")
    void testSuccessfulSettlementInvariant() {
        // 60만 원 후원 (목표 50만 달성)
        pledgeService.createPledge(supporter.getId(), successProject.getId(), 600_000L);

        long initialTotalPoints = getTotalSystemPoints();

        // 정산 실행
        settlementBatchService.settleSingleProject(successProject.getId());

        Customer updatedSupporter = customerRepository.findById(supporter.getId()).orElseThrow();
        Customer updatedCreator = customerRepository.findById(creator.getId()).orElseThrow();
        Project updatedProject = projectRepository.findById(successProject.getId()).orElseThrow();

        assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.SUCCESS);
        assertThat(updatedSupporter.getPoint()).isEqualTo(400_000L);
        assertThat(updatedSupporter.getReservedPoint()).isEqualTo(0L);
        assertThat(updatedCreator.getPoint()).isEqualTo(600_000L);

        // 총 포인트 불변 보존 법칙
        long finalTotalPoints = getTotalSystemPoints();
        assertThat(finalTotalPoints).isEqualTo(initialTotalPoints);
    }

    @Test
    @DisplayName("[하네스 검증] 펀딩 무산 정산 시 예약 포인트 전액 해제 및 차감 없음 검증")
    void testFailedSettlementInvariant() {
        // 20만 원 후원 (목표 50만 미달)
        pledgeService.createPledge(supporter.getId(), failProject.getId(), 200_000L);

        long initialTotalPoints = getTotalSystemPoints();

        // 정산 실행
        settlementBatchService.settleSingleProject(failProject.getId());

        Customer updatedSupporter = customerRepository.findById(supporter.getId()).orElseThrow();
        Customer updatedCreator = customerRepository.findById(creator.getId()).orElseThrow();
        Project updatedProject = projectRepository.findById(failProject.getId()).orElseThrow();

        assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.FAILED);
        assertThat(updatedSupporter.getPoint()).isEqualTo(1_000_000L);
        assertThat(updatedSupporter.getReservedPoint()).isEqualTo(0L);
        assertThat(updatedCreator.getPoint()).isEqualTo(0L);

        long finalTotalPoints = getTotalSystemPoints();
        assertThat(finalTotalPoints).isEqualTo(initialTotalPoints);
    }

    private long getTotalSystemPoints() {
        return customerRepository.findAll().stream()
                .mapToLong(c -> c.getPoint())
                .sum();
    }
}
