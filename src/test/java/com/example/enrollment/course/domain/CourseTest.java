package com.example.enrollment.course.domain;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Course 도메인 단위 테스트.
 * 정원 카운터 증감과 상태 전이 규칙을 검증한다.
 */
class CourseTest {

	private Course newCourse(int capacity) {
		return new Course(1L, "테스트 강의", "설명", 10000, capacity,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
	}

	@Nested
	@DisplayName("정원 카운터")
	class Capacity {

		@Test
		@DisplayName("여유가 있으면 신청 인원이 1 증가한다")
		void increase_success() {
			Course course = newCourse(2);

			course.increaseEnrolledCount();

			assertThat(course.getEnrolledCount()).isEqualTo(1);
			assertThat(course.hasAvailableSeat()).isTrue(); // 2명 중 1명, 아직 여유
		}

		@Test
		@DisplayName("정원이 가득 차면 증가 시 예외")
		void increase_overCapacity_throws() {
			Course course = newCourse(1);
			course.increaseEnrolledCount(); // 1/1, 가득 참

			assertThatThrownBy(course::increaseEnrolledCount)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CAPACITY_EXCEEDED);
		}

		@Test
		@DisplayName("취소로 인원이 감소하면 다시 신청 가능해진다")
		void decrease_thenAvailable() {
			Course course = newCourse(1);
			course.increaseEnrolledCount(); // 가득 참

			course.decreaseEnrolledCount(); // 한 자리 비움

			assertThat(course.getEnrolledCount()).isEqualTo(0);
			assertThat(course.hasAvailableSeat()).isTrue();
		}

		@Test
		@DisplayName("인원이 0일 때 감소해도 음수가 되지 않는다")
		void decrease_belowZero_guarded() {
			Course course = newCourse(1);

			course.decreaseEnrolledCount(); // 0에서 감소 시도

			assertThat(course.getEnrolledCount()).isEqualTo(0); // 음수 방지
		}
	}

	@Nested
	@DisplayName("상태 전이")
	class StatusTransition {

		@Test
		@DisplayName("DRAFT에서 OPEN으로 전이 가능")
		void draftToOpen() {
			Course course = newCourse(10); // 생성 시 DRAFT

			course.changeStatus(CourseStatus.OPEN);

			assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
		}

		@Test
		@DisplayName("DRAFT에서 CLOSED로 바로 건너뛰면 예외")
		void draftToClosed_throws() {
			Course course = newCourse(10);

			assertThatThrownBy(() -> course.changeStatus(CourseStatus.CLOSED))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode",
					ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
		}
	}
}