package com.danasea.backend.modules.vendor.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.infrastructure.mapper.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVendorProfileUseCase {

    private final JpaVendorRepository jpaVendorRepository;
    private final VendorMapper vendorMapper;

    @Transactional(readOnly = true)
    public Vendor execute(UUID userId) {
        return jpaVendorRepository.findByUserId(userId)
                .map(vendorMapper::toDomain)
                .orElseThrow(VendorNotFoundException::new);
    }
}
