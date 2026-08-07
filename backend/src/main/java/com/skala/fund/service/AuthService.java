package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import com.skala.fund.config.JwtTokenProvider;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.RefreshToken;
import com.skala.fund.dto.AuthDtos;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 가입 시 자동 지급되는 초기 포인트. */
    private static final long SIGNUP_BONUS_POINT = 1_000_000L;

    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDtos.UserSummary signup(AuthDtos.SignupRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        Customer customer = customerRepository.save(Customer.builder()
                .email(request.email())
                .nickname(request.nickname())
                .password(passwordEncoder.encode(request.password()))
                .point(SIGNUP_BONUS_POINT)
                .build());

        log.info("회원가입 완료 - customerId={}, 지급 포인트={}", customer.getId(), SIGNUP_BONUS_POINT);
        return AuthDtos.UserSummary.from(customer);
    }

    /**
     * 로그인. Access Token 은 응답 본문으로, Refresh Token 은 DB 에 저장한 뒤 쿠키로 나간다.
     * 반환된 refreshToken 은 컨트롤러가 쿠키로 굽는다.
     */
    @Transactional
    public LoginResult login(AuthDtos.LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), customer.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(customer.getId());
        String refreshToken = issueRefreshToken(customer);

        return new LoginResult(accessToken, refreshToken, AuthDtos.UserSummary.from(customer));
    }

    /**
     * 쿠키의 Refresh Token 을 DB 와 대조해 검증하고 재발급한다.
     * 재사용 공격을 줄이기 위해 매 refresh 마다 토큰을 회전시킨다.
     */
    @Transactional
    public LoginResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Long customerId = jwtTokenProvider.parseCustomerId(refreshTokenValue);
        if (customerId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(customer.getId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(customer.getId());
        stored.updateToken(newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt());

        return new LoginResult(newAccessToken, newRefreshToken, AuthDtos.UserSummary.from(customer));
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return; // 이미 로그아웃 상태다. 멱등 처리한다.
        }
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(refreshTokenRepository::delete);
    }

    private String issueRefreshToken(Customer customer) {
        String token = jwtTokenProvider.createRefreshToken(customer.getId());
        refreshTokenRepository.findByCustomer(customer)
                .ifPresentOrElse(
                        existing -> existing.updateToken(token, jwtTokenProvider.getRefreshTokenExpiresAt()),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .customer(customer)
                                .token(token)
                                .expiresAt(jwtTokenProvider.getRefreshTokenExpiresAt())
                                .build()));
        return token;
    }

    public record LoginResult(String accessToken, String refreshToken, AuthDtos.UserSummary user) {}
}
