package com.example.enrollment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 공통 베이스.
 * 실제 MySQL 컨테이너를 띄우고, 스프링 데이터소스가 그 컨테이너를 바라보게 한다.
 * 통합 테스트 클래스들은 이 클래스를 상속하면 컨테이너 설정을 재사용할 수 있다.
 */
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestSupport {

	/**
	 * static으로 선언해 모든 테스트가 컨테이너 하나를 공유한다.
	 * (테스트마다 새로 띄우면 느리므로, 클래스 단위가 아니라 JVM 단위로 재사용)
	 * 운영과 동일한 MySQL 8.0 이미지를 사용해 비관적 락 동작을 진짜로 검증한다.
	 */
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
		.withDatabaseName("enrollment_test")
		.withUsername("test")
		.withPassword("test");

	static {
		MYSQL.start(); // 컨테이너 시작 (static 블록이라 클래스 로딩 시 1회)
	}

	/**
	 * 컨테이너가 동적으로 할당한 접속 정보를 스프링 프로퍼티에 주입한다.
	 * 컨테이너 포트는 매번 랜덤이라, 실행 시점에 받아와 연결해야 한다.
	 */
	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
	}
}