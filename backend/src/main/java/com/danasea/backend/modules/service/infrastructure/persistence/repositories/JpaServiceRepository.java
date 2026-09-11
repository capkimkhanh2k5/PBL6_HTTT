package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID>, JpaSpecificationExecutor<ServiceJpaEntity> {

    @Modifying
    @Query("UPDATE ServiceJpaEntity s SET s.viewCount = COALESCE(s.viewCount, 0) + 1 WHERE s.id = :id AND s.status = :status")
    int incrementViewCount(@Param("id") UUID id, @Param("status") ServiceStatus status);

    Optional<ServiceJpaEntity> findByIdAndStatus(UUID id, ServiceStatus status);
}

