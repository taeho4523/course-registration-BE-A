package com.example.enrollment.enrollment.dto;

import com.example.enrollment.enrollment.domain.EnrollmentStatus;

import java.time.LocalDateTime;

/**
 * 강의별 수강생 목록 응답 (강사 전용).
 * 수강생 이름을 함께 보여주기 위해 memberName을 포함한다.
 */
public record CourseStudentResponse(
	Long enrollmentId,
	Long memberId,
	String memberName,
	EnrollmentStatus status,
	LocalDateTime enrolledAt,
	LocalDateTime confirmedAt
) {
}