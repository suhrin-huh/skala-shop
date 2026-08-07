package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.dto.AuthDtos;
import com.skala.fund.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰 재발급 / 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final AuthService authService;

    /**
     * prod 는 항상 true. dev 에서만 false 로 낮춘다.
     * 브라우저는 http://localhost 에 한해 Secure 쿠키를 허용하지만, 팀원이 LAN IP 로 접속하면
     * 쿠키가 통째로 무시되어 원인 찾기 어려운 로그인 실패가 난다.
     */
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Operation(summary = "회원가입", description = "가입 시 1,000,000 포인트가 자동 지급된다.")
    @PostMapping("/signup")
    public ApiResponse<AuthDtos.UserSummary> signup(@Valid @RequestBody AuthDtos.SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "Access Token 은 본문, Refresh Token 은 Http-Only 쿠키로 나간다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return withRefreshCookie(result);
    }

    @Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token 을 DB 와 대조 검증한 뒤 회전 발급한다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthService.LoginResult result = authService.refresh(refreshToken);
        return withRefreshCookie(result);
    }

    @Operation(summary = "로그아웃", description = "DB 의 Refresh Token 을 삭제하고 쿠키를 만료시킨다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .body(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> withRefreshCookie(AuthService.LoginResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                .body(ApiResponse.success(new AuthDtos.LoginResponse(result.accessToken(), result.user())));
    }

    /**
     * 동일 오리진이라 SameSite=None 이 필요 없다. Lax 가 CSRF 방어에 더 유리하다.
     * Path 를 /api/auth 로 좁혀 일반 API 요청에는 쿠키가 실려 나가지 않게 한다.
     */
    private ResponseCookie refreshCookie(String token) {
        return baseCookie(token).maxAge(Duration.ofDays(14)).build();
    }

    private ResponseCookie expiredCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth");
    }
}
