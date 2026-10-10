package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeAlreadyResolvedException;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.SubmitVendorResponseRequest;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitVendorDisputeResponseUseCase {

    private final JpaDisputeRepository disputeRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional
    public DisputeResponse execute(UUID disputeId, SubmitVendorResponseRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not authenticated"));
        return execute(disputeId, request, currentUserId);
    }

    @Transactional
    public DisputeResponse execute(UUID disputeId, SubmitVendorResponseRequest request, UUID currentUserId) {
        if (disputeId == null) {
            throw new IllegalArgumentException("Dispute ID cannot be null");
        }
        if (request == null || request.response() == null || request.response().isBlank()) {
            throw new IllegalArgumentException("Response content is required");
        }

        Vendor vendor = vendorInternalApi.findByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("User is not a vendor"));

        DisputeJpaEntity dispute = disputeRepository.findByIdForUpdate(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        SubOrderJpaEntity subOrder = subOrderRepository.findById(dispute.getSubOrderId())
                .orElseThrow(() -> new UnauthorizedDisputeAccessException("Sub-order associated with dispute not found"));

        if (!vendor.getId().equals(subOrder.getVendorId())) {
            log.warn("Vendor {} attempted unauthorized response to dispute {} (subOrderId: {})",
                    vendor.getId(), disputeId, dispute.getSubOrderId());
            throw new UnauthorizedDisputeAccessException("User does not have permission to respond to this dispute");
        }

        if (dispute.getStatus() != null && dispute.getStatus().isResolved()) {
            throw new DisputeAlreadyResolvedException(
                    "Cannot submit response for an already resolved dispute: " + disputeId
            );
        }

        dispute.setVendorResponse(request.response());
        dispute.setVendorEvidenceUrls(request.evidenceUrls() != null ? request.evidenceUrls() : List.of());
        dispute.setVendorRespondedAt(OffsetDateTime.now());

        if (dispute.getStatus() == DisputeStatus.OPEN) {
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        }

        DisputeJpaEntity saved = disputeRepository.save(dispute);
        log.info("Vendor {} submitted response to dispute {}, status updated to {}",
                vendor.getId(), disputeId, saved.getStatus());

        return DisputeResponse.fromEntity(saved);
    }
}
