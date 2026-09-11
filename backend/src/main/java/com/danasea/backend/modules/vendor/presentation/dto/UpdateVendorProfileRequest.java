package com.danasea.backend.modules.vendor.presentation.dto;

import com.danasea.backend.modules.vendor.application.usecase.UpdateVendorProfileCommand;
import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateVendorProfileRequest(
        @JsonAlias("business_name")
        String businessName,

        @JsonAlias("tax_code")
        String taxCode,

        String address,

        @JsonAlias("bank_account_number")
        String bankAccountNumber,

        @JsonAlias("bank_name")
        String bankName,

        @JsonAlias("bank_account_holder")
        String bankAccountHolder
) {
    public UpdateVendorProfileCommand toCommand() {
        return new UpdateVendorProfileCommand(
                businessName,
                taxCode,
                address,
                bankAccountNumber,
                bankName,
                bankAccountHolder
        );
    }

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
