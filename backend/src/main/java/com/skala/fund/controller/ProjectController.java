package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.dto.ProjectDtos;
import com.skala.fund.service.ProjectLikeService;
import com.skala.fund.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Project", description = "프로젝트 목록 / 상세 / 등록 / 수정 / 삭제 / 찜")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectLikeService projectLikeService;

    @Operation(summary = "프로젝트 목록", description = "카테고리 필터 + 키워드 검색 + 정렬 + 페이징. "
            + "ids 파라미터를 주면 최근 본 펀딩 일괄 조회로 동작한다.")
    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> ids,
            @AuthenticationPrincipal Long customerId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (ids != null && !ids.isEmpty()) {
            return ApiResponse.success(projectService.findAllByIds(ids, customerId));
        }
        Page<ProjectDtos.ProjectResponse> page = projectService.search(categoryId, keyword, customerId, pageable);
        return ApiResponse.success(page);
    }

    @Operation(summary = "인기 프로젝트 5개", description = "후원액 합계 내림차순 상위 5개")
    @GetMapping("/popular")
    public ApiResponse<List<ProjectDtos.ProjectResponse>> popular(
            @AuthenticationPrincipal Long customerId) {
        return ApiResponse.success(projectService.findPopular(customerId));
    }

    @Operation(summary = "홈 배너용 최근 프로젝트 3개", description = "등록일 내림차순 상위 3개")
    @GetMapping("/banners")
    public ApiResponse<List<ProjectDtos.ProjectResponse>> banners(
            @AuthenticationPrincipal Long customerId) {
        return ApiResponse.success(projectService.findRecent(customerId));
    }

    @Operation(summary = "프로젝트 상세", description = "삭제된 프로젝트는 404 로 응답한다.")
    @GetMapping("/{id}")
    public ApiResponse<ProjectDtos.ProjectResponse> detail(@PathVariable Long id,
                                                           @AuthenticationPrincipal Long customerId) {
        return ApiResponse.success(projectService.findDetail(id, customerId));
    }

    @Operation(summary = "프로젝트 등록")
    @PostMapping
    public ApiResponse<ProjectDtos.ProjectResponse> create(@AuthenticationPrincipal Long customerId,
                                                           @Valid @RequestBody ProjectDtos.ProjectSaveRequest request) {
        return ApiResponse.success(projectService.create(customerId, request));
    }

    @Operation(summary = "프로젝트 수정", description = "창작자 본인만 가능. 후원자 유무와 무관하게 허용된다.")
    @PutMapping("/{id}")
    public ApiResponse<ProjectDtos.ProjectResponse> update(@AuthenticationPrincipal Long customerId,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ProjectDtos.ProjectSaveRequest request) {
        return ApiResponse.success(projectService.update(customerId, id, request));
    }

    @Operation(summary = "프로젝트 삭제", description = "Soft Delete. PLEDGED 후원을 CANCELLED 로 정리한다.")
    @DeleteMapping("/{id}")
    public ApiResponse<ProjectDtos.ProjectDeleteResponse> delete(@AuthenticationPrincipal Long customerId,
                                                                 @PathVariable Long id) {
        return ApiResponse.success(projectService.delete(customerId, id));
    }

    @Operation(summary = "찜 등록")
    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@AuthenticationPrincipal Long customerId, @PathVariable Long id) {
        projectLikeService.like(customerId, id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "찜 해제")
    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlike(@AuthenticationPrincipal Long customerId, @PathVariable Long id) {
        projectLikeService.unlike(customerId, id);
        return ApiResponse.success(null);
    }
}
