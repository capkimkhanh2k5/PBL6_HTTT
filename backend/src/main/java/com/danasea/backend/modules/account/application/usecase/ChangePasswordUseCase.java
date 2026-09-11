package com.danasea.backend.modules.account.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.domain.models.AuditLog;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.audit.infrastructure.persistence.repositories.JpaAuditLogRepository;
import com.danasea.backend.modules.audit.infrastructure.persistence.entities.AuditLogJpaEntity;
import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authorization.domain.exception.AccessDeniedException;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final AccountInternalApi accountInternalApi;
    private final PasswordHasher passwordHasher;
    private final JpaAuditLogRepository auditLogRepository;

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

        AuditLogJpaEntity auditLog = new AuditLogJpaEntity();
        auditLog.setId(UUID.randomUUID());
        auditLog.setActorUserId(user.getId());
        auditLog.setAction("PASSWORD_CHANGED");
        auditLog.setEntityType("USER");
        auditLog.setEntityId(user.getId());
        auditLog.setMetadata("User changed their own password");
        auditLog.setCreatedAt(OffsetDateTime.now());
        auditLog.setUpdatedAt(OffsetDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
