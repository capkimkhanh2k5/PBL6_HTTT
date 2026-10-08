package com.danasea.backend.modules.service.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.service.presentation.dtos.CreateServiceOptionRequest;
import com.danasea.backend.modules.service.presentation.dtos.ServiceOptionResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CreateServiceOptionUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceOptionRepositoryPort serviceOptionRepository;
    private final VendorPort vendorPort;

    @Transactional
    public ServiceOptionResponse execute(UUID userId, UUID serviceId, CreateServiceOptionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Vendor vendor = vendorPort.findByUserId(userId)
                .orElseThrow(() -> new VendorNotApprovedException("Vendor not found for user: " + userId));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        service.validateOwnership(vendor.getId());

        if (request.optionType() == null || request.pricingUnit() == null || request.price() == null
                || request.price().signum() <= 0) {
            throw new IllegalArgumentException("A valid option type, pricing unit and positive price are required");
        }
        if ((OptionType.PRIVATE.equals(request.optionType())
                && request.pricingUnit() != PricingUnit.PER_PACKAGE)
                || (OptionType.SHARED.equals(request.optionType())
                && request.pricingUnit() != PricingUnit.PER_PERSON)) {
            throw new IllegalArgumentException("Shared options are priced per person; private options are priced per package");
        }

        if (OptionType.PRIVATE.equals(request.optionType())) {
            if (request.maxPaxPerPackage() == null || request.maxPaxPerPackage() <= 0) {
                throw new IllegalArgumentException("PRIVATE package required configuration: maxPaxPerPackage > 0");
            }
        }

        ServiceOption option = ServiceOption.builder()
                .serviceId(serviceId)
                .name(request.name().trim())
                .optionType(request.optionType())
                .pricingUnit(request.pricingUnit())
                .price(request.price())
                .maxPaxPerPackage(request.maxPaxPerPackage())
                .benefits(request.benefits() != null ? request.benefits().trim() : null)
                .status(OptionStatus.ACTIVE)
                .build();
        option.setId(UUID.randomUUID());

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
