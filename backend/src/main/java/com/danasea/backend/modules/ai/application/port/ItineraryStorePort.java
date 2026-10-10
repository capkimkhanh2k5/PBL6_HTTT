package com.danasea.backend.modules.ai.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Alternative;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Preview;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Proposal;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Revision;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface ItineraryStorePort {
    Saved create(UUID ownerId, ItineraryPlan plan);
    Optional<Saved> find(UUID id, UUID ownerId);
    List<Saved> list(UUID ownerId);
    Saved replace(UUID id, UUID ownerId, long expectedVersion, ItineraryPlan plan);
    Saved create(UUID ownerId, ItineraryPlan plan, String idempotencyKey, String requestFingerprint);
    Saved create(UUID ownerId, ItineraryPlan plan, String idempotencyKey, String requestFingerprint, SupportedLanguage language);
    Preview savePreview(UUID ownerId, List<Alternative> alternatives);
    Preview savePreview(UUID ownerId, List<Alternative> alternatives, SupportedLanguage language);
    Optional<Preview> findPreview(UUID previewId, UUID ownerId);
    Saved savePreview(UUID previewId, UUID ownerId, String alternativeId, String idempotencyKey);
    Saved transition(UUID id, UUID ownerId, long expectedVersion, String lifecycle, String reason);
    Proposal propose(UUID itineraryId, UUID ownerId, long expectedVersion, ItineraryPlan plan,
                     String trigger, String sourceEventId);
    Optional<Proposal> findProposal(UUID itineraryId, UUID ownerId, UUID proposalId);
    List<Proposal> proposals(UUID itineraryId, UUID ownerId);
    Saved acceptProposal(UUID itineraryId, UUID ownerId, UUID proposalId, long expectedVersion);
    Proposal rejectProposal(UUID itineraryId, UUID ownerId, UUID proposalId, long expectedVersion);
    List<Revision> revisions(UUID itineraryId, UUID ownerId);
    List<Saved> trackedForSlot(UUID slotId);
    List<UUID> trackedSlotIds(int offset, int limit);
    boolean hasSourceEvent(UUID itineraryId, String eventId);
    List<Saved> list(UUID ownerId, int page, int size);
}
