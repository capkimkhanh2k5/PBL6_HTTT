package com.danasea.backend.modules.admin.application.usecase;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.port.AuditLogPort;
import com.danasea.backend.modules.audit.domain.models.AuditLog;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.domain.exception.SelfLockNotAllowedException;
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyLockedException;
import com.danasea.backend.modules.admin.domain.exception.UserNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockUserUseCaseTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private AuditLogPort auditLogPort;

    private LockUserUseCase lockUserUseCase;

    private UUID actorId;
    private UUID targetUserId;
    private User targetUser;

    @BeforeEach
    void setUp() {
        lockUserUseCase = new LockUserUseCase(accountInternalApi, auditLogPort);
        actorId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEmail("target@example.com");
        targetUser.setFullName("Target User");
        targetUser.setRole(Role.CUSTOMER);
        targetUser.setIsLocked(false);
    }

    @Test
    void shouldLockUserSuccessfully() {
        String reason = "Violation of service terms";
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        lockUserUseCase.execute(actorId, targetUserId, reason);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(accountInternalApi).saveUser(userCaptor.capture());
        assertTrue(userCaptor.getValue().getIsLocked());

        verify(accountInternalApi).revokeAllTokensByUserId(targetUserId);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPort).saveAuditLog(auditCaptor.capture());
        AuditLog savedLog = auditCaptor.getValue();
        assertEquals("USER_LOCKED", savedLog.getAction());
        assertEquals("USER", savedLog.getEntityType());
        assertEquals(targetUserId, savedLog.getEntityId());
        assertEquals(actorId, savedLog.getActorUserId());
        assertEquals("{\"reason\":\"Violation of service terms\"}", savedLog.getMetadata());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> lockUserUseCase.execute(actorId, targetUserId, "Some reason")
        );

        assertTrue(exception.getMessage().contains(targetUserId.toString()));
        verify(accountInternalApi, never()).saveUser(any());
        verify(accountInternalApi, never()).revokeAllTokensByUserId(any());
        verify(auditLogPort, never()).saveAuditLog(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyLocked() {
        targetUser.setIsLocked(true);
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(
                UserAlreadyLockedException.class,
                () -> lockUserUseCase.execute(actorId, targetUserId, "Some reason")
        );

        verify(accountInternalApi, never()).saveUser(any());
        verify(accountInternalApi, never()).revokeAllTokensByUserId(any());
        verify(auditLogPort, never()).saveAuditLog(any());
    }

    @Test
    void shouldThrowWhenAdminAttemptsSelfLock() {
        UUID selfId = actorId;

        assertThrows(
                SelfLockNotAllowedException.class,
                () -> lockUserUseCase.execute(selfId, selfId, "Attempt self lock")
        );

        verify(accountInternalApi, never()).findUserById(any());
        verify(accountInternalApi, never()).saveUser(any());
        verify(accountInternalApi, never()).revokeAllTokensByUserId(any());
        verify(auditLogPort, never()).saveAuditLog(any());
    }

    @Test
    void shouldLockUserWithNullReasonSuccessfully() {
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        lockUserUseCase.execute(actorId, targetUserId, null);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPort).saveAuditLog(auditCaptor.capture());
        assertEquals("{\"reason\":\"\"}", auditCaptor.getValue().getMetadata());
    }
}
