package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryJpaEntity;

public interface JpaAiItineraryRepository extends JpaRepository<AiItineraryJpaEntity, UUID> {
    Optional<AiItineraryJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<AiItineraryJpaEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable page);
    Optional<AiItineraryJpaEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select itinerary from AiItineraryJpaEntity itinerary where itinerary.id=:id and itinerary.ownerId=:ownerId")
    Optional<AiItineraryJpaEntity> locked(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
