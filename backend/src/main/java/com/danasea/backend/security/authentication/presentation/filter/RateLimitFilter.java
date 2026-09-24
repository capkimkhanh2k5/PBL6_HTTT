package com.danasea.backend.security.authentication.presentation.filter;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final LocalizedMessageService messages;
    private final ObjectMapper objectMapper;

    @Autowired
    public RateLimitFilter(
            LettuceBasedProxyManager<byte[]> proxyManager,
            ObjectProvider<LocalizedMessageService> messagesProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.proxyManager = proxyManager;
        this.messages = messagesProvider.getIfAvailable(LocalizedMessageService::standalone);
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    public RateLimitFilter(LettuceBasedProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
        this.messages = LocalizedMessageService.standalone();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            String ip = getClientIP(request);
            Bucket bucket = proxyManager.builder().build(ip.getBytes(), this::getConfig);

            ConsumptionProbe probe;
            try {
                probe = bucket.tryConsumeAndReturnRemaining(1);
            } catch (Exception e) {
                // Fail-open if Redis is down
                filterChain.doFilter(request, response);
                return;
            }
            if (probe.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                writeRateLimitResponse(response, "auth.rate_limit");
            }
        } else if (path.equals("/api/auth/otp/send")) {
            Principal principal = request.getUserPrincipal();
            if (principal != null && principal.getName() != null) {
                String email = principal.getName();
                String key = "otp-send:" + email;
                Bucket bucket = proxyManager.builder().build(key.getBytes(), this::getOtpConfig);

                ConsumptionProbe probe;
                try {
                    probe = bucket.tryConsumeAndReturnRemaining(1);
                } catch (Exception e) {
                    // Fail-open if Redis is down
                    filterChain.doFilter(request, response);
                    return;
                }
                if (probe.isConsumed()) {
                    response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                    filterChain.doFilter(request, response);
                } else {
                    long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                    response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                    writeRateLimitResponse(response, "auth.otp_rate_limit");
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private BucketConfiguration getConfig() {
        // limit 50 requests per 1 minute per IP
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1))))
                .build();
    }

    private BucketConfiguration getOtpConfig() {
        // limit 1 request per 1 minute per email
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(1))))
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, String messageKey) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse("RATE_LIMIT_EXCEEDED", messages.get(messageKey)));
    }
}
