package com.danasea.backend.modules.settlement.application.usecases;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.settlement.application.dto.SettlementDetailResponse;
import com.danasea.backend.modules.settlement.application.dto.SettlementResponse;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
import com.danasea.backend.modules.settlement.domain.exceptions.InvalidSettlementPeriodException;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementLineItemJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.mappers.SettlementMapper;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementLineItemRepository;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class GetSettlementsUseCase {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "createdAt", "periodStart", "periodEnd", "grossAmount",
            "commissionAmount", "netPayableAmount", "status", "vendorId");

    private final JpaSettlementRepository settlementRepository;
    private final JpaSettlementLineItemRepository lineItemRepository;
    private final VendorLookupPort vendorLookupPort;
    private final SettlementMapper mapper;

    @Autowired
    public GetSettlementsUseCase(
            JpaSettlementRepository settlementRepository,
            JpaSettlementLineItemRepository lineItemRepository,
            VendorLookupPort vendorLookupPort,
            SettlementMapper mapper
    ) {
        this.settlementRepository = settlementRepository;
        this.lineItemRepository = lineItemRepository;
        this.vendorLookupPort = vendorLookupPort;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<SettlementResponse> executeForAdmin(
            UUID vendorId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        validateQuery(from, to, pageable);
        return settlementRepository.findFiltered(vendorId, status, from, to, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SettlementResponse> getAdminSettlements(
            UUID vendorId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        return executeForAdmin(vendorId, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SettlementResponse> executeForVendor(
            UUID vendorId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        validateQuery(from, to, pageable);
        return settlementRepository.findFiltered(vendorId, status, from, to, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SettlementResponse> getVendorSettlements(
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("User is not authenticated."));
        UUID vendorId = vendorLookupPort.findVendorIdByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedSettlementAccessException(
                        "No vendor profile is linked to the current account."));

        return executeForVendor(vendorId, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getAdminSettlementById(UUID settlementId) {
        SettlementJpaEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + settlementId));

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(settlementId);

        return mapper.toDetailResponse(settlement, lineItems);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getVendorSettlementById(UUID vendorId, UUID settlementId) {
        SettlementJpaEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found: " + settlementId));

        if (!Objects.equals(settlement.getVendorId(), vendorId)) {
            log.warn("SECURITY_ALERT: Vendor {} attempted to access settlement {} owned by vendor {}",
                    vendorId, settlement.getId(), settlement.getVendorId());
            throw new UnauthorizedSettlementAccessException("Vendor does not have permission to access settlement data of another vendor");
        }

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(settlementId);

        return mapper.toDetailResponse(settlement, lineItems);
    }

    private void validateQuery(LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidSettlementPeriodException("The from date must not be after the to date.");
        }
        if (pageable == null || pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORTS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported settlement sort property: " + order.getProperty());
            }
        });
    }
}
