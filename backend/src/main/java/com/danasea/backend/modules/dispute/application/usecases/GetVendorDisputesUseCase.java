package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetVendorDisputesUseCase {

    private final JpaDisputeRepository disputeRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional(readOnly = true)
    public Page<DisputeResponse> execute(DisputeStatus status, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not authenticated"));
        return execute(status, pageable, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> execute(DisputeStatus status, Pageable pageable, UUID currentUserId) {
        if (pageable == null || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }

        Vendor vendor = vendorInternalApi.findByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not a vendor"));

        List<SubOrderJpaEntity> subOrders = subOrderRepository.findByVendorId(vendor.getId());
        List<UUID> subOrderIds = subOrders.stream()
                .map(SubOrderJpaEntity::getId)
                .toList();

        if (subOrderIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<DisputeJpaEntity> pageResult;
        if (status != null) {
            pageResult = disputeRepository.findBySubOrderIdInAndStatus(subOrderIds, status, pageable);
        } else {
            pageResult = disputeRepository.findBySubOrderIdIn(subOrderIds, pageable);
        }

        return pageResult.map(DisputeResponse::fromEntity);
    }
}
