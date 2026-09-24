package com.danasea.backend.modules.settlement.application.usecases;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.settlement.application.dto.SettlementDetailResponse;
import com.danasea.backend.modules.settlement.application.dto.SettlementResponse;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class GetSettlementsUseCase {

    private final JpaSettlementRepository settlementRepository;
    private final JpaSettlementLineItemRepository lineItemRepository;
    private final VendorLookupPort vendorLookupPort;
    private final SettlementMapper mapper;

    @Autowired
    public GetSettlementsUseCase(
            JpaSettlementRepository settlementRepository,
            @Autowired(required = false) JpaSettlementLineItemRepository lineItemRepository,
            @Autowired(required = false) VendorLookupPort vendorLookupPort,
            SettlementMapper mapper
    ) {
        this.settlementRepository = settlementRepository;
        this.lineItemRepository = lineItemRepository;
        this.vendorLookupPort = vendorLookupPort;
        this.mapper = mapper;
    }

    public GetSettlementsUseCase(
            JpaSettlementRepository settlementRepository,
            JpaSettlementLineItemRepository lineItemRepository,
            SettlementMapper mapper
    ) {
        this(settlementRepository, lineItemRepository, null, mapper);
    }

    public GetSettlementsUseCase(
            JpaSettlementRepository settlementRepository,
            VendorLookupPort vendorLookupPort,
            SettlementMapper mapper
    ) {
        this(settlementRepository, null, vendorLookupPort, mapper);
    }

    @Transactional(readOnly = true)
    public Page<SettlementResponse> executeForAdmin(
            UUID vendorId,
            SettlementStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
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
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("User chưa được xác thực"));
        UUID vendorId = vendorLookupPort != null ? vendorLookupPort.findVendorIdByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("Không tìm thấy hồ sơ Vendor liên kết với tài khoản này"))
                : null;

        return executeForVendor(vendorId, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getAdminSettlementById(UUID settlementId) {
        SettlementJpaEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException("Không tìm thấy kỳ đối soát: " + settlementId));

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository != null
                ? lineItemRepository.findBySettlementId(settlementId)
                : Collections.emptyList();

        return mapper.toDetailResponse(settlement, lineItems);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getVendorSettlementById(UUID vendorId, UUID settlementId) {
        SettlementJpaEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException("Không tìm thấy kỳ đối soát: " + settlementId));

        if (!Objects.equals(settlement.getVendorId(), vendorId)) {
            log.warn("SECURITY_ALERT: Vendor {} cố ý truy cập trái phép kỳ đối soát {} của Vendor {}",
                    vendorId, settlement.getId(), settlement.getVendorId());
            throw new UnauthorizedSettlementAccessException("Vendor does not have permission to access settlement data of another vendor");
        }

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository != null
                ? lineItemRepository.findBySettlementId(settlementId)
                : Collections.emptyList();

        return mapper.toDetailResponse(settlement, lineItems);
    }
}
