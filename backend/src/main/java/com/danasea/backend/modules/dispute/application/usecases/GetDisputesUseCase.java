package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDisputesUseCase {

    private final JpaDisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public Page<DisputeResponse> execute(DisputeStatus status, DisputeReason reason, Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            if (!java.util.Set.of("createdAt", "updatedAt", "status", "reason", "resolvedAt")
                    .contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported sort field: " + order.getProperty());
            }
        }
        Page<DisputeJpaEntity> pageResult;
        if (status != null && reason != null) {
            pageResult = disputeRepository.findByStatusAndReason(status, reason, pageable);
        } else if (status != null) {
            pageResult = disputeRepository.findByStatus(status, pageable);
        } else if (reason != null) {
            pageResult = disputeRepository.findByReason(reason, pageable);
        } else {
            pageResult = disputeRepository.findAll(pageable);
        }
        return pageResult.map(this::toResponse);
    }

    private DisputeResponse toResponse(DisputeJpaEntity entity) {
        return new DisputeResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getSubOrderId(),
                entity.getCustomerId(),
                entity.getReason(),
                entity.getDescription(),
                entity.getEvidenceUrls(),
                entity.getStatus(),
                entity.getRefundPercentage(),
                entity.getAdminNote(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
