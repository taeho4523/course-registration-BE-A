package com.example.enrollment.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 코드.
 * HTTP 상태와 클라이언트용 코드 문자열, 기본 메시지를 함께 정의한다.
 * 응답 본문에는 code(문자열)와 message가 내려가고, HTTP 상태는 status를 따른다.
 */
@Getter
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "사용자 식별에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 멤버
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."),

    // 강의
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강의입니다."),
    INVALID_COURSE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 강의 상태 전이입니다."),
    COURSE_NOT_OPEN(HttpStatus.BAD_REQUEST, "모집 중인 강의가 아닙니다."),

    // 수강 신청
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신청 내역입니다."),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "정원이 초과되어 신청할 수 없습니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 신청한 강의입니다."),
    INVALID_ENROLLMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 신청 상태 전이입니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "취소 가능 기간이 지났습니다."),

    // 대기열
    CAPACITY_AVAILABLE(HttpStatus.BAD_REQUEST, "정원에 여유가 있어 바로 신청할 수 있습니다."),
    ALREADY_WAITING(HttpStatus.CONFLICT, "이미 대기 중입니다."),
    WAITLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "대기 내역이 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
