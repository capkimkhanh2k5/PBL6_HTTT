package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.config.RabbitMQConfig;
import com.danasea.backend.security.authentication.domain.event.OtpEmailRequestedEvent;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OtpEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailConsumer.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@danasea.com}")
    private String fromEmail;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_OTP_EMAIL_QUEUE)
    public void handleOtpEmailRequestedEvent(OtpEmailRequestedEvent event) {
        log.info("Received OtpEmailRequestedEvent for email: {}. OTP code is: {}", event.email(), event.otpCode());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(event.email());
            message.setSubject("Verify your email address - Danasea");
            message.setText("Your OTP code is: " + event.otpCode()
                    + "\n\nThis code will expire in 5 minutes.\nDo not share this code with anyone.");

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", event.email());
        } catch (MailException e) {
            log.error("OTP email send failed for: {}", event.email(), e);
            throw new AmqpRejectAndDontRequeueException("OTP email send failed", e);
        }
    }
}
