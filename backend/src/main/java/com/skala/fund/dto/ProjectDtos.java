package com.skala.fund.dto;

import com.skala.fund.domain.Category;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProjectDtos {

    public record CategoryResponse(Long id, String name, Integer displayOrder) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.getDisplayOrder());
        }
    }

    public record ProjectSaveRequest(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(min = 5, max = 50, message = "제목은 5자 이상 50자 이하이어야 합니다.")
            String title,

            @NotNull(message = "카테고리는 필수입니다.")
            Long categoryId,

            @NotBlank(message = "설명은 필수입니다.")
            @Size(min = 20, message = "설명은 20자 이상이어야 합니다.")
            String description,

            @NotBlank(message = "대표 이미지는 필수입니다.")
            String mainImage,

            @NotNull(message = "목표 금액은 필수입니다.")
            @Min(value = 100_000, message = "목표 금액은 100,000원 이상이어야 합니다.")
            Long targetAmount,

            @NotNull(message = "시작일은 필수입니다.")
            LocalDate startDate,

            @NotNull(message = "마감일은 필수입니다.")
            LocalDate endDate
    ) {}

    /**
     * 목록·상세 공통 응답.
     * 프론트가 project.category.name / project.creator.nickname 형태로 읽으므로 중첩 객체로 내려준다.
     * currentAmount, pledgeCount 는 비정규화 컬럼을 그대로 쓴다.
     */
    public record ProjectResponse(
            Long id,
            AuthDtos.CreatorSummary creator,
            CategoryResponse category,
            String title,
            String description,
            String mainImage,
            Long targetAmount,
            Long currentAmount,
            Long pledgeCount,
            Integer achievementRate,
            LocalDate startDate,
            LocalDate endDate,
            ProjectStatus status,
            Boolean liked
    ) {
        public static ProjectResponse from(Project project) {
            return from(project, false);
        }

        public static ProjectResponse from(Project project, boolean liked) {
            return new ProjectResponse(
                    project.getId(),
                    AuthDtos.CreatorSummary.from(project.getCreator()),
                    CategoryResponse.from(project.getCategory()),
                    project.getTitle(),
                    project.getDescription(),
                    project.getMainImage(),
                    project.getTargetAmount(),
                    project.getCurrentAmount(),
                    project.getPledgeCount(),
                    calculateRate(project),
                    project.getStartDate(),
                    project.getEndDate(),
                    project.getStatus(),
                    liked
            );
        }

        /** 달성률은 100% 를 넘어도 클램프하지 않는다. 진행바만 클램프하고 수치는 실제값을 보여준다. */
        private static int calculateRate(Project project) {
            if (project.getTargetAmount() == null || project.getTargetAmount() == 0L) {
                return 0;
            }
            return (int) (project.getCurrentAmount() * 100 / project.getTargetAmount());
        }
    }

    /** 프로젝트 삭제 시 확인 모달에 후원자 수를 보여주기 위한 응답. */
    public record ProjectDeleteResponse(Long projectId, long cancelledPledgeCount) {}
}
