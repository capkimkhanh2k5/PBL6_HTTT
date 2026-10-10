package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryPreviewJpaEntity;

import jakarta.persistence.LockModeType;

public interface JpaAiItineraryPreviewRepository extends JpaRepository<AiItineraryPreviewJpaEntity, UUID> {
    Optional<AiItineraryPreviewJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select preview from AiItineraryPreviewJpaEntity preview where preview.id=:id and preview.ownerId=:ownerId")
    Optional<AiItineraryPreviewJpaEntity> locked(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
