package com.danasea.backend.modules.ai.application.port;

import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import java.util.Optional;

public interface ConfirmationCardStorePort {
    void save(ConfirmationCard card);
    Optional<ConfirmationCard> findById(String id);
    void delete(String id);
}
