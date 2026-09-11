package com.danasea.backend.modules.service.domain.exceptions;

public class FileStorageException extends RuntimeException {

    public FileStorageException() {
        super("Failed to store or process file.");
    }

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
