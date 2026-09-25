package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import com.danasea.backend.modules.communication.presentation.dtos.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    @Transactional(readOnly = true)
    public Page<NotificationResponse> execute(UUID userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
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
                        .build());
    }
}
