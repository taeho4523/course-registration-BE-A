package com.example.enrollment.enrollment.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.repository.CourseRepository;
import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.member.repository.MemberRepository;
import com.example.enrollment.waitlist.service.WaitlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * EnrollmentService 단위 테스트.
 * Repository를 mock으로 대체해, DB 없이 서비스의 분기 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

	@Mock CourseRepository courseRepository;
	@Mock EnrollmentRepository enrollmentRepository;
	@Mock MemberRepository memberRepository;
	@Mock WaitlistService waitlistService;

	@InjectMocks EnrollmentService enrollmentService;

	private Course openCourse(int capacity) {
		Course course = new Course(1L, "강의", "설명", 10000, capacity,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		course.changeStatus(CourseStatus.OPEN);
		return course;
	}

	@Test
	@DisplayName("정상 신청 시 PENDING 신청이 생성되고 카운터가 증가한다")
	void enroll_success() {
		Course course = openCourse(10);
		// mock 시나리오: 락 조회하면 이 강의를 준다
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
		// 중복 신청 없음
		when(enrollmentRepository.existsByCourseIdAndMemberIdAndStatusIn(eq(1L), eq(5L), anyList()))
			.thenReturn(false);
		// save하면 받은 걸 그대로 돌려준다
		when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

		var response = enrollmentService.enroll(5L, 1L);

		assertThat(response.status()).isEqualTo(EnrollmentStatus.PENDING);
		assertThat(course.getEnrolledCount()).isEqualTo(1); // 카운터 증가 확인
		verify(enrollmentRepository).save(any(Enrollment.class)); // save 호출됐는지
	}

	@Test
	@DisplayName("OPEN이 아닌 강의에 신청하면 예외")
	void enroll_notOpen_throws() {
		Course draft = new Course(1L, "강의", "설명", 10000, 10,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30)); // DRAFT
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draft));

		assertThatThrownBy(() -> enrollmentService.enroll(5L, 1L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_NOT_OPEN);
	}

	@Test
	@DisplayName("이미 활성 신청이 있으면 중복 신청 예외")
	void enroll_duplicate_throws() {
		Course course = openCourse(10);
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByCourseIdAndMemberIdAndStatusIn(eq(1L), eq(5L), anyList()))
			.thenReturn(true); // 이미 신청함

		assertThatThrownBy(() -> enrollmentService.enroll(5L, 1L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ENROLLED);
	}

	@Test
	@DisplayName("존재하지 않는 강의면 예외")
	void enroll_courseNotFound_throws() {
		when(courseRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.enroll(5L, 99L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_NOT_FOUND);
	}

	@Test
	@DisplayName("남의 신청을 취소하려 하면 권한 예외")
	void cancel_notOwner_throws() throws Exception {
		Enrollment enrollment = new Enrollment(1L, 5L); // memberId=5의 신청
		setField(enrollment, "id", 100L);
		when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(enrollment));

		// 다른 사람(99)이 취소 시도
		assertThatThrownBy(() -> enrollmentService.cancel(99L, 100L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}