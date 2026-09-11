package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;

public interface ServiceRepositoryPort {
    Service save(Service service);

    Optional<Service> findById(UUID id);

    List<Service> findByVendorId(UUID vendorId);

    List<Service> findByStatus(ServiceStatus status);

    List<Service> findAll();

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
