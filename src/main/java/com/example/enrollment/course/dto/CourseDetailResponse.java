package com.example.enrollment.course.dto;

import java.time.LocalDateTime;

import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;

/**
 * 강의 상세 응답.
 * 엔티티를 그대로 노출하지 않고 DTO로 변환해 내보낸다.
 * (엔티티 직접 노출 시: 내부 구조가 API에 새고, 순환참조/지연로딩 문제가 생길 수 있음)
 */
public record CourseDetailResponse(
	Long id,
	Long creatorId,
	String title,
	String description,
	int price,
	int capacity,
	int enrolledCount,
	int remainingCount,
	CourseStatus status,
	LocalDateTime startAt,
	LocalDateTime endAt
){
	/**
	 * 엔티티 → 응답 DTO 변환.
	 * 정적 팩토리 메서드로 변환 책임을 DTO 안에 둔다.
	 */
	public static CourseDetailResponse from(Course course) {
		return new CourseDetailResponse(
			course.getId(),
			course.getCreatorId(),
			course.getTitle(),
			course.getDescription(),
			course.getPrice(),
			course.getCapacity(),
			course.getEnrolledCount(),
			course.getCapacity() - course.getEnrolledCount(), // 잔여 정원 계산
			course.getStatus(),
			course.getStartAt(),
			course.getEndAt()
		);
	}
}
