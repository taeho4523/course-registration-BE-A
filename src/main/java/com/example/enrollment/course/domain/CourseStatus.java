package com.example.enrollment.course.domain;

/**
 * 강의 상태.
 * DRAFT(초안) → OPEN(모집중) → CLOSED(마감) 단방향 전이만 허용.
 *
 * 전이 규칙을 enum 자신이 알고 있게 하여(화이트리스트),
 * 서비스 코드 곳곳에 if 분기가 흩어지는 것을 방지한다.
 */
public enum CourseStatus {
    DRAFT,
    OPEN,
    CLOSED;

    /**
     * 현재 상태에서 target으로 전이 가능한지 검증.
     * 허용: DRAFT→OPEN, OPEN→CLOSED
     * 거부: 역방향, 건너뛰기(DRAFT→CLOSED), 자기 자신으로의 전이
     */
    public boolean canTransitionTo(CourseStatus target) {
        return switch (this) {
            case DRAFT -> target == OPEN;
            case OPEN -> target == CLOSED;
            case CLOSED -> false; // 종착 상태: 더 이상 전이 불가
        };
    }

    /** 신청 가능한 상태인지 (OPEN일 때만 신청 허용) */
    public boolean isEnrollable() {
        return this == OPEN;
    }
}
