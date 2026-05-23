package com.example.enrollment.course.repository;

import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 강의 저장소.
 * 커서 기반 목록 조회만 메서드 이름 규칙으로 추가 정의한다.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

	/**
	 * 커서 기반 목록 조회 (상태 필터 없음).
	 * id < cursor 인 것들을 id 내림차순으로 limit개 조회.
	 * → 최신(id 큰) 강의부터, 커서보다 이전 것들을 가져온다.
	 */
	List<Course> findByIdLessThanOrderByIdDesc(Long cursor, Limit limit);

	/**
	 * 커서 기반 목록 조회 (상태 필터 있음).
	 */
	List<Course> findByStatusAndIdLessThanOrderByIdDesc(CourseStatus status, Long cursor, Limit limit);
}