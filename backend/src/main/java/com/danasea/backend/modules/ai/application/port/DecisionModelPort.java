package com.danasea.backend.modules.ai.application.port;

import java.util.Map;

import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;

public interface DecisionModelPort {
    DecisionResult decide(DecisionTask task, Map<String, Object> state);
}
