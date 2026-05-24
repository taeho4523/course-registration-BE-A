package com.example.enrollment.waitlist.dto;

import com.example.enrollment.waitlist.domain.WaitlistEntry;
import com.example.enrollment.waitlist.domain.WaitlistStatus;

/**
 * 대기열 응답.
 */
public record WaitlistResponse(
	Long waitlistId,
	Long courseId,
	Long memberId,
	int position,
	WaitlistStatus status
) {
	public static WaitlistResponse from(WaitlistEntry entry) {
		return new WaitlistResponse(
			entry.getId(),
			entry.getCourseId(),
			entry.getMemberId(),
			entry.getPosition(),
			entry.getStatus()
		);
	}
}