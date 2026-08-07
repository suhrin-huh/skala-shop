package com.skala.fund.harness.controller;

import com.skala.fund.common.ApiResponse;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CategoryRepository;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.service.SettlementBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Profile({"dev", "local"})
@RestController
@RequestMapping("/api/harness")
@RequiredArgsConstructor
public class HarnessSimulatorController {

    private final SettlementBatchService settlementBatchService;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;

    @PostMapping("/batch/run-status-transition")
    public ApiResponse<String> runStatusTransitionBatch() {
        int count = settlementBatchService.runStatusTransitionBatch();
        return ApiResponse.success("상태 전이 배치 실행 완료. 처리 건수: " + count);
    }

    @PostMapping("/batch/run-settlement")
    public ApiResponse<String> runSettlementBatch() {
        int count = settlementBatchService.runSettlementBatch();
        return ApiResponse.success("정산 배치 실행 완료. 처리 건수: " + count);
    }

    @PostMapping("/seed")
    public ApiResponse<String> seedData() {
        pledgeRepository.deleteAll();
        projectRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. 카테고리 20종 세딩
        List<String> categoryNames = Arrays.asList(
                "디자인 문구", "푸드", "출판", "영화·비디오", "보드게임·TRPG",
                "캐릭터·굿즈", "향수·뷰티", "디자인·일러스트", "공연", "홈·리빙",
                "의류", "문화·예술", "웹툰·만화", "테크·가전", "잡화",
                "사진", "웹툰 리소스", "반려동물", "주얼리", "음악"
        );

        int order = 1;
        for (String name : categoryNames) {
            categoryRepository.save(new Category(name, order++));
        }

        Category techCategory = categoryRepository.findAllByOrderByDisplayOrderAsc().get(13); // 테크·가전
        Category designCategory = categoryRepository.findAllByOrderByDisplayOrderAsc().get(0); // 디자인 문구
        Category gameCategory = categoryRepository.findAllByOrderByDisplayOrderAsc().get(4); // 보드게임

        // 2. 가상 창작자 & 후원자
        Customer creator = customerRepository.save(Customer.builder()
                .email("creator@skala.com")
                .nickname("스칼라 스튜디오")
                .password("$2a$10$e8wB.z4mX5fW1l8K5q1H.eG8.z4mX5fW1l8K5q1H")
                .point(100_000L)
                .build());

        Customer supporter = customerRepository.save(Customer.builder()
                .email("test@skala.com")
                .nickname("테스트 후원자")
                .password("$2a$10$e8wB.z4mX5fW1l8K5q1H.eG8.z4mX5fW1l8K5q1H")
                .point(1_000_000L)
                .build());

        // 3. 샘플 프로젝트들 생성
        Project p1 = Project.builder()
                .creator(creator)
                .category(techCategory)
                .title("스마트 레트로 기계식 키보드")
                .description("클래식한 타자기 감성과 현대적인 무선 성능이 결합된 커스텀 기계식 키보드입니다.")
                .mainImage("https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80")
                .targetAmount(5_000_000L)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(20))
                .status(ProjectStatus.ONGOING)
                .build();
        p1.addPledgeAmount(21_600_000L); // 432% 달성
        projectRepository.save(p1);

        Project p2 = Project.builder()
                .creator(creator)
                .category(designCategory)
                .title("2027 다이어리 & 모듈러 플래너 패키지")
                .description("당신의 일상을 체계적이고 아름답게 정리해 줄 커스텀 모듈러 만년 다이어리 세트.")
                .mainImage("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80")
                .targetAmount(1_000_000L)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(15))
                .status(ProjectStatus.ONGOING)
                .build();
        p2.addPledgeAmount(3_200_000L); // 320% 달성
        projectRepository.save(p2);

        Project p3 = Project.builder()
                .creator(creator)
                .category(gameCategory)
                .title("신화 속 세계관 TRPG 룰북 & 다이스 세트")
                .description("고대 신화에서 영감을 얻은 오리지널 스토리 테일링 TRPG 시스템과 핸드메이드 원석 다이스.")
                .mainImage("https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&w=800&q=80")
                .targetAmount(3_000_000L)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(5))
                .status(ProjectStatus.ONGOING)
                .build();
        p3.addPledgeAmount(8_900_000L); // 296% 달성
        projectRepository.save(p3);

        return ApiResponse.success("테스트 시딩 완료! 카테고리 20개, 샘플 프로젝트 3개, 계정 2개가 생성되었습니다.");
    }
}
