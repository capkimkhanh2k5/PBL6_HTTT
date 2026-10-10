package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.application.dtos.AcceptSubOrderWaiverCommand;
import com.danasea.backend.modules.order.application.dtos.SubOrderWaiverAcceptanceResult;
import com.danasea.backend.modules.order.application.usecases.AcceptSubOrderWaiverUseCase;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderWaiverAcceptanceRequest;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderWaiverAcceptanceResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sub-orders")
public class SubOrderWaiverController {

    private final AcceptSubOrderWaiverUseCase acceptSubOrderWaiverUseCase;

    public SubOrderWaiverController(AcceptSubOrderWaiverUseCase acceptSubOrderWaiverUseCase) {
        this.acceptSubOrderWaiverUseCase = acceptSubOrderWaiverUseCase;
    }

    @PostMapping("/{id}/waiver-acceptance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SubOrderWaiverAcceptanceResponse> acceptWaiver(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SubOrderWaiverAcceptanceRequest request) {
        UUID userId = currentUserId();
        AcceptSubOrderWaiverCommand command =
                new AcceptSubOrderWaiverCommand(
                        request.accepted(), request.version(), request.language());
        SubOrderWaiverAcceptanceResult result =
                acceptSubOrderWaiverUseCase.execute(userId, id, command);
        return ResponseEntity.ok(
                new SubOrderWaiverAcceptanceResponse(
                        result.subOrderId(),
                        result.masterOrderId(),
                        result.accepted(),
                        result.waiverVersion(),
                        result.language(),
                        result.acceptedAt()));
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
    }
}
