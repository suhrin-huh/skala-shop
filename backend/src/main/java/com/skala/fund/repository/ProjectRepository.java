package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    List<Project> findTop5ByDeletedAtIsNullOrderByCurrentAmountDesc();

    Page<Project> findByCreatorAndDeletedAtIsNull(Customer creator, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:searchKeyword IS NULL OR p.searchTitle LIKE CONCAT('%', :searchKeyword, '%'))")
    Page<Project> searchProjects(@Param("categoryId") Long categoryId,
                                 @Param("searchKeyword") String searchKeyword,
                                 Pageable pageable);

    List<Project> findByStatusAndStartDateLessThanEqualAndDeletedAtIsNull(ProjectStatus status, LocalDate date);

    List<Project> findByStatusAndEndDateLessThanAndDeletedAtIsNull(ProjectStatus status, LocalDate date);
}
