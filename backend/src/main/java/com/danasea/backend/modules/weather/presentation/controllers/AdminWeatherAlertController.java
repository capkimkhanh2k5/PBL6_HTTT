package com.danasea.backend.modules.weather.presentation.controllers;

import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/weather-alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWeatherAlertController {

    private final JpaSafetyRuleEvaluationRepository evaluationRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaRefundRepository refundRepository;
    private final JpaServiceSlotRepository slotRepository;
    private final JpaServiceRepository serviceRepository;
    private final SendNotificationUseCase sendNotificationUseCase;

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
        if (alerts == null || alerts.isEmpty()) {
            alerts = evaluationRepository.findByIsSafeFalse();
        }

        List<WeatherAlertResponse> response = alerts.stream().map(alert -> {
            String sName = "Service " + alert.getServiceId();
            if (alert.getServiceId() != null) {
                sName = serviceRepository.findById(alert.getServiceId())
                        .map(s -> s.getName())
                        .orElse(sName);
            }

            String dateStr = "";
            String timeStr = "";
            int bookedCount = 0;
            if (alert.getSlotId() != null) {
                var slotOpt = slotRepository.findById(alert.getSlotId());
                if (slotOpt.isPresent()) {
                    dateStr = slotOpt.get().getDate() != null ? slotOpt.get().getDate().toString() : "";
                    timeStr = slotOpt.get().getStartTime() != null ? slotOpt.get().getStartTime().toString() : "";
                    bookedCount = slotOpt.get().getBookedCount() != null ? slotOpt.get().getBookedCount() : 0;
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

        SafetyRuleEvaluationJpaEntity alert = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Not found alert with ID: " + evaluationId));

        String action = request.action() != null ? request.action().toUpperCase() : "CANCEL_AND_REFUND";
        int refundedCount = 0;

        if ("CANCEL_AND_REFUND".equals(action)) {
            // Cancel all sub-orders for this slot and trigger 100% refund
            if (alert.getSlotId() != null) {
                List<SubOrderJpaEntity> subOrders = subOrderRepository.findBySlotId(alert.getSlotId());
                for (SubOrderJpaEntity subOrder : subOrders) {
                    if (subOrder.getStatus() != SubOrderStatus.CANCELLED
                            && subOrder.getStatus() != SubOrderStatus.REFUNDED) {
                        subOrder.setStatus(SubOrderStatus.CANCELLED);
                        subOrderRepository.save(subOrder);

                        // Create 100% refund record
                        RefundJpaEntity refund = new RefundJpaEntity();
                        refund.setSubOrderId(subOrder.getId());
                        refund.setAmount(subOrder.getSubtotalAmount());
                        refund.setRefundPercentage(BigDecimal.valueOf(100.0));
                        refund.setReason(RefundReason.WEATHER);
                        refund.setStatus(RefundStatus.PROCESSED);
                        refund.setProcessedAt(OffsetDateTime.now());
                        refundRepository.save(refund);

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
                    "message", "Canceled and refunded 100% due to bad weather."));
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
