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
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyUnlockedException;
import com.danasea.backend.modules.admin.domain.exception.UserNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnlockUserUseCaseTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private AuditLogPort auditLogPort;

    private UnlockUserUseCase unlockUserUseCase;

    private UUID actorId;
    private UUID targetUserId;
    private User targetUser;

    @BeforeEach
    void setUp() {
        unlockUserUseCase = new UnlockUserUseCase(accountInternalApi, auditLogPort);
        actorId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEmail("target@example.com");
        targetUser.setFullName("Target User");
        targetUser.setRole(Role.CUSTOMER);
        targetUser.setIsLocked(true);
    }

    @Test
    void shouldUnlockUserSuccessfully() {
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        unlockUserUseCase.execute(actorId, targetUserId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(accountInternalApi).saveUser(userCaptor.capture());
        assertFalse(userCaptor.getValue().getIsLocked());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPort).saveAuditLog(auditCaptor.capture());
        AuditLog savedLog = auditCaptor.getValue();
        assertEquals("USER_UNLOCKED", savedLog.getAction());
        assertEquals("USER", savedLog.getEntityType());
        assertEquals(targetUserId, savedLog.getEntityId());
        assertEquals(actorId, savedLog.getActorUserId());

        verify(accountInternalApi, never()).revokeAllTokensByUserId(any());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> unlockUserUseCase.execute(actorId, targetUserId)
        );

        assertTrue(exception.getMessage().contains(targetUserId.toString()));
        verify(accountInternalApi, never()).saveUser(any());
        verify(auditLogPort, never()).saveAuditLog(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyUnlocked() {
        targetUser.setIsLocked(false);
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(
                UserAlreadyUnlockedException.class,
                () -> unlockUserUseCase.execute(actorId, targetUserId)
        );

        verify(accountInternalApi, never()).saveUser(any());
        verify(auditLogPort, never()).saveAuditLog(any());
    }
}
