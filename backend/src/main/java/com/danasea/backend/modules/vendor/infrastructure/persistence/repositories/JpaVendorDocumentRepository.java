package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;

@Repository
public interface JpaVendorDocumentRepository extends JpaRepository<VendorDocumentJpaEntity, UUID> {

    List<VendorDocumentJpaEntity> findByVendorId(UUID vendorId);

    List<VendorDocumentJpaEntity> findAllByVendorId(UUID vendorId);
}
