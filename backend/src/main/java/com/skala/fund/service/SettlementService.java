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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 프로젝트 상태 전이와 정산의 비즈니스 로직을 담당한다.
 * 스케줄 트리거는 batch 패키지가 담당하며, 이 클래스는 호출 경로(배치/하네스/테스트)와 무관하게 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final CustomerRepository customerRepository;

    /**
     * 시작일이 도래한 SCHEDULED 프로젝트를 ONGOING 으로 전이시킨다.
     */
    @Transactional
    public int runStatusTransition() {
        LocalDate today = LocalDate.now();
        List<Project> scheduledProjects = projectRepository
                .findByStatusAndStartDateLessThanEqualAndDeletedAtIsNull(ProjectStatus.SCHEDULED, today);

        for (Project project : scheduledProjects) {
            project.changeStatus(ProjectStatus.ONGOING);
            log.info("상태 전이 배치: Project #{} -> ONGOING", project.getId());
        }
        log.info("상태 전이 배치 완료 - 대상 {}건", scheduledProjects.size());
        return scheduledProjects.size();
    }

    /**
     * 정산 대상 프로젝트 ID 목록. 마감일이 지난 ONGOING 프로젝트만 뽑는다.
     * 삭제된 프로젝트는 Repository 쿼리에서 제외된다.
     */
    @Transactional(readOnly = true)
    public List<Long> findSettlementTargetIds() {
        return projectRepository
                .findByStatusAndEndDateLessThanAndDeletedAtIsNull(ProjectStatus.ONGOING, LocalDate.now())
                .stream()
                .map(Project::getId)
                .toList();
    }

    /**
     * 프로젝트 한 건을 정산한다. 반드시 프로젝트 단위 트랜잭션으로 실행되어야 하므로
     * 반복 호출은 이 메서드를 외부(다른 빈)에서 건별로 호출하는 방식으로 이루어진다.
     * 같은 클래스 안에서 루프를 돌며 자기 자신을 호출하면 프록시를 거치지 않아
     * 트랜잭션이 걸리지 않고 변경 사항이 flush 되지 않는다.
     */
    @Transactional
    public void settleSingleProject(Long projectId) {
        // 잠금 순서는 서비스 전역 규칙과 동일하게 Project -> Customer 다.
        Project project = projectRepository.findByIdWithLockIgnoringDeleted(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // status == ONGOING 조건이 멱등성 장치다. 재실행해도 두 번 정산되지 않는다.
        if (project.getStatus() != ProjectStatus.ONGOING) {
            return;
        }

        List<Pledge> pledges = pledgeRepository.findByProjectAndStatus(project, PledgeStatus.PLEDGED);
        long totalPledgedAmount = pledges.stream().mapToLong(Pledge::getAmount).sum();

        if (totalPledgedAmount >= project.getTargetAmount()) {
            settleSuccess(project, pledges, totalPledgedAmount);
        } else {
            settleFailure(project, pledges, totalPledgedAmount);
        }
    }

    private void settleSuccess(Project project, List<Pledge> pledges, long totalPledgedAmount) {
        project.changeStatus(ProjectStatus.SUCCESS);
        long totalConfirmedAmount = 0L;

        // 데드락 방지를 위해 회원 id 오름차순으로 잠근다.
        List<Pledge> ordered = pledges.stream()
                .sorted(Comparator.comparing(p -> p.getCustomer().getId()))
                .toList();

        for (Pledge pledge : ordered) {
            Customer customer = customerRepository.findByIdWithLock(pledge.getCustomer().getId())
                    .orElseThrow();

            if (customer.getPoint() >= pledge.getAmount()) {
                customer.confirmDeduction(pledge.getAmount()); // 여기서 최초로 결제가 발생한다
                pledge.confirm();
                totalConfirmedAmount += pledge.getAmount();
            } else {
                // 사용 가능 포인트 검증이 정상 동작했다면 도달할 수 없는 분기다.
                // 도달했다면 그 자체가 상위 검증이 뚫렸다는 증거이므로 ERROR 로 남긴다.
                pledge.markFailed();
                customer.releaseReservedPoint(pledge.getAmount());
                log.error("정산 잔액 부족 - pledgeId={}, customerId={}, amount={}, point={}",
                        pledge.getId(), customer.getId(), pledge.getAmount(), customer.getPoint());
            }
        }

        Customer creator = customerRepository.findByIdWithLock(project.getCreator().getId()).orElseThrow();
        creator.addPoint(totalConfirmedAmount);

        log.info("정산 성공 - projectId={}, 모금액={}, 실제 결제액={}, 후원 건수={}",
                project.getId(), totalPledgedAmount, totalConfirmedAmount, pledges.size());
    }

    private void settleFailure(Project project, List<Pledge> pledges, long totalPledgedAmount) {
        project.changeStatus(ProjectStatus.FAILED);

        List<Pledge> ordered = pledges.stream()
                .sorted(Comparator.comparing(p -> p.getCustomer().getId()))
                .toList();

        for (Pledge pledge : ordered) {
            Customer customer = customerRepository.findByIdWithLock(pledge.getCustomer().getId())
                    .orElseThrow();
            // 포인트가 차감된 적이 없으므로 환급이 아니라 예약 해제만 한다.
            customer.releaseReservedPoint(pledge.getAmount());
            pledge.markFailed();
        }

        log.info("정산 무산 - projectId={}, 모금액={}, 목표액={}, 후원 건수={}",
                project.getId(), totalPledgedAmount, project.getTargetAmount(), pledges.size());
    }
}
