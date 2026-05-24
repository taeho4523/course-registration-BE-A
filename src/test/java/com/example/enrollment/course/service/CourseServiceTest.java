package com.example.enrollment.course.service;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.dto.CourseCreateRequest;
import com.example.enrollment.course.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

	@Mock CourseRepository courseRepository;
	@InjectMocks CourseService courseService;

	private CourseCreateRequest request(LocalDateTime start, LocalDateTime end) {
		return new CourseCreateRequest("강의", "설명", 10000, 30, start, end);
	}

	@Test
	@DisplayName("정상 등록 시 DRAFT 강의가 생성된다")
	void create_success() {
		when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

		var res = courseService.create(1L,
			request(LocalDateTime.now(), LocalDateTime.now().plusDays(30)));

		assertThat(res.status()).isEqualTo(CourseStatus.DRAFT);
		assertThat(res.creatorId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("종료일이 시작일보다 앞서면 예외")
	void create_endBeforeStart_throws() {
		var req = request(LocalDateTime.now().plusDays(10), LocalDateTime.now());

		assertThatThrownBy(() -> courseService.create(1L, req))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("강사 본인이 아니면 상태 변경 시 권한 예외")
	void changeStatus_notOwner_throws() throws Exception {
		Course course = new Course(1L, "강의", "설명", 10000, 30,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		setField(course, "id", 1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

		// creatorId는 1인데 99가 변경 시도
		assertThatThrownBy(() -> courseService.changeStatus(99L, 1L, CourseStatus.OPEN))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 강의 상세 조회 시 예외")
	void getDetail_notFound_throws() {
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> courseService.getDetail(99L))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COURSE_NOT_FOUND);
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}
}