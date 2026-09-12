package com.danasea.backend.modules.admin.application.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.admin.presentation.dto.AdminVendorResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApproveVendorUseCase {

    private final VendorInternalApi vendorInternalApi;
    private final AccountInternalApi accountInternalApi;
    private final AuditLogInternalApi auditLogInternalApi;

    @Transactional
    public AdminVendorResponse execute(UUID vendorId, UUID adminId) {
        Vendor vendor = vendorInternalApi.findById(vendorId)
                .orElseThrow(() -> new VendorNotFoundException("Vendor not found with id: " + vendorId));

        if (vendor.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new IllegalArgumentException("Vendor is already approved");
        }

        vendor.setVerificationStatus(VerificationStatus.APPROVED);
        vendor.setVerifiedBy(adminId);
        vendor.setVerifiedAt(OffsetDateTime.now());

        Vendor savedVendor = vendorInternalApi.saveVendor(vendor);

        User user = accountInternalApi.findUserById(savedVendor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + savedVendor.getUserId()));

        user.setRole(Role.VENDOR);
        accountInternalApi.saveUser(user);

        // Revoke all refresh tokens for the user to force them to login again with new role
        accountInternalApi.revokeAllRefreshTokensByUserId(savedVendor.getUserId());

        // Log audit
        auditLogInternalApi.recordAuditLog(
                adminId,
                "VENDOR_APPROVAL",
                "VENDOR",
                vendorId,
                "Vendor approved by admin"
        );

        return mapToResponse(savedVendor);
    }

    private AdminVendorResponse mapToResponse(Vendor vendor) {
        return AdminVendorResponse.builder()
                .id(vendor.getId())
                .userId(vendor.getUserId())
                .businessName(vendor.getBusinessName())
                .taxCode(vendor.getTaxCode())
                .address(vendor.getAddress())
                .bankAccountNumber(vendor.getBankAccountNumber())
                .bankName(vendor.getBankName())
                .bankAccountHolder(vendor.getBankAccountHolder())
                .verificationStatus(vendor.getVerificationStatus())
                .verifiedBy(vendor.getVerifiedBy())
                .verifiedAt(vendor.getVerifiedAt())
                .ratingAvg(vendor.getRatingAvg())
                .ratingCount(vendor.getRatingCount())
                .badgeTier(vendor.getBadgeTier())
                .createdAt(vendor.getCreatedAt())
                .updatedAt(vendor.getUpdatedAt())
                .build();
    }
}
