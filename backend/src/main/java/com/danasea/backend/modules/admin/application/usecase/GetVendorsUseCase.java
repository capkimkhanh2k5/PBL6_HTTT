package com.danasea.backend.modules.admin.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.admin.presentation.dto.AdminVendorResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVendorsUseCase {

    private final VendorInternalApi vendorInternalApi;

    public Page<AdminVendorResponse> execute(VerificationStatus status, Pageable pageable) {
        Page<Vendor> vendors = vendorInternalApi.getVendors(status, pageable);
        return vendors.map(this::mapToResponse);
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
