package com.example.enrollment.enrollment.controller;

import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 강의에 종속된 신청 API.
 * URL이 강의 하위(/api/courses/{courseId}/enrollments)라 별도 컨트롤러로 분리.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/enrollments")
@RequiredArgsConstructor
public class CourseEnrollmentController {

	private final EnrollmentService enrollmentService;

	/** 수강 신청 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentResponse enroll(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long courseId
	) {
		return enrollmentService.enroll(memberId, courseId);
	}
}