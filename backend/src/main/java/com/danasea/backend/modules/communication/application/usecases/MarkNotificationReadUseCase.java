package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.domain.exceptions.NotificationNotFoundException;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import com.danasea.backend.modules.communication.presentation.dtos.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final JpaNotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse execute(UUID notificationId, UUID currentUserId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("Notification ID is required");
        }
        if (currentUserId == null) {
            throw new AccessDeniedException("User must be authenticated");
        }

        NotificationJpaEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!currentUserId.equals(notification.getUserId())) {
            throw new AccessDeniedException("You are not authorized to update this notification");
        }

        if (!notification.isRead()) {
            notification.setIsRead(true);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .body(notification.getBody())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .build();
    }
}
