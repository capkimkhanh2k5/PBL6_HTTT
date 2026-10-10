package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetAdminDisputeDetailUseCase {

    private final JpaDisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public DisputeResponse execute(UUID disputeId) {
        if (disputeId == null) {
            throw new IllegalArgumentException("Dispute ID cannot be null");
        }

        DisputeJpaEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        log.info("Admin retrieved dispute dossier for id: {}", disputeId);
        return DisputeResponse.fromEntity(dispute);
    }
}
