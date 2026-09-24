package com.danasea.backend.modules.communication.infrastructure.messaging;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.modules.communication.domain.events.NotificationEmailEvent;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.mail.autoconfigure.MailProperties;
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
    private final MailProperties mailProperties;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE)
    public void handleNotificationEmail(NotificationEmailEvent event) {
        log.info("Processing notification email with subject: {}", event.subject());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(event.toEmail());
            message.setSubject("[DANASEA] " + event.subject());
            message.setText(event.content() + "\n\n---\nDANASEA Marine Tourism Platform\nDa Nang, Vietnam");

            mailSender.send(message);
            log.info("Notification email sent successfully");

            if (event.notificationId() != null) {
                notificationRepository.findById(event.notificationId()).ifPresent(entity -> {
                    entity.setStatus(NotificationStatus.SENT);
                    entity.setSentAt(OffsetDateTime.now());
                    notificationRepository.save(entity);
                });
            }
        } catch (Exception e) {
            log.error("Notification email delivery failed", e);
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
