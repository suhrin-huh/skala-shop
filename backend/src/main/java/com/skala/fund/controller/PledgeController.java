package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.dto.PledgeDtos;
import com.skala.fund.service.PledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pledge", description = "후원 / 후원 취소 / 배송 상태 변경")
@RestController
@RequestMapping("/api/pledges")
@RequiredArgsConstructor
public class PledgeController {

    private final PledgeService pledgeService;

    @Operation(summary = "후원하기",
            description = "지금 결제되지 않는다. 사용 가능 포인트만 예약하고 마감일에 정산 배치가 결제한다.")
    @PostMapping
    public ApiResponse<PledgeDtos.PledgeResponse> create(@AuthenticationPrincipal Long customerId,
                                                          @Valid @RequestBody PledgeDtos.PledgeCreateRequest request) {
        return ApiResponse.success(PledgeDtos.PledgeResponse.from(
                pledgeService.createPledge(customerId, request.projectId(), request.amount())));
    }

    @Operation(summary = "후원 취소", description = "마감 전에만 가능. 포인트 조작 없이 예약만 해제된다.")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Long customerId, @PathVariable Long id) {
        pledgeService.cancelPledge(customerId, id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "배송 상태 변경", description = "해당 프로젝트 창작자만 가능. 상태 역행 불가.")
    @PatchMapping("/{id}/delivery")
    public ApiResponse<PledgeDtos.BackerResponse> updateDelivery(
            @AuthenticationPrincipal Long customerId,
            @PathVariable Long id,
            @Valid @RequestBody PledgeDtos.DeliveryStatusUpdateRequest request) {
        return ApiResponse.success(
                pledgeService.updateDeliveryStatus(customerId, id, request.deliveryStatus()));
    }

    @Operation(summary = "내 프로젝트 후원자 목록", description = "결제 완료된 후원자만 조회된다. 창작자 전용.")
    @GetMapping("/backers")
    public ApiResponse<List<PledgeDtos.BackerResponse>> backers(@AuthenticationPrincipal Long customerId,
                                                                @RequestParam Long projectId) {
        return ApiResponse.success(pledgeService.findBackers(customerId, projectId));
    }
}
