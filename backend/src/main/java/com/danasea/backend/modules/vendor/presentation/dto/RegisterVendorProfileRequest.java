package com.danasea.backend.modules.vendor.presentation.dto;

import com.danasea.backend.modules.vendor.application.usecase.RegisterVendorProfileCommand;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record RegisterVendorProfileRequest(
        @NotBlank(message = "Business name is required")
        @JsonAlias("business_name")
        String businessName,

        @NotBlank(message = "Tax code is required")
        @JsonAlias("tax_code")
        String taxCode,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "Bank account number is required")
        @JsonAlias("bank_account_number")
        String bankAccountNumber,

        @NotBlank(message = "Bank name is required")
        @JsonAlias("bank_name")
        String bankName,

        @NotBlank(message = "Bank account holder is required")
        @JsonAlias("bank_account_holder")
        String bankAccountHolder
) {
    public RegisterVendorProfileCommand toCommand() {
        return new RegisterVendorProfileCommand(
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
