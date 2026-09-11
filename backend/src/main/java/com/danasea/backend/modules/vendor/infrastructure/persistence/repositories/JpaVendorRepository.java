package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVendorRepository extends JpaRepository<VendorJpaEntity, UUID> {
    Optional<VendorJpaEntity> findByUserId(UUID userId);
}
