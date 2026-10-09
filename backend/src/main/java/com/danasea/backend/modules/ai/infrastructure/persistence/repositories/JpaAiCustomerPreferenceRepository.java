package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiCustomerPreferenceJpaEntity;

public interface JpaAiCustomerPreferenceRepository extends JpaRepository<AiCustomerPreferenceJpaEntity, UUID> {
    Optional<AiCustomerPreferenceJpaEntity> findByOwnerId(UUID ownerId);
}
