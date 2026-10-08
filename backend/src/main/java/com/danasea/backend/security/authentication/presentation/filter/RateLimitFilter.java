package com.danasea.backend.security.authentication.presentation.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasea.backend.configs.properties.RateLimitProperties;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
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
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String FORGOT_PASSWORD_PATH = "/api/auth/forgot-password";
    private static final String RESET_PASSWORD_PATH = "/api/auth/reset-password";

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties.Limit limit;
    private final RateLimitProperties.Limit forgotPasswordLimit;
    private final LocalizedMessageService messages;

    @Autowired
    public RateLimitFilter(
            LettuceBasedProxyManager<byte[]> proxyManager,
            ObjectMapper objectMapper,
            RateLimitProperties properties,
            LocalizedMessageService messages) {
        this.proxyManager = proxyManager;
        this.objectMapper = objectMapper;
        this.limit = properties.loginRegistration();
        this.forgotPasswordLimit = properties.forgotPassword();
        this.messages = messages;
    }

    public RateLimitFilter(LettuceBasedProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
        this.objectMapper = new ObjectMapper();
        this.limit = new RateLimitProperties.Limit(50, Duration.ofMinutes(1));
        this.forgotPasswordLimit = new RateLimitProperties.Limit(5, Duration.ofMinutes(1));
        this.messages = LocalizedMessageService.standalone();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isRateLimitedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean isForgotPassword = FORGOT_PASSWORD_PATH.equals(request.getRequestURI());
            String keyPrefix = isForgotPassword ? "forgot-pw:" : (RESET_PASSWORD_PATH.equals(request.getRequestURI()) ? "reset-pw:" : "");
            RateLimitProperties.Limit effectiveLimit = isForgotPassword ? forgotPasswordLimit : limit;

            Bucket bucket = proxyManager.builder().build(
                    (keyPrefix + request.getRemoteAddr()).getBytes(StandardCharsets.UTF_8),
                    () -> configuration(effectiveLimit));
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
                    messages.get("auth.rate_limit"));
        } catch (Exception exception) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    messages.get("auth.rate_limit_unavailable"));
        }
    }

    private boolean isRateLimitedPath(String path) {
        return LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path)
                || FORGOT_PASSWORD_PATH.equals(path) || RESET_PASSWORD_PATH.equals(path);
    }

    private BucketConfiguration configuration(RateLimitProperties.Limit limitToUse) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(limitToUse.capacity(), Refill.intervally(limitToUse.capacity(), limitToUse.refillPeriod())))
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
