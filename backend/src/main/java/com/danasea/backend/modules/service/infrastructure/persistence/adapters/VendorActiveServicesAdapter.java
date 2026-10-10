package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.vendor.application.ports.VendorActiveServicesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VendorActiveServicesAdapter implements VendorActiveServicesPort {

    private final JpaServiceRepository jpaServiceRepository;

    @Override
    @Transactional(readOnly = true)
    public long countActiveServices(UUID vendorId) {
        if (vendorId == null) {
            return 0L;
        }
        return jpaServiceRepository.countByVendorIdAndStatus(vendorId, ServiceStatus.PUBLISHED);
    }
}
