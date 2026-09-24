package com.danasea.backend.modules.weather.presentation.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.weather.domain.exceptions.CategorySafetyRuleNotFoundException;
import com.danasea.backend.modules.weather.domain.exceptions.InvalidSafetyRuleThresholdException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.danasea.backend.shared.presentation.LocalizedExceptionHandlerSupport;

@RestControllerAdvice
public class WeatherExceptionHandler extends LocalizedExceptionHandlerSupport {

    @ExceptionHandler(CategorySafetyRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(CategorySafetyRuleNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error("CATEGORY_SAFETY_RULE_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidSafetyRuleThresholdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidThreshold(InvalidSafetyRuleThresholdException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("INVALID_SAFETY_RULE_THRESHOLD"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(validationError(ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("BAD_REQUEST"));
    }
}
