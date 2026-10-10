package com.danasea.backend.modules.order.infrastructure.listeners;

import com.danasea.backend.modules.order.application.services.RescheduleNotificationService;
import com.danasea.backend.modules.order.domain.events.RescheduleProposalCreatedEvent;
import com.danasea.backend.modules.order.domain.events.SubOrderRescheduledEvent;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRescheduleHistoryRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRescheduleProposalRepository;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubOrderRescheduledEventListener {
    private final RescheduleNotificationService notifications;
    private final JpaSubOrderRescheduleHistoryRepository histories;
    private final JpaSubOrderRescheduleProposalRepository proposals;
    private final CheckAdvanceBookingSafetyUseCase safety;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void completed(SubOrderRescheduledEvent event) {
        try {
            notifications.completed(event.historyId());
        } catch (Exception ex) {
            log.warn("Reschedule notification queued for retry: {}", event.historyId(), ex);
        }
        try {
            safety.checkBySlotId(event.toSlotId());
        } catch (Exception ex) {
            log.warn("Reschedule weather check failed for slot {}", event.toSlotId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void proposed(RescheduleProposalCreatedEvent event) {
        try {
            notifications.proposal(event.proposalId());
        } catch (Exception ex) {
            log.warn("Proposal notification queued for retry: {}", event.proposalId(), ex);
        }
    }

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(fixedDelayString = "${app.scheduler.reschedule-notification-retry-ms:60000}")
    public void retry() {
        if (!schedulerEnabled) return;
        for (var history : histories.findTop50ByNotificationSentAtIsNullOrderByCreatedAtAsc()) {
            try {
                notifications.completed(history.getId());
            } catch (Exception ex) {
                log.warn("Reschedule notification retry failed for {}", history.getId(), ex);
            }
        }
        for (var proposal : proposals.findTop50ByNotificationSentAtIsNullOrderByCreatedAtAsc()) {
            try {
                notifications.proposal(proposal.getId());
            } catch (Exception ex) {
                log.warn("Proposal notification retry failed for {}", proposal.getId(), ex);
            }
        }
    }
}
