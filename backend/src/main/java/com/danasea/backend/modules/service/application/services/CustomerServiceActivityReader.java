package com.danasea.backend.modules.service.application.services;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.application.api.CustomerServiceActivityReadApi;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.WishlistRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceActivityReader implements CustomerServiceActivityReadApi {
    private final WishlistRepositoryPort wishlists;
    private final RecentlyViewedRepositoryPort recentViews;
    private final ServiceRepositoryPort services;
    @Override
    @Transactional(readOnly = true)
    public Activity read(UUID customerId) {
        if (customerId == null) return new Activity(Set.of(), Set.of());
        Set<UUID> wished = wishlists.findAllByUserId(customerId).stream().limit(50)
                .map(item -> item.getServiceId()).filter(id -> services.findPublishedById(id).isPresent()).collect(Collectors.toUnmodifiableSet());
        Set<UUID> viewed = recentViews.findAllByUserId(customerId).stream().limit(50)
                .map(item -> item.getServiceId()).filter(id -> services.findPublishedById(id).isPresent()).collect(Collectors.toUnmodifiableSet());
        return new Activity(wished, viewed);
    }
}
