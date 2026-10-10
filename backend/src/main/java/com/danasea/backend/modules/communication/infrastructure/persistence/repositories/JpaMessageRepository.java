package com.danasea.backend.modules.communication.infrastructure.persistence.repositories;

import com.danasea.backend.modules.communication.infrastructure.persistence.entities.MessageJpaEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
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

@Repository
public interface JpaMessageRepository extends JpaRepository<MessageJpaEntity, UUID> {

    Page<MessageJpaEntity> findByConversationId(UUID conversationId, Pageable pageable);

    List<MessageJpaEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<MessageJpaEntity> findByConversationIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(UUID conversationId, OffsetDateTime after);

    Optional<MessageJpaEntity> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndSenderIdNotAndIsReadFalse(UUID conversationId, UUID readerId);

    List<MessageJpaEntity> findByConversationIdAndSenderIdNotAndIsReadFalse(UUID conversationId, UUID currentUserId);

    @Modifying
    @Query("UPDATE MessageJpaEntity m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.senderId != :currentUserId AND m.isRead = false")
    int markMessagesAsRead(@Param("conversationId") UUID conversationId, @Param("currentUserId") UUID currentUserId);
    Optional<MessageJpaEntity> findFirstByConversationIdOrderBySequenceDesc(UUID conversationId);

    List<MessageJpaEntity> findByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(
            UUID conversationId, Long sequence, Pageable pageable);

    List<MessageJpaEntity> findByConversationIdAndCreatedAtGreaterThanOrderBySequenceAsc(
            UUID conversationId, OffsetDateTime after, Pageable pageable);

    @Query("SELECT MIN(m.sequence) FROM MessageJpaEntity m WHERE m.conversationId = :conversationId AND m.createdAt = :after")
    Optional<Long> findFirstSequenceAtTimestamp(@Param("conversationId") UUID conversationId,
            @Param("after") OffsetDateTime after);

    @Modifying
    @Query("UPDATE MessageJpaEntity m SET m.isRead = true WHERE m.conversationId = :conversationId "
            + "AND m.senderId != :currentUserId AND m.isRead = false AND m.id IN :messageIds")
    int markDeliveredMessagesAsRead(@Param("conversationId") UUID conversationId,
            @Param("currentUserId") UUID currentUserId, @Param("messageIds") Collection<UUID> messageIds);
}
