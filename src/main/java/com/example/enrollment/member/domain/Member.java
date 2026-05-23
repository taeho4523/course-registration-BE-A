package com.example.enrollment.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버(사용자).
 *
 * 강사(크리에이터)와 수강생(클래스메이트)을 별도 테이블로 나누지 않고
 * 단일 엔티티 + role 필드로 구분한다.
 * 한 사람이 강사이면서 동시에 다른 강의의 수강생일 수 있기 때문이다.
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자. 외부 직접 생성 차단.
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING) // ORDINAL 대신 STRING: 순서 변경에 안전하고 DB 값이 읽기 쉬움
    @Column(nullable = false, length = 20)
    private MemberRole role;

    public Member(String name, MemberRole role) {
        this.name = name;
        this.role = role;
    }
}
