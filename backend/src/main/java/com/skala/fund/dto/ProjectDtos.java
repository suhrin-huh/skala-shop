package com.skala.fund.dto;

import com.skala.fund.domain.ProjectStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ProjectDtos {

    public record ProjectCreateRequest(
            @NotBlank @Size(min = 5, max = 50) String title,
            @NotNull Long categoryId,
            @NotBlank @Size(min = 20) String description,
            @NotBlank String mainImage,
            @NotNull @Min(100000) Long targetAmount,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record ProjectResponse(
            Long id,
            AuthDtos.UserSummary creator,
            Long categoryId,
            String categoryName,
            String title,
            String description,
            String mainImage,
            Long targetAmount,
            Long currentAmount,
            Long pledgeCount,
            LocalDate startDate,
            LocalDate endDate,
            ProjectStatus status
    ) {}
}
