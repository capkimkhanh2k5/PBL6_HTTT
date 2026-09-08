package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaServiceSlotRepository extends JpaRepository<ServiceSlotJpaEntity, UUID> {
}
