package com.danasea.backend.modules.vendor.domain.exception;

public class InvalidDocTypeException extends RuntimeException {

    public InvalidDocTypeException() {
        super("Invalid document type");
    }

    public InvalidDocTypeException(String message) {
        super(message);
    }
}
