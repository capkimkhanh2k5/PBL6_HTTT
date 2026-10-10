package com.danasea.backend.modules.order.application.services;

import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRescheduleHistoryRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRescheduleProposalRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescheduleNotificationService {
    private final JpaSubOrderRescheduleHistoryRepository histories;
    private final JpaSubOrderRescheduleProposalRepository proposals;
    private final JpaSubOrderRepository subOrders;
    private final JpaMasterOrderRepository orders;
    private final JpaServiceSlotRepository slots;
    private final VendorInternalApi vendors;
    private final SendNotificationUseCase notifications;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void proposal(UUID id) {
        var proposal = proposals.findByIdForUpdate(id).orElseThrow();
        if (proposal.getNotificationSentAt() != null) return;
        if ("PENDING".equals(proposal.getStatus())
                && proposal.getExpiresAt().isAfter(OffsetDateTime.now())) {
            var sub = subOrders.findById(proposal.getSubOrderId()).orElseThrow();
            var order = orders.findById(sub.getMasterOrderId()).orElseThrow();
            var slot = slots.findById(proposal.getProposedSlotId()).orElseThrow();
            send(
                    order.getCustomerId(),
                    "RESCHEDULE_PROPOSAL",
                    sub.getId(),
                    "proposal",
                    new Object[] {
                        slot.getDate(),
                        slot.getStartTime(),
                        proposal.getReason(),
                        proposal.getExpiresAt()
                    },
                    "reschedule-proposal:" + id);
        }
        proposal.setNotificationSentAt(OffsetDateTime.now());
        proposals.save(proposal);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completed(UUID id) {
        var history = histories.findByIdForUpdate(id).orElseThrow();
        if (history.getNotificationSentAt() != null) return;
        var sub = subOrders.findById(history.getSubOrderId()).orElseThrow();
        var order = orders.findById(sub.getMasterOrderId()).orElseThrow();
        Object[] args = {
            history.getSubOrderId(), history.getToBookingDate(), history.getToBookingTime()
        };
        send(
                order.getCustomerId(),
                "RESCHEDULE_SUCCESS",
                sub.getId(),
                "completed",
                args,
                "reschedule:" + id + ":customer");
        var vendor = vendors.findById(sub.getVendorId()).orElseThrow();
        send(
                vendor.getUserId(),
                "RESCHEDULE_SUCCESS",
                sub.getId(),
                "completed",
                args,
                "reschedule:" + id + ":vendor");
        history.setNotificationSentAt(OffsetDateTime.now());
        histories.save(history);
    }

    private void send(
            UUID userId,
            String type,
            UUID subId,
            String key,
            Object[] args,
            String idempotencyKey) {
        notifications.executeOnce(
                new NotificationCommand(
                        userId,
                        type,
                        NotificationChannel.IN_APP,
                        LocalizedMessageRef.of("notification.reschedule." + key + ".title"),
                        LocalizedMessageRef.of("notification.reschedule." + key + ".body", args),
                        "SUB_ORDER",
                        subId,
                        null),
                idempotencyKey);
    }
}
