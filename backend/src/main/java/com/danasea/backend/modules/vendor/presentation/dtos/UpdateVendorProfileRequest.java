package com.danasea.backend.modules.vendor.presentation.dtos;

import com.danasea.backend.modules.vendor.application.usecases.UpdateVendorProfileCommand;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateVendorProfileRequest(
        @JsonAlias("business_name")
        @Pattern(regexp = ".*\\S.*") @Size(max = 255)
        String businessName,

        @JsonAlias("tax_code")
        @Pattern(regexp = "[A-Za-z0-9-]{5,30}")
        String taxCode,

        @Pattern(regexp = ".*\\S.*") @Size(max = 500)
        String address,

        @JsonAlias("bank_account_number")
        @Pattern(regexp = "[0-9]{6,30}")
        String bankAccountNumber,

        @JsonAlias("bank_name")
        @Pattern(regexp = ".*\\S.*") @Size(max = 150)
        String bankName,

        @JsonAlias("bank_account_holder")
        @Pattern(regexp = ".*\\S.*") @Size(max = 150)
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
