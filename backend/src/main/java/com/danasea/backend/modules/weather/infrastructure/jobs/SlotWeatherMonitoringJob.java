package com.danasea.backend.modules.weather.infrastructure.jobs;

import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.CategorySafetyRuleService;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class SlotWeatherMonitoringJob {

    private final JpaServiceSlotRepository slotRepository;
    private final JpaServiceRepository serviceRepository;
    private final JpaCategoryRepository categoryRepository;
    private final JpaSafetyRuleEvaluationRepository evaluationRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final WeatherProviderPort weatherProviderPort;
    private final WeatherRuleEngine weatherRuleEngine;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final JpaRefundRepository refundRepository;
    private final CategorySafetyRuleService categorySafetyRuleService;
    private final JpaMasterOrderRepository masterOrderRepository;

    private static final double DEFAULT_LAT = 16.089035780716284;
    private static final double DEFAULT_LNG = 108.24959555394304;

    @Autowired
    public SlotWeatherMonitoringJob(
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            JpaCategoryRepository categoryRepository,
            JpaSafetyRuleEvaluationRepository evaluationRepository,
            JpaSubOrderRepository subOrderRepository,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            SendNotificationUseCase sendNotificationUseCase,
            @Autowired(required = false) JpaRefundRepository refundRepository,
            @Autowired(required = false) CategorySafetyRuleService categorySafetyRuleService,
            @Autowired(required = false) JpaMasterOrderRepository masterOrderRepository
    ) {
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.evaluationRepository = evaluationRepository;
        this.subOrderRepository = subOrderRepository;
        this.weatherProviderPort = weatherProviderPort;
        this.weatherRuleEngine = weatherRuleEngine;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.refundRepository = refundRepository;
        this.categorySafetyRuleService = categorySafetyRuleService;
        this.masterOrderRepository = masterOrderRepository;
    }

    public SlotWeatherMonitoringJob(
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            JpaCategoryRepository categoryRepository,
            JpaSafetyRuleEvaluationRepository evaluationRepository,
            JpaSubOrderRepository subOrderRepository,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            SendNotificationUseCase sendNotificationUseCase,
            JpaRefundRepository refundRepository,
            CategorySafetyRuleService categorySafetyRuleService
    ) {
        this(slotRepository, serviceRepository, categoryRepository, evaluationRepository,
             subOrderRepository, weatherProviderPort, weatherRuleEngine, sendNotificationUseCase,
             refundRepository, categorySafetyRuleService, null);
    }

    public SlotWeatherMonitoringJob(
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            JpaCategoryRepository categoryRepository,
            JpaSafetyRuleEvaluationRepository evaluationRepository,
            JpaSubOrderRepository subOrderRepository,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            SendNotificationUseCase sendNotificationUseCase
    ) {
        this(slotRepository, serviceRepository, categoryRepository, evaluationRepository,
             subOrderRepository, weatherProviderPort, weatherRuleEngine, sendNotificationUseCase,
             null, null, null);
    }

    /**
     * Sliding window scheduler running every 30 minutes.
     * Evaluates booked slots in sliding windows T-24h and T-2h,
     * and performs auto-escalation fallback for unhandled RED incidents within 60 minutes.
     */
    @Scheduled(cron = "${app.weather.monitoring-cron:0 */30 * * * *}")
    @Transactional
    public void monitorUpcomingSlots() {
        monitorUpcomingSlots(LocalDateTime.now());
    }

    @Transactional
    public void monitorUpcomingSlots(LocalDateTime now) {
        log.info("Starting SlotWeatherMonitoringJob: scanning upcoming booked slots (T-24h and T-2h sliding windows) at {}...", now);
        LocalDate today = now.toLocalDate();
        LocalDate dayAfterTomorrow = today.plusDays(2);

        List<ServiceSlotJpaEntity> slots = slotRepository.findByDateBetween(today, dayAfterTomorrow);

        int alertCount = 0;
        for (ServiceSlotJpaEntity slot : slots) {
            if (slot.getBookedCount() != null && slot.getBookedCount() > 0) {
                // Check Auto-Escalation Fallback (T <= 60 minutes)
                boolean autoCancelled = checkAutoEscalationFallback(slot, now);
                if (autoCancelled) {
                    continue; // Do not evaluate in sliding window if just auto-cancelled!
                }

                // Check Sliding Window: T-24h [now+23h, now+25h] and T-2h [now+1h, now+3h]
                if (isSlotInSlidingWindow(slot, now)) {
                    boolean hasAlert = evaluateAndAlertSlot(slot);
                    if (hasAlert) alertCount++;
                }
            }
        }
        log.info("Completed SlotWeatherMonitoringJob. Total slots evaluated in range: {}, alerts active/triggered: {}", slots.size(), alertCount);
    }

    /**
     * Determines whether a slot falls within either the T-24h or T-2h sliding windows.
     * T-24h window: [now + 23h, now + 25h]
     * T-2h window:  [now + 1h, now + 3h]
     */
    public boolean isSlotInSlidingWindow(ServiceSlotJpaEntity slot, LocalDateTime now) {
        if (slot == null || slot.getDate() == null || slot.getStartTime() == null) {
            return false;
        }
        LocalDateTime slotStart = LocalDateTime.of(slot.getDate(), slot.getStartTime());
        LocalDateTime t24Start = now.plusHours(23);
        LocalDateTime t24End = now.plusHours(25);
        LocalDateTime t2Start = now.plusHours(1);
        LocalDateTime t2End = now.plusHours(3);

        boolean inT24 = (!slotStart.isBefore(t24Start)) && (!slotStart.isAfter(t24End));
        boolean inT2 = (!slotStart.isBefore(t2Start)) && (!slotStart.isAfter(t2End));
        return inT24 || inT2;
    }

    /**
     * Auto-escalation fallback for a specific slot:
     * If unresolved RED incident exists (status = 'AWAITING_ADMIN_RESOLUTION') and departure is within 60 minutes,
     * auto-cancels sub-orders, generates 100% refund, updates status to AUTO_CANCELLED_FOR_SAFETY,
     * and sends urgent notifications to Admin, Vendor, and Customer.
     */
    @Transactional
    public boolean checkAutoEscalationFallback(ServiceSlotJpaEntity slot) {
        return checkAutoEscalationFallback(slot, LocalDateTime.now());
    }

    @Transactional
    public boolean checkAutoEscalationFallback(ServiceSlotJpaEntity slot, LocalDateTime now) {
        if (slot == null || slot.getDate() == null || slot.getStartTime() == null) return false;

        LocalDateTime slotStart = LocalDateTime.of(slot.getDate(), slot.getStartTime());
        Duration durationToStart = Duration.between(now, slotStart);

        // Within 60 minutes of departure: T_remain <= 60m and T_remain >= 0
        if (!durationToStart.isNegative() && durationToStart.toMinutes() <= 60) {
            Optional<SafetyRuleEvaluationJpaEntity> latestAlertOpt = findActiveAlertForSlot(slot.getId());

            if (latestAlertOpt.isPresent()) {
                SafetyRuleEvaluationJpaEntity alert = latestAlertOpt.get();
                // Unresolved RED alert awaiting admin resolution
                boolean isUnresolvedRed = "AWAITING_ADMIN_RESOLUTION".equalsIgnoreCase(alert.getStatus())
                        || (Boolean.FALSE.equals(alert.getIsSafe())
                            && !"RESOLVED_CANCEL_AND_REFUND".equalsIgnoreCase(alert.getStatus())
                            && !"RESOLVED_DISMISSED".equalsIgnoreCase(alert.getStatus())
                            && !"AUTO_CANCELLED_FOR_SAFETY".equalsIgnoreCase(alert.getStatus()));

                if (isUnresolvedRed) {
                    log.warn("AUTO-ESCALATION FALLBACK: Slot {} has unresolved RED alert with {} min remaining. Auto-cancelling and refunding 100%.",
                            slot.getId(), durationToStart.toMinutes());

                    List<SubOrderJpaEntity> subOrders = subOrderRepository.findBySlotId(slot.getId());
                    int cancelledCount = 0;
                    for (SubOrderJpaEntity subOrder : subOrders) {
                        if (subOrder.getStatus() != SubOrderStatus.CANCELLED && subOrder.getStatus() != SubOrderStatus.REFUNDED) {
                            subOrder.setStatus(SubOrderStatus.CANCELLED);
                            subOrderRepository.save(subOrder);

                            if (refundRepository != null && subOrder.getSubtotalAmount() != null) {
                                RefundJpaEntity refund = new RefundJpaEntity();
                                refund.setSubOrderId(subOrder.getId());
                                refund.setAmount(subOrder.getSubtotalAmount());
                                refund.setRefundPercentage(BigDecimal.valueOf(100.0));
                                refund.setReason(RefundReason.WEATHER);
                                refund.setStatus(RefundStatus.PROCESSED);
                                refund.setProcessedAt(OffsetDateTime.now());
                                refundRepository.save(refund);
                            }
                            cancelledCount++;
                        }
                    }

                    // Close the slot to prevent further bookings
                    slot.setStatus(SlotStatus.CLOSED);
                    slotRepository.save(slot);

                    // Update alert status
                    alert.setStatus("AUTO_CANCELLED_FOR_SAFETY");
                    alert.setIsSafe(true);
                    alert.setWarningMessage("[AUTO-CANCELLED FOR SAFETY] Canceled and refunded 100% due to unresolved RED alert within 60 minutes: "
                            + alert.getWarningMessage());
                    evaluationRepository.save(alert);

                    // Tri-party urgent notifications
                    Optional<ServiceJpaEntity> serviceOpt = serviceRepository.findById(slot.getServiceId());
                    String serviceName = serviceOpt.map(ServiceJpaEntity::getName).orElse("Marine Service");

                    // 1. Notify Admin
                    sendNotificationUseCase.execute(
                            null,
                            "AUTO_CANCELLED_FOR_SAFETY",
                            NotificationChannel.IN_APP,
                            "System Auto-Cancelled: Slot cancelled and 100% refund due to dangerous weather (T <= 60m)",
                            String.format("Slot %s (%s at %s) has been auto-cancelled due to RED alert not being addressed within 60 minutes.",
                                    slot.getId(), slot.getDate(), slot.getStartTime()),
                            "SERVICE_SLOT",
                            slot.getId(),
                            null
                    );

                    // 2. Notify Vendor
                    if (serviceOpt.isPresent() && serviceOpt.get().getVendorId() != null) {
                        sendNotificationUseCase.execute(
                                serviceOpt.get().getVendorId(),
                                "AUTO_CANCELLED_FOR_SAFETY",
                                NotificationChannel.IN_APP,
                                "Trip auto-cancelled for maritime safety: " + serviceName,
                                String.format("Trip at %s on %s has been auto-cancelled by the system for maritime safety reasons.",
                                        slot.getStartTime(), slot.getDate()),
                                "SERVICE_SLOT",
                                slot.getId(),
                                null
                        );
                    }

                    // 3. Notify Customer
                    for (SubOrderJpaEntity subOrder : subOrders) {
                        UUID customerId = resolveCustomerId(subOrder);
                        sendNotificationUseCase.execute(
                                customerId,
                                "AUTO_CANCELLED_FOR_SAFETY",
                                NotificationChannel.IN_APP,
                                "Trip cancelled for maritime safety & 100% refund: " + serviceName,
                                String.format("Your trip at %s on %s has been cancelled due to dangerous weather conditions. A full 100%% refund has been processed successfully.",
                                        slot.getStartTime(), slot.getDate()),
                                "SUB_ORDER",
                                subOrder.getId(),
                                null
                        );
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluates weather condition for a slot with Idempotency, In-Place Metric Update,
     * YELLOW Early Warning, and Escalation detection.
     */
    @Transactional
    public boolean evaluateAndAlertSlot(ServiceSlotJpaEntity slot) {
        if (slot == null || slot.getServiceId() == null) return false;

        Optional<ServiceJpaEntity> serviceOpt = serviceRepository.findById(slot.getServiceId());
        if (serviceOpt.isEmpty()) return false;
        ServiceJpaEntity service = serviceOpt.get();

        String categorySlug = "default";
        if (service.getCategoryId() != null) {
            Optional<CategoryJpaEntity> catOpt = categoryRepository.findById(service.getCategoryId());
            if (catOpt.isPresent() && catOpt.get().getSlug() != null) {
                categorySlug = catOpt.get().getSlug();
            }
        }

        double lat = service.getLatitude() != null ? service.getLatitude().doubleValue() : DEFAULT_LAT;
        double lng = service.getLongitude() != null ? service.getLongitude().doubleValue() : DEFAULT_LNG;

        WeatherInfoDto.TimeWindowForecast forecast = weatherProviderPort.getTimeWindowForecast(
                lat, lng, slot.getDate(), slot.getStartTime(), slot.getEndTime()
        );

        if (forecast == null) {
            log.warn("Could not retrieve forecast for slot {}", slot.getId());
            return false;
        }

        CategorySafetyRule rule = null;
        if (categorySafetyRuleService != null) {
            rule = categorySafetyRuleService.getRuleByCategorySlug(categorySlug);
        }
        if (rule == null) {
            rule = CategorySafetyRule.getBySlug(categorySlug);
        }

        WeatherRuleEngine.SafetyEvaluationResult evalResult = weatherRuleEngine.evaluate(
                rule,
                forecast.getPeakWaveHeight(),
                forecast.getPeakWindSpeed(),
                forecast.getPeakWindGust(),
                forecast.getPeakOceanCurrent(),
                forecast.getMinVisibility(),
                forecast.getSevereWeatherCode()
        );

        String currentLevel = evalResult.getAlertLevel();
        if (currentLevel == null) {
            currentLevel = evalResult.isSafe() ? WeatherRuleEngine.ALERT_GREEN : WeatherRuleEngine.ALERT_RED;
        }

        // 0. GREEN: Safe condition
        if (WeatherRuleEngine.ALERT_GREEN.equals(currentLevel)) {
            Optional<SafetyRuleEvaluationJpaEntity> activeAlertOpt = findActiveAlertForSlot(slot.getId());
            if (activeAlertOpt.isPresent()) {
                SafetyRuleEvaluationJpaEntity existingAlert = activeAlertOpt.get();
                existingAlert.setIsSafe(true);
                existingAlert.setStatus("RESOLVED_SAFE");
                existingAlert.setWarningMessage("Weather conditions have improved to safe levels: " + evalResult.getWarningMessage());
                existingAlert.setEvaluatedAt(OffsetDateTime.now());
                evaluationRepository.save(existingAlert);
            }
            return false;
        }

        // Check if an active alert already exists for this slot
        Optional<SafetyRuleEvaluationJpaEntity> existingAlertOpt = findActiveAlertForSlot(slot.getId());

        // 1. RED ALERT CASE:
        if (!evalResult.isSafe() || WeatherRuleEngine.ALERT_RED.equals(currentLevel)) {
            log.warn("WEATHER ALERT (RED) for slot {} (Date: {}, Start: {}). Reason: {}",
                    slot.getId(), slot.getDate(), slot.getStartTime(), evalResult.getWarningMessage());

            if (existingAlertOpt.isPresent()) {
                SafetyRuleEvaluationJpaEntity existing = existingAlertOpt.get();
                String prevLevel = existing.getAlertLevel();
                if (prevLevel == null) {
                    prevLevel = "MONITORING_YELLOW".equalsIgnoreCase(existing.getStatus())
                            ? WeatherRuleEngine.ALERT_YELLOW
                            : WeatherRuleEngine.ALERT_RED;
                }

                if (WeatherRuleEngine.ALERT_YELLOW.equalsIgnoreCase(prevLevel)) {
                    // ESCALATION: YELLOW -> RED
                    log.warn("ESCALATION: Slot {} weather worsened from YELLOW to RED. Updating status and triggering urgent alerts.",
                            slot.getId());
                    existing.setStatus("AWAITING_ADMIN_RESOLUTION");
                    existing.setAlertLevel(WeatherRuleEngine.ALERT_RED);
                    existing.setIsSafe(false);
                    updateEntityMetrics(existing, forecast, evalResult);
                    evaluationRepository.save(existing);

                    sendRedAlertNotifications(service, slot, evalResult, true);
                    return true;
                } else {
                    // IDEMPOTENCY: Unchanged RED severity -> in-place update without duplicate alert or spam
                    log.info("Idempotency: Active RED alert for slot {} severity unchanged. Updating metrics in-place.",
                            slot.getId());
                    updateEntityMetrics(existing, forecast, evalResult);
                    evaluationRepository.save(existing);
                    return true;
                }
            } else {
                // New RED alert record
                SafetyRuleEvaluationJpaEntity entity = new SafetyRuleEvaluationJpaEntity();
                entity.setServiceId(service.getId());
                entity.setSlotId(slot.getId());
                entity.setIsSafe(false);
                entity.setAlertLevel(WeatherRuleEngine.ALERT_RED);
                entity.setStatus("AWAITING_ADMIN_RESOLUTION");
                updateEntityMetrics(entity, forecast, evalResult);
                evaluationRepository.save(entity);

                sendRedAlertNotifications(service, slot, evalResult, false);
                return true;
            }
        }

        // 2. YELLOW EARLY WARNING CASE:
        else if (WeatherRuleEngine.ALERT_YELLOW.equals(currentLevel)) {
            log.info("WEATHER CAUTION (YELLOW) for slot {} (Date: {}, Start: {}). Reason: {}",
                    slot.getId(), slot.getDate(), slot.getStartTime(), evalResult.getWarningMessage());

            if (existingAlertOpt.isPresent()) {
                SafetyRuleEvaluationJpaEntity existing = existingAlertOpt.get();
                String prevLevel = existing.getAlertLevel();
                if (prevLevel == null) {
                    prevLevel = "MONITORING_YELLOW".equalsIgnoreCase(existing.getStatus())
                            ? WeatherRuleEngine.ALERT_YELLOW
                            : WeatherRuleEngine.ALERT_RED;
                }

                if (WeatherRuleEngine.ALERT_YELLOW.equalsIgnoreCase(prevLevel)) {
                    // IDEMPOTENCY: Unchanged YELLOW severity -> in-place metric update without spam
                    log.info("Idempotency: Active YELLOW alert for slot {} severity unchanged. Updating metrics in-place.",
                            slot.getId());
                    updateEntityMetrics(existing, forecast, evalResult);
                    evaluationRepository.save(existing);
                    return true;
                } else {
                    // De-escalation (RED -> YELLOW)
                    existing.setStatus("MONITORING_YELLOW");
                    existing.setAlertLevel(WeatherRuleEngine.ALERT_YELLOW);
                    existing.setIsSafe(true);
                    updateEntityMetrics(existing, forecast, evalResult);
                    evaluationRepository.save(existing);
                    return true;
                }
            } else {
                // New YELLOW alert record: record with MONITORING_YELLOW for active tracking on Admin Portal
                SafetyRuleEvaluationJpaEntity entity = new SafetyRuleEvaluationJpaEntity();
                entity.setServiceId(service.getId());
                entity.setSlotId(slot.getId());
                entity.setIsSafe(true);
                entity.setAlertLevel(WeatherRuleEngine.ALERT_YELLOW);
                entity.setStatus("MONITORING_YELLOW");
                updateEntityMetrics(entity, forecast, evalResult);
                evaluationRepository.save(entity);

                // Send Early Warning to Vendor and Customer
                sendYellowEarlyWarningNotifications(service, slot, evalResult);
                return true;
            }
        }

        return false;
    }

    private Optional<SafetyRuleEvaluationJpaEntity> findActiveAlertForSlot(UUID slotId) {
        if (slotId == null) return Optional.empty();

        Optional<SafetyRuleEvaluationJpaEntity> alertOpt = evaluationRepository
                .findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(
                        slotId, List.of("AWAITING_ADMIN_RESOLUTION", "MONITORING_YELLOW")
                );
        if (alertOpt.isPresent()) {
            return alertOpt;
        }

        return evaluationRepository.findTopBySlotIdOrderByEvaluatedAtDesc(slotId)
                .filter(a -> Boolean.FALSE.equals(a.getIsSafe())
                        && !"RESOLVED_CANCEL_AND_REFUND".equalsIgnoreCase(a.getStatus())
                        && !"RESOLVED_DISMISSED".equalsIgnoreCase(a.getStatus())
                        && !"AUTO_CANCELLED_FOR_SAFETY".equalsIgnoreCase(a.getStatus()));
    }

    private void sendRedAlertNotifications(ServiceJpaEntity service, ServiceSlotJpaEntity slot,
                                           WeatherRuleEngine.SafetyEvaluationResult evalResult, boolean isEscalation) {
        String prefix = isEscalation ? "[ESCALATION ALERT] " : "";

        // 1. Vendor
        if (service.getVendorId() != null) {
            sendNotificationUseCase.execute(
                    service.getVendorId(),
                    "WEATHER_ALERT",
                    NotificationChannel.IN_APP,
                    prefix + "Dangerous weather alert for service: " + service.getName(),
                    evalResult.getWarningMessage(),
                    "SERVICE_SLOT",
                    slot.getId(),
                    null
            );
        }

        // 2. Customer
        List<SubOrderJpaEntity> subOrders = subOrderRepository.findBySlotId(slot.getId());
        for (SubOrderJpaEntity subOrder : subOrders) {
            if (subOrder.getStatus() == SubOrderStatus.CONFIRMED || subOrder.getStatus() == SubOrderStatus.PENDING) {
                UUID customerId = resolveCustomerId(subOrder);
                sendNotificationUseCase.execute(
                        customerId,
                        "WEATHER_WARNING",
                        NotificationChannel.IN_APP,
                        prefix + "Weather alert for upcoming trip: " + service.getName(),
                        String.format("Weather forecast for %s at %s indicates potentially unsafe conditions: %s. The system is generating a report for management and partners to review.",
                                slot.getDate(), slot.getStartTime(), evalResult.getWarningMessage()),
                        "SUB_ORDER",
                        subOrder.getId(),
                        null
                );
            }
        }

        // 3. Admin (on escalation)
        if (isEscalation) {
            sendNotificationUseCase.execute(
                    null,
                    "WEATHER_ALERT",
                    NotificationChannel.IN_APP,
                    "Weather alert escalated to RED level",
                    String.format("Slot %s of service %s has escalated to RED alert level: %s",
                            slot.getId(), service.getName(), evalResult.getWarningMessage()),
                    "SERVICE_SLOT",
                    slot.getId(),
                    null
            );
        }
    }

    private void sendYellowEarlyWarningNotifications(ServiceJpaEntity service, ServiceSlotJpaEntity slot,
                                                     WeatherRuleEngine.SafetyEvaluationResult evalResult) {
        // 1. Vendor
        if (service.getVendorId() != null) {
            sendNotificationUseCase.execute(
                    service.getVendorId(),
                    "WEATHER_EARLY_WARNING",
                    NotificationChannel.IN_APP,
                    "Early weather warning for service: " + service.getName(),
                    "Marine weather shows cautionary signs (YELLOW). Please prepare contingency plans: "
                            + evalResult.getWarningMessage(),
                    "SERVICE_SLOT",
                    slot.getId(),
                    null
            );
        }

        // 2. Customer
        List<SubOrderJpaEntity> subOrders = subOrderRepository.findBySlotId(slot.getId());
        for (SubOrderJpaEntity subOrder : subOrders) {
            if (subOrder.getStatus() == SubOrderStatus.CONFIRMED || subOrder.getStatus() == SubOrderStatus.PENDING) {
                UUID customerId = resolveCustomerId(subOrder);
                sendNotificationUseCase.execute(
                        customerId,
                        "WEATHER_EARLY_WARNING",
                        NotificationChannel.IN_APP,
                        "Weather update for your upcoming trip: " + service.getName(),
                        "Marine weather conditions require attention (YELLOW): " + evalResult.getWarningMessage()
                                + ". The organizer is continuing to monitor departure conditions.",
                        "SUB_ORDER",
                        subOrder.getId(),
                        null
                );
            }
        }
    }

    private UUID resolveCustomerId(SubOrderJpaEntity subOrder) {
        if (subOrder == null || subOrder.getMasterOrderId() == null || masterOrderRepository == null) {
            return null;
        }
        return masterOrderRepository.findById(subOrder.getMasterOrderId())
                .map(MasterOrderJpaEntity::getCustomerId)
                .orElse(null);
    }

    private void updateEntityMetrics(SafetyRuleEvaluationJpaEntity entity,
                                     WeatherInfoDto.TimeWindowForecast forecast,
                                     WeatherRuleEngine.SafetyEvaluationResult evalResult) {
        entity.setWarningMessage(evalResult.getWarningMessage());
        entity.setEvaluatedAt(OffsetDateTime.now());
        if (forecast != null) {
            entity.setPeakWaveHeightM(forecast.getPeakWaveHeight());
            entity.setPeakWindSpeedKmh(forecast.getPeakWindSpeed());
            entity.setPeakWindGustKmh(forecast.getPeakWindGust());
            entity.setPeakOceanCurrentMs(forecast.getPeakOceanCurrent());
            entity.setMinVisibilityM(forecast.getMinVisibility());
            entity.setSevereWeatherCode(forecast.getSevereWeatherCode());
        }
    }
}
