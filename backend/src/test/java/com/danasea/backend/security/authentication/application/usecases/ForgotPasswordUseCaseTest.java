package com.danasea.backend.security.authentication.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.domain.events.PasswordResetRequestedEvent;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private AuthEventPublisher authEventPublisher;

    private ForgotPasswordUseCase forgotPasswordUseCase;

    @BeforeEach
    void setUp() {
        forgotPasswordUseCase = new ForgotPasswordUseCase(accountInternalApi, authEventPublisher);
    }

    @Test
    void execute_UserNotFound_ReturnsSilentlyWithoutPublishingEvent() {
        String email = "notfound@example.com";
        when(accountInternalApi.findUserByEmailForUpdate(email)).thenReturn(Optional.empty());

        forgotPasswordUseCase.execute(email, SupportedLanguage.VI);

        verify(accountInternalApi, never()).invalidatePasswordResetTokens(any());
        verify(accountInternalApi, never()).createPasswordResetToken(any(), any(), any());
        verify(authEventPublisher, never()).publishPasswordResetRequestedEvent(any());
    }

    @Test
    void execute_UserLocked_ReturnsSilentlyWithoutPublishingEvent() {
        String email = "locked@example.com";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setIsLocked(true);
        when(accountInternalApi.findUserByEmailForUpdate(email)).thenReturn(Optional.of(user));

        forgotPasswordUseCase.execute(email, SupportedLanguage.VI);

        verify(accountInternalApi, never()).invalidatePasswordResetTokens(any());
        verify(accountInternalApi, never()).createPasswordResetToken(any(), any(), any());
        verify(authEventPublisher, never()).publishPasswordResetRequestedEvent(any());
    }

    @Test
    void execute_BlankEmail_ReturnsSilently() {
        forgotPasswordUseCase.execute("   ", SupportedLanguage.VI);

        verifyNoInteractions(accountInternalApi);
        verifyNoInteractions(authEventPublisher);
    }

    @Test
    void execute_Success_InvalidatesOldTokens_GeneratesOtp_SavesToken_AndPublishesEvent() {
        String email = "user@example.com";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);
        user.setIsLocked(false);

        when(accountInternalApi.findUserByEmailForUpdate(email)).thenReturn(Optional.of(user));

        forgotPasswordUseCase.execute(email, SupportedLanguage.EN);

        verify(accountInternalApi).invalidatePasswordResetTokens(userId);

        ArgumentCaptor<String> tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expiresAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.now().getClass());
        verify(accountInternalApi).createPasswordResetToken(eq(userId), tokenHashCaptor.capture(), expiresAtCaptor.capture());

        assertThat(tokenHashCaptor.getValue()).isNotEmpty().hasSize(44); // Base64 encoded SHA-256 is 44 chars
        assertThat(expiresAtCaptor.getValue()).isAfter(OffsetDateTime.now());

        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(authEventPublisher).publishPasswordResetRequestedEvent(eventCaptor.capture());

        PasswordResetRequestedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo(email);
        assertThat(event.otpCode()).isNotNull().hasSize(6);
        assertThat(event.locale()).isEqualTo("en");
    }
}
