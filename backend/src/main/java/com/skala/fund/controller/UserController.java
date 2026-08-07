package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.dto.AuthDtos;
import com.skala.fund.dto.PledgeDtos;
import com.skala.fund.dto.ProjectDtos;
import com.skala.fund.service.CustomerService;
import com.skala.fund.service.ProjectLikeService;
import com.skala.fund.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "프로필 / 후원 내역 / 찜 목록 / 내가 등록한 프로젝트")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final CustomerService customerService;
    private final ProjectService projectService;
    private final ProjectLikeService projectLikeService;

    @Operation(summary = "내 프로필", description = "보유 포인트 + 예약액 + 사용 가능 포인트를 함께 내려준다.")
    @GetMapping
    public ApiResponse<AuthDtos.UserSummary> me(@AuthenticationPrincipal Long customerId) {
        return ApiResponse.success(customerService.getMyProfile(customerId));
    }

    @Operation(summary = "내 후원 내역",
            description = "삭제된 프로젝트의 후원도 포함하며 projectDeleted 플래그로 구분한다.")
    @GetMapping("/pledges")
    public ApiResponse<Page<PledgeDtos.PledgeResponse>> pledges(
            @AuthenticationPrincipal Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(customerService.getMyPledges(customerId, pageable));
    }

    @Operation(summary = "내 찜 목록", description = "삭제된 프로젝트는 제외된다.")
    @GetMapping("/likes")
    public ApiResponse<Page<ProjectDtos.ProjectResponse>> likes(
            @AuthenticationPrincipal Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(projectLikeService.findMyLikes(customerId, pageable));
    }

    @Operation(summary = "내가 등록한 프로젝트", description = "삭제한 프로젝트는 제외된다.")
    @GetMapping("/projects")
    public ApiResponse<Page<ProjectDtos.ProjectResponse>> myProjects(
            @AuthenticationPrincipal Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(projectService.findMyProjects(customerId, pageable));
    }
}
