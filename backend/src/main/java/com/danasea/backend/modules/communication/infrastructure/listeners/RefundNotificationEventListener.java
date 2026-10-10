package com.danasea.backend.modules.communication.infrastructure.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.order.domain.events.RefundCompletedEvent;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;

import lombok.RequiredArgsConstructor;

@Component("refundNotificationEventListener")
@RequiredArgsConstructor
public class RefundNotificationEventListener {
    private final SendNotificationUseCase notifications;

    @EventListener
    @Transactional
    public void handleRefundCompleted(RefundCompletedEvent event) {
        if (event == null || event.refundId() == null || event.customerId() == null
                || event.status() != RefundStatus.PROCESSED) {
            return;
        }
        notifications.executeOnce(new NotificationCommand(event.customerId(), "REFUND_COMPLETED",
                NotificationChannel.IN_APP, LocalizedMessageRef.of("notification.refund.completed.title"),
                LocalizedMessageRef.of("notification.refund.completed.body", event.amount()),
                "REFUND", event.refundId(), null), "refund:" + event.refundId() + ":customer:" + event.customerId());
    }
}
