package com.danasea.backend.modules.account.application.usecases;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authorization.domain.exceptions.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateProfileUseCaseTest {

    private AccountInternalApi accountInternalApi;
    private UpdateProfileUseCase updateProfileUseCase;

    @BeforeEach
    void setUp() {
        accountInternalApi = mock(AccountInternalApi.class);
        updateProfileUseCase = new UpdateProfileUseCase(accountInternalApi);
    }

    @Test
    void execute_Success() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFullName("Old Name");
        user.setRole(Role.CUSTOMER);

        when(accountInternalApi.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(accountInternalApi.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = updateProfileUseCase.execute(email, "New Name", "http://avatar.com", "vi-VN");

        assertNotNull(result);
        assertEquals("New Name", result.getFullName());
        assertEquals("http://avatar.com", result.getAvatarUrl());
        assertEquals("vi-VN", result.getLocale());
        assertEquals(Role.CUSTOMER, result.getRole()); // Unchanged

        verify(accountInternalApi, times(1)).saveUser(user);
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        String email = "notfound@example.com";
        when(accountInternalApi.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> 
            updateProfileUseCase.execute(email, "New Name", null, null)
        );
        
        verify(accountInternalApi, never()).saveUser(any());
    }

    @Test
    void execute_MassAssignmentProtection() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFullName("Old Name");
        user.setRole(Role.CUSTOMER);
        user.setIsLocked(false);
        user.setIsEmailVerified(true);

        when(accountInternalApi.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(accountInternalApi.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Attempting to pass role or isLocked is prevented by the use case signature,
        // which only takes fullName, avatarUrl, locale.
        User result = updateProfileUseCase.execute(email, "New Name", null, null);

        assertEquals("New Name", result.getFullName());
        assertEquals(Role.CUSTOMER, result.getRole());
        assertFalse(result.getIsLocked());
        assertTrue(result.getIsEmailVerified());
        assertEquals(email, result.getEmail());
    }
}
