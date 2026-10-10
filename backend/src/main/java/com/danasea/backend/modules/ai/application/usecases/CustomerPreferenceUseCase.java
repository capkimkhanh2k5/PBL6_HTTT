package com.danasea.backend.modules.ai.application.usecases;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.api.CustomerPreferenceReadApi;
import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort;
import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort.Feedback;
import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort.Profile;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.service.application.api.CustomerServiceActivityReadApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPreferenceUseCase implements CustomerPreferenceReadApi {
    private static final Set<String> SIGNALS = Set.of("SHOWN", "CLICK", "POSITIVE", "NEGATIVE");
    private final CustomerPreferenceStorePort store;
    private final CustomerServiceActivityReadApi activity;

    public Profile get(UUID ownerId) { requireOwner(ownerId); return store.read(ownerId); }
    public Profile replace(UUID ownerId, Long expectedVersion, Boolean enabled, List<String> interests, List<String> exclusions) {
        requireOwner(ownerId);
        if (expectedVersion == null || expectedVersion < 0 || enabled == null) throw new IllegalArgumentException("expectedVersion and explicit personalization consent are required");
        return store.replace(ownerId, expectedVersion, enabled, terms(interests), terms(exclusions));
    }
    @Override public Signals load(UUID ownerId) {
        if (ownerId == null) return Signals.disabled();
        Profile profile = store.read(ownerId);
        if (!profile.enabled()) return Signals.disabled();
        var existingActivity = activity.read(ownerId);
        Set<UUID> positive = new HashSet<>(), negative = new HashSet<>(), classified = new HashSet<>();
        for (Feedback feedback : store.recentFeedback(ownerId, 100)) {
            if (!Set.of("POSITIVE", "NEGATIVE").contains(feedback.signal()) || !classified.add(feedback.serviceId())) continue;
            if (feedback.signal().equals("POSITIVE")) positive.add(feedback.serviceId()); else negative.add(feedback.serviceId());
        }
        return new Signals(true, profile.interests(), profile.exclusions(), existingActivity.wishedServiceIds(),
                existingActivity.recentlyViewedServiceIds(), Set.copyOf(positive), Set.copyOf(negative));
    }
    @Override public UUID recordRecommendation(UUID ownerId, List<UUID> returnedServiceIds, String fingerprint) {
        requireOwner(ownerId);
        if (returnedServiceIds == null || returnedServiceIds.size() > 15 || returnedServiceIds.stream().anyMatch(id -> id == null)
                || fingerprint == null || fingerprint.isBlank() || fingerprint.length() > 128) throw new IllegalArgumentException("A bounded backend recommendation result and criteria fingerprint are required");
        return store.createRecommendation(ownerId, returnedServiceIds.stream().distinct().toList(), fingerprint).id();
    }
    public Feedback feedback(UUID ownerId, UUID recommendationId, UUID serviceId, String signal, String idempotencyKey) {
        requireOwner(ownerId); validateKey(idempotencyKey);
        if (recommendationId == null || serviceId == null || signal == null || !SIGNALS.contains(signal)) throw new IllegalArgumentException("A recommendation, service and valid customer signal are required");
        String hash = hash(recommendationId + "|" + serviceId + "|" + signal);
        var replay = store.findFeedback(ownerId, idempotencyKey);
        if (replay.isPresent()) {
            if (!replay.get().requestHash().equals(hash)) throw new AiStateConflictException("Idempotency key was already used for another request");
            return replay.get();
        }
        var result = store.findRecommendation(recommendationId, ownerId)
                .orElseThrow(() -> new AiResourceNotFoundException("Recommendation not found"));
        if (!result.expiresAt().isAfter(OffsetDateTime.now())) throw new AiStateConflictException("Recommendation feedback window has expired");
        if (!result.serviceIds().contains(serviceId)) throw new IllegalArgumentException("Feedback service must belong to the backend recommendation result");
        return store.createFeedback(ownerId, recommendationId, serviceId, signal, idempotencyKey, hash);
    }
    private List<String> terms(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > 20 || values.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 80)) throw new IllegalArgumentException("Preference terms must contain 1 to 80 characters, at most twenty terms");
        return values.stream().map(value -> value.strip().toLowerCase(Locale.ROOT)).distinct().toList();
    }
    private void requireOwner(UUID ownerId) { if (ownerId == null) throw new IllegalArgumentException("An authenticated customer is required"); }
    public static void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,120}")) throw new IllegalArgumentException("A valid Idempotency-Key containing 1 to 120 characters is required");
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("Request fingerprint is unavailable", exception); }
    }
}
