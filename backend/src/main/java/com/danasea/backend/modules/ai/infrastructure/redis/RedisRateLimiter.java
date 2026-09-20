package com.danasea.backend.modules.ai.infrastructure.redis;

import com.danasea.backend.modules.ai.application.port.RateLimiterPort;
import com.danasea.backend.modules.ai.domain.TrustTier;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimiter implements RateLimiterPort {

    private static final String IP_RATE_LIMIT_PREFIX = "ai:ratelimit:ip:";
    private static final String USER_RATE_LIMIT_PREFIX = "ai:ratelimit:user:";
    private static final String ANON_RATE_LIMIT_PREFIX = "ai:ratelimit:anon:";

    private static final int IP_CAPACITY_PER_MINUTE = 100;
    private static final int UNVERIFIED_CAPACITY_PER_MINUTE = 5;
    private static final int VERIFIED_CAPACITY_PER_MINUTE = 20;
    private static final int TRUSTED_CAPACITY_PER_MINUTE = 50;

    private final LettuceBasedProxyManager<byte[]> proxyManager;

    public RedisRateLimiter(LettuceBasedProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean isAllowed(String userId, String ipAddress, TrustTier tier) {
        if (tier == TrustTier.RESTRICTED) {
            return false;
        }

        TrustTier effectiveTier = tier;
        // Layer 1: IP Anomaly Detection (soft throttle)
        if (ipAddress != null && !ipAddress.isBlank()) {
            byte[] ipKey = (IP_RATE_LIMIT_PREFIX + ipAddress).getBytes();
            BucketConfiguration ipConfig = BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(IP_CAPACITY_PER_MINUTE)
                            .refillGreedy(IP_CAPACITY_PER_MINUTE, Duration.ofMinutes(1))
                            .build())
                    .build();

            boolean ipAllowed = proxyManager.builder().build(ipKey, ipConfig).tryConsume(1);
            if (!ipAllowed && effectiveTier != TrustTier.RESTRICTED) {
                // Soft throttle: Degrade tier for this request if IP is spamming
                effectiveTier = TrustTier.UNVERIFIED;
            }
        }

        // Layer 2: User token bucket based on TrustTier
        if (userId != null && !userId.isBlank()) {
            if (effectiveTier == TrustTier.RESTRICTED) {
                return false;
            }

            int tierCapacity = switch (effectiveTier) {
                case UNVERIFIED -> UNVERIFIED_CAPACITY_PER_MINUTE;
                case VERIFIED -> VERIFIED_CAPACITY_PER_MINUTE;
                case TRUSTED -> TRUSTED_CAPACITY_PER_MINUTE;
                case RESTRICTED -> 0; // handled above
            };

            if (tierCapacity == 0) {
                return false;
            }

            byte[] userKey = (USER_RATE_LIMIT_PREFIX + userId).getBytes();
            BucketConfiguration userConfig = BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(tierCapacity)
                            .refillGreedy(tierCapacity, Duration.ofMinutes(1))
                            .build())
                    .build();

            return proxyManager.builder().build(userKey, userConfig).tryConsume(1);
        } else if (ipAddress != null && !ipAddress.isBlank()) {
            // Anonymous / Guest session: Apply key ai:ratelimit:anon:{ip} with TrustTier.UNVERIFIED
            if (effectiveTier == TrustTier.RESTRICTED) {
                return false;
            }

            int anonCapacity = UNVERIFIED_CAPACITY_PER_MINUTE;
            byte[] anonKey = (ANON_RATE_LIMIT_PREFIX + ipAddress).getBytes();
            BucketConfiguration anonConfig = BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(anonCapacity)
                            .refillGreedy(anonCapacity, Duration.ofMinutes(1))
                            .build())
                    .build();

            return proxyManager.builder().build(anonKey, anonConfig).tryConsume(1);
        }

        // If both userId and ipAddress are null or blank, reject to prevent rate-limit bypass
        return false;
    }
}
