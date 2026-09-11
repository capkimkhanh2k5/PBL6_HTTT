package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceImage;

public interface ServiceImageRepositoryPort {
    List<ServiceImage> findByServiceId(UUID serviceId);

    List<ServiceImage> saveAll(List<ServiceImage> images);

    void deleteByServiceId(UUID serviceId);

    void deleteById(UUID id);

    boolean existsByServiceId(UUID serviceId);
}
