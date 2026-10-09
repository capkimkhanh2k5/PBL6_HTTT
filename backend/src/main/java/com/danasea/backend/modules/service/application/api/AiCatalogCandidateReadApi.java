package com.danasea.backend.modules.service.application.api;

import java.util.List;
import java.util.UUID;

public interface AiCatalogCandidateReadApi {
    List<UUID> candidateIds(AiCatalogReadApi.Query criteria, int offset, int batchSize);
}
