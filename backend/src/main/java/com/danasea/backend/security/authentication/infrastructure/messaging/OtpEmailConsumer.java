package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.mail.autoconfigure.MailProperties;
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
    private final MailProperties mailProperties;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_OTP_EMAIL_QUEUE)
    public void handleOtpEmailRequestedEvent(OtpEmailRequestedEvent event) {
        log.info("Processing an OTP email request");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(event.email());
            message.setSubject("Verify your email address - Danasea");
            message.setText("Your OTP code is: " + event.otpCode()
                    + "\n\nThis code will expire in 5 minutes.\nDo not share this code with anyone.");

            mailSender.send(message);
            log.info("OTP email sent successfully");
        } catch (MailException e) {
            log.error("OTP email delivery failed", e);
            throw new AmqpRejectAndDontRequeueException("OTP email send failed", e);
        }
    }
}
