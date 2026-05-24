package com.example.enrollment.course.repository;

import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CourseRepository 테스트.
 * JPA 계층만 띄워(@DataJpaTest) 커서 페이지네이션 쿼리를 검증한다.
 * 락이 아닌 일반 쿼리 동작 검증이므로 H2로 충분하다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 자동 사용
class CourseRepositoryTest {

	@Autowired CourseRepository courseRepository;

	@BeforeEach
	void setUp() {
		// 강의 5개 생성: 3개 OPEN, 2개 DRAFT
		for (int i = 0; i < 3; i++) {
			courseRepository.save(openCourse("OPEN 강의 " + i));
		}
		for (int i = 0; i < 2; i++) {
			courseRepository.save(draftCourse("DRAFT 강의 " + i));
		}
	}

	private Course openCourse(String title) {
		Course c = new Course(1L, title, "설명", 10000, 30,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		c.changeStatus(CourseStatus.OPEN);
		return c;
	}

	private Course draftCourse(String title) {
		return new Course(1L, title, "설명", 10000, 30,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
	}

	@Test
	@DisplayName("상태 필터 없이 조회하면 id 내림차순으로 가져온다")
	void findAll_orderByIdDesc() {
		List<Course> result = courseRepository
			.findByIdLessThanOrderByIdDesc(Long.MAX_VALUE, Limit.of(10));

		assertThat(result).hasSize(5);
		// id 내림차순 검증: 앞 요소의 id가 뒤 요소보다 커야 함
		assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
	}

	@Test
	@DisplayName("상태 필터로 OPEN만 조회된다")
	void findByStatus_onlyOpen() {
		List<Course> result = courseRepository
			.findByStatusAndIdLessThanOrderByIdDesc(
				CourseStatus.OPEN, Long.MAX_VALUE, Limit.of(10));

		assertThat(result).hasSize(3); // OPEN 3개만
		assertThat(result).allMatch(c -> c.getStatus() == CourseStatus.OPEN);
	}

	@Test
	@DisplayName("커서 기반 페이지네이션: size+1로 다음 페이지 존재를 판단할 수 있다")
	void cursorPagination() {
		// 첫 페이지: size=2 → 다음 존재 확인용으로 3개 요청
		List<Course> firstPage = courseRepository
			.findByIdLessThanOrderByIdDesc(Long.MAX_VALUE, Limit.of(3));
		assertThat(firstPage).hasSize(3); // 3개 왔으니 다음 페이지 있음(size 2 기준)

		// 마지막 요소의 id를 커서로 다음 페이지 조회
		Long cursor = firstPage.get(2).getId();
		List<Course> nextPage = courseRepository
			.findByIdLessThanOrderByIdDesc(cursor, Limit.of(3));

		// 다음 페이지 요소들은 모두 커서보다 작은 id
		assertThat(nextPage).allMatch(c -> c.getId() < cursor);
	}
}