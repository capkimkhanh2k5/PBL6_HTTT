package com.danasea.backend.security.authorization;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class BaseSecurityIntegrationTest {

    protected static final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withEnv("POSTGRES_DB", "testdb")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    protected static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("SPRING_DATASOURCE_URL", () -> "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/testdb");
        registry.add("SPRING_DATASOURCE_USERNAME", () -> "test");
        registry.add("SPRING_DATASOURCE_PASSWORD", () -> "test");
        registry.add("SPRING_REDIS_HOST", () -> redis.getHost());
        registry.add("SPRING_REDIS_PORT", () -> redis.getFirstMappedPort());
        registry.add("APP_JWT_SECRET", () -> "test-secret-key-that-is-at-least-256-bits-long-for-danasea-rbac-testing-purposes");
        registry.add("SPRING_MAIL_USERNAME", () -> "test@example.com");
        registry.add("SPRING_MAIL_PASSWORD", () -> "testpassword");
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }
}
