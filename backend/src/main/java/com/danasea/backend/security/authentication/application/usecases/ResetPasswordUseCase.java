package com.danasea.backend.security.authentication.application.usecases;

import java.time.Duration;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.PasswordResetToken;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.security.authentication.application.ports.PasswordHasher;
import com.danasea.backend.security.authentication.domain.exceptions.OtpExpiredException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpInvalidException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpMaxAttemptsExceededException;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResetPasswordUseCase {

    private final AccountInternalApi accountInternalApi;
    private final PasswordHasher passwordHasher;
    private final AuditLogInternalApi auditLogInternalApi;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(15);
    private static final String ATTEMPTS_PREFIX = "otp:reset-attempts:";

    public ResetPasswordUseCase(
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            AuditLogInternalApi auditLogInternalApi,
            StringRedisTemplate redisTemplate) {
        this.accountInternalApi = accountInternalApi;
        this.passwordHasher = passwordHasher;
        this.auditLogInternalApi = auditLogInternalApi;
        this.redisTemplate = redisTemplate;
    }

    public ResetPasswordUseCase(
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            AuditLogInternalApi auditLogInternalApi) {
        this(accountInternalApi, passwordHasher, auditLogInternalApi, null);
    }

    @Transactional
    public void execute(String email, String rawOtp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";

        User user = accountInternalApi.findUserByEmail(normalizedEmail)
                .orElseThrow(OtpInvalidException::new);

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new OtpInvalidException();
        }

        PasswordResetToken token = accountInternalApi.findLatestActivePasswordResetToken(user.getId())
                .orElseThrow(OtpExpiredException::new);

        String attemptKey = ATTEMPTS_PREFIX + user.getId();
        int attempts = getAttempts(attemptKey);

        if (attempts >= MAX_ATTEMPTS) {
            accountInternalApi.markPasswordResetTokenUsed(token.getId());
            clearAttempts(attemptKey);
            throw new OtpMaxAttemptsExceededException();
        }

        String hashedOtp = HashUtils.sha256(rawOtp);
        if (!hashedOtp.equals(token.getTokenHash())) {
            int newAttempts = incrementAttempts(attemptKey);
            if (newAttempts >= MAX_ATTEMPTS) {
                accountInternalApi.markPasswordResetTokenUsed(token.getId());
                clearAttempts(attemptKey);
                throw new OtpMaxAttemptsExceededException();
            }
            throw new OtpInvalidException();
        }

        // OTP is valid - reset password
        accountInternalApi.markPasswordResetTokenUsed(token.getId());
        clearAttempts(attemptKey);

        user.setPasswordHash(passwordHasher.hash(newPassword));
        accountInternalApi.saveUser(user);

        // Revoke all existing sessions
        accountInternalApi.revokeAllRefreshTokensByUserId(user.getId());

        // Record audit log
        auditLogInternalApi.recordAuditLog(
                user.getId(),
                "PASSWORD_RESET",
                "USER",
                user.getId(),
                "User reset password via OTP"
        );

        log.info("Password successfully reset for userId: {}", user.getId());
    }

    private int getAttempts(String key) {
        if (redisTemplate == null) {
            return 0;
        }
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Integer.parseInt(val) : 0;
        } catch (Exception e) {
            log.warn("Failed to read OTP reset attempts from Redis for key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    private int incrementAttempts(String key) {
        if (redisTemplate == null) {
            return 1;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, ATTEMPT_TTL);
            }
            return count != null ? count.intValue() : 1;
        } catch (Exception e) {
            log.warn("Failed to increment OTP reset attempts in Redis for key {}: {}", key, e.getMessage());
            return 1;
        }
    }

    private void clearAttempts(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Failed to delete OTP reset attempts in Redis for key {}: {}", key, e.getMessage());
            }
        }
    }
}
