package com.devrlab.shoppy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 테스트 전용 PostgreSQL·Redis 컨테이너. 개발용 DB(docker compose)는 건드리지 않음
// @ServiceConnection 이 접속 정보(url, host, port, 계정)를 컨테이너 값으로 자동 설정
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:16");
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redis() {
		return new GenericContainer<>("redis:7").withExposedPorts(6379);
	}
}
