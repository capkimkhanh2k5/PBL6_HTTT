package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleServiceNotFound(ServiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "SERVICE_NOT_FOUND",
                "message", ex.getMessage()
        ));
    }
}
