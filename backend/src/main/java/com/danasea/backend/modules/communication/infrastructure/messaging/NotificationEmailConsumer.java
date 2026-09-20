package com.danasea.backend.modules.communication.infrastructure.messaging;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.modules.communication.domain.events.NotificationEmailEvent;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailConsumer {

    private final JavaMailSender mailSender;
    private final JpaNotificationRepository notificationRepository;

    @Value("${spring.mail.username:noreply@danasea.com}")
    private String fromEmail;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE)
    public void handleNotificationEmail(NotificationEmailEvent event) {
        log.info("Processing NotificationEmailEvent for: {} (Subject: {})", event.toEmail(), event.subject());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(event.toEmail());
            message.setSubject("[DANASEA] " + event.subject());
            message.setText(event.content() + "\n\n---\nDANASEA Marine Tourism Platform\nĐà Nẵng, Việt Nam");

            mailSender.send(message);
            log.info("Successfully sent notification email to: {}", event.toEmail());

            if (event.notificationId() != null) {
                notificationRepository.findById(event.notificationId()).ifPresent(entity -> {
                    entity.setStatus(NotificationStatus.SENT);
                    entity.setSentAt(OffsetDateTime.now());
                    notificationRepository.save(entity);
                });
            }
        } catch (Exception e) {
            log.error("Failed to send notification email to: {}", event.toEmail(), e);
            if (event.notificationId() != null) {
                notificationRepository.findById(event.notificationId()).ifPresent(entity -> {
                    entity.setStatus(NotificationStatus.FAILED);
                    notificationRepository.save(entity);
                });
            }
            throw new AmqpRejectAndDontRequeueException("Notification email send failed", e);
        }
    }
}
