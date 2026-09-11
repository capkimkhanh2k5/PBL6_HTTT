package com.danasea.backend.modules.service.domain.ports;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;

public interface ServiceRepositoryPort {

    Optional<Service> findById(UUID id);

    Optional<Service> findPublishedById(UUID id);

    int incrementViewCount(UUID id, ServiceStatus status);

    List<Service> searchPublishedServices(
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal lat,
            BigDecimal lng,
            Double radiusKm,
            int page,
            int size
    );

    long countPublishedServices(
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal lat,
            BigDecimal lng,
            Double radiusKm
    );

    Service save(Service service);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
