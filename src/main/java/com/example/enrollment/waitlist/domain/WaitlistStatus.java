package com.example.enrollment.waitlist.domain;

/**
 * 대기열 상태.
 *  - WAITING: 대기 중 (순번 보유)
 *  - PROMOTED: 자리가 나서 신청(PENDING)으로 전환됨
 *  - EXPIRED: 대기 취소 또는 승격 후 기한 내 미결제로 만료
 */
public enum WaitlistStatus {
    WAITING,
    PROMOTED,
    EXPIRED
}
