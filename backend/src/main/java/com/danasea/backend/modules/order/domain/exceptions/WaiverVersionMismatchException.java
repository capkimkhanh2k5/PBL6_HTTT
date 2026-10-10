package com.danasea.backend.modules.order.domain.exceptions;

public class WaiverVersionMismatchException extends RuntimeException {

    public WaiverVersionMismatchException(String message) {
        super(message);
    }
}
