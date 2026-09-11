package com.danasea.backend.modules.service.presentation.controllers;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.service.application.dto.RejectServiceCommand;
import com.danasea.backend.modules.service.application.dto.ServiceResult;
import com.danasea.backend.modules.service.application.usecases.ApproveServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.GetAdminServicesUseCase;
import com.danasea.backend.modules.service.application.usecases.RejectServiceUseCase;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.presentation.dto.RejectServiceRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceController {

    private final GetAdminServicesUseCase getAdminServicesUseCase;
    private final ApproveServiceUseCase approveServiceUseCase;
    private final RejectServiceUseCase rejectServiceUseCase;

    @GetMapping
    public ResponseEntity<List<ServiceResult>> listServices(
            @RequestParam(required = false) ServiceStatus status) {
        return ResponseEntity.ok(getAdminServicesUseCase.execute(status));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ServiceResult> approve(
            @PathVariable UUID id,
            Principal principal) {
        UUID adminUserId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(approveServiceUseCase.execute(adminUserId, id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ServiceResult> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectServiceRequest request,
            Principal principal) {
        UUID adminUserId = UUID.fromString(principal.getName());
        RejectServiceCommand cmd = new RejectServiceCommand(adminUserId, id, request.reason());
        return ResponseEntity.ok(rejectServiceUseCase.execute(cmd));
    }
}
