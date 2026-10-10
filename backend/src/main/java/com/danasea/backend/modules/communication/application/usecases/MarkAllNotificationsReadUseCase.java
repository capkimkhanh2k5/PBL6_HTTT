package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkAllNotificationsReadUseCase {

    private final JpaNotificationRepository notificationRepository;

    @Transactional
    public int execute(UUID currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("User must be authenticated");
        }
        return notificationRepository.markAllAsRead(currentUserId, OffsetDateTime.now());
    }
}
