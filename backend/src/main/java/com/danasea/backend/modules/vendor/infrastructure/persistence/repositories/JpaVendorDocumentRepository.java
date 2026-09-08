package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaVendorDocumentRepository extends JpaRepository<VendorDocumentJpaEntity, UUID> {
}
