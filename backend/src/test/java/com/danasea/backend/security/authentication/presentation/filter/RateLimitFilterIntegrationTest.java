package com.danasea.backend.security.authentication.presentation.filter;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Testcontainers
public class RateLimitFilterIntegrationTest {

    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379);

    private static RedisClient redisClient;
    private static RateLimitFilter rateLimitFilter;

    @BeforeAll
    static void setUp() {
        redis.start();
        String redisUrl = "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort();
        redisClient = RedisClient.create(redisUrl);
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);

        LettuceBasedProxyManager<byte[]> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();

        rateLimitFilter = new RateLimitFilter(proxyManager);
    }

    @AfterAll
    static void tearDown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        redis.stop();
    }

    @Test
    void shouldBlockAfter5Requests() throws Exception {
        String ip = "192.168.1.100";

        for (int i = 0; i < 5; i++) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);

            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getHeader("X-Forwarded-For")).thenReturn(ip);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Verify request was passed down the chain
            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).sendError(anyInt(), anyString());
        }

        // The 6th request should be blocked
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(ip);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Verify request was blocked
        verify(filterChain, never()).doFilter(request, response);
        verify(response, times(1)).sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                "You have exhausted your API Request Quota");
    }

    @Test
    void shouldAllowDifferentIPsSimultaneously() throws Exception {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        // Exhaust limit for IP1
        for (int i = 0; i < 50; i++) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);

            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getHeader("X-Forwarded-For")).thenReturn(ip1);

            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // 6th request from IP1 blocked
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletResponse response1 = mock(HttpServletResponse.class);
        FilterChain filterChain1 = mock(FilterChain.class);

        when(request1.getRequestURI()).thenReturn("/api/auth/login");
        when(request1.getHeader("X-Forwarded-For")).thenReturn(ip1);

        rateLimitFilter.doFilterInternal(request1, response1, filterChain1);
        verify(response1, times(1)).sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                "You have exhausted your API Request Quota");

        // 1st request from IP2 should be allowed
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        FilterChain filterChain2 = mock(FilterChain.class);

        when(request2.getRequestURI()).thenReturn("/api/auth/login");
        when(request2.getHeader("X-Forwarded-For")).thenReturn(ip2);

        rateLimitFilter.doFilterInternal(request2, response2, filterChain2);
        verify(filterChain2, times(1)).doFilter(request2, response2);
    }
}
