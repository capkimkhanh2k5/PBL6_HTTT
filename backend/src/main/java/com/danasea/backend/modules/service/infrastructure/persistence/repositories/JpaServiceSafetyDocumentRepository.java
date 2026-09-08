package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaServiceSafetyDocumentRepository extends JpaRepository<ServiceSafetyDocumentJpaEntity, UUID> {
}
