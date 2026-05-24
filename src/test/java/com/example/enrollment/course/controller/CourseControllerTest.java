package com.example.enrollment.course.controller;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import com.example.enrollment.course.dto.CourseDetailResponse;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.service.CourseService;
import com.example.enrollment.enrollment.service.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CourseController 웹 계층 테스트.
 * Service는 mock으로 대체하고, HTTP 요청/응답 매핑과 검증만 확인한다.
 */
@WebMvcTest(CourseController.class)
class CourseControllerTest {

	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;

	@MockBean CourseService courseService;
	@MockBean EnrollmentService enrollmentService; // CourseController가 의존하므로 필요

	private CourseDetailResponse sampleResponse() {
		return new CourseDetailResponse(
			1L, 1L, "강의", "설명", 10000, 30, 0, 30,
			CourseStatus.DRAFT,
			LocalDateTime.of(2025, 6, 1, 0, 0),
			LocalDateTime.of(2025, 7, 31, 23, 59));
	}

	@Test
	@DisplayName("강의 등록 성공 시 201과 강의 정보를 반환한다")
	void create_success() throws Exception {
		when(courseService.create(eq(1L), any())).thenReturn(sampleResponse());

		String body = """
            {
              "title": "강의",
              "description": "설명",
              "price": 10000,
              "capacity": 30,
              "startAt": "2025-06-01T00:00:00",
              "endAt": "2025-07-31T23:59:59"
            }
            """;

		mockMvc.perform(post("/api/courses")
				.header("X-Member-Id", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	@DisplayName("제목이 비어 있으면 검증 실패로 400")
	void create_blankTitle_400() throws Exception {
		String body = """
            {
              "title": "",
              "price": 10000,
              "capacity": 30,
              "startAt": "2025-06-01T00:00:00",
              "endAt": "2025-07-31T23:59:59"
            }
            """;

		mockMvc.perform(post("/api/courses")
				.header("X-Member-Id", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("X-Member-Id 헤더가 없으면 400")
	void create_noHeader_400() throws Exception {
		String body = """
            {
              "title": "강의",
              "price": 10000,
              "capacity": 30,
              "startAt": "2025-06-01T00:00:00",
              "endAt": "2025-07-31T23:59:59"
            }
            """;

		mockMvc.perform(post("/api/courses")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("강의 상세 조회 성공")
	void getDetail_success() throws Exception {
		when(courseService.getDetail(1L)).thenReturn(sampleResponse());

		mockMvc.perform(get("/api/courses/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.remainingCount").value(30));
	}

	@Test
	@DisplayName("존재하지 않는 강의 조회 시 404")
	void getDetail_notFound_404() throws Exception {
		when(courseService.getDetail(99L))
			.thenThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND));

		mockMvc.perform(get("/api/courses/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
	}
}