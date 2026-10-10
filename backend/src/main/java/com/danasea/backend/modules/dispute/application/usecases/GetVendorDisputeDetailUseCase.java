package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetVendorDisputeDetailUseCase {

    private final JpaDisputeRepository disputeRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional(readOnly = true)
    public DisputeResponse execute(UUID disputeId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not authenticated"));
        return execute(disputeId, currentUserId);
    }

    @Transactional(readOnly = true)
    public DisputeResponse execute(UUID disputeId, UUID currentUserId) {
        if (disputeId == null) {
            throw new IllegalArgumentException("Dispute ID cannot be null");
        }

        Vendor vendor = vendorInternalApi.findByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not a vendor"));

        DisputeJpaEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        SubOrderJpaEntity subOrder = subOrderRepository.findById(dispute.getSubOrderId())
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("Sub-order associated with dispute not found"));

        if (!vendor.getId().equals(subOrder.getVendorId())) {
            log.warn("Vendor {} attempted unauthorized access to dispute {} (subOrderId: {})",
                    vendor.getId(), disputeId, dispute.getSubOrderId());
            throw new UnauthorizedDisputeAccessException("User does not have permission to access this dispute");
        }

        return DisputeResponse.fromEntity(dispute);
    }
}
