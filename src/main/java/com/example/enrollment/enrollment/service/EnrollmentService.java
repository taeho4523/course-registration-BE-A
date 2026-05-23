package com.example.enrollment.enrollment.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.repository.CourseRepository;
import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.enrollment.common.response.CursorPage;
import com.example.enrollment.enrollment.dto.MyEnrollmentResponse;
import org.springframework.data.domain.Limit;

import java.util.List;

/**
 * 수강 신청 비즈니스 로직.
 * 정원 동시성 제어가 이 클래스의 핵심.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

	private final EnrollmentRepository enrollmentRepository;
	private final CourseRepository courseRepository;

	// 활성 상태(중복 신청 판단 기준)
	private static final List<EnrollmentStatus> ACTIVE_STATUSES =
		List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

	// 취소 가능 기간(일). application.yml의 enrollment.cancel.period-days 주입.
	@Value("${enrollment.cancel.period-days}")
	private int cancelPeriodDays;

	/**
	 * 수강 신청. 정원 동시성 제어의 핵심 메서드.
	 *
	 * 처리 순서:
	 *  1) Course를 비관적 락으로 조회 (SELECT ... FOR UPDATE) → 이 시점부터 행 잠금
	 *  2) 강의가 OPEN 상태인지 확인
	 *  3) 중복 신청(이미 활성 신청 보유) 확인
	 *  4) 정원 확인 + 카운터 증가 (엔티티 내부에서 원자적으로)
	 *  5) Enrollment(PENDING) 생성
	 *  → 트랜잭션 커밋 시 락 해제
	 */
	@Transactional
	public EnrollmentResponse enroll(Long memberId, Long courseId) {
		// 1) 비관적 락으로 강의 조회. 동시 신청이 와도 여기서 직렬화된다.
		Course course = courseRepository.findByIdForUpdate(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

		// 2) OPEN 상태가 아니면 신청 불가
		if (!course.isEnrollable()) {
			throw new BusinessException(ErrorCode.COURSE_NOT_OPEN);
		}

		// 3) 중복 신청 방지: 이미 활성 신청이 있으면 거부
		boolean alreadyEnrolled = enrollmentRepository
			.existsByCourseIdAndMemberIdAndStatusIn(courseId, memberId, ACTIVE_STATUSES);
		if (alreadyEnrolled) {
			throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
		}

		// 4) 정원 확인 + 카운터 증가. 정원 초과면 엔티티가 예외를 던진다.
		course.increaseEnrolledCount();

		// 5) 신청 생성 (PENDING)
		Enrollment enrollment = new Enrollment(courseId, memberId);
		Enrollment saved = enrollmentRepository.save(enrollment);

		return EnrollmentResponse.from(saved);
	}

	/**
	 * 결제 확정. PENDING → CONFIRMED.
	 * 상태 전이 검증은 엔티티(Enrollment.confirm)가 담당.
	 */
	@Transactional
	public EnrollmentResponse confirm(Long memberId, Long enrollmentId) {
		Enrollment enrollment = findMyEnrollmentOrThrow(memberId, enrollmentId);
		enrollment.confirm(); // 변경 감지로 커밋 시 자동 UPDATE
		return EnrollmentResponse.from(enrollment);
	}

	/**
	 * 수강 취소. PENDING/CONFIRMED → CANCELLED.
	 * 취소 시 강의 정원 카운터를 1 감소시켜야 하므로, 여기서도 Course에 락을 건다.
	 * (카운터 증감이 신청/취소 양쪽에서 일어나므로 일관되게 락으로 보호)
	 */
	@Transactional
	public EnrollmentResponse cancel(Long memberId, Long enrollmentId) {
		Enrollment enrollment = findMyEnrollmentOrThrow(memberId, enrollmentId);

		// 취소 처리. CONFIRMED면 취소 가능 기간(cancelPeriodDays)을 엔티티가 검증.
		enrollment.cancel(cancelPeriodDays);

		// 자리가 하나 비므로 강의 카운터 감소. 락으로 보호.
		Course course = courseRepository.findByIdForUpdate(enrollment.getCourseId())
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
		course.decreaseEnrolledCount();

		return EnrollmentResponse.from(enrollment);
	}

	/**
	 * 내 수강 신청 단건 조회 + 본인 소유 검증 (내부 헬퍼).
	 */
	private Enrollment findMyEnrollmentOrThrow(Long memberId, Long enrollmentId) {
		Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
		// 남의 신청을 조작하지 못하도록 소유자 검증
		if (!enrollment.getMemberId().equals(memberId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return enrollment;
	}
	/**
	 * 내 수강 신청 목록 (커서 기반).
	 * 응답에 강의 제목이 필요한데 Enrollment엔 courseId만 있으므로,
	 * 조회된 신청들의 courseId를 모아 강의를 한 번에 조회(IN 쿼리)해서 매핑한다.
	 * → 신청마다 강의를 따로 조회하면 N+1 문제가 생기므로 이를 피하기 위함.
	 */
	@Transactional(readOnly = true)
	public CursorPage<MyEnrollmentResponse> getMyEnrollments(
		Long memberId, EnrollmentStatus status, Long cursor, int size) {

		long effectiveCursor = (cursor != null) ? cursor : Long.MAX_VALUE;
		Limit limit = Limit.of(size + 1);

		List<Enrollment> enrollments = (status != null)
			? enrollmentRepository.findByMemberIdAndStatusAndIdLessThanOrderByIdDesc(
			memberId, status, effectiveCursor, limit)
			: enrollmentRepository.findByMemberIdAndIdLessThanOrderByIdDesc(
			memberId, effectiveCursor, limit);

		// courseId들을 모아 강의 제목을 한 번에 조회 (N+1 방지)
		List<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).distinct().toList();
		var courseTitleMap = courseRepository.findAllById(courseIds).stream()
			.collect(java.util.stream.Collectors.toMap(Course::getId, Course::getTitle));

		List<MyEnrollmentResponse> content = enrollments.stream()
			.map(e -> new MyEnrollmentResponse(
				e.getId(),
				e.getCourseId(),
				courseTitleMap.get(e.getCourseId()), // 매핑된 제목
				e.getStatus(),
				e.getEnrolledAt(),
				e.getConfirmedAt(),
				e.getCancelledAt()
			))
			.toList();

		return CursorPage.of(content, size, MyEnrollmentResponse::enrollmentId);
	}

}