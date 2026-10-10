package com.danasea.backend.modules.order.application.usecases;

import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.DiscountCodeRules;
import com.danasea.backend.modules.order.domain.exceptions.InvalidDiscountException;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.presentation.dtos.CreateDiscountCodeRequest;
import com.danasea.backend.modules.order.presentation.dtos.DiscountCodeResponse;
import com.danasea.backend.modules.order.presentation.dtos.UpdateDiscountCodeRequest;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;

@Service
public class VendorDiscountCodeUseCase {

    private final JpaDiscountCodeRepository discountCodeRepository;
    private final JpaServiceRepository serviceRepository;

    @Autowired
    public VendorDiscountCodeUseCase(JpaDiscountCodeRepository discountCodeRepository, JpaServiceRepository serviceRepository) {
        this.discountCodeRepository = discountCodeRepository;
        this.serviceRepository = serviceRepository;
    }

    public VendorDiscountCodeUseCase(JpaDiscountCodeRepository discountCodeRepository) {
        this(discountCodeRepository, null);
    }

    @Transactional(readOnly = true)
    public Page<DiscountCodeResponse> getVendorDiscountCodes(UUID vendorId, Pageable pageable) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID is required.");
        }
        return discountCodeRepository.findByVendorId(vendorId, pageable).map(DiscountCodeResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DiscountCodeResponse getVendorDiscountCodeById(UUID vendorId, UUID id) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID is required.");
        }
        DiscountCodeJpaEntity entity = discountCodeRepository.findById(id)
                .orElseThrow(() -> new InvalidDiscountException("DISCOUNT_NOT_FOUND", "Discount code was not found: " + id));

        if (!vendorId.equals(entity.getVendorId())) {
            throw new AccessDeniedException("Vendor does not own this discount code.");
        }

        return DiscountCodeResponse.fromEntity(entity);
    }

    @Transactional
    public DiscountCodeResponse createVendorDiscountCode(UUID vendorId, CreateDiscountCodeRequest request) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID is required.");
        }

        String cleanCode = request.code().trim().toUpperCase(Locale.ROOT);
        if (discountCodeRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new InvalidDiscountException("CODE_ALREADY_EXISTS", "Discount code already exists: " + cleanCode);
        }

        DiscountCodeJpaEntity entity = new DiscountCodeJpaEntity();
        entity.setCode(cleanCode);
        entity.setScope(DiscountScope.VENDOR);
        entity.setSponsorType(DiscountSponsorType.VENDOR);
        entity.setVendorId(vendorId);
        entity.setServiceId(request.serviceId());
        entity.setDiscountType(request.discountType());
        entity.setDiscountValue(request.discountValue());
        entity.setMinOrderAmount(request.minOrderAmount());
        entity.setMaxDiscountAmount(request.maxDiscountAmount());
        entity.setMaxUses(request.maxUses());
        entity.setUsedCount(0);
        entity.setMaxUsesPerUser(request.maxUsesPerUser());
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setIsActive(request.isActive() != null ? request.isActive() : true);

        validateConfiguration(entity);
        DiscountCodeJpaEntity saved = discountCodeRepository.save(entity);
        return DiscountCodeResponse.fromEntity(saved);
    }

    @Transactional
    public DiscountCodeResponse updateVendorDiscountCode(UUID vendorId, UUID id, UpdateDiscountCodeRequest request) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID is required.");
        }

        DiscountCodeJpaEntity entity = discountCodeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new InvalidDiscountException("DISCOUNT_NOT_FOUND", "Discount code was not found: " + id));

        if (!vendorId.equals(entity.getVendorId()) || entity.getScope() != DiscountScope.VENDOR
                || entity.getSponsorType() != DiscountSponsorType.VENDOR) {
            throw new AccessDeniedException("Vendor cannot edit this discount code.");
        }

        if (request.discountValue() != null) {
            entity.setDiscountValue(request.discountValue());
        }
        if (request.minOrderAmount() != null) {
            entity.setMinOrderAmount(request.minOrderAmount());
        }
        if (request.maxDiscountAmount() != null) {
            entity.setMaxDiscountAmount(request.maxDiscountAmount());
        }
        if (request.maxUses() != null) {
            if (entity.getUsedCount() != null && request.maxUses() < entity.getUsedCount()) {
                throw new InvalidDiscountException("MAX_USES_LESS_THAN_USED", "Maximum uses cannot be less than reserved or consumed uses.");
            }
            entity.setMaxUses(request.maxUses());
        }
        if (request.maxUsesPerUser() != null) {
            entity.setMaxUsesPerUser(request.maxUsesPerUser());
        }
        if (request.validFrom() != null) {
            entity.setValidFrom(request.validFrom());
        }
        if (request.validTo() != null) {
            entity.setValidTo(request.validTo());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }

        validateConfiguration(entity);
        DiscountCodeJpaEntity saved = discountCodeRepository.save(entity);
        return DiscountCodeResponse.fromEntity(saved);
    }
    private void validateConfiguration(DiscountCodeJpaEntity entity) {
        DiscountCodeRules.validate(entity);
        if (entity.getServiceId() != null) {
            if (serviceRepository == null) {
                throw new IllegalStateException("Service ownership lookup is not configured.");
            }
            var service = serviceRepository.findById(entity.getServiceId())
                    .orElseThrow(() -> new InvalidDiscountException("DISCOUNT_SERVICE_INVALID", "Discount service was not found."));
            if (entity.getScope() == DiscountScope.VENDOR && !entity.getVendorId().equals(service.getVendorId())) {
                throw new InvalidDiscountException("DISCOUNT_SERVICE_INVALID", "Discount service does not belong to the vendor.");
            }
        }
    }

}
