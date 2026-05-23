package com.example.enrollment.course.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 강의 등록 요청.
 * @Valid와 함께 쓰여 컨트롤러 진입 시점에 검증된다.
 * 검증 실패 시 GlobalExceptionHandler의 MethodArgumentNotValidException 핸들러가 400으로 응답.
 */
public record CourseCreateRequest (

	@NotBlank(message="강의명은 필수입니다.")
	@Size(max=100, message="강의명은 100자를 넘길 수 없습니다.")
	String title,

	String description,

	@NotNull(message = "가격은 필수입니다.")
	@PositiveOrZero(message="가격은 0 이상이어야 합니다.")
	Integer price,
	//int가 아닌 Integer객체타입으로 null값 검증을 유효화. int는 자동으로 0을 넣어버림 자바에서
	@NotNull(message="정원은 필수입니다.")
	@Positive(message = "정원은 1 이상이어야 합니다.")
	Integer capacity,

	@NotNull(message="시작일은 필수입니다.")
	LocalDateTime startAt,

	@NotNull(message = "종료일은 필수입니다.")
	LocalDateTime endAt

){
}
