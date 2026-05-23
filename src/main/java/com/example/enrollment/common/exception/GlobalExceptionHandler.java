package com.example.enrollment.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.enrollment.common.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 규칙 위반(BusinessException).
	 * ErrorCode가 가진 HTTP 상태와 메시지로 응답을 만든다.
	 */

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		//비즈니스 예외는 예상된 흐름이므로 warn레벨만 기록 (스택 트레이스 x)
		log.warn("BusinessException: {} - {}", code.name(), e.getMessage());
		return ResponseEntity
			.status(code.getStatus())
			.body(ErrorResponse.of(code, e.getMessage()));
	}

	/**
	 * 요청 본문 검증 실패(@Valid 위반).
	 * 첫 번째 필드 에러 메시지를 꺼내 400으로 응답.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValdation(MethodArgumentNotValidException e) {
		String detail=e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(err->err.getField() + ": "+err.getDefaultMessage())
			.orElse(ErrorCode.INVALID_REQUEST.getMessage());
		log.warn("Validation failed: {}", detail);
		return ResponseEntity
			.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, detail));
	}

	/**
	 * 그 외 예상치 못한 예외.
	 * 진짜 버그일 가능성이 높으므로 error 레벨로 스택트레이스까지 기록.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e){
		log.error("Unexpected exception", e);
		return ResponseEntity
			.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
	}
}


