package com.danasea.backend.modules.communication.infrastructure.persistence.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;

@Repository
public interface JpaNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    long countByUserIdAndIsReadFalse(UUID userId);

    Optional<NotificationJpaEntity> findByIdempotencyKey(String key);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO notifications (id, user_id, type, channel, title, body, locale,
                related_entity_type, related_entity_id, status, sent_at, created_at, updated_at, is_read, idempotency_key)
            VALUES (:id, :userId, :type, 'IN_APP', :title, :body, :locale,
                :entityType, :entityId, 'SENT', :now, :now, :now, FALSE, :key)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertInAppOnce(@Param("id") UUID id, @Param("userId") UUID userId, @Param("type") String type,
            @Param("title") String title, @Param("body") String body, @Param("locale") String locale,
            @Param("entityType") String entityType, @Param("entityId") UUID entityId,
            @Param("now") OffsetDateTime now, @Param("key") String key);


    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);
}
