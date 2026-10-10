package com.danasea.backend.modules.service.application.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.application.api.CustomerChangeSlotReadApi;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceSlotRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerChangeSlotReader implements CustomerChangeSlotReadApi {
    private final ServiceSlotRepositoryPort slots;
    private final ServiceRepositoryPort services;
    @Override @Transactional(readOnly = true)
    public SlotTarget read(UUID slotId) {
        var slot = slots.findById(slotId).orElseThrow(() -> new IllegalArgumentException("Requested slot not found"));
        services.findPublishedById(slot.getServiceId()).orElseThrow(() -> new ServiceNotFoundException("Requested service is not published"));
        return new SlotTarget(slot.getId(), slot.getServiceId(), slot.getDate(), slot.getStartTime(), slot.getStatus().name());
    }
}
