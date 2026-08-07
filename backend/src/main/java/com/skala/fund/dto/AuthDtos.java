package com.skala.fund.dto;

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
                    message = "비밀번호는 8자 이상이며 특수문자를 포함해야 합니다.")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record UserSummary(
            Long id,
            String email,
            String nickname,
            Long point,
            Long reservedPoint,
            Long availablePoint
    ) {}

    public record LoginResponse(
            String accessToken,
            UserSummary user
    ) {}
}
