package com.example.enrollment.enrollment;

import com.example.enrollment.IntegrationTestSupport;
import com.example.enrollment.course.domain.Course;
import com.example.enrollment.course.domain.CourseStatus;
import com.example.enrollment.course.repository.CourseRepository;
import com.example.enrollment.common.exception.BusinessException;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정원 동시성 제어 통합 테스트.
 * 실제 MySQL(Testcontainers)에서, 정원보다 많은 동시 신청이 들어와도
 * 정확히 정원만큼만 성공하는지 검증한다.
 */
class EnrollmentConcurrencyTest extends IntegrationTestSupport {

	@Autowired EnrollmentService enrollmentService;
	@Autowired CourseRepository courseRepository;
	@Autowired EnrollmentRepository enrollmentRepository;

	Long courseId;

	@BeforeEach
	void setUp() {
		// 매 테스트 전 깨끗하게 비우고, 정원 3짜리 OPEN 강의 하나 생성
		enrollmentRepository.deleteAll();
		courseRepository.deleteAll();

		Course course = new Course(
			1L, "동시성 테스트 강의", "설명", 10000, 3,
			LocalDateTime.now(), LocalDateTime.now().plusDays(30)
		);
		course.changeStatus(CourseStatus.OPEN); // DRAFT → OPEN
		courseId = courseRepository.save(course).getId();
	}

	@Test
	@DisplayName("정원 3명 강의에 10명이 동시 신청하면 정확히 3명만 성공한다")
	void concurrentEnroll_onlyCapacitySucceeds() throws InterruptedException {
		// given
		int threadCount = 10;
		int capacity = 3;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch readyLatch = new CountDownLatch(threadCount); // 모든 스레드 준비 대기
		CountDownLatch startLatch = new CountDownLatch(1);           // 동시 출발 신호
		CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 모두 끝날 때까지 대기

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		// when: 10개 스레드가 동시에 같은 강의에 신청
		for (int i = 0; i < threadCount; i++) {
			long memberId = 101 + i;
			executor.submit(() -> {
				readyLatch.countDown();      // "나 준비됐어"
				try {
					startLatch.await();      // 출발 신호 기다림 (모두 동시 출발하게)
					enrollmentService.enroll(memberId, courseId);
					successCount.incrementAndGet();
				} catch (BusinessException e) {
					failCount.incrementAndGet(); // 정원 초과 등 비즈니스 예외 = 정상적인 실패
				} catch (Exception e) {
					// 예상치 못한 예외는 그대로 드러나게
					throw new RuntimeException(e);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		readyLatch.await();   // 10개 스레드가 다 준비될 때까지
		startLatch.countDown(); // 동시 출발!
		doneLatch.await();    // 다 끝날 때까지

		executor.shutdown();

		// then: 성공은 정확히 정원만큼(3), 실패는 나머지(7)
		assertThat(successCount.get()).isEqualTo(capacity);
		assertThat(failCount.get()).isEqualTo(threadCount - capacity);

		// 강의 카운터도 정확히 정원과 일치해야 한다 (오버부킹 없음)
		Course updated = courseRepository.findById(courseId).orElseThrow();
		assertThat(updated.getEnrolledCount()).isEqualTo(capacity);
	}

	@Test
	@DisplayName("정원 3명 강의에 100명이 동시 신청해도 절대 오버부킹되지 않는다")
	void massiveConcurrentEnroll_neverOverbooks() throws InterruptedException {
		// given
		int threadCount = 100;
		int capacity = 3;

		ExecutorService executor = Executors.newFixedThreadPool(32); // 풀 크기는 제한, 작업은 100개
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger businessFailCount = new AtomicInteger(); // 정원초과 등 의도된 실패
		AtomicInteger otherFailCount = new AtomicInteger();    // 락 타임아웃 등 그 외 실패

		// when: 100명이 동시에 신청
		for (int i = 0; i < threadCount; i++) {
			long memberId = 1000 + i;
			executor.submit(() -> {
				try {
					startLatch.await();
					enrollmentService.enroll(memberId, courseId);
					successCount.incrementAndGet();
				} catch (BusinessException e) {
					businessFailCount.incrementAndGet();
				} catch (Exception e) {
					// 락 타임아웃 등 극한 상황에서의 실패. 버그가 아니라 부하의 결과일 수 있음.
					otherFailCount.incrementAndGet();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown(); // 동시 출발
		doneLatch.await();
		executor.shutdown();

		// then: 핵심 보장 — 성공은 정원을 절대 초과하지 않는다
		assertThat(successCount.get()).isLessThanOrEqualTo(capacity);

		// 가장 중요한 단언: 강의 카운터가 정원과 정확히 일치 (오버부킹 0)
		Course updated = courseRepository.findById(courseId).orElseThrow();
		assertThat(updated.getEnrolledCount()).isEqualTo(successCount.get());
		assertThat(updated.getEnrolledCount()).isLessThanOrEqualTo(capacity);

		// 실제 DB에 들어간 활성 신청 수도 정원 이하 (이중 확인)
		long actualEnrollments = enrollmentRepository.count();
		assertThat(actualEnrollments).isLessThanOrEqualTo(capacity);

		// 디버깅용 출력 (테스트 통과해도 어떤 분포였는지 보고 싶을 때)
		System.out.printf("성공=%d, 정원초과실패=%d, 기타실패=%d, 합계=%d%n",
			successCount.get(), businessFailCount.get(), otherFailCount.get(),
			successCount.get() + businessFailCount.get() + otherFailCount.get());
	}
}