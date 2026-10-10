package com.danasea.backend.modules.order.application.services;

import com.danasea.backend.modules.order.domain.models.MissingWaiverItem;
import com.danasea.backend.modules.order.domain.ports.ServiceWaiverLookupPort;
import com.danasea.backend.shared.i18n.LocalizedContentSelector;

import java.util.Optional;
import java.util.UUID;

public final class WaiverAcceptanceGuard {
    private WaiverAcceptanceGuard() {}

    public static Optional<MissingWaiverItem> missing(
            UUID subOrderId,
            UUID serviceId,
            Integer version,
            Boolean required,
            Boolean accepted,
            String vietnamese,
            String english,
            ServiceWaiverLookupPort lookup,
            LocalizedContentSelector selector) {
        if (!Boolean.TRUE.equals(required) || Boolean.TRUE.equals(accepted))
            return Optional.empty();
        String name =
                lookup == null
                        ? "Service"
                        : lookup.findWaiverSnapshot(serviceId)
                                .map(ServiceWaiverLookupPort.ServiceWaiverSnapshot::serviceName)
                                .orElse("Service");
        var selection =
                (selector == null ? new LocalizedContentSelector() : selector)
                        .selectDetailed(vietnamese, english);
        return Optional.of(
                new MissingWaiverItem(
                        subOrderId,
                        serviceId,
                        name,
                        version == null ? 1 : version,
                        true,
                        selection.content(),
                        selection.language(),
                        selection.fallbackUsed()));
    }
}
