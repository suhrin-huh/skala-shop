package com.skala.fund.dto;

import com.skala.fund.domain.DeliveryStatus;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PledgeDtos {

    public record PledgeCreateRequest(
            @NotNull(message = "프로젝트 ID는 필수입니다.")
            Long projectId,

            @NotNull(message = "후원 금액은 필수입니다.")
            @Min(value = 1_000, message = "최소 후원 금액은 1,000원 이상이어야 합니다.")
            Long amount
    ) {}

    public record DeliveryStatusUpdateRequest(
            @NotNull(message = "배송 상태는 필수입니다.")
            DeliveryStatus deliveryStatus
    ) {}

    /**
     * 마이페이지 후원 내역용 프로젝트 요약.
     * 삭제된 프로젝트도 내려가야 하므로 projectDeleted 플래그를 함께 준다.
     * 프론트는 이 플래그로 "삭제된 프로젝트" 배지를 붙이고 상세 링크를 비활성화한다.
     */
    public record PledgedProjectSummary(
            Long id,
            String title,
            String mainImage,
            ProjectStatus status,
            LocalDate endDate
    ) {
        public static PledgedProjectSummary from(Project project) {
            return new PledgedProjectSummary(
                    project.getId(),
                    project.getTitle(),
                    project.getMainImage(),
                    project.getStatus(),
                    project.getEndDate()
            );
        }
    }

    public record PledgeResponse(
            Long id,
            Long amount,
            PledgeStatus status,
            DeliveryStatus deliveryStatus,
            LocalDateTime createdAt,
            PledgedProjectSummary project,
            boolean projectDeleted
    ) {
        public static PledgeResponse from(Pledge pledge) {
            Project project = pledge.getProject();
            return new PledgeResponse(
                    pledge.getId(),
                    pledge.getAmount(),
                    pledge.getStatus(),
                    pledge.getDeliveryStatus(),
                    pledge.getCreatedAt(),
                    PledgedProjectSummary.from(project),
                    project.isDeleted()
            );
        }
    }

    /** 창작자가 자기 프로젝트의 후원자별 배송 상태를 관리할 때 쓰는 응답. */
    public record BackerResponse(
            Long pledgeId,
            Long customerId,
            String nickname,
            Long amount,
            DeliveryStatus deliveryStatus
    ) {
        public static BackerResponse from(Pledge pledge) {
            return new BackerResponse(
                    pledge.getId(),
                    pledge.getCustomer().getId(),
                    pledge.getCustomer().getNickname(),
                    pledge.getAmount(),
                    pledge.getDeliveryStatus()
            );
        }
    }
}
