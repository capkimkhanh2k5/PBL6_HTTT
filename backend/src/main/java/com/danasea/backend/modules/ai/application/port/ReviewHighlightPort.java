package com.danasea.backend.modules.ai.application.port;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface ReviewHighlightPort {
    List<UUID> select(List<ReviewEvidence> reviews, SupportedLanguage language);
}
