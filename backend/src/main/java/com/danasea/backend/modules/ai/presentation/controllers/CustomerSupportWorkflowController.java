package com.danasea.backend.modules.ai.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort.Request;
import com.danasea.backend.modules.ai.application.usecase.CustomerSupportRequestUseCase;
import com.danasea.backend.modules.ai.application.usecase.CustomerSupportRequestUseCase.Command;
import com.danasea.backend.modules.ai.application.usecase.CustomerSupportRequestUseCase.Preview;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/support/requests")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CustomerSupportWorkflowController {
    public record CancelRequest(Long expectedVersion) {}
    private final CustomerSupportRequestUseCase requests;
    @PostMapping("/preview") public Preview preview(@RequestBody Command command) { return requests.preview(owner(), command); }
    @PostMapping public Request create(@RequestBody Command command, @RequestHeader("Idempotency-Key") String key) { return requests.create(owner(), command, key); }
    @GetMapping public List<Request> list() { return requests.list(owner()); }
    @GetMapping("/{id}") public Request get(@PathVariable UUID id) { return requests.get(owner(), id); }
    @PostMapping("/{id}/cancel") public Request cancel(@PathVariable UUID id, @RequestBody CancelRequest command) { return requests.cancel(owner(), id, command.expectedVersion()); }
    private UUID owner() { return SecurityUtils.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Authentication required")); }
}
