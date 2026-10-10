package com.danasea.backend.modules.order.presentation.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.order.application.usecases.VendorDiscountCodeUseCase;
import com.danasea.backend.modules.order.presentation.dtos.CreateDiscountCodeRequest;
import com.danasea.backend.modules.order.presentation.dtos.DiscountCodeResponse;
import com.danasea.backend.modules.order.presentation.dtos.UpdateDiscountCodeRequest;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;
import com.danasea.backend.security.infrastructure.SecurityUtils;

@RestController
@RequestMapping("/api/vendor/discount-codes")
@PreAuthorize("hasRole('VENDOR')")
public class VendorDiscountCodeController {

    private final VendorDiscountCodeUseCase vendorDiscountCodeUseCase;
    private final VendorLookupPort vendorLookupPort;

    @Autowired
    public VendorDiscountCodeController(
            VendorDiscountCodeUseCase vendorDiscountCodeUseCase,
            @Autowired(required = false) VendorLookupPort vendorLookupPort
    ) {
        this.vendorDiscountCodeUseCase = vendorDiscountCodeUseCase;
        this.vendorLookupPort = vendorLookupPort;
    }

    public VendorDiscountCodeController(VendorDiscountCodeUseCase vendorDiscountCodeUseCase) {
        this(vendorDiscountCodeUseCase, null);
    }

    @GetMapping
    public ResponseEntity<Page<DiscountCodeResponse>> getVendorDiscountCodes(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID vendorId = resolveCurrentVendorId();
        Page<DiscountCodeResponse> page = vendorDiscountCodeUseCase.getVendorDiscountCodes(vendorId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountCodeResponse> getVendorDiscountCodeById(@PathVariable UUID id) {
        UUID vendorId = resolveCurrentVendorId();
        DiscountCodeResponse response = vendorDiscountCodeUseCase.getVendorDiscountCodeById(vendorId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DiscountCodeResponse> createVendorDiscountCode(
            @Valid @RequestBody CreateDiscountCodeRequest request
    ) {
        UUID vendorId = resolveCurrentVendorId();
        DiscountCodeResponse response = vendorDiscountCodeUseCase.createVendorDiscountCode(vendorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DiscountCodeResponse> updateVendorDiscountCode(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiscountCodeRequest request
    ) {
        UUID vendorId = resolveCurrentVendorId();
        DiscountCodeResponse response = vendorDiscountCodeUseCase.updateVendorDiscountCode(vendorId, id, request);
        return ResponseEntity.ok(response);
    }

    private UUID resolveCurrentVendorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new AccessDeniedException("User is not authenticated");
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
            throw new AccessDeniedException("User is not authenticated");
        }

        if (vendorLookupPort != null) {
            final UUID finalUserId = currentUserId;
            return vendorLookupPort.findVendorIdByUserId(finalUserId)
                    .orElseThrow(() -> new AccessDeniedException("Vendor profile was not found."));
        }
        return currentUserId;
    }
}
