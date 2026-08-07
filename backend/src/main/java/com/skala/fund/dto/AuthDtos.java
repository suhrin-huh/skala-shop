package com.skala.fund.dto;

import com.skala.fund.domain.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record SignupRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "닉네임은 필수입니다.")
            @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하이어야 합니다.")
            String nickname,

            @NotBlank(message = "비밀번호는 필수입니다.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                    message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "이메일은 필수입니다.") String email,
            @NotBlank(message = "비밀번호는 필수입니다.") String password
    ) {}

    /**
     * 프론트가 포인트 3종(보유/예약/사용 가능)을 나란히 표시해야 하므로 셋 다 내려준다.
     * availablePoint 는 point - reservedPoint 이지만 계산을 프론트에 맡기지 않는다.
     */
    public record UserSummary(
            Long id,
            String email,
            String nickname,
            Long point,
            Long reservedPoint,
            Long availablePoint
    ) {
        public static UserSummary from(Customer customer) {
            return new UserSummary(
                    customer.getId(),
                    customer.getEmail(),
                    customer.getNickname(),
                    customer.getPoint(),
                    customer.getReservedPoint(),
                    customer.getAvailablePoint()
            );
        }
    }

    /** 카드/상세에 붙는 최소 창작자 정보. 이메일과 포인트는 노출하지 않는다. */
    public record CreatorSummary(Long id, String nickname) {
        public static CreatorSummary from(Customer customer) {
            return new CreatorSummary(customer.getId(), customer.getNickname());
        }
    }

    /** Access Token 은 본문으로, Refresh Token 은 Http-Only 쿠키로 나간다. */
    public record LoginResponse(String accessToken, UserSummary user) {}

    public record AccessTokenResponse(String accessToken) {}
}
