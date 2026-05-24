package com.example.enrollment.waitlist.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WaitlistEntry 도메인 단위 테스트.
 * 승격/만료 시 상태와 승격 시각이 올바르게 바뀌는지 검증한다.
 */
class WaitlistEntryTest {

	@Test
	@DisplayName("생성 시 WAITING 상태이고 승격 시각은 비어 있다")
	void create_initialState() {
		WaitlistEntry entry = new WaitlistEntry(1L, 20L, 1);

		assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.WAITING);
		assertThat(entry.getPosition()).isEqualTo(1);
		assertThat(entry.getPromotedAt()).isNull();
		assertThat(entry.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("승격하면 PROMOTED 상태가 되고 승격 시각이 기록된다")
	void promote_recordsTime() {
		WaitlistEntry entry = new WaitlistEntry(1L, 20L, 1);

		entry.promote();

		assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
		assertThat(entry.getPromotedAt()).isNotNull(); // 만료 기한 계산의 기준점
	}

	@Test
	@DisplayName("만료하면 EXPIRED 상태가 된다")
	void expire_changesStatus() {
		WaitlistEntry entry = new WaitlistEntry(1L, 20L, 1);

		entry.expire();

		assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
	}
}