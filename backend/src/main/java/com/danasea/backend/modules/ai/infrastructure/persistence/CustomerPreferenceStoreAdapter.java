package com.danasea.backend.modules.ai.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiCustomerPreferenceJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiRecommendationFeedbackJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiRecommendationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiCustomerPreferenceRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiRecommendationFeedbackRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiRecommendationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerPreferenceStoreAdapter implements CustomerPreferenceStorePort {
    private final JpaAiCustomerPreferenceRepository profiles;
    private final JpaAiRecommendationRepository recommendations;
    private final JpaAiRecommendationFeedbackRepository feedback;
    private final ObjectMapper mapper;

    @Override public Profile read(UUID ownerId) {
        return profiles.findByOwnerId(ownerId).map(this::profile).orElse(new Profile(ownerId, 0, false, List.of(), List.of(), null));
    }
    @Override public Profile replace(UUID ownerId, long expectedVersion, boolean enabled, List<String> interests, List<String> exclusions) {
        var entity = profiles.findByOwnerId(ownerId).orElseGet(() -> {
            var created = new AiCustomerPreferenceJpaEntity(); created.setOwnerId(ownerId); return created;
        });
        long version = entity.getVersion() == null ? 0 : entity.getVersion();
        if (version != expectedVersion) throw new AiStateConflictException("Preference version has changed");
        entity.setEnabled(enabled); entity.setInterestsJson(json(interests)); entity.setExclusionsJson(json(exclusions));
        // The absent profile is version zero; a created profile starts at one.
        if (entity.getVersion() == null) entity.setVersion(1L);
        try { return profile(profiles.saveAndFlush(entity)); }
        catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException exception) { throw new AiStateConflictException("Preference version has changed"); }
    }
    @Override public Recommendation createRecommendation(UUID ownerId, List<UUID> serviceIds, String fingerprint) {
        var entity = new AiRecommendationJpaEntity(); entity.setOwnerId(ownerId); entity.setServiceIdsJson(json(serviceIds));
        entity.setCriteriaFingerprint(fingerprint); entity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return recommendation(recommendations.saveAndFlush(entity));
    }
    @Override public Optional<Recommendation> findRecommendation(UUID id, UUID ownerId) {
        return recommendations.findByIdAndOwnerId(id, ownerId).map(this::recommendation);
    }
    @Override public Optional<Feedback> findFeedback(UUID ownerId, String key) {
        return feedback.findByOwnerIdAndIdempotencyKey(ownerId, key).map(this::feedback);
    }
    @Override public Feedback createFeedback(UUID ownerId, UUID recommendationId, UUID serviceId, String signal, String key, String hash) {
        var entity = new AiRecommendationFeedbackJpaEntity(); entity.setOwnerId(ownerId); entity.setRecommendationId(recommendationId);
        entity.setServiceId(serviceId); entity.setSignal(signal); entity.setIdempotencyKey(key); entity.setRequestHash(hash);
        try { return feedback(feedback.saveAndFlush(entity)); }
        catch (DataIntegrityViolationException exception) {
            var replay = findFeedback(ownerId, key).orElseThrow(() -> exception);
            if (!replay.requestHash().equals(hash)) throw new AiStateConflictException("Idempotency key was already used for another request");
            return replay;
        }
    }
    @Override public List<Feedback> recentFeedback(UUID ownerId, int limit) {
        return feedback.findByOwnerIdAndSignalInOrderByCreatedAtDescIdDesc(ownerId, List.of("POSITIVE", "NEGATIVE"), PageRequest.of(0, Math.min(100, Math.max(1, limit)))).stream().map(this::feedback).toList();
    }
    private Profile profile(AiCustomerPreferenceJpaEntity entity) {
        return new Profile(entity.getOwnerId(), entity.getVersion(), entity.isEnabled(), readStrings(entity.getInterestsJson()), readStrings(entity.getExclusionsJson()), entity.getUpdatedAt());
    }
    private Recommendation recommendation(AiRecommendationJpaEntity entity) {
        try { return new Recommendation(entity.getId(), entity.getOwnerId(), mapper.readValue(entity.getServiceIdsJson(), new TypeReference<List<UUID>>() {}), entity.getCriteriaFingerprint(), entity.getExpiresAt()); }
        catch (Exception exception) { throw new IllegalStateException("Cannot read recommendation", exception); }
    }
    private Feedback feedback(AiRecommendationFeedbackJpaEntity entity) {
        return new Feedback(entity.getId(), entity.getRecommendationId(), entity.getOwnerId(), entity.getServiceId(), entity.getSignal(), entity.getIdempotencyKey(), entity.getRequestHash(), entity.getCreatedAt());
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize customer preferences", exception); }
    }
    private List<String> readStrings(String value) {
        try { return mapper.readValue(value, new TypeReference<List<String>>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Cannot read customer preferences", exception); }
    }
}
