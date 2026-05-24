package com.example.enrollment.waitlist.repository;

import com.example.enrollment.waitlist.domain.WaitlistEntry;
import com.example.enrollment.waitlist.domain.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class WaitlistRepositoryTest {

	@Autowired WaitlistRepository waitlistRepository;

	@BeforeEach
	void setUp() {
		// course 1에 대기자 2명 (position 1, 2)
		waitlistRepository.save(new WaitlistEntry(1L, 20L, 1));
		waitlistRepository.save(new WaitlistEntry(1L, 30L, 2));
	}

	@Test
	@DisplayName("현재 대기 인원을 센다")
	void countWaiting() {
		long count = waitlistRepository
			.countByCourseIdAndStatus(1L, WaitlistStatus.WAITING);
		assertThat(count).isEqualTo(2);
	}

	@Test
	@DisplayName("승격 대상은 position이 가장 앞선 대기자다")
	void findNextToPromote() {
		Optional<WaitlistEntry> next = waitlistRepository.findNextToPromote(1L);

		assertThat(next).isPresent();
		assertThat(next.get().getPosition()).isEqualTo(1); // 1번 대기자
		assertThat(next.get().getMemberId()).isEqualTo(20L);
	}

	@Test
	@DisplayName("결제 기한이 지난 PROMOTED 항목을 조회한다 (스케줄러용)")
	void findOverduePromotions() {
		// 승격된 지 오래된 항목 추가
		WaitlistEntry promoted = new WaitlistEntry(2L, 40L, 1);
		promoted.promote();
		waitlistRepository.save(promoted);

		// 미래 시각 기준으로 조회하면 방금 승격된 것도 "기한 지남"으로 잡힘
		List<WaitlistEntry> overdue = waitlistRepository
			.findByStatusAndPromotedAtBefore(
				WaitlistStatus.PROMOTED, LocalDateTime.now().plusHours(1));

		assertThat(overdue).hasSize(1);
		assertThat(overdue.get(0).getMemberId()).isEqualTo(40L);
	}
}