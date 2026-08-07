package com.skala.fund.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_001", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않거나 만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_004", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_005", "권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    INSUFFICIENT_AVAILABLE_POINT(HttpStatus.BAD_REQUEST, "USER_002", "사용 가능 포인트가 부족합니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_001", "존재하지 않는 프로젝트입니다."),
    NOT_PROJECT_OWNER(HttpStatus.FORBIDDEN, "PROJECT_002", "프로젝트 창작자만 수정/삭제할 수 있습니다."),
    PROJECT_NOT_ONGOING(HttpStatus.BAD_REQUEST, "PROJECT_003", "진행 중인 프로젝트만 후원할 수 있습니다."),
    PROJECT_CLOSED(HttpStatus.BAD_REQUEST, "PROJECT_004", "이미 마감된 프로젝트는 수정할 수 없습니다."),

    // Pledge
    PLEDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLEDGE_001", "후원 내역을 찾을 수 없습니다."),
    PLEDGE_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "PLEDGE_002", "취소할 수 없는 후원 상태입니다."),
    INVALID_PLEDGE_AMOUNT(HttpStatus.BAD_REQUEST, "PLEDGE_003", "최소 후원 금액은 1,000원 이상이어야 합니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_001", "존재하지 않는 카테고리입니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
