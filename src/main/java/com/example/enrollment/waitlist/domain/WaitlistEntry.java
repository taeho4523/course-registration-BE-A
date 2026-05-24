package com.example.enrollment.waitlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대기열 항목.
 *
 * 정원이 가득 찬 강의에 대기 등록하면 순번(position)을 부여받는다.
 * 취소로 자리가 나면 position이 가장 앞선 WAITING 항목을 승격(PROMOTED)시킨다.
 *
 * Enrollment와 분리한 이유: 대기 순번/승격/결제기한 같은 관심사가
 * 신청 본류의 상태 전이와 성격이 달라, 한 엔티티에 섞으면 상태가 지저분해진다.
 */
@Entity
@Table(
    name = "waitlist_entry",
    indexes = {
        @Index(name = "idx_waitlist_course_position", columnList = "course_id, position")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 대기 순번 (1부터 시작). 같은 강의 내에서 대기 등록 순서.
    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 승격된 시각. 승격 후 결제 기한 계산의 기준점. 승격 전엔 null.
    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    public WaitlistEntry(Long courseId, Long memberId, int position) {
        this.courseId = courseId;
        this.memberId = memberId;
        this.position = position;
        this.status = WaitlistStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    /** 자리가 나서 신청으로 전환됨. 승격 시각을 기록한다. */
    public void promote() {
        this.status = WaitlistStatus.PROMOTED;
        this.promotedAt = LocalDateTime.now();
    }
    /** 대기 취소 또는 기한 만료 */
    public void expire() {
        this.status = WaitlistStatus.EXPIRED;
    }
}
