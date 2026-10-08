package com.danasea.backend.modules.service.application.usecases;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.service.presentation.dtos.ServiceOptionResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetVendorServiceOptionsUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceOptionRepositoryPort serviceOptionRepository;
    private final VendorPort vendorPort;

    @Transactional(readOnly = true)
    public List<ServiceOptionResponse> execute(UUID userId, UUID serviceId) {
        Vendor vendor = vendorPort.findByUserId(userId)
                .orElseThrow(() -> new VendorNotApprovedException("Vendor not found for user: " + userId));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        service.validateOwnership(vendor.getId());

        List<ServiceOption> options = serviceOptionRepository.findByServiceId(serviceId);
        return options.stream()
                .map(this::toResponse)
                .toList();
    }

    private ServiceOptionResponse toResponse(ServiceOption o) {
        return ServiceOptionResponse.builder()
                .id(o.getId())
                .serviceId(o.getServiceId())
                .name(o.getName())
                .optionType(o.getOptionType())
                .pricingUnit(o.getPricingUnit())
                .price(o.getPrice())
                .maxPaxPerPackage(o.getMaxPaxPerPackage())
                .benefits(o.getBenefits())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
