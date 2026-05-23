package com.example.enrollment.enrollment.domain;

/**
 * 수강 신청 상태.
 * PENDING(결제대기) → CONFIRMED(확정) → CANCELLED(취소)
 *
 * 허용 전이:
 *  - PENDING → CONFIRMED (결제 확정)
 *  - PENDING → CANCELLED (결제 전 취소)
 *  - CONFIRMED → CANCELLED (확정 후 취소, 7일 이내)
 * 그 외 전이는 모두 거부.
 */
public enum EnrollmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(EnrollmentStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false; // 종착 상태
        };
    }

    /** 정원 카운터에 포함되는 활성 상태인지 (PENDING, CONFIRMED) */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}
