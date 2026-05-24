package com.example.enrollment.waitlist.scheduler;

import com.example.enrollment.waitlist.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기열 관련 주기 작업.
 * 승격 후 결제 기한이 지난 신청을 만료시키고 다음 대기자를 승격한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistScheduler {

	private final WaitlistService waitlistService;

	/**
	 * 일정 주기마다 만료 대상을 정리한다.
	 * fixedDelayString: 이전 실행이 끝난 뒤 다음 실행까지의 간격(ms). 설정값으로 외부화.
	 * 운영에선 분 단위가 적절하나, 테스트/시연 편의를 위해 설정으로 뺀다.
	 */
	@Scheduled(fixedDelayString = "${enrollment.waitlist.expire-scan-interval-ms:60000}")
	public void expireOverduePromotions() {
		int expired = waitlistService.expireOverduePromotions();
		if (expired > 0) {
			log.info("대기열 승격 만료 처리: {}건", expired);
		}
	}
}