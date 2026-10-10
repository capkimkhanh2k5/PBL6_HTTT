package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryProposalJpaEntity;


public interface JpaAiItineraryProposalRepository extends JpaRepository<AiItineraryProposalJpaEntity, UUID> {
    Optional<AiItineraryProposalJpaEntity> findByIdAndItineraryIdAndOwnerId(UUID id, UUID itineraryId, UUID ownerId);
    Optional<AiItineraryProposalJpaEntity> findByItineraryIdAndSourceEventId(UUID itineraryId, String eventId);
    List<AiItineraryProposalJpaEntity> findByItineraryIdAndOwnerIdOrderByCreatedAtDesc(UUID itineraryId, UUID ownerId, Pageable page);
    List<AiItineraryProposalJpaEntity> findByItineraryIdAndState(UUID itineraryId, String state);
}
