package com.example.enrollment.enrollment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentStatusTest {

	@Test
	@DisplayName("허용된 전이만 true를 반환한다")
	void allowedTransitions() {
		assertThat(EnrollmentStatus.PENDING.canTransitionTo(EnrollmentStatus.CONFIRMED)).isTrue();
		assertThat(EnrollmentStatus.PENDING.canTransitionTo(EnrollmentStatus.CANCELLED)).isTrue();
		assertThat(EnrollmentStatus.CONFIRMED.canTransitionTo(EnrollmentStatus.CANCELLED)).isTrue();
	}

	@Test
	@DisplayName("역방향/종착 전이는 false를 반환한다")
	void disallowedTransitions() {
		assertThat(EnrollmentStatus.CONFIRMED.canTransitionTo(EnrollmentStatus.PENDING)).isFalse();
		assertThat(EnrollmentStatus.CANCELLED.canTransitionTo(EnrollmentStatus.CONFIRMED)).isFalse();
		assertThat(EnrollmentStatus.CANCELLED.canTransitionTo(EnrollmentStatus.PENDING)).isFalse();
	}

	@Test
	@DisplayName("PENDING과 CONFIRMED만 활성 상태다")
	void activeStatuses() {
		assertThat(EnrollmentStatus.PENDING.isActive()).isTrue();
		assertThat(EnrollmentStatus.CONFIRMED.isActive()).isTrue();
		assertThat(EnrollmentStatus.CANCELLED.isActive()).isFalse();
	}
}