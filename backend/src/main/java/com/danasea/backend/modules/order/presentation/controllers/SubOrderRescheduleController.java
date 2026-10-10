package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.application.usecases.ConfirmSubOrderRescheduleUseCase;
import com.danasea.backend.modules.order.application.usecases.CreateRescheduleProposalUseCase;
import com.danasea.backend.modules.order.application.usecases.GetRescheduleOptionsUseCase;
import com.danasea.backend.modules.order.presentation.dtos.ConfirmRescheduleRequest;
import com.danasea.backend.modules.order.presentation.dtos.CreateRescheduleProposalRequest;
import com.danasea.backend.modules.order.presentation.dtos.RescheduleOptionsSummaryResponse;
import com.danasea.backend.modules.order.presentation.dtos.RescheduleProposalResponse;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderRescheduleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
        name = "SubOrder Reschedule",
        description = "APIs xử lý đổi lịch do thời tiết và lý do vận hành")
public class SubOrderRescheduleController {

    private final GetRescheduleOptionsUseCase getRescheduleOptionsUseCase;
    private final CreateRescheduleProposalUseCase createRescheduleProposalUseCase;
    private final ConfirmSubOrderRescheduleUseCase confirmSubOrderRescheduleUseCase;

    @GetMapping("/api/sub-orders/{id}/reschedule-options")
    @Operation(summary = "Xem danh sách ca thay thế khả dụng và điều kiện đổi lịch")
    public ResponseEntity<RescheduleOptionsSummaryResponse> getRescheduleOptions(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(getRescheduleOptionsUseCase.execute(id));
    }

    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping("/api/vendor/sub-orders/{id}/reschedule-proposals")
    @Operation(summary = "Vendor gửi đề xuất đổi ca thay thế do lý do vận hành")
    public ResponseEntity<RescheduleProposalResponse> createProposal(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateRescheduleProposalRequest request) {
        return ResponseEntity.ok(createRescheduleProposalUseCase.execute(id, request));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/sub-orders/{id}/reschedule")
    @Operation(summary = "Khách hàng xác nhận chuyển sang ca khởi hành mới")
    public ResponseEntity<SubOrderRescheduleResponse> confirmReschedule(
            @PathVariable("id") UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConfirmRescheduleRequest request) {
        return ResponseEntity.ok(
                confirmSubOrderRescheduleUseCase.execute(id, request, idempotencyKey));
    }
}
