package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID> {
    boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status);
}
