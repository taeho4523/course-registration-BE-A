package com.example.enrollment.course.dto;

import com.example.enrollment.course.domain.CourseStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 강의 상태 변경 요청.
 * 예: { "status": "OPEN" }
 */
public record CourseStatusUpdateRequest(
	@NotNull(message = "변경할 상태는 필수입니다.")
	CourseStatus status
) {
}