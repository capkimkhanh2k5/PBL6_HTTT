package com.danasea.backend.modules.settlement.application.usecases;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.settlement.application.dto.SettlementDetailResponse;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementLineItemJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.mappers.SettlementMapper;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementLineItemRepository;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetSettlementDetailUseCase {

    private final JpaSettlementRepository settlementRepository;
    private final JpaSettlementLineItemRepository lineItemRepository;
    private final VendorLookupPort vendorLookupPort;
    private final SettlementMapper mapper;

    @Transactional(readOnly = true)
    public SettlementDetailResponse getAdminSettlementDetail(UUID id) {
        SettlementJpaEntity settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(id);
        return mapper.toDetailResponse(settlement, lineItems);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getVendorSettlementDetail(UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("User is not authenticated"));
        UUID vendorId = vendorLookupPort.findVendorIdByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("No vendor profile is associated with this account"));

        SettlementJpaEntity settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + id));

        if (!Objects.equals(settlement.getVendorId(), vendorId)) {
            log.warn("SECURITY_ALERT: Vendor {} attempted to access settlement {} owned by vendor {}",
                    vendorId, settlement.getId(), settlement.getVendorId());
            throw new UnauthorizedSettlementAccessException(
                    String.format("You cannot view another vendor's settlement (owner: %s)", settlement.getVendorId()));
        }

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(id);
        return mapper.toDetailResponse(settlement, lineItems);
    }
}
