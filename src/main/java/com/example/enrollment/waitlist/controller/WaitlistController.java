package com.example.enrollment.waitlist.controller;

import com.example.enrollment.waitlist.dto.WaitlistResponse;
import com.example.enrollment.waitlist.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 대기열 API.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

	private final WaitlistService waitlistService;

	/** 대기 등록 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WaitlistResponse join(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long courseId
	) {
		return waitlistService.join(memberId, courseId);
	}

	/** 내 대기 순번 조회 */
	@GetMapping("/me")
	public WaitlistResponse getMyWaiting(
		@RequestHeader("X-Member-Id") Long memberId,
		@PathVariable Long courseId
	) {
		return waitlistService.getMyWaiting(memberId, courseId);
	}
}
