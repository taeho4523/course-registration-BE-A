package com.example.enrollment.waitlist.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.repository.CourseRepository;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.waitlist.domain.WaitlistEntry;
import com.example.enrollment.waitlist.domain.WaitlistStatus;
import com.example.enrollment.waitlist.repository.WaitlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WaitlistService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

	@Mock WaitlistRepository waitlistRepository;
	@Mock CourseRepository courseRepository;
	@Mock EnrollmentRepository enrollmentRepository;

	@InjectMocks WaitlistService waitlistService;

	private Course fullCourse() {
		Course course = new Course(1L, "강의", "설명", 10000, 1,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		course.changeStatus(CourseStatus.OPEN);
		course.increaseEnrolledCount(); // 1/1 가득 참
		return course;
	}

	@Test
	@DisplayName("정원이 가득 찬 강의에 대기 등록하면 순번을 받는다")
	void join_success() {
		Course full = fullCourse();
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(full));
		when(waitlistRepository.existsByCourseIdAndMemberIdAndStatus(1L, 20L, WaitlistStatus.WAITING))
			.thenReturn(false);
		when(waitlistRepository.countByCourseIdAndStatus(1L, WaitlistStatus.WAITING))
			.thenReturn(0L); // 첫 대기자
		when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		var response = waitlistService.join(20L, 1L);

		assertThat(response.position()).isEqualTo(1); // 0명 + 1 = 1번
		assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING);
	}

	@Test
	@DisplayName("정원에 여유가 있으면 대기 등록 거부")
	void join_seatAvailable_throws() {
		Course course = new Course(1L, "강의", "설명", 10000, 5,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		course.changeStatus(CourseStatus.OPEN); // 0/5, 여유 있음
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> waitlistService.join(20L, 1L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CAPACITY_AVAILABLE);
	}

	@Test
	@DisplayName("이미 대기 중이면 중복 거부")
	void join_alreadyWaiting_throws() {
		Course full = fullCourse();
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(full));
		when(waitlistRepository.existsByCourseIdAndMemberIdAndStatus(1L, 20L, WaitlistStatus.WAITING))
			.thenReturn(true); // 이미 대기 중

		assertThatThrownBy(() -> waitlistService.join(20L, 1L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WAITING);
	}

	@Test
	@DisplayName("승격: 대기자가 있으면 1번을 PENDING으로 승격하고 카운터를 채운다")
	void promote_success() {
		Course course = new Course(1L, "강의", "설명", 10000, 1,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		course.changeStatus(CourseStatus.OPEN); // 0/1, 방금 취소로 비워진 상태 가정
		WaitlistEntry waiting = new WaitlistEntry(1L, 20L, 1);

		when(waitlistRepository.findNextToPromote(1L)).thenReturn(Optional.of(waiting));
		when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));

		boolean promoted = waitlistService.promoteNextIfAvailable(1L);

		assertThat(promoted).isTrue();
		assertThat(waiting.getStatus()).isEqualTo(WaitlistStatus.PROMOTED); // 대기 항목 승격됨
		assertThat(course.getEnrolledCount()).isEqualTo(1); // 카운터 회수
		verify(enrollmentRepository).save(any()); // PENDING 신청 생성됨
	}

	@Test
	@DisplayName("승격: 대기자가 없으면 아무 일도 일어나지 않는다")
	void promote_noWaiting_returnsFalse() {
		when(waitlistRepository.findNextToPromote(1L)).thenReturn(Optional.empty());

		boolean promoted = waitlistService.promoteNextIfAvailable(1L);

		assertThat(promoted).isFalse();
		verify(enrollmentRepository, never()).save(any()); // 신청 생성 안 됨
		verify(courseRepository, never()).findByIdForUpdate(any()); // 카운터도 안 건드림
	}
}