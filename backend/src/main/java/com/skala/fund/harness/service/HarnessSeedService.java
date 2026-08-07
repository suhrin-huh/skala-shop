package com.skala.fund.harness.service;

import com.skala.fund.common.util.CategoryCatalog;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CategoryRepository;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectLikeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.repository.RefreshTokenRepository;
import com.skala.fund.service.PledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 하네스 시뮬레이션용 샘플 데이터 세딩.
 *
 * 정산 배치를 눈으로 검증하려면 "이미 마감됐지만 아직 정산되지 않은" 프로젝트가 필요하다.
 * 후원은 마감 전에만 가능하므로, 후원을 먼저 만들고 마감일을 과거로 되돌리는 순서로 만든다.
 */
@Slf4j
@Profile({"dev", "local"})
@Service
@RequiredArgsConstructor
public class HarnessSeedService {

    private static final String SEED_PASSWORD = "skala123!";

    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final ProjectLikeRepository projectLikeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PledgeService pledgeService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String seed() {
        clearAll();

        List<Category> categories = seedCategories();
        Customer creator = saveCustomer("creator@skala.com", "스칼라 스튜디오", 100_000L);
        Customer supporter = saveCustomer("test@skala.com", "테스트 후원자", 1_000_000L);

        List<Project> ongoing = seedOngoingProjects(creator, categories);
        Project settlementTarget = seedSettlementTarget(creator, categories.get(0));

        // 정산 배치가 즉시 처리할 수 있도록 마감된 프로젝트에 목표 초과 후원을 붙여둔다.
        pledgeService.createPledge(supporter.getId(), settlementTarget.getId(), 300_000L);
        closeProject(settlementTarget);

        String message = String.format(
                "시딩 완료 - 카테고리 %d개, 계정 2개(creator@skala.com / test@skala.com, 비밀번호 %s), "
                        + "진행중 프로젝트 %d개, 정산 대기 프로젝트 1개",
                categories.size(), SEED_PASSWORD, ongoing.size());
        log.info(message);
        return message;
    }

    private void clearAll() {
        // FK 역순으로 지운다.
        refreshTokenRepository.deleteAllInBatch();
        projectLikeRepository.deleteAllInBatch();
        pledgeRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    private List<Category> seedCategories() {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < CategoryCatalog.NAMES.size(); i++) {
            categories.add(Category.builder()
                    .name(CategoryCatalog.NAMES.get(i))
                    .displayOrder(i + 1)
                    .build());
        }
        return categoryRepository.saveAll(categories);
    }

    private Customer saveCustomer(String email, String nickname, long point) {
        return customerRepository.save(Customer.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .point(point)
                .build());
    }

    private List<Project> seedOngoingProjects(Customer creator, List<Category> categories) {
        List<Project> projects = List.of(
                buildProject(creator, categories.get(13), "스마트 레트로 기계식 키보드",
                        "클래식한 타자기 감성과 현대적인 무선 성능이 결합된 커스텀 기계식 키보드입니다.",
                        "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80",
                        5_000_000L, 5, 20),
                buildProject(creator, categories.get(0), "2027 다이어리 & 모듈러 플래너 패키지",
                        "당신의 일상을 체계적이고 아름답게 정리해 줄 커스텀 모듈러 만년 다이어리 세트.",
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80",
                        1_000_000L, 2, 15),
                buildProject(creator, categories.get(4), "신화 속 세계관 TRPG 룰북 & 다이스 세트",
                        "고대 신화에서 영감을 얻은 오리지널 스토리텔링 TRPG 시스템과 핸드메이드 원석 다이스.",
                        "https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&w=800&q=80",
                        3_000_000L, 10, 5)
        );
        List<Project> saved = projectRepository.saveAll(projects);

        // 인기 정렬과 달성률 표시를 확인하기 위한 모금액. 실제 Pledge 없이 비정규화 값만
        // 올리면 하네스 불변식(I3/I4)이 깨지므로, 데모용 수치는 여기서 조작하지 않는다.
        return saved;
    }

    private Project seedSettlementTarget(Customer creator, Category category) {
        return projectRepository.save(buildProject(creator, category, "정산 대기 데모 프로젝트",
                "하네스 정산 배치가 즉시 처리할 수 있도록 마감 직전 상태로 준비된 프로젝트입니다.",
                "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?auto=format&fit=crop&w=800&q=80",
                200_000L, 10, 0));
    }

    private Project buildProject(Customer creator, Category category, String title, String description,
                                 String mainImage, long targetAmount, int startedDaysAgo, int endsInDays) {
        return Project.builder()
                .creator(creator)
                .category(category)
                .title(title)
                .description(description)
                .mainImage(mainImage)
                .targetAmount(targetAmount)
                .startDate(LocalDate.now().minusDays(startedDaysAgo))
                .endDate(LocalDate.now().plusDays(endsInDays))
                .status(ProjectStatus.ONGOING)
                .build();
    }

    /** 후원을 받은 뒤 마감일을 어제로 되돌려 정산 대상으로 만든다. */
    private void closeProject(Project project) {
        project.update(project.getCategory(), project.getTitle(), project.getDescription(),
                project.getMainImage(), project.getTargetAmount(),
                project.getStartDate(), LocalDate.now().minusDays(1));
    }
}
