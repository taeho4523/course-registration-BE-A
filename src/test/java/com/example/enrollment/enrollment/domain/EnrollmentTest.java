package com.example.enrollment.enrollment.domain;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enrollment 도메인 단위 테스트.
 * 스프링/DB 없이 순수 객체의 비즈니스 규칙만 검증한다.
 */
class EnrollmentTest {

	@Nested
	@DisplayName("결제 확정(confirm)")
	class Confirm {

		@Test
		@DisplayName("PENDING 상태면 CONFIRMED로 전이되고 confirmedAt이 기록된다")
		void confirm_success() {
			Enrollment enrollment = new Enrollment(1L, 1L); // 생성 시 PENDING

			enrollment.confirm();

			assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
			assertThat(enrollment.getConfirmedAt()).isNotNull();
		}

		@Test
		@DisplayName("이미 CONFIRMED인 건 다시 확정하면 예외")
		void confirm_alreadyConfirmed_throws() {
			Enrollment enrollment = new Enrollment(1L, 1L);
			enrollment.confirm();

			assertThatThrownBy(enrollment::confirm)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode",
					ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION);
		}
	}

	@Nested
	@DisplayName("취소(cancel)")
	class Cancel {

		@Test
		@DisplayName("PENDING은 기간 제한 없이 취소 가능")
		void cancel_pending_noLimit() {
			Enrollment enrollment = new Enrollment(1L, 1L);

			enrollment.cancel(7);

			assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
			assertThat(enrollment.getCancelledAt()).isNotNull();
		}

		@Test
		@DisplayName("CONFIRMED는 확정 후 7일 이내면 취소 가능")
		void cancel_confirmed_withinPeriod() {
			Enrollment enrollment = new Enrollment(1L, 1L);
			enrollment.confirm();

			enrollment.cancel(7); // 방금 확정했으니 7일 이내

			assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
		}

		@Test
		@DisplayName("CONFIRMED는 확정 후 7일이 지나면 취소 불가")
		void cancel_confirmed_afterPeriod_throws() throws Exception {
			Enrollment enrollment = new Enrollment(1L, 1L);
			enrollment.confirm();
			// confirmedAt을 8일 전으로 강제 조작 (시간 흐름을 시뮬레이션)
			setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(8));

			assertThatThrownBy(() -> enrollment.cancel(7))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANCEL_PERIOD_EXPIRED);
		}

		@Test
		@DisplayName("이미 취소된 건 다시 취소하면 예외")
		void cancel_alreadyCancelled_throws() {
			Enrollment enrollment = new Enrollment(1L, 1L);
			enrollment.cancel(7);

			assertThatThrownBy(() -> enrollment.cancel(7))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode",
					ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION);
		}
	}

	/**
	 * 테스트에서 시간 흐름을 시뮬레이션하기 위해 private 필드를 강제로 바꾼다.
	 * (confirmedAt에 setter가 없으므로 리플렉션 사용)
	 */
	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}