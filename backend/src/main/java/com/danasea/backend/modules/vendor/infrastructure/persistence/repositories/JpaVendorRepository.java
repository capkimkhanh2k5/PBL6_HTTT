package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;

@Repository
public interface JpaVendorRepository extends JpaRepository<VendorJpaEntity, UUID> {

    Optional<VendorJpaEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
