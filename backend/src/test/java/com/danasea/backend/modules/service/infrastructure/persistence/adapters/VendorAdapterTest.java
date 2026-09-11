package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorAdapterTest {

    @Mock
    private VendorInternalApi vendorInternalApi;

    @InjectMocks
    private VendorAdapter adapter;

    private UUID userId;
    private UUID vendorId;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserId(userId);
        vendor.setVerificationStatus(VerificationStatus.APPROVED);
    }

    @Test
    @DisplayName("findByUserId delegates to vendorInternalApi")
    void findByUserId_delegates() {
        when(vendorInternalApi.findByUserId(userId)).thenReturn(Optional.of(vendor));

        Optional<Vendor> result = adapter.findByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(vendor, result.get());
    }

    @Test
    @DisplayName("findById delegates to vendorInternalApi")
    void findById_delegates() {
        when(vendorInternalApi.findById(vendorId)).thenReturn(Optional.of(vendor));

        Optional<Vendor> result = adapter.findById(vendorId);

        assertTrue(result.isPresent());
        assertEquals(vendor, result.get());
    }

    @Test
    @DisplayName("isVendorApproved delegates to vendorInternalApi")
    void isVendorApproved_delegates() {
        when(vendorInternalApi.isVendorApproved(vendorId)).thenReturn(true);
        assertTrue(adapter.isVendorApproved(vendorId));

        when(vendorInternalApi.isVendorApproved(vendorId)).thenReturn(false);
        assertFalse(adapter.isVendorApproved(vendorId));
    }
}
