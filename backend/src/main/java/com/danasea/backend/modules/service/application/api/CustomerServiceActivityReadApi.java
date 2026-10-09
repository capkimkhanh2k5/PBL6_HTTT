package com.danasea.backend.modules.service.application.api;

import java.util.Set;
import java.util.UUID;

public interface CustomerServiceActivityReadApi {
    record Activity(Set<UUID> wishedServiceIds, Set<UUID> recentlyViewedServiceIds) {}
    Activity read(UUID customerId);
}
