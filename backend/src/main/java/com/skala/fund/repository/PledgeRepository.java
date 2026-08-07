package com.skala.fund.repository;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PledgeRepository extends JpaRepository<Pledge, Long> {

    Page<Pledge> findByCustomerOrderByCreatedAtDesc(Customer customer, Pageable pageable);

    List<Pledge> findByProject(Project project);

    List<Pledge> findByProjectAndStatus(Project project, PledgeStatus status);
}
