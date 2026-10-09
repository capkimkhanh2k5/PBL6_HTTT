package com.danasea.backend.modules.ai.application.port;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Query;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface TravelDataPort {
    List<PublishedService> search(Query query);
    PublishedService find(UUID id, Query query);
    PublishedService metadata(UUID id, SupportedLanguage language);
    List<ReviewEvidence> reviews(UUID serviceId, int limit);
    long reviewCount(UUID serviceId);
}
