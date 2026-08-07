package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.dto.ProjectDtos;
import com.skala.fund.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category", description = "카테고리 목록")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @Operation(summary = "카테고리 목록", description = "displayOrder 오름차순 20종")
    @Transactional(readOnly = true)
    @GetMapping
    public ApiResponse<List<ProjectDtos.CategoryResponse>> list() {
        return ApiResponse.success(categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(ProjectDtos.CategoryResponse::from)
                .toList());
    }
}
