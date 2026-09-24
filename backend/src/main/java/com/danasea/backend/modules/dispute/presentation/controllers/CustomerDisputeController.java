package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.CreateDisputeUseCase;
import com.danasea.backend.modules.dispute.presentation.dtos.CreateDisputeRequest;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerDisputeController {

    private final CreateDisputeUseCase createDisputeUseCase;

    @PostMapping("/{id}/disputes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputeResponse> createDispute(
            @PathVariable("id") UUID orderId,
            @Valid @RequestBody CreateDisputeRequest request
    ) {
        log.info("Received customer request to create dispute for orderId: {}, subOrderId: {}", orderId, request.subOrderId());
        DisputeResponse response = createDisputeUseCase.execute(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
