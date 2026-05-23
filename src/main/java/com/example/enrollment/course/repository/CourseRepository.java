package com.example.enrollment.course.repository;

import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

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

	/**
	 * 비관적 쓰기 락(PESSIMISTIC_WRITE)을 걸고 강의를 조회한다.
	 * SELECT ... FOR UPDATE 가 실행되어, 이 행을 다른 트랜잭션이 동시에
	 * 수정하지 못하도록 잠근다. 정원 체크와 카운터 증가를 원자적으로 만들기 위함.
	 *
	 * 마지막 한 자리에 여러 신청이 동시에 들어와도, 이 락 덕분에 한 번에 한
	 * 트랜잭션만 정원을 확인/증가시킬 수 있어 초과 신청이 방지된다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Course c WHERE c.id = :id")
	Optional<Course> findByIdForUpdate(@Param("id") Long id);

}