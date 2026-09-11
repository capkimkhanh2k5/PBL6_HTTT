package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID> {
    boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status);

    List<ServiceJpaEntity> findByVendorId(UUID vendorId);

    List<ServiceJpaEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    List<ServiceJpaEntity> findByStatus(ServiceStatus status);

    List<ServiceJpaEntity> findByStatusOrderByCreatedAtDesc(ServiceStatus status);

    boolean existsBySlug(String slug);
}
