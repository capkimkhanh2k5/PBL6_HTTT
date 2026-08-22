package com.danasport.backend.authorization.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasport.backend.shared.presentation.ErrorResponse;
import com.danasport.backend.authorization.domain.exception.AccessDeniedException;

@RestControllerAdvice
public class AuthorizationHandler {
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                            "ACCESS_DENIED", 
                            exception.getMessage()
                ));
    }
}
    