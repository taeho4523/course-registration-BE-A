package com.example.enrollment.course.domain;

import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의.
 *
 * 정원 관리의 핵심: enrolledCount(현재 신청 인원)를 카운터로 보유한다.
 * COUNT 쿼리로 매번 세는 대신 카운터를 두어, 정원 체크 시 Course 행 하나에
 * 비관적 락을 걸고 단순 비교로 처리할 수 있게 한다.
 *
 * 카운터와 실제 Enrollment 수의 정합성은 "증감을 항상 같은 트랜잭션 안에서"
 * 수행하는 것으로 보장한다. (increase/decrease는 신청·취소 트랜잭션 내에서만 호출)
 */
@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 강사(크리에이터)의 member id. 단순화를 위해 연관관계 매핑 대신 id 값만 보유.
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 100)
    private String title;

    // 강의 설명은 선택 입력 → nullable
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int capacity;

    // 현재 신청 인원 카운터 (PENDING + CONFIRMED 합). 취소 시 감소.
    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    public Course(Long creatorId, String title, String description,
                  int price, int capacity, LocalDateTime startAt, LocalDateTime endAt) {
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
        this.enrolledCount = 0;
        this.status = CourseStatus.DRAFT; // 등록 시 항상 DRAFT
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * 상태 전이. 화이트리스트(CourseStatus.canTransitionTo)에 어긋나면 예외.
     */
    public void changeStatus(CourseStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }
        this.status = target;
    }

    /** 정원 여유가 있는지 */
    public boolean hasAvailableSeat() {
        return enrolledCount < capacity;
    }

    /**
     * 신청 인원 1 증가. 정원 초과 시 예외.
     * 반드시 Course 행에 비관적 락을 건 트랜잭션 안에서 호출해야 동시성이 보장된다.
     */
    public void increaseEnrolledCount() {
        if (!hasAvailableSeat()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }
        this.enrolledCount++;
    }

    /** 신청 인원 1 감소(취소 시). 0 미만으로 내려가지 않도록 방어. */
    public void decreaseEnrolledCount() {
        if (this.enrolledCount > 0) {
            this.enrolledCount--;
        }
    }

    /** 신청 가능한 상태인지 (OPEN 여부) */
    public boolean isEnrollable() {
        return this.status.isEnrollable();
    }
}
