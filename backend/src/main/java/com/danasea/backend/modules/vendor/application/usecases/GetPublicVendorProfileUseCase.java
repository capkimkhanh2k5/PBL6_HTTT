package com.danasea.backend.modules.vendor.application.usecases;

import com.danasea.backend.modules.vendor.application.ports.VendorActiveServicesPort;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.infrastructure.mappers.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;
import com.danasea.backend.modules.vendor.presentation.dtos.PublicVendorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetPublicVendorProfileUseCase {

    private final JpaVendorRepository jpaVendorRepository;
    private final VendorMapper vendorMapper;
    private final VendorActiveServicesPort vendorActiveServicesPort;

    @Autowired
    public GetPublicVendorProfileUseCase(
            JpaVendorRepository jpaVendorRepository,
            VendorMapper vendorMapper,
            @Autowired(required = false) VendorActiveServicesPort vendorActiveServicesPort) {
        this.jpaVendorRepository = jpaVendorRepository;
        this.vendorMapper = vendorMapper;
        this.vendorActiveServicesPort = vendorActiveServicesPort;
    }

    @Transactional(readOnly = true)
    public PublicVendorResponse execute(UUID vendorId) {
        if (vendorId == null) {
            throw new VendorNotFoundException();
        }

        Vendor vendor = jpaVendorRepository.findById(vendorId)
                .map(vendorMapper::toDomain)
                .orElseThrow(VendorNotFoundException::new);

        long activeServicesCount = vendorActiveServicesPort != null
                ? vendorActiveServicesPort.countActiveServices(vendorId)
                : 0L;

        return PublicVendorResponse.builder()
                .id(vendor.getId())
                .businessName(vendor.getBusinessName())
                .address(vendor.getAddress())
                .badgeTier(vendor.getBadgeTier())
                .ratingAvg(vendor.getRatingAvg())
                .ratingCount(vendor.getRatingCount())
                .activeServicesCount(activeServicesCount)
                .build();
    }
}
