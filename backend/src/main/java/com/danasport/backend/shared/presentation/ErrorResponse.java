package com.danasport.backend.shared.presentation;

public record ErrorResponse(
        String code,
        String message
) {
}
