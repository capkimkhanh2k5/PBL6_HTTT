package com.danasea.backend.modules.ai.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort.Feedback;
import com.danasea.backend.modules.ai.application.ports.CustomerPreferenceStorePort.Profile;
import com.danasea.backend.modules.ai.application.usecases.CustomerPreferenceUseCase;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CustomerPreferenceController {
    public record PreferenceRequest(Long expectedVersion, Boolean enabled, List<String> interests, List<String> exclusions) {}
    public record FeedbackRequest(UUID serviceId, String signal) {}
    private final CustomerPreferenceUseCase preferences;
    @GetMapping("/preferences") public Profile get() { return preferences.get(owner()); }
    @PutMapping("/preferences") public Profile put(@RequestBody PreferenceRequest request) {
        return preferences.replace(owner(), request.expectedVersion(), request.enabled(), request.interests(), request.exclusions());
    }
    @PostMapping("/recommendations/{id}/feedback") public Feedback feedback(@PathVariable UUID id, @RequestBody FeedbackRequest request,
                                                                         @RequestHeader("Idempotency-Key") String key) {
        return preferences.feedback(owner(), id, request.serviceId(), request.signal(), key);
    }
    private UUID owner() { return SecurityUtils.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Authentication required")); }
}
