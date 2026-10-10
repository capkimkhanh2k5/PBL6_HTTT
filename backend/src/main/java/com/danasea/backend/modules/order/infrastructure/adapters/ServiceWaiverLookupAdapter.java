package com.danasea.backend.modules.order.infrastructure.adapters;

import com.danasea.backend.modules.order.domain.ports.ServiceWaiverLookupPort;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ServiceWaiverLookupAdapter implements ServiceWaiverLookupPort {

    private final JpaServiceRepository serviceRepository;

    public ServiceWaiverLookupAdapter(JpaServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public Optional<ServiceWaiverSnapshot> findWaiverSnapshot(UUID serviceId) {
        if (serviceId == null) {
            return Optional.empty();
        }
        return serviceRepository
                .findById(serviceId)
                .map(
                        service ->
                                new ServiceWaiverSnapshot(
                                        service.getId(),
                                        service.getName(),
                                        Boolean.TRUE.equals(service.getWaiverRequired()),
                                        service.getWaiverVersion() != null
                                                ? service.getWaiverVersion()
                                                : 1,
                                        service.getWaiverContent(),
                                        service.getWaiverContentEn()));
    }
}
