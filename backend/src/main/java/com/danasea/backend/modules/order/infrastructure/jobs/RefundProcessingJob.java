package com.danasea.backend.modules.order.infrastructure.jobs;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.application.RefundProcessingService;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundProcessingJob {

    private final JpaRefundRepository refundRepository;
    private final RefundProcessingService refundProcessingService;

    @Scheduled(fixedDelayString = "${app.scheduler.refund-processing-delay:30s}")
    public void processPendingRefunds() {
        List<RefundJpaEntity> pendingRefunds = refundRepository
                .findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                        RefundStatus.PENDING, RefundProcessingService.MAX_RETRIES);

        if (pendingRefunds.isEmpty()) {
            return;
        }

        log.info("Found {} pending refund(s) to process", pendingRefunds.size());
        for (RefundJpaEntity refund : pendingRefunds) {
            if (refund.getNextAttemptAt() != null && refund.getNextAttemptAt().isAfter(OffsetDateTime.now())) {
                continue;
            }
            try {
                refundProcessingService.processRefund(refund.getId());
            } catch (Exception ex) {
                log.error("Unhandled error in RefundProcessingJob for refund {}: {}",
                        refund.getId(), ex.getMessage(), ex);
            }
        }
    }
}
