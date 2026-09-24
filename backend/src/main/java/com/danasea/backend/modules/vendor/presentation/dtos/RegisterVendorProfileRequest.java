package com.danasea.backend.modules.vendor.presentation.dtos;

import com.danasea.backend.modules.vendor.application.usecases.RegisterVendorProfileCommand;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record RegisterVendorProfileRequest(
        @NotBlank(message = "{validation.vendor.business_name.required}")
        @JsonAlias("business_name")
        String businessName,

        @NotBlank(message = "{validation.vendor.tax_code.required}")
        @JsonAlias("tax_code")
        String taxCode,

        @NotBlank(message = "{validation.vendor.address.required}")
        String address,

        @NotBlank(message = "{validation.vendor.bank_account.required}")
        @JsonAlias("bank_account_number")
        String bankAccountNumber,

        @NotBlank(message = "{validation.vendor.bank_name.required}")
        @JsonAlias("bank_name")
        String bankName,

        @NotBlank(message = "{validation.vendor.bank_holder.required}")
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
