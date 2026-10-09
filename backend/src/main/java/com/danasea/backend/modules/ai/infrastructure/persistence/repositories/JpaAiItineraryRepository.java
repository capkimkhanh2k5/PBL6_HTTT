package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryJpaEntity;

public interface JpaAiItineraryRepository extends JpaRepository<AiItineraryJpaEntity, UUID> {
    Optional<AiItineraryJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<AiItineraryJpaEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable page);
}
