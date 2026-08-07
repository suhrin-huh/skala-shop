package com.skala.fund.harness;

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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 하네스 테스트용 데이터 준비 헬퍼.
 *
 * 하네스 테스트 자체에는 @Transactional 을 붙이지 않는다.
 * 테스트 트랜잭션이 걸리면 스레드마다 트랜잭션이 분리되지 않아 비관적 락이 의미를 잃고,
 * 롤백 때문에 최종 상태도 확인할 수 없다. 그래서 준비/정리를 이 빈이 대신 맡는다.
 */
@TestComponent
@RequiredArgsConstructor
public class HarnessFixture {

    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final ProjectLikeRepository projectLikeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void clearAll() {
        refreshTokenRepository.deleteAllInBatch();
        projectLikeRepository.deleteAllInBatch();
        pledgeRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Transactional
    public Category createCategory(String name) {
        return categoryRepository.save(Category.builder().name(name).displayOrder(1).build());
    }

    @Transactional
    public Customer createCustomer(String email, String nickname, long point) {
        return customerRepository.save(Customer.builder()
                .email(email)
                .nickname(nickname)
                .password("encoded-password")
                .point(point)
                .build());
    }

    @Transactional
    public Project createOngoingProject(Customer creator, Category category, String title, long targetAmount) {
        return projectRepository.save(Project.builder()
                .creator(creator)
                .category(category)
                .title(title)
                .description("하네스 검증용 프로젝트 설명입니다. 최소 20자를 넘겨야 합니다.")
                .mainImage("http://localhost/image.png")
                .targetAmount(targetAmount)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .status(ProjectStatus.ONGOING)
                .build());
    }

    /**
     * 후원을 받은 뒤 마감일을 어제로 되돌려 정산 대상으로 만든다.
     * 후원은 마감 전에만 가능하므로 "이미 마감된 프로젝트"를 직접 만들어놓고 후원할 수는 없다.
     */
    @Transactional
    public void closeProject(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        project.update(project.getCategory(), project.getTitle(), project.getDescription(),
                project.getMainImage(), project.getTargetAmount(),
                project.getStartDate(), LocalDate.now().minusDays(1));
    }
}
