package com.danasea.backend.modules.ai.application.port;

import com.danasea.backend.modules.ai.domain.TrustTier;

public interface RateLimiterPort {
    /**
     * Checks if the request is allowed based on rate limits.
     * 
     * @param userId    The ID of the user making the request.
     * @param ipAddress The IP address of the user (used for anomaly detection).
     * @param tier      The trust tier of the user.
     * @return true if the request is allowed, false if rate limited.
     */
    boolean isAllowed(String userId, String ipAddress, TrustTier tier);
}
