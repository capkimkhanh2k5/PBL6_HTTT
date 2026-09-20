package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import com.danasea.backend.modules.communication.presentation.dtos.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCase {

    private final JpaNotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> execute(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(entity -> NotificationResponse.builder()
                        .id(entity.getId())
                        .userId(entity.getUserId())
                        .type(entity.getType())
                        .channel(entity.getChannel())
                        .title(entity.getTitle())
                        .body(entity.getBody())
                        .relatedEntityType(entity.getRelatedEntityType())
                        .relatedEntityId(entity.getRelatedEntityId())
                        .status(entity.getStatus())
                        .sentAt(entity.getSentAt())
                        .createdAt(entity.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
