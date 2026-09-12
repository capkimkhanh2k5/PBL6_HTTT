package com.danasea.backend.modules.admin.application.usecase;

import java.util.UUID;
import org.springframework.stereotype.Service;

import java.util.List;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.application.usecase.GetVendorDocumentsUseCase;
import com.danasea.backend.modules.vendor.domain.models.VendorDocument;
import com.danasea.backend.modules.vendor.presentation.dto.VendorDocumentResponse;
import com.danasea.backend.modules.admin.presentation.dto.AdminVendorResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVendorDetailUseCase {

    private final VendorInternalApi vendorInternalApi;
    private final GetVendorDocumentsUseCase getVendorDocumentsUseCase;

    public AdminVendorResponse execute(UUID vendorId) {
        Vendor vendor = vendorInternalApi.findById(vendorId)
                .orElseThrow(() -> new VendorNotFoundException("Vendor not found with id: " + vendorId));
        
        List<VendorDocument> documents = getVendorDocumentsUseCase.execute(vendor.getUserId());
        List<VendorDocumentResponse> documentResponses = documents.stream()
                .map(VendorDocumentResponse::fromDomain)
                .toList();
                
        return mapToResponse(vendor, documentResponses);
    }

    private AdminVendorResponse mapToResponse(Vendor vendor, List<VendorDocumentResponse> documents) {
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
                .documents(documents)
                .build();
    }
}
