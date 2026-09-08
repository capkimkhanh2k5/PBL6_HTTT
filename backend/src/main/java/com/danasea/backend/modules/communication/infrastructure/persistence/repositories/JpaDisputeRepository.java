package com.danasea.backend.modules.communication.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.communication.infrastructure.persistence.entities.DisputeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaDisputeRepository extends JpaRepository<DisputeJpaEntity, UUID> {
}
