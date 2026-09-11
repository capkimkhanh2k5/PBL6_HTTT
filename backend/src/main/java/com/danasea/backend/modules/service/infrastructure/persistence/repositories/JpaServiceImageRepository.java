package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;

@Repository
public interface JpaServiceImageRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {
    List<ServiceImageJpaEntity> findByServiceId(UUID serviceId);

    List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);

    void deleteByServiceId(UUID serviceId);

    boolean existsByServiceId(UUID serviceId);
}
