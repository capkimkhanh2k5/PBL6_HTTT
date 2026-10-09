package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.TravelDataPort;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Query;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TravelDataAdapter implements TravelDataPort {
    private final AiCatalogReadApi catalog;
    private final AiReviewReadApi reviews;

    @Override public List<PublishedService> search(Query query) { return catalog.search(query); }
    @Override public PublishedService find(UUID id, Query query) { return catalog.find(id, query); }
    @Override public PublishedService metadata(UUID id, SupportedLanguage language) { return catalog.metadata(id, language); }
    @Override public List<ReviewEvidence> reviews(UUID serviceId, int limit) { return reviews.findPublicByService(serviceId, limit); }
    @Override public long reviewCount(UUID serviceId) { return reviews.countPublicByService(serviceId); }
}
