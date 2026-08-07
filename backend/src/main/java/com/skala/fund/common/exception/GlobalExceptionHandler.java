package com.skala.fund.common.exception;

import com.skala.fund.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("CustomException: [{}] {}", code.getCode(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String detailMsg = e.getBindingResult().getAllErrors().isEmpty() ?
                "유효하지 않은 요청 데이터입니다." :
                e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation Error: {}", detailMsg);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", detailMsg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
