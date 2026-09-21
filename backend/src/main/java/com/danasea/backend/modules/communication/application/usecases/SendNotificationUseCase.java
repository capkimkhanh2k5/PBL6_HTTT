package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.modules.communication.domain.events.NotificationEmailEvent;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendNotificationUseCase {

    private final JpaNotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public NotificationJpaEntity execute(UUID userId,
                                         String type,
                                         NotificationChannel channel,
                                         String title,
                                         String body,
                                         String relatedEntityType,
                                         UUID relatedEntityId,
                                         String recipientEmail) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setChannel(channel != null ? channel : NotificationChannel.IN_APP);
        entity.setTitle(title);
        entity.setBody(body);
        entity.setRelatedEntityType(relatedEntityType);
        entity.setRelatedEntityId(relatedEntityId);
        entity.setStatus(NotificationStatus.PENDING);

        if (entity.getChannel() == NotificationChannel.IN_APP) {
            entity.setStatus(NotificationStatus.SENT);
            entity.setSentAt(OffsetDateTime.now());
        }

        NotificationJpaEntity saved = notificationRepository.save(entity);

        if (entity.getChannel() == NotificationChannel.EMAIL && recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                NotificationEmailEvent event = new NotificationEmailEvent(
                        saved.getId(),
                        recipientEmail,
                        title,
                        body
                );
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE, event);
                log.info("Queued notification email to {}: {}", recipientEmail, title);
            } catch (Exception e) {
                log.warn("Failed to queue notification email for notification id {}", saved.getId(), e);
            }
        }

        return saved;
    }
}
