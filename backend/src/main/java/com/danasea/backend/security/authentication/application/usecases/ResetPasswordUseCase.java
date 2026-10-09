package com.danasea.backend.security.authentication.application.usecases;

import java.util.Locale;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private static final int MAX_ATTEMPTS = 5;

    private final AccountInternalApi accountInternalApi;
    private final PasswordHasher passwordHasher;
    private final AuditLogInternalApi auditLogInternalApi;

    @Transactional(noRollbackFor = {OtpInvalidException.class, OtpMaxAttemptsExceededException.class})
    public void execute(String email, String rawOtp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        // All reset issuance, consumption and session creation use this account lock.
        User user = accountInternalApi.findUserByEmailForUpdate(normalizedEmail)
                .orElseThrow(OtpInvalidException::new);
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new OtpInvalidException();
        }

        PasswordResetToken token = accountInternalApi.findLatestActivePasswordResetToken(user.getId())
                .orElseThrow(OtpExpiredException::new);
        if (token.getFailedAttempts() >= MAX_ATTEMPTS) {
            accountInternalApi.markPasswordResetTokenUsed(token.getId());
            throw new OtpMaxAttemptsExceededException();
        }

        if (!HashUtils.sha256(rawOtp).equals(token.getTokenHash())) {
            accountInternalApi.recordFailedPasswordResetAttempt(token.getId());
            if (token.getFailedAttempts() + 1 >= MAX_ATTEMPTS) {
                throw new OtpMaxAttemptsExceededException();
            }
            throw new OtpInvalidException();
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.setSessionVersion(user.getSessionVersion() + 1);
        accountInternalApi.saveUser(user);
        accountInternalApi.markPasswordResetTokenUsed(token.getId());
        accountInternalApi.revokeAllRefreshTokensByUserId(user.getId());
        auditLogInternalApi.recordAuditLog(user.getId(), "PASSWORD_RESET", "USER", user.getId(),
                "User reset password via OTP");
        log.info("Password successfully reset for userId: {}", user.getId());
    }
}
