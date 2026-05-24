package com.example.enrollment.waitlist.repository;

import com.example.enrollment.waitlist.domain.WaitlistEntry;
import com.example.enrollment.waitlist.domain.WaitlistStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 대기열 저장소.
 */
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

	/**
	 * 중복 대기 체크: 이미 WAITING 상태로 줄 서 있는지.
	 */
	boolean existsByCourseIdAndMemberIdAndStatus(
		Long courseId, Long memberId, WaitlistStatus status);

	/**
	 * 현재 대기 인원 수 (다음 순번 계산용).
	 */
	long countByCourseIdAndStatus(Long courseId, WaitlistStatus status);

	/**
	 * 내 대기 항목 조회.
	 */
	Optional<WaitlistEntry> findByCourseIdAndMemberIdAndStatus(
		Long courseId, Long memberId, WaitlistStatus status);

	/**
	 * 승격 대상: 해당 강의에서 가장 앞선(position 최소) WAITING 항목을 비관적 락으로 조회.
	 * 취소로 자리가 났을 때 동시에 여러 취소가 일어나도, 같은 대기자를 중복 승격하지 않도록 락을 건다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT w FROM WaitlistEntry w " +
		"WHERE w.courseId = :courseId AND w.status = 'WAITING' " +
		"ORDER BY w.position ASC LIMIT 1")
	Optional<WaitlistEntry> findNextToPromote(@Param("courseId") Long courseId);

	/**
	 * 승격됐지만 결제 기한이 지난 PROMOTED 항목들 (스케줄러용).
	 * 이들을 만료시키고 다음 대기자를 승격한다.
	 */
	List<WaitlistEntry> findByStatus(WaitlistStatus status);

	/**
	 * 결제 기한이 지난 PROMOTED 항목 조회 (스케줄러용).
	 * promotedAt이 기준 시각(deadline)보다 이전인 것들 = 기한 초과.
	 */
	List<WaitlistEntry> findByStatusAndPromotedAtBefore(
		WaitlistStatus status, LocalDateTime deadline);
}