package com.example.enrollment.member.domain;

/**
 * 멤버 역할.
 * CREATOR: 강의를 개설하는 강사(크리에이터)
 * STUDENT: 강의를 수강 신청하는 수강생(클래스메이트)
 *
 * 한 사람이 두 역할을 모두 수행할 수 있으나, 과제 단순화를 위해 대표 역할 하나만 보유한다.
 * 권한 체크는 "강의의 creator인가"처럼 소유 관계로 판단하므로 role은 보조 정보다.
 */
public enum MemberRole {
    CREATOR,
    STUDENT
}
