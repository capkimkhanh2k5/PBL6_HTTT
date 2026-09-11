package com.danasea.backend.modules.vendor.application.usecase;

import lombok.Builder;

@Builder
public record RegisterVendorProfileCommand(
    String businessName,
    String taxCode,
    String address,
    String bankAccountNumber,
    String bankName,
    String bankAccountHolder
) {
    public String getBusinessName() {
        return businessName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public String getAddress() {
        return address;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankAccountHolder() {
        return bankAccountHolder;
    }
}
