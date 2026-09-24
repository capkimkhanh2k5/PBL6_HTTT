package com.danasea.backend.modules.settlement.presentation.controllers;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.settlement.application.dto.SettlementDetailResponse;
import com.danasea.backend.modules.settlement.application.dto.SettlementResponse;
import com.danasea.backend.modules.settlement.application.usecases.GetSettlementsUseCase;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vendor/settlements")
@PreAuthorize("hasRole('VENDOR')")
public class VendorSettlementController {

    private final GetSettlementsUseCase getSettlementsUseCase;
    private final VendorLookupPort vendorLookupPort;

    @Autowired
    public VendorSettlementController(
            GetSettlementsUseCase getSettlementsUseCase,
            VendorLookupPort vendorLookupPort
    ) {
        this.getSettlementsUseCase = getSettlementsUseCase;
        this.vendorLookupPort = vendorLookupPort;
    }

    @GetMapping
    public ResponseEntity<Page<SettlementResponse>> getVendorSettlements(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentVendorId = resolveCurrentVendorId();
        Page<SettlementResponse> result = getSettlementsUseCase.executeForVendor(currentVendorId, status, from, to, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementDetailResponse> getVendorSettlementDetail(@PathVariable UUID id) {
        UUID currentVendorId = resolveCurrentVendorId();
        SettlementDetailResponse result = getSettlementsUseCase.getVendorSettlementById(currentVendorId, id);
        return ResponseEntity.ok(result);
    }

    private UUID resolveCurrentVendorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new UnauthorizedSettlementAccessException("User is not authenticated");
        }

        UUID currentUserId = null;
        if (auth.getDetails() instanceof AuthorizationSubject subject) {
            currentUserId = subject.userId();
        }
        if (currentUserId == null) {
            try {
                currentUserId = UUID.fromString(auth.getName());
            } catch (IllegalArgumentException e) {
                currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            }
        }
        if (currentUserId == null) {
            throw new UnauthorizedSettlementAccessException("User is not authenticated");
        }

        final UUID finalUserId = currentUserId;
        return vendorLookupPort.findVendorIdByUserId(finalUserId)
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("Vendor profile not found for current user: " + finalUserId));
    }
}
