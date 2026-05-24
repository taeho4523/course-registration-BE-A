package com.example.enrollment.course.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseStatusTest {

	@Test
	@DisplayName("허용된 전이만 true를 반환한다")
	void allowedTransitions() {
		assertThat(CourseStatus.DRAFT.canTransitionTo(CourseStatus.OPEN)).isTrue();
		assertThat(CourseStatus.OPEN.canTransitionTo(CourseStatus.CLOSED)).isTrue();
	}

	@Test
	@DisplayName("역방향/건너뛰기/종착 전이는 false를 반환한다")
	void disallowedTransitions() {
		assertThat(CourseStatus.DRAFT.canTransitionTo(CourseStatus.CLOSED)).isFalse(); // 건너뛰기
		assertThat(CourseStatus.OPEN.canTransitionTo(CourseStatus.DRAFT)).isFalse();   // 역방향
		assertThat(CourseStatus.CLOSED.canTransitionTo(CourseStatus.OPEN)).isFalse();  // 종착
		assertThat(CourseStatus.DRAFT.canTransitionTo(CourseStatus.DRAFT)).isFalse();  // 자기 자신
	}

	@Test
	@DisplayName("OPEN만 신청 가능 상태다")
	void onlyOpenIsEnrollable() {
		assertThat(CourseStatus.OPEN.isEnrollable()).isTrue();
		assertThat(CourseStatus.DRAFT.isEnrollable()).isFalse();
		assertThat(CourseStatus.CLOSED.isEnrollable()).isFalse();
	}
}