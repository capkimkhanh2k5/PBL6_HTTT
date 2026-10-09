package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface JpaAiConversationRepository extends JpaRepository<AiConversationJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AiConversationJpaEntity c where c.id = :id")
    Optional<AiConversationJpaEntity> findForUpdate(@Param("id") UUID id);
}
