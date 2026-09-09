package com.danasea.backend.security.authentication.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import org.springframework.context.ApplicationEventPublisher;

import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.domain.event.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.model.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterUseCaseTest {

    private UserAccountPort userAccountPort;
    private AccountInternalApi accountInternalApi;
    private PasswordHasher passwordHasher;
    private TokenProvider tokenProvider;
    private JwtProperties jwtProperties;
    private ApplicationEventPublisher eventPublisher;

    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        userAccountPort = mock(UserAccountPort.class);
        accountInternalApi = mock(AccountInternalApi.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenProvider = mock(TokenProvider.class);
        jwtProperties = new JwtProperties("secretsecretsecretsecretsecretsecret", 15, 7);
        eventPublisher = mock(ApplicationEventPublisher.class);

        registerUseCase = new RegisterUseCase(
                userAccountPort,
                accountInternalApi,
                eventPublisher,
                passwordHasher,
                tokenProvider,
                jwtProperties
        );
    }

    @Test
    void shouldRegisterSuccessfully() {
        String email = "test@example.com";
        String password = "Password1!";
        String hashedPassword = "hashedPassword1!";

        when(userAccountPort.existsByEmail(email)).thenReturn(false);
        when(passwordHasher.hash(password)).thenReturn(hashedPassword);

        Authentication savedUser = new Authentication(UUID.randomUUID(), email, hashedPassword, "CUSTOMER", true, true);
        when(userAccountPort.save(any(Authentication.class))).thenReturn(savedUser);

        when(tokenProvider.generateAccessToken(savedUser)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(savedUser)).thenReturn("refresh-token");

        LoginResult result = registerUseCase.execute(email, password);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(savedUser.id(), result.userId());
        assertEquals(email, result.email());
        assertEquals("CUSTOMER", result.role());

        verify(accountInternalApi).saveRefreshToken(any(RefreshToken.class));
        
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserRegisteredEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId(), "Event ID must be generated for idempotency");
        assertEquals(savedUser.id(), publishedEvent.userId());
        assertEquals(email, publishedEvent.email());
        assertEquals("CUSTOMER", publishedEvent.role());
        assertNotNull(publishedEvent.registeredAt());
    }

    @Test
    void shouldThrowWhenEmailAlreadyUsed() {
        String email = "TEST@example.com";
        String normalizedEmail = "test@example.com";
        when(userAccountPort.existsByEmail(normalizedEmail)).thenReturn(true);

        assertThrows(EmailAlreadyUsedException.class, () -> registerUseCase.execute(email, "Password1!"));
    }
}
