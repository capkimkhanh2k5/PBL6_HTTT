package com.danasea.backend.security.authentication.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.PasswordResetToken;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.security.authentication.application.ports.PasswordHasher;
import com.danasea.backend.security.authentication.domain.exceptions.OtpExpiredException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpInvalidException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpMaxAttemptsExceededException;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AuditLogInternalApi auditLogInternalApi;

    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        resetPasswordUseCase = new ResetPasswordUseCase(accountInternalApi, passwordHasher, auditLogInternalApi);
    }

    @Test
    void execute_UserNotFound_ThrowsOtpInvalidException() {
        when(accountInternalApi.findUserByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resetPasswordUseCase.execute("unknown@example.com", "123456", "NewPassword1!"))
                .isInstanceOf(OtpInvalidException.class);
    }

    @Test
    void execute_UserLocked_ThrowsOtpInvalidException() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("locked@example.com");
        user.setIsLocked(true);

        when(accountInternalApi.findUserByEmailForUpdate("locked@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> resetPasswordUseCase.execute("locked@example.com", "123456", "NewPassword1!"))
                .isInstanceOf(OtpInvalidException.class);
    }

    @Test
    void execute_TokenNotFoundOrExpired_ThrowsOtpExpiredException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setIsLocked(false);

        when(accountInternalApi.findUserByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(accountInternalApi.findLatestActivePasswordResetToken(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resetPasswordUseCase.execute("user@example.com", "123456", "NewPassword1!"))
                .isInstanceOf(OtpExpiredException.class);
    }

    @Test
    void execute_InvalidOtp_IncrementsAttempts_ThrowsOtpInvalidException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setIsLocked(false);

        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash(HashUtils.sha256("654321"));
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(15));

        when(accountInternalApi.findUserByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(accountInternalApi.findLatestActivePasswordResetToken(userId)).thenReturn(Optional.of(token));
        token.setFailedAttempts(1);

        assertThatThrownBy(() -> resetPasswordUseCase.execute("user@example.com", "123456", "NewPassword1!"))
                .isInstanceOf(OtpInvalidException.class);

        verify(accountInternalApi, never()).markPasswordResetTokenUsed(any());
        verify(accountInternalApi, never()).saveUser(any());
    }

    @Test
    void execute_ExceedsMaxAttempts_InvalidatesToken_ThrowsOtpMaxAttemptsExceededException() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setIsLocked(false);

        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUserId(userId);
        token.setTokenHash(HashUtils.sha256("654321"));
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(15));

        when(accountInternalApi.findUserByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(accountInternalApi.findLatestActivePasswordResetToken(userId)).thenReturn(Optional.of(token));
        token.setFailedAttempts(4);

        assertThatThrownBy(() -> resetPasswordUseCase.execute("user@example.com", "123456", "NewPassword1!"))
                .isInstanceOf(OtpMaxAttemptsExceededException.class);

        verify(accountInternalApi).recordFailedPasswordResetAttempt(tokenId);
        verify(accountInternalApi, never()).saveUser(any());
    }

    @Test
    void execute_Success_MarksTokenUsed_UpdatesPassword_RevokesSessions_AndRecordsAuditLog() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("oldHashedPassword");
        user.setIsLocked(false);

        String rawOtp = "123456";
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUserId(userId);
        token.setTokenHash(HashUtils.sha256(rawOtp));
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(15));

        when(accountInternalApi.findUserByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(accountInternalApi.findLatestActivePasswordResetToken(userId)).thenReturn(Optional.of(token));
        when(passwordHasher.hash("NewPassword1!")).thenReturn("newHashedPassword");

        resetPasswordUseCase.execute("user@example.com", rawOtp, "NewPassword1!");

        verify(accountInternalApi).markPasswordResetTokenUsed(tokenId);
        assertThat(user.getPasswordHash()).isEqualTo("newHashedPassword");
        assertThat(user.getSessionVersion()).isEqualTo(1);
        verify(accountInternalApi).saveUser(user);
        verify(accountInternalApi).revokeAllRefreshTokensByUserId(userId);
        verify(auditLogInternalApi).recordAuditLog(
                eq(userId),
                eq("PASSWORD_RESET"),
                eq("USER"),
                eq(userId),
                anyString()
        );
    }
}
