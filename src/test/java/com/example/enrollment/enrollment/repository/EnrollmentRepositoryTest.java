package com.example.enrollment.enrollment.repository;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EnrollmentRepositoryTest {

	@Autowired EnrollmentRepository enrollmentRepository;

	@BeforeEach
	void setUp() {
		// member 5가 course 1에 신청(PENDING), course 2에 신청 후 확정(CONFIRMED)
		enrollmentRepository.save(new Enrollment(1L, 5L));
		Enrollment confirmed = new Enrollment(2L, 5L);
		confirmed.confirm();
		enrollmentRepository.save(confirmed);
		// member 6이 course 1에 신청
		enrollmentRepository.save(new Enrollment(1L, 6L));
	}

	@Test
	@DisplayName("활성 신청 존재 여부를 정확히 판단한다")
	void existsActiveEnrollment() {
		boolean exists = enrollmentRepository.existsByCourseIdAndMemberIdAndStatusIn(
			1L, 5L, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
		assertThat(exists).isTrue();

		// course 99엔 신청한 적 없음
		boolean notExists = enrollmentRepository.existsByCourseIdAndMemberIdAndStatusIn(
			99L, 5L, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
		assertThat(notExists).isFalse();
	}

	@Test
	@DisplayName("내 신청 목록을 id 내림차순으로 조회한다")
	void findMyEnrollments() {
		List<Enrollment> result = enrollmentRepository
			.findByMemberIdAndIdLessThanOrderByIdDesc(5L, Long.MAX_VALUE, Limit.of(10));

		assertThat(result).hasSize(2); // member 5의 신청 2개
		assertThat(result).allMatch(e -> e.getMemberId().equals(5L));
	}

	@Test
	@DisplayName("특정 강의의 신청 목록을 조회한다")
	void findByCourse() {
		List<Enrollment> result = enrollmentRepository
			.findByCourseIdAndIdLessThanOrderByIdDesc(1L, Long.MAX_VALUE, Limit.of(10));

		assertThat(result).hasSize(2); // course 1엔 member 5, 6
		assertThat(result).allMatch(e -> e.getCourseId().equals(1L));
	}

	@Test
	@DisplayName("강의+멤버+상태로 신청을 조회한다 (스케줄러용)")
	void findByCourseMemberStatus() {
		List<Enrollment> result = enrollmentRepository
			.findByCourseIdAndMemberIdAndStatusIn(
				2L, 5L, List.of(EnrollmentStatus.CONFIRMED));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
	}
}