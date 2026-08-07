package com.skala.fund.service;

import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final CustomerRepository customerRepository;

    @Scheduled(cron = "0 5 0 * * *") // 매일 00:05
    @Transactional
    public int runStatusTransitionBatch() {
        LocalDate today = LocalDate.now();
        List<Project> scheduledProjects = projectRepository
                .findByStatusAndStartDateLessThanEqualAndDeletedAtIsNull(ProjectStatus.SCHEDULED, today);

        for (Project project : scheduledProjects) {
            project.changeStatus(ProjectStatus.ONGOING);
            log.info("Batch Status Transition: Project #{} -> ONGOING", project.getId());
        }
        return scheduledProjects.size();
    }

    @Scheduled(cron = "0 10 0 * * *") // 매일 00:10
    public int runSettlementBatch() {
        LocalDate today = LocalDate.now();
        List<Project> endedProjects = projectRepository
                .findByStatusAndEndDateLessThanAndDeletedAtIsNull(ProjectStatus.ONGOING, today);

        int processedCount = 0;
        for (Project project : endedProjects) {
            try {
                settleSingleProject(project.getId());
                processedCount++;
            } catch (Exception e) {
                log.error("Failed settlement for Project #{}: {}", project.getId(), e.getMessage(), e);
            }
        }
        return processedCount;
    }

    @Transactional
    public void settleSingleProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ONGOING) {
            return; // 멱등성 유지
        }

        List<Pledge> pledges = pledgeRepository.findByProjectAndStatus(project, PledgeStatus.PLEDGED);
        long totalPledgedAmount = pledges.stream().mapToLong(Pledge::getAmount).sum();

        if (totalPledgedAmount >= project.getTargetAmount()) {
            // 펀딩 성공
            project.changeStatus(ProjectStatus.SUCCESS);
            long totalConfirmedAmount = 0L;

            for (Pledge pledge : pledges) {
                Customer customer = customerRepository.findByIdWithLock(pledge.getCustomer().getId())
                        .orElseThrow();
                
                if (customer.getPoint() >= pledge.getAmount()) {
                    customer.confirmDeduction(pledge.getAmount());
                    pledge.confirm();
                    totalConfirmedAmount += pledge.getAmount();
                } else {
                    pledge.markFailed();
                    customer.releaseReservedPoint(pledge.getAmount());
                    log.error("Settlement Deficit Alert - Pledge #{}, Customer #{}", pledge.getId(), customer.getId());
                }
            }

            Customer creator = customerRepository.findByIdWithLock(project.getCreator().getId())
                    .orElseThrow();
            creator.addPoint(totalConfirmedAmount);

            log.info("Settlement SUCCESS: Project #{}, Total Confirmed = {} P", project.getId(), totalConfirmedAmount);
        } else {
            // 펀딩 무산
            project.changeStatus(ProjectStatus.FAILED);

            for (Pledge pledge : pledges) {
                Customer customer = customerRepository.findByIdWithLock(pledge.getCustomer().getId())
                        .orElseThrow();
                customer.releaseReservedPoint(pledge.getAmount());
                pledge.markFailed();
            }

            log.info("Settlement FAILED: Project #{}, Total Pledged = {} P", project.getId(), totalPledgedAmount);
        }
    }
}
