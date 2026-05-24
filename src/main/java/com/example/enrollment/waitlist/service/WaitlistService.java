package com.example.enrollment.waitlist.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.repository.CourseRepository;
import com.example.enrollment.waitlist.domain.WaitlistEntry;
import com.example.enrollment.waitlist.domain.WaitlistStatus;
import com.example.enrollment.waitlist.dto.WaitlistResponse;
import com.example.enrollment.waitlist.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;

/**
 * 대기열 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class WaitlistService {

	private final WaitlistRepository waitlistRepository;
	private final CourseRepository courseRepository;
	private final EnrollmentRepository enrollmentRepository;

	/**
	 * 대기 등록.
	 * 정원에 여유가 있으면 대기가 아니라 바로 신청해야 하므로 거부한다.
	 * 정원이 가득 찬 경우에만 대기열에 줄을 세우고 순번을 부여한다.
	 */
	@Transactional
	public WaitlistResponse join(Long memberId, Long courseId) {
		// 정원 상태를 정확히 보기 위해 Course에 락을 건다.
		// (신청과 동일한 자원을 보므로 일관되게 락으로 보호)
		Course course = courseRepository.findByIdForUpdate(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

		// 정원에 여유가 있으면 대기 불필요 → 바로 신청하라고 거부
		if (course.hasAvailableSeat()) {
			throw new BusinessException(ErrorCode.CAPACITY_AVAILABLE);
		}

		// 이미 대기 중이면 중복 거부
		boolean alreadyWaiting = waitlistRepository
			.existsByCourseIdAndMemberIdAndStatus(courseId, memberId, WaitlistStatus.WAITING);
		if (alreadyWaiting) {
			throw new BusinessException(ErrorCode.ALREADY_WAITING);
		}

		// 다음 순번 = 현재 대기 인원 + 1
		long waitingCount = waitlistRepository
			.countByCourseIdAndStatus(courseId, WaitlistStatus.WAITING);
		int position = (int) waitingCount + 1;

		WaitlistEntry entry = new WaitlistEntry(courseId, memberId, position);
		WaitlistEntry saved = waitlistRepository.save(entry);

		return WaitlistResponse.from(saved);
	}

	/**
	 * 자리가 났을 때 대기 1번을 승격시킨다.
	 * 취소(EnrollmentService.cancel) 트랜잭션 안에서 호출된다.
	 *
	 * 동작:
	 *  1) 해당 강의의 가장 앞선 WAITING 대기자를 비관적 락으로 조회
	 *     (동시 취소 시 같은 대기자를 중복 승격하지 않도록 락)
	 *  2) 없으면 아무것도 안 함 (대기자가 없는 정상 케이스)
	 *  3) 있으면: 대기 항목을 PROMOTED로 바꾸고,
	 *     그 대기자 명의로 PENDING 신청을 생성 + 강의 카운터를 다시 +1
	 *
	 * 주의: 이 메서드는 호출하는 쪽(cancel)에서 이미 Course 카운터를 -1 한 직후 호출되므로,
	 * 승격으로 자리를 다시 채우면 카운터를 +1 한다. 결과적으로 "취소 1명, 승격 1명"이면
	 * 카운터는 변동 없이 정원이 유지된다.
	 *
	 * @return 승격이 일어났으면 true
	 */
	@Transactional
	public boolean promoteNextIfAvailable(Long courseId) {
		// 1) 다음 승격 대상을 락 걸고 조회
		var nextOpt = waitlistRepository.findNextToPromote(courseId);
		if (nextOpt.isEmpty()) {
			return false; // 대기자 없음
		}
		WaitlistEntry next = nextOpt.get();

		// 2) Course를 락 걸고 카운터를 다시 채운다 (자리 회수)
		Course course = courseRepository.findByIdForUpdate(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

		// 승격으로 자리를 채우므로 카운터 +1.
		// increaseEnrolledCount는 정원 초과면 예외를 던지므로, 방금 취소로 빈 자리에만 들어간다.
		course.increaseEnrolledCount();

		// 3) 대기 항목 PROMOTED 처리 + PENDING 신청 생성
		next.promote();
		Enrollment enrollment = new Enrollment(courseId, next.getMemberId());
		enrollmentRepository.save(enrollment);

		return true;
	}
	/**
	 * 내 대기 순번 조회.
	 */
	@Transactional(readOnly = true)
	public WaitlistResponse getMyWaiting(Long memberId, Long courseId) {
		WaitlistEntry entry = waitlistRepository
			.findByCourseIdAndMemberIdAndStatus(courseId, memberId, WaitlistStatus.WAITING)
			.orElseThrow(() -> new BusinessException(ErrorCode.WAITLIST_NOT_FOUND));
		return WaitlistResponse.from(entry);
	}
}