package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.dto.ProjectDtos;
import com.skala.fund.repository.CategoryRepository;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectLikeRepository;
import com.skala.fund.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int POPULAR_SIZE = 5;
    private static final int MIN_FUNDING_DAYS = 7;

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final PledgeRepository pledgeRepository;
    private final ProjectLikeRepository projectLikeRepository;

    @Transactional(readOnly = true)
    public Page<ProjectDtos.ProjectResponse> search(Long categoryId, String keyword, Long viewerId,
                                                    Pageable pageable) {
        // 키워드는 여기서 공백 제거 + 소문자로 정규화한다. searchTitle 컬럼이 같은 규칙으로 저장돼 있다.
        String normalized = normalizeKeyword(keyword);
        Page<Project> page = projectRepository.searchProjects(categoryId, normalized, pageable);
        Set<Long> liked = findLikedIds(viewerId, page.getContent());
        return page.map(project -> ProjectDtos.ProjectResponse.from(project, liked.contains(project.getId())));
    }

    /** 인기 프로젝트 5개. 후원액 합계 내림차순. */
    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> findPopular(Long viewerId) {
        List<Project> projects = projectRepository.findPopular(PageRequest.of(0, POPULAR_SIZE));
        return toResponses(projects, viewerId);
    }

    /** 최근 본 펀딩 일괄 조회. 없거나 삭제된 ID 는 조용히 빠진다. 404 로 전체를 실패시키지 않는다. */
    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> findAllByIds(List<Long> ids, Long viewerId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return toResponses(projectRepository.findAllByIds(ids), viewerId);
    }

    private List<ProjectDtos.ProjectResponse> toResponses(List<Project> projects, Long viewerId) {
        Set<Long> liked = findLikedIds(viewerId, projects);
        return projects.stream()
                .map(project -> ProjectDtos.ProjectResponse.from(project, liked.contains(project.getId())))
                .toList();
    }

    /**
     * 목록에 실린 프로젝트들의 찜 여부를 한 번의 쿼리로 받아온다.
     * 카드마다 exists 를 날리면 N+1 이 된다. 비로그인 요청은 쿼리 없이 빈 집합이다.
     */
    private Set<Long> findLikedIds(Long viewerId, List<Project> projects) {
        if (viewerId == null || projects.isEmpty()) {
            return Set.of();
        }
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        return Set.copyOf(projectLikeRepository.findLikedProjectIds(viewerId, projectIds));
    }

    /** 상세. 삭제된 프로젝트는 404 다. */
    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse findDetail(Long projectId, Long viewerId) {
        Project project = projectRepository.findDetailById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        boolean liked = false;
        if (viewerId != null) {
            liked = customerRepository.findById(viewerId)
                    .map(viewer -> projectLikeRepository.existsByCustomerAndProject(viewer, project))
                    .orElse(false);
        }
        return ProjectDtos.ProjectResponse.from(project, liked);
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(Long creatorId, ProjectDtos.ProjectSaveRequest request) {
        validatePeriod(request.startDate(), request.endDate());

        Customer creator = customerRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        // 시작일이 이미 지났으면 곧바로 진행 중으로 만든다. 아니면 상태 전이 배치가 처리한다.
        ProjectStatus initialStatus = request.startDate().isAfter(LocalDate.now())
                ? ProjectStatus.SCHEDULED
                : ProjectStatus.ONGOING;

        Project project = projectRepository.save(Project.builder()
                .creator(creator)
                .category(category)
                .title(request.title())
                .description(request.description())
                .mainImage(request.mainImage())
                .targetAmount(request.targetAmount())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(initialStatus)
                .build());

        log.info("프로젝트 등록 - projectId={}, creatorId={}, status={}",
                project.getId(), creatorId, initialStatus);
        return ProjectDtos.ProjectResponse.from(project);
    }

    /**
     * 수정. 후원자가 있어도 창작자 본인이면 허용한다.
     * 다만 목표 금액·마감일 변경은 이미 후원한 사람에게 영향이 가므로 변경 이력을 로그로 남긴다.
     */
    @Transactional
    public ProjectDtos.ProjectResponse update(Long requesterId, Long projectId,
                                              ProjectDtos.ProjectSaveRequest request) {
        validatePeriod(request.startDate(), request.endDate());

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        validateOwner(project, requesterId);

        if (project.getStatus() == ProjectStatus.SUCCESS || project.getStatus() == ProjectStatus.FAILED) {
            throw new CustomException(ErrorCode.PROJECT_CLOSED);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!project.getTargetAmount().equals(request.targetAmount())
                || !project.getEndDate().equals(request.endDate())) {
            log.info("프로젝트 핵심 조건 변경 - projectId={}, requesterId={}, 후원 건수={}, "
                            + "목표액 {} -> {}, 마감일 {} -> {}",
                    projectId, requesterId, project.getPledgeCount(),
                    project.getTargetAmount(), request.targetAmount(),
                    project.getEndDate(), request.endDate());
        }

        project.update(category, request.title(), request.description(), request.mainImage(),
                request.targetAmount(), request.startDate(), request.endDate());

        return ProjectDtos.ProjectResponse.from(project);
    }

    /**
     * Soft Delete. 물리 삭제하지 않는다.
     * PLEDGED 후원을 CANCELLED 로 정리하면서 각 후원자의 예약 포인트를 해제한다.
     * 포인트가 차감된 적이 없으므로 환급 처리는 하지 않는다.
     */
    @Transactional
    public ProjectDtos.ProjectDeleteResponse delete(Long requesterId, Long projectId) {
        // 잠금 순서는 후원 경로와 동일하게 Project -> Customer 다.
        Project project = projectRepository.findByIdWithLock(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        validateOwner(project, requesterId);

        List<Pledge> pledged = pledgeRepository.findByProjectAndStatus(project, PledgeStatus.PLEDGED);

        // 데드락 방지를 위해 회원 id 오름차순으로 잠근다.
        pledged.stream()
                .sorted(java.util.Comparator.comparing(p -> p.getCustomer().getId()))
                .forEach(pledge -> {
                    Customer customer = customerRepository.findByIdWithLock(pledge.getCustomer().getId())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    customer.releaseReservedPoint(pledge.getAmount());
                    project.removePledgeAmount(pledge.getAmount());
                    pledge.cancel();
                });

        project.softDelete();
        log.info("프로젝트 삭제(soft) - projectId={}, requesterId={}, 취소된 후원 {}건",
                projectId, requesterId, pledged.size());

        return new ProjectDtos.ProjectDeleteResponse(projectId, pledged.size());
    }

    @Transactional(readOnly = true)
    public Page<ProjectDtos.ProjectResponse> findMyProjects(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return projectRepository.findMyProjects(customer, pageable)
                .map(ProjectDtos.ProjectResponse::from);
    }

    private void validateOwner(Project project, Long requesterId) {
        if (!project.getCreator().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_PROJECT_OWNER);
        }
    }

    /** 마감일은 시작일로부터 최소 7일 이후여야 한다. */
    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate.plusDays(MIN_FUNDING_DAYS))) {
            throw new CustomException(ErrorCode.INVALID_PROJECT_PERIOD);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.replaceAll("\\s+", "").toLowerCase();
    }
}
