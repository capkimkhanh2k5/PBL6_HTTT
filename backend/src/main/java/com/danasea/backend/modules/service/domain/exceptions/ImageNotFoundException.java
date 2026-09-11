package com.danasea.backend.modules.service.domain.exceptions;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException() {
        super("Image not found.");
    }

    public ImageNotFoundException(String message) {
        super(message);
    }
}
