package com.danasea.backend.security.authentication.application.usecase;

import com.danasea.backend.security.authentication.application.port.AuthEventPublisher;
import com.danasea.backend.security.authentication.application.port.OtpStorePort;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.domain.event.OtpEmailRequestedEvent;
import com.danasea.backend.security.authentication.domain.exception.OtpRequestTooFrequentException;
import com.danasea.backend.security.authentication.domain.model.Authentication;
import com.danasea.backend.security.authentication.domain.model.OtpDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendVerificationOtpUseCaseTest {

    @Mock
    private UserAccountPort userAccountPort;

    @Mock
    private OtpStorePort otpStorePort;

    @Mock
    private AuthEventPublisher authEventPublisher;

    private SendVerificationOtpUseCase sendVerificationOtpUseCase;

    @BeforeEach
    void setUp() {
        sendVerificationOtpUseCase = new SendVerificationOtpUseCase(userAccountPort, otpStorePort, authEventPublisher);
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        // Arrange
        String email = "test@example.com";
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sendVerificationOtpUseCase.execute(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void execute_EmailAlreadyVerified_DoesNothing() {
        // Arrange
        String email = "test@example.com";
        Authentication user = new Authentication(UUID.randomUUID(), email, "password", "USER", true, true);
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        sendVerificationOtpUseCase.execute(email);

        // Assert
        verify(otpStorePort, never()).findByEmail(anyString());
        verify(otpStorePort, never()).save(anyString(), any(), any());
        verify(authEventPublisher, never()).publishOtpRequestedEvent(any());
    }

    @Test
    void execute_OtpRequestedTooFrequently_ThrowsException() {
        // Arrange
        String email = "test@example.com";
        Authentication user = new Authentication(UUID.randomUUID(), email, "password", "USER", true, false);
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(user));

        // Create an OtpDetails that was generated less than 60 seconds ago
        OtpDetails recentOtp = new OtpDetails("hash", 0, OffsetDateTime.now().minusSeconds(30));
        when(otpStorePort.findByEmail(email)).thenReturn(Optional.of(recentOtp));

        // Act & Assert
        assertThatThrownBy(() -> sendVerificationOtpUseCase.execute(email))
                .isInstanceOf(OtpRequestTooFrequentException.class);
    }

    @Test
    void execute_Success_GeneratesAndSavesOtpAndPublishesEvent() {
        // Arrange
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        Authentication user = new Authentication(userId, email, "password", "USER", true, false);
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(user));

        // No recent OTP or old OTP
        OtpDetails oldOtp = new OtpDetails("hash", 0, OffsetDateTime.now().minusSeconds(120));
        when(otpStorePort.findByEmail(email)).thenReturn(Optional.of(oldOtp));

        // Act
        sendVerificationOtpUseCase.execute(email);

        // Assert
        ArgumentCaptor<OtpDetails> otpDetailsCaptor = ArgumentCaptor.forClass(OtpDetails.class);
        verify(otpStorePort).save(eq(email), otpDetailsCaptor.capture(), eq(Duration.ofMinutes(5)));

        OtpDetails savedOtp = otpDetailsCaptor.getValue();
        assertThat(savedOtp.codeHash()).isNotNull();
        assertThat(savedOtp.attempts()).isZero();

        ArgumentCaptor<OtpEmailRequestedEvent> eventCaptor = ArgumentCaptor.forClass(OtpEmailRequestedEvent.class);
        verify(authEventPublisher).publishOtpRequestedEvent(eventCaptor.capture());

        OtpEmailRequestedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.userId()).isEqualTo(userId);
        assertThat(publishedEvent.email()).isEqualTo(email);
        assertThat(publishedEvent.otpCode()).isNotNull().hasSize(6); // Assuming OTP is 6 chars long
    }
}
