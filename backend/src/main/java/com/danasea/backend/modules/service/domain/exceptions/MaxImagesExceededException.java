package com.danasea.backend.modules.service.domain.exceptions;

public class MaxImagesExceededException extends RuntimeException {

    public MaxImagesExceededException() {
        super("Maximum number of images for this service has been exceeded.");
    }

    public MaxImagesExceededException(String message) {
        super(message);
    }
}
