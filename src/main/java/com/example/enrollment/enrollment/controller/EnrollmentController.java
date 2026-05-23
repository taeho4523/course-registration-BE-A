package com.example.enrollment.enrollment.controller;

import com.example.enrollment.common.response.CursorPage;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.dto.MyEnrollmentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 신청 자체를 다루는 API (확정/취소/내 목록).
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	/** 결제 확정 */
	@PostMapping("/{enrollmentId}/confirm")
	public EnrollmentResponse confirm(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long enrollmentId
	) {
		return enrollmentService.confirm(memberId, enrollmentId);
	}

	/** 수강 취소 */
	@PostMapping("/{enrollmentId}/cancel")
	public EnrollmentResponse cancel(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long enrollmentId
	) {
		return enrollmentService.cancel(memberId, enrollmentId);
	}

	/** 내 수강 신청 목록 */
	@GetMapping("/me")
	public CursorPage<MyEnrollmentResponse> getMyEnrollments(
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestParam(required = false) EnrollmentStatus status,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") int size
	) {
		return enrollmentService.getMyEnrollments(memberId, status, cursor, size);
	}
}