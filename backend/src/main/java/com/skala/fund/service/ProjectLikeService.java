package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectLike;
import com.skala.fund.dto.ProjectDtos;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.ProjectLikeRepository;
import com.skala.fund.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectLikeService {

    private final ProjectLikeRepository projectLikeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void like(Long customerId, Long projectId) {
        Customer customer = getCustomer(customerId);
        Project project = getProject(projectId);

        if (projectLikeRepository.existsByCustomerAndProject(customer, project)) {
            throw new CustomException(ErrorCode.ALREADY_LIKED);
        }

        projectLikeRepository.save(ProjectLike.builder()
                .customer(customer)
                .project(project)
                .build());
    }

    @Transactional
    public void unlike(Long customerId, Long projectId) {
        Customer customer = getCustomer(customerId);
        Project project = getProject(projectId);

        ProjectLike like = projectLikeRepository.findByCustomerAndProject(customer, project)
                .orElseThrow(() -> new CustomException(ErrorCode.LIKE_NOT_FOUND));

        projectLikeRepository.delete(like);
    }

    /** 찜 목록. 삭제된 프로젝트는 제외된다. */
    @Transactional(readOnly = true)
    public Page<ProjectDtos.ProjectResponse> findMyLikes(Long customerId, Pageable pageable) {
        Customer customer = getCustomer(customerId);
        return projectLikeRepository.findMyLikes(customer, pageable)
                .map(like -> ProjectDtos.ProjectResponse.from(like.getProject(), true));
    }

    private Customer getCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /** 삭제된 프로젝트는 찜할 수 없다. */
    private Project getProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
