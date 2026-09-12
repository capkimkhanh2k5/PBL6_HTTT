package com.danasea.backend.modules.admin.application.usecase;

import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyUnlockedException;
import com.danasea.backend.modules.admin.domain.exception.UserNotFoundException;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockUserUseCase {

    private final AccountInternalApi accountInternalApi;
    private final AuditLogInternalApi auditLogInternalApi;

    @Transactional
    public void execute(UUID actorId, UUID targetUserId) {
        if (actorId == null) {
            throw new IllegalArgumentException("Actor ID must not be null");
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user ID must not be null");
        }

        User user = accountInternalApi.findUserById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + targetUserId));

        if (Boolean.FALSE.equals(user.getIsLocked())) {
            throw new UserAlreadyUnlockedException("User is already unlocked");
        }

        user.setIsLocked(false);
        accountInternalApi.saveUser(user);

        auditLogInternalApi.recordAuditLog(
                actorId,
                "USER_UNLOCKED",
                "USER",
                targetUserId,
                ""
        );
    }
}
