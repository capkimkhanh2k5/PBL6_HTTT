package com.danasea.backend.security.authentication.presentation.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasea.backend.configs.properties.RateLimitProperties;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnBean(LettuceBasedProxyManager.class)
public class OtpRateLimitFilter extends OncePerRequestFilter {

    private static final String OTP_SEND_PATH = "/api/auth/otp/send";

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties.Limit limit;

    public OtpRateLimitFilter(
            LettuceBasedProxyManager<byte[]> proxyManager,
            ObjectMapper objectMapper,
            RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.objectMapper = objectMapper;
        this.limit = properties.otp();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!OTP_SEND_PATH.equals(request.getRequestURI()) || request.getUserPrincipal() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String key = "otp-send:" + request.getUserPrincipal().getName();
            Bucket bucket = proxyManager.builder().build(
                    key.getBytes(StandardCharsets.UTF_8),
                    this::configuration);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
                return;
            }

            long retryAfter = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfter));
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "OTP requests are too frequent. Please try again later.");
        } catch (Exception exception) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    "OTP verification is temporarily unavailable. Please try again later.");
        }
    }

    private BucketConfiguration configuration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(limit.capacity(), Refill.intervally(limit.capacity(), limit.refillPeriod())))
                .build();
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(code, message));
    }
}
