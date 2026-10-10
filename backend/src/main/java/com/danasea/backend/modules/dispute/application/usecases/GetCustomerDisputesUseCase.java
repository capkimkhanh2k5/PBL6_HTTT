package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomerDisputesUseCase {

    private final JpaDisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public Page<DisputeResponse> execute(Pageable pageable) {
        UUID customerId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not authenticated"));
        return execute(pageable, customerId);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> execute(Pageable pageable, UUID customerId) {
        if (pageable == null || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }

        Page<DisputeJpaEntity> pageResult = disputeRepository.findByCustomerId(customerId, pageable);
        return pageResult.map(DisputeResponse::fromEntity);
    }
}
