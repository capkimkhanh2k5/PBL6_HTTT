package com.danasea.backend.modules.vendor.domain.exceptions;

public class VendorAlreadyExistsException extends RuntimeException {

    public VendorAlreadyExistsException() {
        super("Vendor profile already exists for this user");
    }

    public VendorAlreadyExistsException(String message) {
        super(message);
    }
}
