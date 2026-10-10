package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryRevisionJpaEntity;


public interface JpaAiItineraryRevisionRepository extends JpaRepository<AiItineraryRevisionJpaEntity, UUID> {
    List<AiItineraryRevisionJpaEntity> findByItineraryIdOrderByItineraryVersionDesc(UUID itineraryId, Pageable page);
}
