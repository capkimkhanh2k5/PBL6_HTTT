package com.danasea.backend.modules.admin.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.presentation.dto.AdminVendorResponse;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApproveVendorUseCaseTest {

    @Mock
    private VendorInternalApi vendorInternalApi;

    @Mock
    private AccountInternalApi accountInternalApi;

    @Mock
    private AuditLogInternalApi auditLogInternalApi;

    private ApproveVendorUseCase approveVendorUseCase;

    private UUID vendorId;
    private UUID adminId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        approveVendorUseCase = new ApproveVendorUseCase(vendorInternalApi, accountInternalApi, auditLogInternalApi);
        vendorId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void execute_WhenVendorNotFound_ThrowsVendorNotFoundException() {
        when(vendorInternalApi.findById(vendorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approveVendorUseCase.execute(vendorId, adminId))
                .isInstanceOf(VendorNotFoundException.class)
                .hasMessageContaining("Vendor not found with id");
    }

    @Test
    void execute_WhenVendorNotPending_ThrowsIllegalArgumentException() {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setVerificationStatus(VerificationStatus.APPROVED);

        when(vendorInternalApi.findById(vendorId)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> approveVendorUseCase.execute(vendorId, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vendor is not in PENDING status");

        verify(vendorInternalApi, never()).saveVendor(any());
        verify(accountInternalApi, never()).saveUser(any());
    }

    @Test
    void execute_WhenVendorIsPending_Success() {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserId(userId);
        vendor.setVerificationStatus(VerificationStatus.PENDING);

        when(vendorInternalApi.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorInternalApi.saveVendor(any(Vendor.class))).thenAnswer(i -> i.getArgument(0));

        User user = new User();
        user.setId(userId);
        user.setRole(Role.CUSTOMER);
        
        when(accountInternalApi.findUserById(userId)).thenReturn(Optional.of(user));

        AdminVendorResponse response = approveVendorUseCase.execute(vendorId, adminId);

        // Verify Vendor updated
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.getVerifiedBy()).isEqualTo(adminId);
        assertThat(vendor.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(vendor.getVerifiedBy()).isEqualTo(adminId);
        assertThat(vendor.getVerifiedAt()).isNotNull();
        verify(vendorInternalApi).saveVendor(vendor);

        // Verify User updated
        assertThat(user.getRole()).isEqualTo(Role.VENDOR);
        verify(accountInternalApi).saveUser(user);

        // Verify Tokens Revoked
        verify(accountInternalApi).revokeAllRefreshTokensByUserId(userId);

        // Verify Audit Log
        verify(auditLogInternalApi).recordAuditLog(
                eq(adminId),
                eq("VENDOR_APPROVAL"),
                eq("VENDOR"),
                eq(vendorId),
                anyString()
        );
    }
}
