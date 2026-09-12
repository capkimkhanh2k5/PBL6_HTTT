package com.danasea.backend.modules.vendor.domain.exceptions;

public class VendorNotFoundException extends RuntimeException {

    public VendorNotFoundException() {
        super("Vendor profile not found");
    }

    public VendorNotFoundException(String message) {
        super(message);
    }
}
