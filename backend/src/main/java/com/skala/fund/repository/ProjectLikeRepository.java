package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectLikeRepository extends JpaRepository<ProjectLike, Long> {

    Optional<ProjectLike> findByCustomerAndProject(Customer customer, Project project);

    boolean existsByCustomerAndProject(Customer customer, Project project);

    Page<ProjectLike> findByCustomerOrderByCreatedAtDesc(Customer customer, Pageable pageable);

    void deleteByCustomerAndProject(Customer customer, Project project);
}
