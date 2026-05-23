package com.example.enrollment.course.dto;

import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;

/**
 * 강의 목록용 요약 응답.
 * 목록에선 description 같은 무거운 필드를 빼고 핵심만 내보낸다.
 */
public record CourseSummaryResponse(
	Long id,
	String title,
	Long creatorId,
	int price,
	int capacity,
	int enrolledCount,
	CourseStatus status
) {
	public static CourseSummaryResponse from(Course course) {
		return new CourseSummaryResponse(
			course.getId(),
			course.getTitle(),
			course.getCreatorId(),
			course.getPrice(),
			course.getCapacity(),
			course.getEnrolledCount(),
			course.getStatus()
		);
	}
}