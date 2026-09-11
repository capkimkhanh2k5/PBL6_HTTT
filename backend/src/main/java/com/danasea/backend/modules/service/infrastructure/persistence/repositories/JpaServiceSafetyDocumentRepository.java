package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.DocStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceSafetyDocumentRepository extends JpaRepository<ServiceSafetyDocumentJpaEntity, UUID> {

    List<ServiceSafetyDocumentJpaEntity> findByServiceId(UUID serviceId);

    Optional<ServiceSafetyDocumentJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);

    boolean existsByServiceIdAndStatus(UUID serviceId, DocStatus status);
}
