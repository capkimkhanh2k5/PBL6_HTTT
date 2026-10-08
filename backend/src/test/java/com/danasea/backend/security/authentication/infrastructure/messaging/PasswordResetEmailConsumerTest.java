package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.security.authentication.domain.events.PasswordResetRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetEmailConsumer consumer;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("noreply@danasea.com");
        consumer = new PasswordResetEmailConsumer(mailSender, mailProperties);
    }

    @Test
    void handlePasswordResetRequestedEvent_whenMailSenderFails_throwsAmqpRejectAndDontRequeueException() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(UUID.randomUUID(), "user@example.com", "123456", "vi");

        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
            consumer.handlePasswordResetRequestedEvent(event);
        });

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void handlePasswordResetRequestedEvent_whenSuccess_sendsEmail() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(UUID.randomUUID(), "user@example.com", "123456", "en");

        consumer.handlePasswordResetRequestedEvent(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
