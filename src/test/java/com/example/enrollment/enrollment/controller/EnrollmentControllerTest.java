package com.example.enrollment.enrollment.controller;

import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

	@Autowired MockMvc mockMvc;
	@MockBean EnrollmentService enrollmentService;

	@Test
	@DisplayName("결제 확정 성공 시 CONFIRMED 응답")
	void confirm_success() throws Exception {
		var res = new EnrollmentResponse(1L, 1L, 5L, EnrollmentStatus.CONFIRMED,
			LocalDateTime.now(), LocalDateTime.now(), null);
		when(enrollmentService.confirm(anyLong(), anyLong())).thenReturn(res);

		mockMvc.perform(post("/api/enrollments/1/confirm").header("X-Member-Id", 5))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	@DisplayName("취소 성공 시 CANCELLED 응답")
	void cancel_success() throws Exception {
		var res = new EnrollmentResponse(1L, 1L, 5L, EnrollmentStatus.CANCELLED,
			LocalDateTime.now(), null, LocalDateTime.now());
		when(enrollmentService.cancel(anyLong(), anyLong())).thenReturn(res);

		mockMvc.perform(post("/api/enrollments/1/cancel").header("X-Member-Id", 5))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
	}
}