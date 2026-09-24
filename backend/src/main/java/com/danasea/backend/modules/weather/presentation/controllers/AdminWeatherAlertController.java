package com.danasea.backend.modules.weather.presentation.controllers;

import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/weather-alerts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWeatherAlertController {

    private final JpaSafetyRuleEvaluationRepository evaluationRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaRefundRepository refundRepository;
    private final JpaServiceSlotRepository slotRepository;
    private final JpaServiceRepository serviceRepository;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final RefundPolicyEngine refundPolicyEngine;

    @Autowired
    public AdminWeatherAlertController(
            JpaSafetyRuleEvaluationRepository evaluationRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaRefundRepository refundRepository,
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            SendNotificationUseCase sendNotificationUseCase,
            RefundPolicyEngine refundPolicyEngine
    ) {
        this.evaluationRepository = evaluationRepository;
        this.subOrderRepository = subOrderRepository;
        this.refundRepository = refundRepository;
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.refundPolicyEngine = java.util.Objects.requireNonNull(refundPolicyEngine, "refundPolicyEngine");
    }

    public AdminWeatherAlertController(
            JpaSafetyRuleEvaluationRepository evaluationRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaRefundRepository refundRepository,
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            SendNotificationUseCase sendNotificationUseCase
    ) {
        this(evaluationRepository, subOrderRepository, refundRepository, slotRepository,
             serviceRepository, sendNotificationUseCase, new RefundPolicyEngine());
    }

    @Data
    @Builder
    public static class WeatherAlertResponse {
        private UUID id;
        private UUID serviceId;
        private String serviceName;
        private UUID slotId;
        private String slotDate;
        private String slotTime;
        private Boolean isSafe;
        private String status;
        private String alertLevel;
        private String warningMessage;
        private OffsetDateTime evaluatedAt;
        private int affectedBookingsCount;
        private Double peakWaveHeightM;
        private Double peakWindSpeedKmh;
        private Double peakWindGustKmh;
        private Double peakOceanCurrentMs;
        private Double minVisibilityM;
        private Integer severeWeatherCode;
    }

    public record ResolveAlertRequest(String action, String resolutionNote) {
    }

    @GetMapping
    public ResponseEntity<List<WeatherAlertResponse>> getActiveWeatherAlerts() {
        List<SafetyRuleEvaluationJpaEntity> alerts = evaluationRepository.findByStatusIn(
                List.of("AWAITING_ADMIN_RESOLUTION", "MONITORING_YELLOW"));
        alerts = alerts == null ? List.of() : alerts;

        Map<UUID, com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity> services =
                serviceRepository.findAllById(alerts.stream()
                                .map(SafetyRuleEvaluationJpaEntity::getServiceId)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(
                                com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity::getId,
                                Function.identity()));
        Map<UUID, com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity> slots =
                slotRepository.findAllById(alerts.stream()
                                .map(SafetyRuleEvaluationJpaEntity::getSlotId)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(
                                com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity::getId,
                                Function.identity()));

        List<WeatherAlertResponse> response = alerts.stream().map(alert -> {
            String sName = "Service " + alert.getServiceId();
            if (alert.getServiceId() != null) {
                var service = services.get(alert.getServiceId());
                sName = service != null && service.getName() != null ? service.getName() : sName;
            }

            String dateStr = "";
            String timeStr = "";
            int bookedCount = 0;
            if (alert.getSlotId() != null) {
                var slot = slots.get(alert.getSlotId());
                if (slot != null) {
                    dateStr = slot.getDate() != null ? slot.getDate().toString() : "";
                    timeStr = slot.getStartTime() != null ? slot.getStartTime().toString() : "";
                    bookedCount = slot.getBookedCount() != null ? slot.getBookedCount() : 0;
                }
            }

            return WeatherAlertResponse.builder()
                    .id(alert.getId())
                    .serviceId(alert.getServiceId())
                    .serviceName(sName)
                    .slotId(alert.getSlotId())
                    .slotDate(dateStr)
                    .slotTime(timeStr)
                    .isSafe(alert.getIsSafe())
                    .status(alert.getStatus())
                    .alertLevel(alert.getAlertLevel())
                    .warningMessage(alert.getWarningMessage())
                    .evaluatedAt(alert.getEvaluatedAt())
                    .affectedBookingsCount(bookedCount)
                    .peakWaveHeightM(alert.getPeakWaveHeightM())
                    .peakWindSpeedKmh(alert.getPeakWindSpeedKmh())
                    .peakWindGustKmh(alert.getPeakWindGustKmh())
                    .peakOceanCurrentMs(alert.getPeakOceanCurrentMs())
                    .minVisibilityM(alert.getMinVisibilityM())
                    .severeWeatherCode(alert.getSevereWeatherCode())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{evaluationId}/resolve")
    @Transactional
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable UUID evaluationId,
            @RequestBody ResolveAlertRequest request) {

        if (request == null || request.action() == null || request.action().isBlank()) {
            throw new IllegalArgumentException("Resolution action is required.");
        }
        String action = request.action().trim().toUpperCase();
        if (!"CANCEL_AND_REFUND".equals(action) && !"DISMISSED".equals(action)) {
            throw new IllegalArgumentException("Resolution action must be CANCEL_AND_REFUND or DISMISSED.");
        }

        SafetyRuleEvaluationJpaEntity alert = evaluationRepository.findByIdForUpdate(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Weather alert not found with ID: " + evaluationId));

        if (alert.getStatus() != null && alert.getStatus().startsWith("RESOLVED_")) {
            return ResponseEntity.ok(Map.of(
                    "status", alert.getStatus(),
                    "evaluationId", evaluationId,
                    "message", "Weather alert was already resolved."));
        }
        int refundedCount = 0;

        if ("CANCEL_AND_REFUND".equals(action)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime slotStart = now;
            if (alert.getSlotId() != null) {
                var slotOpt = slotRepository.findById(alert.getSlotId());
                if (slotOpt.isPresent() && slotOpt.get().getDate() != null && slotOpt.get().getStartTime() != null) {
                    slotStart = LocalDateTime.of(slotOpt.get().getDate(), slotOpt.get().getStartTime());
                }
            }

            // Cancel all sub-orders for this slot and trigger 100% refund
            if (alert.getSlotId() != null) {
                List<SubOrderJpaEntity> subOrders = subOrderRepository.findBySlotId(alert.getSlotId());
                for (SubOrderJpaEntity subOrder : subOrders) {
                    if (subOrder.getStatus() != SubOrderStatus.CANCELLED
                            && subOrder.getStatus() != SubOrderStatus.REFUNDED) {
                        subOrder.setStatus(SubOrderStatus.CANCELLED);
                        subOrderRepository.save(subOrder);

                        RefundEvaluationResult evalResult = refundPolicyEngine.evaluate(
                                RefundReason.WEATHER,
                                slotStart,
                                now,
                                subOrder.getSubtotalAmount()
                        );

                        String idempotencyKey = "weather-" + evaluationId;
                        if (refundRepository.findBySubOrderIdAndIdempotencyKey(
                                subOrder.getId(), idempotencyKey).isEmpty()) {
                            RefundJpaEntity refund = new RefundJpaEntity();
                            refund.setSubOrderId(subOrder.getId());
                            refund.setAmount(evalResult.refundAmount());
                            refund.setRefundPercentage(evalResult.refundPercentage());
                            refund.setReason(RefundReason.WEATHER);
                            refund.setStatus(RefundStatus.PENDING);
                            refund.setRequestedBy(SecurityUtils.getCurrentUserId().orElse(null));
                            refund.setIdempotencyKey(idempotencyKey);
                            refundRepository.save(refund);
                        }

                        refundedCount++;
                    }
                }
            }

            // Mark alert as resolved
            alert.setIsSafe(true);
            alert.setStatus("RESOLVED_CANCEL_AND_REFUND");
            alert.setWarningMessage(
                    "[ADMIN RESOLVED] Canceled and refunded 100% due to bad weather. Note: "
                            + request.resolutionNote());
            evaluationRepository.save(alert);

            log.info("Admin resolved weather alert {}: CANCEL_AND_REFUND for {} sub-orders.", evaluationId,
                    refundedCount);
            return ResponseEntity.ok(Map.of(
                    "status", "RESOLVED_CANCEL_AND_REFUND",
                    "evaluationId", evaluationId,
                    "refundedOrdersCount", refundedCount,
                    "refundRequestsCount", refundedCount,
                    "message", "Orders were cancelled and full refund requests are pending provider processing."));
        } else {
            // Dismiss alert / Reschedule
            alert.setIsSafe(true);
            alert.setStatus("RESOLVED_DISMISSED");
            alert.setWarningMessage("[ADMIN RESOLVED] Rescheduled or dismissed due to weather. Note: "
                    + request.resolutionNote());
            evaluationRepository.save(alert);

            return ResponseEntity.ok(Map.of(
                    "status", "RESOLVED_DISMISSED",
                    "evaluationId", evaluationId,
                    "message", "Rescheduled or dismissed due to weather."));
        }
    }
}
