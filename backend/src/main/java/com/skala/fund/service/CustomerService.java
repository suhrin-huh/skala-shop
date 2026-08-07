package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.domain.Customer;
import com.skala.fund.dto.AuthDtos;
import com.skala.fund.dto.PledgeDtos;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PledgeRepository pledgeRepository;

    /** 프로필 + 포인트 3종(보유/예약/사용 가능). */
    @Transactional(readOnly = true)
    public AuthDtos.UserSummary getMyProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return AuthDtos.UserSummary.from(customer);
    }

    /**
     * 내 후원 내역.
     * 삭제된 프로젝트의 후원도 포함하고 projectDeleted 플래그를 붙여 내려준다.
     * 후원자 입장에서 자기 후원 기록이 사라진 것처럼 보이면 안 되기 때문이다.
     */
    @Transactional(readOnly = true)
    public Page<PledgeDtos.PledgeResponse> getMyPledges(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return pledgeRepository.findMyPledges(customer, pageable)
                .map(PledgeDtos.PledgeResponse::from);
    }
}
