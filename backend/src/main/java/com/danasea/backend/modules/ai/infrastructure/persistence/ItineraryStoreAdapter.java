package com.danasea.backend.modules.ai.infrastructure.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Alternative;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Item;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Preview;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Proposal;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Revision;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryItemJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryPreviewJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryProposalJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryRevisionJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryItemRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryPreviewRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryProposalRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryRevisionRepository;
import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ItineraryStoreAdapter implements ItineraryStorePort {
    private final JpaAiItineraryRepository repository;
    private final ObjectMapper mapper;
    private final JpaAiItineraryPreviewRepository previews;
    private final JpaAiItineraryProposalRepository proposals;
    private final JpaAiItineraryRevisionRepository revisions;
    private final JpaAiItineraryItemRepository items;
    private final SendNotificationUseCase notifications;

    @Override
    @Transactional
    public Saved create(UUID ownerId, ItineraryPlan plan) {
        return createEntity(ownerId, plan, null, null, SupportedLanguage.VI);
    }

    @Override
    @Transactional
    public Saved create(UUID ownerId, ItineraryPlan plan, String key, String fingerprint) {
        validateKey(key);
        var previous = repository.findByOwnerIdAndIdempotencyKey(ownerId, key);
        if (previous.isPresent()) return replay(previous.get(), fingerprint);
        return createEntity(ownerId, plan, key, fingerprint, SupportedLanguage.VI);
    }

    @Override
    @Transactional
    public Saved create(UUID ownerId, ItineraryPlan plan, String key, String fingerprint, SupportedLanguage language) {
        if (key == null) return createEntity(ownerId, plan, null, null, language);
        validateKey(key);
        String localizedFingerprint = fingerprint + ":LOCALE:" + language.code();
        var previous = repository.findByOwnerIdAndIdempotencyKey(ownerId, key);
        if (previous.isPresent()) return replay(previous.get(), localizedFingerprint);
        return createEntity(ownerId, plan, key, localizedFingerprint, language);
    }

    @Override
    @Transactional
    public Preview savePreview(UUID ownerId, List<Alternative> alternatives) {
        return savePreview(ownerId, alternatives, SupportedLanguage.VI);
    }

    @Override
    @Transactional
    public Preview savePreview(UUID ownerId, List<Alternative> alternatives, SupportedLanguage language) {
        AiItineraryPreviewJpaEntity entity = new AiItineraryPreviewJpaEntity();
        entity.setOwnerId(ownerId);
        entity.setLocale(language.code());
        entity.setAlternativesJson(serialize(alternatives));
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        return preview(previews.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Preview> findPreview(UUID id, UUID ownerId) {
        return previews.findByIdAndOwnerId(id, ownerId).map(this::preview);
    }

    @Override
    @Transactional
    public Saved savePreview(UUID id, UUID ownerId, String alternativeId, String key) {
        validateKey(key);
        String fingerprint = "PREVIEW:" + id + ":" + alternativeId;
        var previous = repository.findByOwnerIdAndIdempotencyKey(ownerId, key);
        if (previous.isPresent()) return replay(previous.get(), fingerprint);
        AiItineraryPreviewJpaEntity entity = previews.locked(id, ownerId)
                .orElseThrow(() -> new AiResourceNotFoundException("Itinerary preview not found"));
        // Locking the preview serializes repeated saves while the unique owner/key index protects other previews.
        previous = repository.findByOwnerIdAndIdempotencyKey(ownerId, key);
        if (previous.isPresent()) return replay(previous.get(), fingerprint);
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) throw new AiStateConflictException("Itinerary preview has expired");
        ItineraryPlan plan = preview(entity).alternatives().stream().filter(value -> value.id().equals(alternativeId))
                .map(Alternative::plan).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown itinerary alternative"));
        return createEntity(ownerId, plan, key, fingerprint, SupportedLanguage.fromTag(entity.getLocale()).orElse(SupportedLanguage.VI));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Saved> find(UUID id, UUID ownerId) { return repository.findByIdAndOwnerId(id, ownerId).map(this::toSaved); }

    @Override
    @Transactional(readOnly = true)
    public List<Saved> list(UUID ownerId) { return list(ownerId, 0, 50); }

    @Override
    @Transactional(readOnly = true)
    public List<Saved> list(UUID ownerId, int page, int size) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId, PageRequest.of(page, size)).stream().map(this::toSaved).toList();
    }

    @Override
    @Transactional
    public Saved replace(UUID id, UUID ownerId, long version, ItineraryPlan plan) {
        AiItineraryJpaEntity entity = locked(id, ownerId, version);
        entity.setPlanJson(serialize(plan));
        entity.setLifecycle("DRAFT");
        repository.saveAndFlush(entity);
        supersede(id, null);
        recordRevision(entity, "CUSTOMER_EDIT", null);
        index(entity, plan);
        return toSaved(entity);
    }

    @Override
    @Transactional
    public Saved transition(UUID id, UUID ownerId, long version, String lifecycle, String reason) {
        if (!Set.of("ACCEPTED", "STALE", "ARCHIVED").contains(lifecycle)) throw new IllegalArgumentException("Invalid itinerary lifecycle");
        AiItineraryJpaEntity entity = locked(id, ownerId, version);
        if (entity.getLifecycle().equals(lifecycle)) return toSaved(entity);
        if ("ARCHIVED".equals(entity.getLifecycle())) throw new AiStateConflictException("An archived itinerary cannot be changed");
        entity.setLifecycle(lifecycle);
        repository.saveAndFlush(entity);
        supersede(id, null);
        recordRevision(entity, reason, null);
        if ("ACCEPTED".equals(lifecycle)) index(entity, readPlan(entity.getPlanJson()));
        return toSaved(entity);
    }

    @Override
    @Transactional
    public Proposal propose(UUID id, UUID ownerId, long version, ItineraryPlan plan, String trigger, String sourceEventId) {
        AiItineraryJpaEntity itinerary = locked(id, ownerId, version);
        if ("ARCHIVED".equals(itinerary.getLifecycle())) throw new AiStateConflictException("An archived itinerary cannot be replanned");
        if (sourceEventId != null) {
            var previous = proposals.findByItineraryIdAndSourceEventId(id, sourceEventId);
            if (previous.isPresent()) return proposal(previous.get());
            supersede(id, null);
        }
        AiItineraryProposalJpaEntity entity = new AiItineraryProposalJpaEntity();
        entity.setItineraryId(id);
        entity.setOwnerId(ownerId);
        entity.setBaseVersion(version);
        entity.setState("PENDING");
        entity.setPlanJson(serialize(plan));
        entity.setOriginalPlanJson(itinerary.getPlanJson());
        entity.setTrigger(trigger);
        entity.setSourceEventId(sourceEventId);
        proposals.saveAndFlush(entity);
        if (sourceEventId != null) notifications.execute(new NotificationCommand(ownerId, "AI_ITINERARY_SOURCE_CHANGED",
                NotificationChannel.IN_APP, LocalizedMessageRef.of("notification.ai.itinerary.changed.title"),
                LocalizedMessageRef.of("notification.ai.itinerary.changed.body"), "AI_ITINERARY_PROPOSAL", entity.getId(), null));
        return proposal(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Proposal> findProposal(UUID id, UUID ownerId, UUID proposalId) {
        return proposals.findByIdAndItineraryIdAndOwnerId(proposalId, id, ownerId).map(this::proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proposal> proposals(UUID id, UUID ownerId) {
        return proposals.findByItineraryIdAndOwnerIdOrderByCreatedAtDesc(id, ownerId, PageRequest.of(0, 50))
                .stream().map(this::proposal).toList();
    }

    @Override
    @Transactional
    public Saved acceptProposal(UUID id, UUID ownerId, UUID proposalId, long version) {
        AiItineraryJpaEntity itinerary = locked(id, ownerId, version);
        AiItineraryProposalJpaEntity proposal = pending(id, ownerId, proposalId, version);
        if ("ARCHIVED".equals(itinerary.getLifecycle())) throw new AiStateConflictException("An archived itinerary cannot be changed");
        itinerary.setPlanJson(proposal.getPlanJson());
        itinerary.setLifecycle("ACCEPTED");
        // updatedAt is dirty even if a proposal has the same items; acceptance must advance the CAS version.
        itinerary.setUpdatedAt(OffsetDateTime.now());
        repository.saveAndFlush(itinerary);
        proposal.setState("ACCEPTED");
        proposals.saveAndFlush(proposal);
        supersede(id, proposalId);
        recordRevision(itinerary, "PROPOSAL_ACCEPTED", proposalId);
        index(itinerary, readPlan(proposal.getPlanJson()));
        return toSaved(itinerary);
    }

    @Override
    @Transactional
    public Proposal rejectProposal(UUID id, UUID ownerId, UUID proposalId, long version) {
        locked(id, ownerId, version);
        AiItineraryProposalJpaEntity entity = pending(id, ownerId, proposalId, version);
        entity.setState("REJECTED");
        return proposal(proposals.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Revision> revisions(UUID id, UUID ownerId) {
        if (repository.findByIdAndOwnerId(id, ownerId).isEmpty()) throw new AiResourceNotFoundException("Itinerary not found");
        return revisions.findByItineraryIdOrderByItineraryVersionDesc(id, PageRequest.of(0, 100)).stream()
                .map(entity -> new Revision(entity.getItineraryVersion(), entity.getLifecycle(), readPlan(entity.getPlanJson()),
                        entity.getReason(), entity.getProposalId(), entity.getCreatedAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Saved> trackedForSlot(UUID slotId) {
        return repository.findAllById(items.trackedForSlot(slotId, LocalDate.now(TravelContext.ZONE))).stream().map(this::toSaved).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> trackedSlotIds(int offset, int limit) {
        return items.trackedSlots(LocalDate.now(TravelContext.ZONE), PageRequest.of(offset, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSourceEvent(UUID id, String eventId) { return proposals.findByItineraryIdAndSourceEventId(id, eventId).isPresent(); }

    private Saved createEntity(UUID ownerId, ItineraryPlan plan, String key, String fingerprint, SupportedLanguage language) {
        AiItineraryJpaEntity entity = new AiItineraryJpaEntity();
        entity.setOwnerId(ownerId);
        entity.setLocale(language.code());
        entity.setPlanJson(serialize(plan));
        entity.setIdempotencyKey(key);
        entity.setRequestFingerprint(fingerprint);
        repository.saveAndFlush(entity);
        recordRevision(entity, "DRAFT_CREATED", null);
        index(entity, plan);
        return toSaved(entity);
    }

    private Saved replay(AiItineraryJpaEntity entity, String fingerprint) {
        if (!Objects.equals(entity.getRequestFingerprint(), fingerprint)) throw new AiStateConflictException("Idempotency key was already used for another request");
        return toSaved(entity);
    }

    private AiItineraryJpaEntity locked(UUID id, UUID ownerId, long version) {
        AiItineraryJpaEntity entity = repository.locked(id, ownerId).orElseThrow(() -> new AiResourceNotFoundException("Itinerary not found"));
        if (entity.getVersion() != version) throw new AiStateConflictException("Itinerary version has changed");
        return entity;
    }

    private AiItineraryProposalJpaEntity pending(UUID id, UUID ownerId, UUID proposalId, long version) {
        var entity = proposals.findByIdAndItineraryIdAndOwnerId(proposalId, id, ownerId)
                .orElseThrow(() -> new AiResourceNotFoundException("Itinerary proposal not found"));
        if (!"PENDING".equals(entity.getState()) || entity.getBaseVersion() != version) throw new AiStateConflictException("This proposal is no longer current");
        return entity;
    }

    private void supersede(UUID id, UUID acceptedProposalId) {
        var pending = proposals.findByItineraryIdAndState(id, "PENDING");
        pending.stream().filter(entity -> !entity.getId().equals(acceptedProposalId)).forEach(entity -> entity.setState("SUPERSEDED"));
        proposals.saveAll(pending);
    }

    private void recordRevision(AiItineraryJpaEntity itinerary, String reason, UUID proposalId) {
        var revision = new AiItineraryRevisionJpaEntity();
        revision.setItineraryId(itinerary.getId());
        revision.setItineraryVersion(itinerary.getVersion());
        revision.setLifecycle(itinerary.getLifecycle());
        revision.setPlanJson(itinerary.getPlanJson());
        revision.setReason(reason);
        revision.setProposalId(proposalId);
        revisions.saveAndFlush(revision);
    }

    private void index(AiItineraryJpaEntity itinerary, ItineraryPlan plan) {
        items.deleteByItineraryId(itinerary.getId());
        items.flush();
        for (Item activity : plan.items()) {
            var entity = new AiItineraryItemJpaEntity();
            entity.setItineraryId(itinerary.getId());
            entity.setSlotId(activity.slotId());
            entity.setServiceId(activity.serviceId());
            entity.setActivityDate(activity.start().toLocalDate());
            items.save(entity);
        }
    }

    private Preview preview(AiItineraryPreviewJpaEntity entity) {
        try {
            List<Alternative> alternatives = mapper.readValue(entity.getAlternativesJson(), new TypeReference<>() {});
            return new Preview(entity.getId(), alternatives, entity.getCreatedAt(), entity.getExpiresAt(), false);
        } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read itinerary preview", exception); }
    }

    private Proposal proposal(AiItineraryProposalJpaEntity entity) {
        ItineraryPlan previous = readPlan(entity.getOriginalPlanJson());
        ItineraryPlan next = readPlan(entity.getPlanJson());
        Set<UUID> oldIds = previous.items().stream().map(Item::slotId).collect(Collectors.toSet());
        Set<UUID> nextIds = next.items().stream().map(Item::slotId).collect(Collectors.toSet());
        return new Proposal(entity.getId(), entity.getItineraryId(), entity.getBaseVersion(), entity.getState(), next,
                oldIds.stream().filter(id -> !nextIds.contains(id)).sorted().toList(),
                nextIds.stream().filter(id -> !oldIds.contains(id)).sorted().toList(),
                next.items().stream().filter(item -> previous.items().stream().anyMatch(old -> old.slotId().equals(item.slotId()) && !old.equals(item))).toList(),
                entity.getTrigger(), entity.getSourceEventId(), entity.getCreatedAt(), false);
    }

    private String serialize(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize itinerary", exception); }
    }

    private ItineraryPlan readPlan(String json) {
        try { return mapper.readValue(json, ItineraryPlan.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read itinerary", exception); }
    }

    private Saved toSaved(AiItineraryJpaEntity entity) {
        return new Saved(entity.getId(), entity.getOwnerId(), entity.getVersion(), readPlan(entity.getPlanJson()),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getLifecycle(), false, entity.getLocale());
    }

    private void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,128}")) throw new IllegalArgumentException("An Idempotency-Key of 8 to 128 characters is required");
    }
}
