package com.danasea.backend.modules.ai.application.port;

import com.danasea.backend.modules.order.application.api.AiPolicyReadApi.Snapshot;

public interface TravelPolicyPort {
    Snapshot current();
}
