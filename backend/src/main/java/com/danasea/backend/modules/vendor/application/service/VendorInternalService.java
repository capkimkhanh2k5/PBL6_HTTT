package com.danasea.backend.modules.vendor.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.vendor.infrastructure.mappers.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorInternalService implements VendorInternalApi {

    private final JpaVendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    public Optional<Vendor> findByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return vendorRepository.findByUserId(userId)
                .map(vendorMapper::toDomain);
    }

    @Override
    public Optional<Vendor> findById(UUID vendorId) {
        if (vendorId == null) {
            return Optional.empty();
        }
        return vendorRepository.findById(vendorId)
                .map(vendorMapper::toDomain);
    }

    @Override
    public boolean isVendorApproved(UUID vendorId) {
        if (vendorId == null) {
            return false;
        }
        return vendorRepository.findById(vendorId)
                .map(v -> VerificationStatus.APPROVED.equals(v.getVerificationStatus()))
                .orElse(false);
    }

    @Override
    public Vendor saveVendor(Vendor vendor) {
        VendorJpaEntity entity = vendorMapper.toEntity(vendor);
        entity = vendorRepository.save(entity);
        return vendorMapper.toDomain(entity);
    }

    @Override
    public Page<Vendor> getVendors(VerificationStatus status, Pageable pageable) {
        if (status == null) {
            return vendorRepository.findAll(pageable).map(vendorMapper::toDomain);
        }
        return vendorRepository.findByVerificationStatus(status, pageable).map(vendorMapper::toDomain);
    }
}
