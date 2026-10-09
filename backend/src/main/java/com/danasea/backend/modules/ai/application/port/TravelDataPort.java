package com.danasea.backend.modules.ai.application.port;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Query;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface TravelDataPort {
    List<PublishedService> search(Query query);
    default com.danasea.backend.modules.service.application.api.AiCatalogReadApi.SearchPage searchPage(Query query) {
        return new com.danasea.backend.modules.service.application.api.AiCatalogReadApi.SearchPage(search(query), null, false, 0, List.of("LEGACY_RETRIEVAL_COVERAGE_UNKNOWN"));
    }
    PublishedService find(UUID id, Query query);
    PublishedService metadata(UUID id, SupportedLanguage language);
    List<ReviewEvidence> reviews(UUID serviceId, int limit);
    long reviewCount(UUID serviceId);
}
