package com.example.enrollment.enrollment.domain;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강 신청.
 *
 * 상태 변화 시점을 세 개의 시각 필드로 추적한다.
 *  - enrolledAt: 신청 시각 (PENDING이 된 순간, 항상 존재)
 *  - confirmedAt: 결제 확정 시각 (CONFIRMED가 된 순간, 그 전엔 null)
 *  - cancelledAt: 취소 시각 (CANCELLED가 된 순간, 그 전엔 null)
 *
 * 취소 가능 기간은 confirmedAt 기준으로 계산한다(신청일 기준이 아님).
 */
@Entity
@Table(
    name = "enrollment",
    // 같은 멤버가 같은 강의에 활성 신청을 중복 보유하지 못하도록 하는 제약은
    // 취소 후 재신청 허용 때문에 단순 유니크로는 부족하다(취소 건도 행으로 남음).
    // 따라서 중복 체크는 애플리케이션 레벨(활성 상태 존재 여부 조회)로 처리한다.
    indexes = {
        @Index(name = "idx_enrollment_course", columnList = "course_id"),
        @Index(name = "idx_enrollment_member", columnList = "member_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    // 결제 확정 전에는 null
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // 취소 전에는 null
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public Enrollment(Long courseId, Long memberId) {
        this.courseId = courseId;
        this.memberId = memberId;
        this.status = EnrollmentStatus.PENDING; // 신청 직후엔 결제 대기
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * 결제 확정. PENDING → CONFIRMED.
     */
    public void confirm() {
        if (!status.canTransitionTo(EnrollmentStatus.CONFIRMED)) {
            throw new BusinessException(ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION);
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * 취소. PENDING/CONFIRMED → CANCELLED.
     * CONFIRMED인 경우에만 취소 가능 기간(cancelPeriodDays)을 검증한다.
     * PENDING(결제 전)은 기간 제한 없이 취소 가능.
     *
     * @param cancelPeriodDays 확정 후 취소 가능 일수 (설정값)
     */
    public void cancel(int cancelPeriodDays) {
        if (!status.canTransitionTo(EnrollmentStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.INVALID_ENROLLMENT_STATUS_TRANSITION);
        }
        // 확정 상태라면 취소 가능 기간 검증. confirmedAt이 기준 시점.
        if (this.status == EnrollmentStatus.CONFIRMED) {
            LocalDateTime deadline = this.confirmedAt.plusDays(cancelPeriodDays);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new BusinessException(ErrorCode.CANCEL_PERIOD_EXPIRED);
            }
        }
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status.isActive();
    }
}
