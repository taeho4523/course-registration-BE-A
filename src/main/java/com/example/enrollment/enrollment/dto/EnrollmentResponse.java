package com.example.enrollment.enrollment.dto;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;

import java.time.LocalDateTime;

/**
 * 수강 신청 응답.
 */
public record EnrollmentResponse(
	Long enrollmentId,
	Long courseId,
	Long memberId,
	EnrollmentStatus status,
	LocalDateTime enrolledAt,
	LocalDateTime confirmedAt,  // 확정 전이면 null
	LocalDateTime cancelledAt   // 취소 전이면 null
) {
	public static EnrollmentResponse from(Enrollment e) {
		return new EnrollmentResponse(
			e.getId(),
			e.getCourseId(),
			e.getMemberId(),
			e.getStatus(),
			e.getEnrolledAt(),
			e.getConfirmedAt(),
			e.getCancelledAt()
		);
	}
}