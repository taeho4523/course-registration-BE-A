package com.example.enrollment.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 예외.
 * ErrorCode를 담아서 던지면 전역 예외 핸들러(GlobalExceptionHandler)가
 * 적절한 HTTP 상태와 응답 본문으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 상황별 상세 메시지를 덧붙이고 싶을 때
    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
