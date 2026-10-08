package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.ServiceOption;

public interface ServiceOptionRepositoryPort {

    ServiceOption save(ServiceOption option);

    Optional<ServiceOption> findById(UUID id);

    List<ServiceOption> findByServiceId(UUID serviceId);

    List<ServiceOption> findByServiceIdAndStatus(UUID serviceId, OptionStatus status);
}
