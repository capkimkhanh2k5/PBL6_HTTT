package com.danasea.backend.modules.service.application.usecases;

import java.util.UUID;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.service.presentation.dtos.ServiceOptionResponse;
import com.danasea.backend.modules.service.presentation.dtos.UpdateServiceOptionRequest;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateServiceOptionUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceOptionRepositoryPort serviceOptionRepository;
    private final VendorPort vendorPort;

    @Transactional
    public ServiceOptionResponse execute(UUID userId, UUID serviceId, UUID optionId, UpdateServiceOptionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Vendor vendor = vendorPort.findByUserId(userId)
                .orElseThrow(() -> new VendorNotApprovedException("Vendor not found for user: " + userId));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        service.validateOwnership(vendor.getId());

        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Option not found: " + optionId));

        if (!serviceId.equals(option.getServiceId())) {
            throw new IllegalArgumentException("Option does not belong to the specified service");
        }

        if (request.name() != null && !request.name().isBlank()) {
            option.setName(request.name().trim());
        }

        if (request.price() != null) {
            option.setPrice(request.price());
        }

        if (request.maxPaxPerPackage() != null) {
            if (OptionType.PRIVATE.equals(option.getOptionType()) && request.maxPaxPerPackage() <= 0) {
                throw new IllegalArgumentException("Private package maximum pax must be greater than 0");
            }
            option.setMaxPaxPerPackage(request.maxPaxPerPackage());
        }

        if (request.benefits() != null) {
            option.setBenefits(request.benefits().trim());
        }

        if (request.status() != null) {
            option.setStatus(request.status());
        }

        ServiceOption saved = serviceOptionRepository.save(option);
        return toResponse(saved);
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
