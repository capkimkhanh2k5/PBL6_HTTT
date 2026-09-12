package com.danasea.backend.modules.account.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authorization.domain.exception.AccessDeniedException;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final AccountInternalApi accountInternalApi;
    private final PasswordHasher passwordHasher;
    private final AuditLogInternalApi auditLogInternalApi;

    @Transactional
    public void execute(String email, String currentPassword, String newPassword) {
        User user = accountInternalApi.findUserByEmail(email)
                .orElseThrow(() -> new AccessDeniedException());

        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        accountInternalApi.saveUser(user);

        accountInternalApi.revokeAllRefreshTokensByUserId(user.getId());

        auditLogInternalApi.recordAuditLog(
                user.getId(),
                "PASSWORD_CHANGED",
                "USER",
                user.getId(),
                "User changed their own password"
        );
    }
}
