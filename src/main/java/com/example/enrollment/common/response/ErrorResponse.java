package com.example.enrollment.common.response;

import com.example.enrollment.common.exception.ErrorCode;

/**
 * 에러 응답 본문.
 * record로 만들어 불변 + 보일러플레이트 제거.
 * 예: { "code": "CAPACITY_EXCEEDED", "message": "정원이 초과되어..." }
 */

public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode){
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message){
		return new ErrorResponse(errorCode.name(), message);
	}
}
