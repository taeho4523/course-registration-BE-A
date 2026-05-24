package com.example.enrollment.enrollment.repository;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 수강 신청 저장소.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	/**
	 * 중복 신청 체크용.
	 * 같은 멤버가 같은 강의에 활성(PENDING/CONFIRMED) 신청을 이미 가졌는지 확인.
	 * 취소(CANCELLED) 건은 행으로 남아있어도 제외되므로, 취소 후 재신청은 허용된다.
	 */
	boolean existsByCourseIdAndMemberIdAndStatusIn(
		Long courseId, Long memberId, List<EnrollmentStatus> statuses);

	/**
	 * 내 신청 목록 (커서 기반).
	 */
	List<Enrollment> findByMemberIdAndIdLessThanOrderByIdDesc(
		Long memberId, Long cursor, Limit limit);

	/**
	 * 내 신청 목록 (상태 필터 + 커서 기반).
	 */
	List<Enrollment> findByMemberIdAndStatusAndIdLessThanOrderByIdDesc(
		Long memberId, EnrollmentStatus status, Long cursor, Limit limit);

	/**
	 * 특정 강의의 신청 목록 (커서 기반). 강사용.
	 */
	List<Enrollment> findByCourseIdAndIdLessThanOrderByIdDesc(
		Long courseId, Long cursor, Limit limit);

	/**
	 * 특정 강의의 신청 목록 (상태 필터 + 커서 기반). 강사용.
	 */
	List<Enrollment> findByCourseIdAndStatusAndIdLessThanOrderByIdDesc(
		Long courseId, EnrollmentStatus status, Long cursor, Limit limit);

	/**
	 * 특정 강의+멤버의 특정 상태 신청들 조회.
	 */
	List<Enrollment> findByCourseIdAndMemberIdAndStatusIn(
		Long courseId, Long memberId, List<EnrollmentStatus> statuses);
}