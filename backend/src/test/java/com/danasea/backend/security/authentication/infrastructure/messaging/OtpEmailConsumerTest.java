package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpEmailConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpEmailConsumer otpEmailConsumer;

    @Test
    void handleOtpEmailRequestedEvent_whenMailSenderFails_throwsAmqpRejectAndDontRequeueException() throws Exception {
        // Arrange
        // We use reflection to set the @Value field
        Field fromEmailField = OtpEmailConsumer.class.getDeclaredField("fromEmail");
        fromEmailField.setAccessible(true);
        fromEmailField.set(otpEmailConsumer, "noreply@test.com");

        OtpEmailRequestedEvent event = new OtpEmailRequestedEvent(UUID.randomUUID(), "user@test.com", "123456");

        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            otpEmailConsumer.handleOtpEmailRequestedEvent(event);
        });

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void handleOtpEmailRequestedEvent_whenSuccess_doesNotThrow() throws Exception {
        // Arrange
        Field fromEmailField = OtpEmailConsumer.class.getDeclaredField("fromEmail");
        fromEmailField.setAccessible(true);
        fromEmailField.set(otpEmailConsumer, "noreply@test.com");

        OtpEmailRequestedEvent event = new OtpEmailRequestedEvent(UUID.randomUUID(), "user@test.com", "123456");

        // Act
        otpEmailConsumer.handleOtpEmailRequestedEvent(event);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
