package com.danasea.backend.security.authentication.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.port.AuditLogPort;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.model.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginUseCaseTest {

    private UserAccountPort userAccountPort;
    private AccountInternalApi accountInternalApi;
    private PasswordHasher passwordHasher;
    private TokenProvider tokenProvider;
    private JwtProperties jwtProperties;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        userAccountPort = mock(UserAccountPort.class);
        accountInternalApi = mock(AccountInternalApi.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenProvider = mock(TokenProvider.class);
        jwtProperties = new JwtProperties("secretsecretsecretsecretsecretsecret", 15, 7);

        loginUseCase = new LoginUseCase(
                userAccountPort,
                accountInternalApi,
                passwordHasher,
                tokenProvider,
                jwtProperties
        );
    }

    @Test
    void shouldLoginSuccessfully() {
        String email = "TEST@example.com";
        String normalizedEmail = "test@example.com";
        String password = "Password1!";
        String hashedPassword = "hashedPassword1!";

        Authentication user = new Authentication(UUID.randomUUID(), normalizedEmail, hashedPassword, "CUSTOMER", true, true);
        
        when(userAccountPort.findByEmail(normalizedEmail)).thenReturn(Optional.of(user));
        when(passwordHasher.matches(password, hashedPassword)).thenReturn(true);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        LoginResult result = loginUseCase.execute(email, password);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        
        verify(accountInternalApi).saveRefreshToken(any(RefreshToken.class));
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userAccountPort.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute("test@example.com", "Password1!"));
    }

    @Test
    void shouldThrowWhenPasswordMismatches() {
        Authentication user = new Authentication(UUID.randomUUID(), "test@example.com", "hash", "CUSTOMER", true, true);
        when(userAccountPort.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute("test@example.com", "wrong"));
    }

    @Test
    void shouldThrowWhenUserIsLocked() {
        Authentication user = new Authentication(UUID.randomUUID(), "test@example.com", "hash", "CUSTOMER", false, true);
        when(userAccountPort.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute("test@example.com", "password"));
    }

    @Test
    void shouldThrowWhenEmailIsNotVerified() {
        Authentication user = new Authentication(UUID.randomUUID(), "test@example.com", "hash", "CUSTOMER", true, false);
        when(userAccountPort.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute("test@example.com", "password"));
    }
}
