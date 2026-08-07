package com.skala.fund.service;

import com.skala.fund.common.CustomException;
import com.skala.fund.common.ErrorCode;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PledgeService {

    private final PledgeRepository pledgeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Pledge createPledge(Long customerId, Long projectId, long amount) {
        if (amount < 1_000L) {
            throw new CustomException(ErrorCode.INVALID_PLEDGE_AMOUNT);
        }

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.getStatus() != ProjectStatus.ONGOING) {
            throw new CustomException(ErrorCode.PROJECT_NOT_ONGOING);
        }

        if (LocalDate.now().isAfter(project.getEndDate())) {
            throw new CustomException(ErrorCode.PROJECT_CLOSED);
        }

        // Customer 비관적 락 조회로 동시성 안전 보장
        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용 가능 포인트 예약 (예약 불가 시 InsufficientAvailablePointException 예외 발생)
        customer.reservePoint(amount);

        // 프로젝트 비정규화 수치 즉시 반영
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

        Project project = pledge.getProject();
        if (LocalDate.now().isAfter(project.getEndDate())) {
            throw new CustomException(ErrorCode.PLEDGE_NOT_CANCELLABLE, "마감일이 지난 프로젝트 후원은 취소할 수 없습니다.");
        }

        // Customer 비관적 락으로 안전한 포인트 예약 해제
        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        customer.releaseReservedPoint(pledge.getAmount());
        project.removePledgeAmount(pledge.getAmount());

        pledge.cancel();
    }
}
