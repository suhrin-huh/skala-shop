package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.DeliveryStatus;
import com.skala.fund.domain.Pledge;
import com.skala.fund.domain.PledgeStatus;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.dto.PledgeDtos;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약형 All-or-Nothing 모델의 핵심.
 * 후원해도 customer.point 는 줄지 않는다. 포인트가 실제로 빠지는 유일한 경로는 정산 배치다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PledgeService {

    private static final long MIN_PLEDGE_AMOUNT = 1_000L;

    private final PledgeRepository pledgeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Pledge createPledge(Long customerId, Long projectId, long amount) {
        if (amount < MIN_PLEDGE_AMOUNT) {
            throw new CustomException(ErrorCode.INVALID_PLEDGE_AMOUNT);
        }

        // 잠금 순서는 항상 Project -> Customer 다. 서비스 전역에서 같은 순서를 지켜 데드락을 막는다.
        //
        // Project 를 잠그는 이유: currentAmount / pledgeCount 는 읽고-더하고-쓰는 갱신이라,
        // 서로 다른 회원이 같은 프로젝트에 동시 후원하면 Customer 락만으로는 갱신이 유실된다.
        // TODO(성능): 프로젝트 단위 직렬화라 인기 프로젝트에서 후원 처리량이 제한된다.
        //  정합성 최우선 정책에 따른 선택이며, 처리량이 문제가 되면 원자적 UPDATE 쿼리로 전환 검토.
        Project project = projectRepository.findByIdWithLock(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.getStatus() != ProjectStatus.ONGOING) {
            throw new CustomException(ErrorCode.PROJECT_NOT_ONGOING);
        }

        // 상태가 ONGOING 이어도 배치가 아직 안 돌았을 수 있으므로 마감일을 다시 본다.
        if (LocalDate.now().isAfter(project.getEndDate())) {
            throw new CustomException(ErrorCode.PROJECT_CLOSED);
        }

        // Customer 행을 잠가 같은 회원의 동시 요청을 직렬화한다.
        // 이게 없으면 여러 요청이 같은 사용 가능 포인트를 동시에 읽고 초과 후원할 수 있다.
        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용 가능 포인트(point - reservedPoint) 검증 후 예약. 부족하면 여기서 예외가 난다.
        customer.reservePoint(amount);

        // 비정규화 수치 즉시 반영
        project.addPledgeAmount(amount);

        Pledge pledge = Pledge.builder()
                .customer(customer)
                .project(project)
                .amount(amount)
                .status(PledgeStatus.PLEDGED)
                .build();

        return pledgeRepository.save(pledge);
    }

    @Transactional
    public void cancelPledge(Long customerId, Long pledgeId) {
        Pledge pledge = pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLEDGE_NOT_FOUND));

        if (!pledge.getCustomer().getId().equals(customerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (pledge.getStatus() != PledgeStatus.PLEDGED) {
            throw new CustomException(ErrorCode.PLEDGE_NOT_CANCELLABLE);
        }

        // 후원과 동일하게 Project -> Customer 순서로 잠근다.
        Project project = projectRepository.findByIdWithLock(pledge.getProject().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (LocalDate.now().isAfter(project.getEndDate())) {
            throw new CustomException(ErrorCode.PLEDGE_NOT_CANCELLABLE,
                    "마감일이 지난 프로젝트 후원은 취소할 수 없습니다.");
        }

        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 포인트가 차감된 적이 없으므로 환급이 아니라 예약 해제만 한다.
        customer.releaseReservedPoint(pledge.getAmount());
        project.removePledgeAmount(pledge.getAmount());

        pledge.cancel();
    }

    /**
     * 배송 상태 변경. 해당 프로젝트의 창작자만 가능하며 CONFIRMED 후원에만 허용된다.
     * ORDER_COMPLETED -> SHIPPING -> DELIVERED 순서를 역행할 수 없다.
     */
    @Transactional
    public PledgeDtos.BackerResponse updateDeliveryStatus(Long requesterId, Long pledgeId,
                                                          DeliveryStatus newStatus) {
        Pledge pledge = pledgeRepository.findById(pledgeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLEDGE_NOT_FOUND));

        if (!pledge.getProject().getCreator().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_PROJECT_OWNER);
        }

        if (pledge.getStatus() != PledgeStatus.CONFIRMED || pledge.getDeliveryStatus() == null) {
            throw new CustomException(ErrorCode.PLEDGE_NOT_CONFIRMED);
        }

        if (!pledge.getDeliveryStatus().canTransitionTo(newStatus)) {
            throw new CustomException(ErrorCode.INVALID_DELIVERY_TRANSITION);
        }

        pledge.updateDeliveryStatus(newStatus);
        return PledgeDtos.BackerResponse.from(pledge);
    }

    /** 창작자가 자기 프로젝트의 결제 완료 후원자 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<PledgeDtos.BackerResponse> findBackers(Long requesterId, Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreator().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.NOT_PROJECT_OWNER);
        }

        return pledgeRepository.findConfirmedPledgesByProject(projectId).stream()
                .map(PledgeDtos.BackerResponse::from)
                .toList();
    }
}
