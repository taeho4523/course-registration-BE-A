package com.example.enrollment.course.controller;

import com.example.enrollment.common.response.CursorPage;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.dto.*;
import com.example.enrollment.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.CourseStudentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;

/**
 * 강의 API.
 * 인증은 X-Member-Id 헤더로 사용자를 식별한다(과제 허용 방식).
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

	private final CourseService courseService;
	private final EnrollmentService enrollmentService;

	/** 강의 등록 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED) // 생성 성공은 201
	public CourseDetailResponse create(
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody CourseCreateRequest request
	) {
		return courseService.create(memberId, request);
	}

	/** 강의 상태 변경 */
	@PatchMapping("/{courseId}/status")
	public CourseDetailResponse changeStatus(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long courseId,
		@Valid @RequestBody CourseStatusUpdateRequest request
	) {
		return courseService.changeStatus(memberId, courseId, request.status());
	}

	/** 강의 상세 조회 */
	@GetMapping("/{courseId}")
	public CourseDetailResponse getDetail(@PathVariable Long courseId) {
		return courseService.getDetail(courseId);
	}

	/** 강의 목록 조회 (상태 필터 + 커서 페이지네이션) */
	@GetMapping
	public CursorPage<CourseSummaryResponse> getList(
		@RequestParam(required = false) CourseStatus status,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") int size
	) {
		return courseService.getList(status, cursor, size);
	}

	/** 강의별 수강생 목록 조회 (강사 전용) */
	@GetMapping("/{courseId}/enrollments")
	public CursorPage<CourseStudentResponse> getCourseStudents(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long courseId,
		@RequestParam(required = false) EnrollmentStatus status,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") int size
	) {
		return enrollmentService.getCourseStudents(memberId, courseId, status, cursor, size);
	}
}