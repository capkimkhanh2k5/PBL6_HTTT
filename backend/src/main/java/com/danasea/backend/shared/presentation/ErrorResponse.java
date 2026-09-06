package com.danasea.backend.shared.presentation;

public record ErrorResponse(
        String code,
        String message
) {
}
