package com.danasea.backend.modules.admin.application.usecases;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.domain.exceptions.SelfLockNotAllowedException;
import com.danasea.backend.modules.admin.domain.exceptions.UserAlreadyLockedException;
import com.danasea.backend.modules.admin.domain.exceptions.UserNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockUserUseCaseTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private AuditLogInternalApi auditLogInternalApi;

    private LockUserUseCase lockUserUseCase;

    private UUID actorId;
    private UUID targetUserId;
    private User targetUser;

    @BeforeEach
    void setUp() {
        lockUserUseCase = new LockUserUseCase(accountInternalApi, auditLogInternalApi);
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

        verify(accountInternalApi).saveUser(targetUser);
        assertTrue(targetUser.getIsLocked());

        verify(accountInternalApi).revokeAllTokensByUserId(targetUserId);

        verify(auditLogInternalApi).recordAuditLog(
                eq(actorId),
                eq("USER_LOCKED"),
                eq("USER"),
                eq(targetUserId),
                eq("{\"reason\":\"Violation of service terms\"}")
        );
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
        verify(auditLogInternalApi, never()).recordAuditLog(any(), any(), any(), any(), any());
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
        verify(auditLogInternalApi, never()).recordAuditLog(any(), any(), any(), any(), any());
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
        verify(auditLogInternalApi, never()).recordAuditLog(any(), any(), any(), any(), any());
    }

    @Test
    void shouldLockUserWithNullReasonSuccessfully() {
        when(accountInternalApi.findUserById(targetUserId)).thenReturn(Optional.of(targetUser));

        lockUserUseCase.execute(actorId, targetUserId, null);

        verify(auditLogInternalApi).recordAuditLog(
                eq(actorId),
                eq("USER_LOCKED"),
                eq("USER"),
                eq(targetUserId),
                eq("{\"reason\":\"\"}")
        );
    }
}
