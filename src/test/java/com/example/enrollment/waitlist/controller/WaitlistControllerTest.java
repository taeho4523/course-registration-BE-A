package com.example.enrollment.waitlist.controller;

import com.example.enrollment.waitlist.domain.WaitlistStatus;
import com.example.enrollment.waitlist.dto.WaitlistResponse;
import com.example.enrollment.waitlist.service.WaitlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WaitlistController.class)
class WaitlistControllerTest {

	@Autowired MockMvc mockMvc;
	@MockBean WaitlistService waitlistService;

	@Test
	@DisplayName("대기 등록 성공 시 201과 순번 반환")
	void join_success() throws Exception {
		var res = new WaitlistResponse(1L, 1L, 20L, 1, WaitlistStatus.WAITING);
		when(waitlistService.join(anyLong(), anyLong())).thenReturn(res);

		mockMvc.perform(post("/api/courses/1/waitlist").header("X-Member-Id", 20))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.position").value(1))
			.andExpect(jsonPath("$.status").value("WAITING"));
	}
}