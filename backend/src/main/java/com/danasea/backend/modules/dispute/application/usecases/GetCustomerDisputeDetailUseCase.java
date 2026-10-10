package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomerDisputeDetailUseCase {

    private final JpaDisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public DisputeResponse execute(UUID disputeId) {
        UUID customerId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not authenticated"));
        return execute(disputeId, customerId);
    }

    @Transactional(readOnly = true)
    public DisputeResponse execute(UUID disputeId, UUID customerId) {
        if (disputeId == null) {
            throw new IllegalArgumentException("Dispute ID cannot be null");
        }

        DisputeJpaEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        if (!dispute.getCustomerId().equals(customerId)) {
            log.warn("Customer {} attempted unauthorized access to dispute {}", customerId, disputeId);
            throw new UnauthorizedDisputeAccessException("User does not have permission to access this dispute");
        }

        return DisputeResponse.fromEntity(dispute);
    }
}
