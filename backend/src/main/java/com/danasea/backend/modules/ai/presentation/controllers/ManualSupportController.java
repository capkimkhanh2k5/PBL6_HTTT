package com.danasea.backend.modules.ai.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.ai.application.ports.CustomerSupportRequestStorePort.Request;
import com.danasea.backend.modules.ai.application.usecases.CustomerSupportRequestUseCase;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/support/requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ManualSupportController {
    public record HandleRequest(Long expectedVersion, String status, String responseNote) {}
    private final CustomerSupportRequestUseCase requests;
    @GetMapping public List<Request> list(@RequestParam(defaultValue = "WAITING_REVIEW") String status, @RequestParam(defaultValue = "50") int limit) { return requests.supportQueue(status, limit); }
    @GetMapping("/{id}") public Request get(@PathVariable UUID id) { return requests.getForSupport(id); }
    @PostMapping("/{id}/handle") public Request handle(@PathVariable UUID id, @RequestBody HandleRequest command) {
        return requests.handle(SecurityUtils.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Authentication required")), id, command.expectedVersion(), command.status(), command.responseNote());
    }
}
