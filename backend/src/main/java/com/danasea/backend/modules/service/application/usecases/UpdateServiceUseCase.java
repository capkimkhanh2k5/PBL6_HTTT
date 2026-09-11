package com.danasea.backend.modules.service.application.usecases;

import java.util.List;

import com.danasea.backend.modules.service.application.dto.ServiceResult;
import com.danasea.backend.modules.service.application.dto.UpdateServiceCommand;
import com.danasea.backend.modules.service.application.usecases.helpers.ServiceResultMapper;
import com.danasea.backend.modules.service.domain.exceptions.CategoryInactiveException;
import com.danasea.backend.modules.service.domain.exceptions.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final ServiceImageRepositoryPort serviceImageRepository;
    private final VendorPort vendorPort;

    public ServiceResult execute(UpdateServiceCommand cmd) {
        Vendor vendor = vendorPort.findByUserId(cmd.userId())
                .orElseThrow(() -> new VendorNotApprovedException("Vendor not found for user: " + cmd.userId()));

        Service service = serviceRepository.findById(cmd.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException(cmd.serviceId()));

        // Check ownership
        service.validateOwnership(vendor.getId());

        // Validate category if provided
        if (cmd.categoryId() != null) {
            Category category = categoryRepository.findById(cmd.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(cmd.categoryId()));
            if (!category.isActive()) {
                throw new CategoryInactiveException(cmd.categoryId());
            }
            service.setCategoryId(cmd.categoryId());
        }

        // Apply updates
        if (cmd.name() != null) service.setName(cmd.name());
        if (cmd.nameEn() != null) service.setNameEn(cmd.nameEn());
        if (cmd.description() != null) service.setDescription(cmd.description());
        if (cmd.descriptionEn() != null) service.setDescriptionEn(cmd.descriptionEn());
        if (cmd.price() != null) service.setPrice(cmd.price());
        if (cmd.durationMinutes() != null) service.setDurationMinutes(cmd.durationMinutes());
        if (cmd.capacityPerSlot() != null) service.setCapacityPerSlot(cmd.capacityPerSlot());
        if (cmd.locationName() != null) service.setLocationName(cmd.locationName());
        if (cmd.address() != null) service.setAddress(cmd.address());
        if (cmd.latitude() != null) service.setLatitude(cmd.latitude());
        if (cmd.longitude() != null) service.setLongitude(cmd.longitude());
        if (cmd.waiverContent() != null) service.setWaiverContent(cmd.waiverContent());
        if (cmd.weatherSensitive() != null) service.setWeatherSensitive(cmd.weatherSensitive());
        if (cmd.minWindKmh() != null) service.setMinWindKmh(cmd.minWindKmh());
        if (cmd.maxWaveM() != null) service.setMaxWaveM(cmd.maxWaveM());

        // Validate weather requirements after update
        service.validateWeatherRequirements();

        // Business rule: if PUBLISHED, transition to PENDING_REVIEW
        service.transitionOnUpdate();

        Service saved = serviceRepository.save(service);
        List<ServiceImage> images = serviceImageRepository.findByServiceId(saved.getId());
        return ServiceResultMapper.toResult(saved, images);
    }
}
