package com.example.enrollment.course.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.common.response.CursorPage;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.dto.*;
import com.example.enrollment.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강의 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor // final 필드를 받는 생성자를 Lombok이 생성 → 생성자 주입
public class CourseService {

	private final CourseRepository courseRepository;

	/**
	 * 강의 등록. 등록자가 강사(creator)가 되며 상태는 DRAFT로 시작.
	 */
	@Transactional
	public CourseDetailResponse create(Long creatorId, CourseCreateRequest request) {
		// 두 필드 간 관계 검증: 종료일은 시작일보다 뒤여야 한다 (어노테이션으로 못 잡는 부분)
		if (!request.endAt().isAfter(request.startAt())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 뒤여야 합니다.");
		}

		Course course = new Course(
			creatorId,
			request.title(),
			request.description(),
			request.price(),
			request.capacity(),
			request.startAt(),
			request.endAt()
		);
		Course saved = courseRepository.save(course);
		return CourseDetailResponse.from(saved);
	}

	/**
	 * 강의 상태 변경. 강사 본인만 가능하며, 전이 규칙은 엔티티가 검증한다.
	 */
	@Transactional
	public CourseDetailResponse changeStatus(Long memberId, Long courseId, CourseStatus target) {
		Course course = findCourseOrThrow(courseId);
		validateOwner(course, memberId); // 본인 강의인지 확인

		course.changeStatus(target); // 화이트리스트 검증은 엔티티 내부에서
		// 변경 감지(dirty checking)로 트랜잭션 커밋 시 자동 UPDATE. 별도 save 불필요.
		return CourseDetailResponse.from(course);
	}

	/**
	 * 강의 상세 조회.
	 */
	@Transactional(readOnly = true)
	public CourseDetailResponse getDetail(Long courseId) {
		return CourseDetailResponse.from(findCourseOrThrow(courseId));
	}

	/**
	 * 강의 목록 조회 (커서 기반, 상태 필터 선택).
	 */
	@Transactional(readOnly = true)
	public CursorPage<CourseSummaryResponse> getList(CourseStatus status, Long cursor, int size) {
		// 첫 페이지면 커서가 없으므로 가장 큰 id 값으로 대체 → "처음부터" 효과
		long effectiveCursor = (cursor != null) ? cursor : Long.MAX_VALUE;

		// size+1개를 조회해서 다음 페이지 존재 여부를 판단 (CursorPage가 처리)
		Limit limit = Limit.of(size + 1);

		List<Course> courses = (status != null)
			? courseRepository.findByStatusAndIdLessThanOrderByIdDesc(status, effectiveCursor, limit)
			: courseRepository.findByIdLessThanOrderByIdDesc(effectiveCursor, limit);

		// 엔티티 리스트를 요약 DTO 리스트로 변환
		List<CourseSummaryResponse> content = courses.stream()
			.map(CourseSummaryResponse::from)
			.toList();

		return CursorPage.of(content, size, CourseSummaryResponse::id);
	}

	// --- 내부 헬퍼 ---

	private Course findCourseOrThrow(Long courseId) {
		return courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
	}

	private void validateOwner(Course course, Long memberId) {
		if (!course.getCreatorId().equals(memberId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}