package com.example.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 수강 신청 시스템 메인 애플리케이션.
 *
 * - @ConfigurationPropertiesScan: enrollment.* 설정값을 타입 안전하게 바인딩하기 위함
 * - @EnableScheduling: 대기열 승격 후 결제 기한 만료 처리 스케줄러용
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EnrollmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentApplication.class, args);
    }
}
