package com.danasea.backend.modules.ai.infrastructure.adapters;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.ports.TravelPolicyPort;
import com.danasea.backend.modules.order.application.api.AiPolicyReadApi.Snapshot;
import com.danasea.backend.modules.order.application.api.AiPolicyReadApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TravelPolicyAdapter implements TravelPolicyPort {
    private final AiPolicyReadApi policy;
    @Override
    public Snapshot current() { return policy.current(); }
}
