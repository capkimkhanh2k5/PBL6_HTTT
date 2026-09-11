package com.danasea.backend.modules.service.domain.exceptions;

public class ServiceImagesRequiredException extends ServiceDomainException {

    public ServiceImagesRequiredException() {
        super("At least one image is required before submitting service for review");
    }

    public ServiceImagesRequiredException(String message) {
        super(message);
    }
}
