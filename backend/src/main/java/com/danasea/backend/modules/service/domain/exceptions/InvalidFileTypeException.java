package com.danasea.backend.modules.service.domain.exceptions;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException() {
        super("Invalid file type. Supported formats are JPEG, PNG, and WEBP.");
    }

    public InvalidFileTypeException(String message) {
        super(message);
    }
}
