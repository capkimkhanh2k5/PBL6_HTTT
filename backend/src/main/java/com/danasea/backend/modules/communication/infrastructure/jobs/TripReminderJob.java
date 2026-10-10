package com.danasea.backend.modules.communication.infrastructure.jobs;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.ports.PushNotificationPort;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("tripReminderJob")
@RequiredArgsConstructor
public class TripReminderJob {
    private final SendNotificationUseCase notifications;
    private final JpaSubOrderRepository subOrders;
    private final JpaMasterOrderRepository masterOrders;
    private final JpaServiceSlotRepository slots;
    private final PushNotificationPort push;

    @Scheduled(cron = "${app.scheduler.trip-reminder.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void executeTripReminders() {
        scanAndSendReminders();
    }

    @Transactional
    public void scanAndSendReminders() {
        LocalDateTime now = LocalDateTime.now(Booking.VIETNAM_ZONE);
        LocalDateTime horizon = now.plusHours(24);
        int page = 0;
        boolean hasNext;
        do {
            var batch = slots.findDeparturesInWindow(now.toLocalDate(), now.toLocalTime(),
                    horizon.toLocalDate(), horizon.toLocalTime(), PageRequest.of(page++, 200));
            hasNext = batch.hasNext();
            for (var slot : batch) {
                for (var subOrder : subOrders.findBySlotId(slot.getId())) {
                    if (subOrder.getStatus() != SubOrderStatus.CONFIRMED) {
                        continue;
                    }
                    var order = masterOrders.findById(subOrder.getMasterOrderId()).orElse(null);
                    if (order == null || order.getCustomerId() == null || order.getPaymentStatus() != PaymentOrderStatus.PAID
                            || (order.getStatus() != MasterOrderStatus.PAID
                                && order.getStatus() != MasterOrderStatus.PARTIALLY_COMPLETED)) {
                        continue;
                    }
                    notifications.executeOnce(new NotificationCommand(order.getCustomerId(), "TRIP_REMINDER",
                            NotificationChannel.IN_APP, LocalizedMessageRef.of("notification.trip.reminder.title"),
                            LocalizedMessageRef.of("notification.trip.reminder.body", slot.getStartTime(), slot.getDate()),
                            "SERVICE_SLOT", slot.getId(), null),
                            "trip:" + slot.getId() + ":customer:" + order.getCustomerId()).ifPresent(notification -> {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    push.sendPush(notification.getUserId(), notification.getTitle(), notification.getBody(),
                                            Map.of("type", "TRIP_REMINDER", "slotId", slot.getId().toString()));
                                } catch (Exception failure) {
                                    log.warn("Push delivery failed for notification {}", notification.getId(), failure);
                                }
                            }
                        });
                    });
                }
            }
        } while (hasNext);
    }
}
