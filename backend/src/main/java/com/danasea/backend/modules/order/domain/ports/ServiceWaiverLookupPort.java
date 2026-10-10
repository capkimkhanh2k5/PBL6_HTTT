package com.danasea.backend.modules.order.domain.ports;

import java.util.Optional;
import java.util.UUID;

public interface ServiceWaiverLookupPort {

    record ServiceWaiverSnapshot(
            UUID serviceId,
            String serviceName,
            boolean waiverRequired,
            int waiverVersion,
            String waiverContent,
            String waiverContentEn) {}

    Optional<ServiceWaiverSnapshot> findWaiverSnapshot(UUID serviceId);
}
