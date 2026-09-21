package com.danasea.backend.modules.communication.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, com.danasea.backend.modules.communication.domain.models.NotificationStatus status);
}
