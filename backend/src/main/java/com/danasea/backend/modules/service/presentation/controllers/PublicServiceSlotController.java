package com.danasea.backend.modules.service.presentation.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.application.usecases.GetServiceSlotsAvailabilityUseCase;
import com.danasea.backend.modules.service.presentation.dtos.PublicServiceSlotAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services/{id}/slots")
@RequiredArgsConstructor
public class PublicServiceSlotController {

    private final GetServiceSlotsAvailabilityUseCase getServiceSlotsAvailabilityUseCase;

    @GetMapping
    public ResponseEntity<List<PublicServiceSlotAvailabilityResponse>> getAvailableSlots(
            @PathVariable("id") UUID serviceId,
            @RequestParam(required = false) UUID optionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer quantity
    ) {
        List<PublicServiceSlotAvailabilityResponse> response = getServiceSlotsAvailabilityUseCase.execute(
                serviceId, optionId, from, to, quantity
        );
        return ResponseEntity.ok(response);
    }
}
