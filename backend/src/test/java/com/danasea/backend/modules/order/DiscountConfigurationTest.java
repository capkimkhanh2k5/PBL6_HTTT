package com.danasea.backend.modules.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.danasea.backend.modules.order.application.usecases.AdminDiscountCodeUseCase;
import com.danasea.backend.modules.order.application.usecases.VendorDiscountCodeUseCase;
import com.danasea.backend.modules.order.domain.exceptions.InvalidDiscountException;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.presentation.dtos.CreateDiscountCodeRequest;
import com.danasea.backend.modules.order.presentation.dtos.UpdateDiscountCodeRequest;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;

class DiscountConfigurationTest {
    private final JpaDiscountCodeRepository codes = mock(JpaDiscountCodeRepository.class);
    private final AdminDiscountCodeUseCase admin = new AdminDiscountCodeUseCase(codes);

    @Test
    void vendorCannotEditPlatformFundedVoucherForItsOwnServices() {
        UUID vendor = UUID.randomUUID();
        var code = new DiscountCodeJpaEntity();
        code.setId(UUID.randomUUID());
        code.setVendorId(vendor);
        code.setScope(DiscountScope.VENDOR);
        code.setSponsorType(DiscountSponsorType.PLATFORM);
        when(codes.findByIdForUpdate(code.getId())).thenReturn(Optional.of(code));
        var patch = new UpdateDiscountCodeRequest(new BigDecimal("1000000"), null, null, null, null, null, null, true);
        assertThatThrownBy(() -> new VendorDiscountCodeUseCase(codes).updateVendorDiscountCode(vendor, code.getId(), patch))
                .isInstanceOf(AccessDeniedException.class);
        verify(codes, never()).save(any());
    }

    @Test
    void vendorCannotCreateVoucherForAnotherVendorsService() {
        var services = mock(JpaServiceRepository.class);
        UUID vendor = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        var service = new ServiceJpaEntity();
        service.setVendorId(UUID.randomUUID());
        when(services.findById(serviceId)).thenReturn(Optional.of(service));
        var request = new CreateDiscountCodeRequest("FOREIGN", DiscountScope.VENDOR, DiscountSponsorType.VENDOR,
                vendor, serviceId, DiscountType.FIXED, new BigDecimal("20000"), null, null, null, null, null, null, true);
        assertInvalid("DISCOUNT_SERVICE_INVALID", () -> new VendorDiscountCodeUseCase(codes, services)
                .createVendorDiscountCode(vendor, request));
    }

    @Test
    void percentageGreaterThanOneHundredIsRejected() {
        assertInvalid("DISCOUNT_PERCENTAGE_INVALID", () -> admin.createDiscountCode(
                platformRequest(DiscountType.PERCENTAGE, "101", null, null)));
    }

    @Test
    void invalidDateWindowIsRejected() {
        OffsetDateTime now = OffsetDateTime.now();
        assertInvalid("DISCOUNT_DATE_RANGE_INVALID", () -> admin.createDiscountCode(
                platformRequest(DiscountType.FIXED, "20000", now, now.minusDays(1))));
    }

    @Test
    void patchValidatesMergedDatesAndPercentageType() {
        var code = new DiscountCodeJpaEntity();
        code.setId(UUID.randomUUID());
        code.setScope(DiscountScope.PLATFORM);
        code.setDiscountType(DiscountType.PERCENTAGE);
        code.setDiscountValue(new BigDecimal("10"));
        code.setValidFrom(OffsetDateTime.now());
        code.setValidTo(OffsetDateTime.now().plusDays(2));
        when(codes.findByIdForUpdate(code.getId())).thenReturn(Optional.of(code));
        assertInvalid("DISCOUNT_DATE_RANGE_INVALID", () -> admin.updateDiscountCode(code.getId(),
                new UpdateDiscountCodeRequest(null, null, null, null, null, null, code.getValidFrom().minusDays(1), null)));
        code.setValidTo(code.getValidFrom().plusDays(2));
        assertInvalid("DISCOUNT_PERCENTAGE_INVALID", () -> admin.updateDiscountCode(code.getId(),
                new UpdateDiscountCodeRequest(new BigDecimal("150"), null, null, null, null, null, null, null)));
    }

    @Test
    void vendorFundingCannotChargeItemsAcrossPlatformScope() {
        var request = new CreateDiscountCodeRequest("INVALIDSPONSOR", DiscountScope.PLATFORM, DiscountSponsorType.VENDOR,
                null, null, DiscountType.FIXED, new BigDecimal("20000"), null, null, null, null, null, null, true);
        assertInvalid("DISCOUNT_SPONSOR_SCOPE_INVALID", () -> admin.createDiscountCode(request));
    }

    private CreateDiscountCodeRequest platformRequest(DiscountType type, String value,
            OffsetDateTime from, OffsetDateTime to) {
        return new CreateDiscountCodeRequest("CONFIG", DiscountScope.PLATFORM, DiscountSponsorType.PLATFORM,
                null, null, type, new BigDecimal(value), null, null, null, null, from, to, true);
    }

    private void assertInvalid(String errorCode, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(InvalidDiscountException.class)
                .extracting(ex -> ((InvalidDiscountException) ex).getErrorCode()).isEqualTo(errorCode);
        verify(codes, never()).save(any());
    }
}
