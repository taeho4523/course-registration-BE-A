package com.example.enrollment.enrollment.dto;

import com.example.enrollment.enrollment.domain.EnrollmentStatus;

import java.time.LocalDateTime;

/**
 * 내 수강 신청 목록용 응답.
 * 신청 정보 + 강의 제목을 함께 보여준다.
 * (Enrollment는 courseId만 갖고 있으므로, 제목은 조회 시 조인/별도 조회로 채운다)
 */
public record MyEnrollmentResponse(
	Long enrollmentId,
	Long courseId,
	String courseTitle,
	EnrollmentStatus status,
	LocalDateTime enrolledAt,
	LocalDateTime confirmedAt,
	LocalDateTime cancelledAt
) {
}