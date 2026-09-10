package com.danasea.backend.modules.admin.application.usecase;

import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.domain.models.AuditLog;
import com.danasea.backend.modules.audit.application.port.AuditLogPort;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyUnlockedException;
import com.danasea.backend.modules.admin.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockUserUseCase {

    private final AccountInternalApi accountInternalApi;
    private final AuditLogPort auditLogPort;

    public void execute(UUID actorId, UUID targetUserId) {
        User user = accountInternalApi.findUserById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + targetUserId));

        if (!Boolean.TRUE.equals(user.getIsLocked())) {
            throw new UserAlreadyUnlockedException("User is already unlocked");
        }

        user.setIsLocked(false);
        accountInternalApi.saveUser(user);

        AuditLog auditLog = new AuditLog();
        auditLog.setAction("USER_UNLOCKED");
        auditLog.setEntityType("USER");
        auditLog.setEntityId(targetUserId);
        auditLog.setActorUserId(actorId);

        auditLogPort.saveAuditLog(auditLog);
    }
}
