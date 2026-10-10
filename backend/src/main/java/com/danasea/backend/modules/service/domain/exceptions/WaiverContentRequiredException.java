package com.danasea.backend.modules.service.domain.exceptions;

public class WaiverContentRequiredException extends ServiceDomainException {

    public WaiverContentRequiredException() {
        super("Safety waiver content is required when waiverRequired is true");
    }

    public WaiverContentRequiredException(String message) {
        super(message);
    }
}
