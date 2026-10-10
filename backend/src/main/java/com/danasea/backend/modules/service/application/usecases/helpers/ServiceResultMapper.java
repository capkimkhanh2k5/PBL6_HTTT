package com.danasea.backend.modules.service.application.usecases.helpers;

import com.danasea.backend.modules.service.application.dtos.ServiceImageResult;
import com.danasea.backend.modules.service.application.dtos.ServiceResult;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import java.util.List;
import java.util.stream.Collectors;

public final class ServiceResultMapper {

    private ServiceResultMapper() {}

    public static ServiceResult toResult(Service service, List<ServiceImage> images) {
        List<ServiceImageResult> imageResults = images == null ? List.of() : images.stream()
                .map(img -> new ServiceImageResult(img.getId(), img.getServiceId(), img.getUrl(), img.getSortOrder()))
                .collect(Collectors.toList());

        return new ServiceResult(
                service.getId(),
                service.getVendorId(),
                service.getCategoryId(),
                service.getName(),
                service.getNameEn(),
                service.getSlug(),
                service.getDescription(),
                service.getDescriptionEn(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getCapacityPerSlot(),
                service.getLocationName(),
                service.getAddress(),
                service.getLatitude(),
                service.getLongitude(),
                service.getStatus(),
                service.getRejectionReason(),
                service.getWaiverContent(),
                service.getWaiverContentEn(),
                Boolean.TRUE.equals(service.getWaiverRequired()),
                service.getWaiverVersion() != null ? service.getWaiverVersion() : 1,
                service.getWeatherSensitive(),
                service.getMinWindKmh(),
                service.getMaxWaveM(),
                service.getAvgRating(),
                service.getRatingCount(),
                service.getViewCount(),
                imageResults,
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
