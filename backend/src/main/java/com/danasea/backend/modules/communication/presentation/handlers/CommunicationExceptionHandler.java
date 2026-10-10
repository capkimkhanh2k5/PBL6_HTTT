package com.danasea.backend.modules.communication.presentation.handlers;

import com.danasea.backend.modules.communication.domain.exceptions.NotificationNotFoundException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.danasea.backend.shared.presentation.LocalizedExceptionHandlerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.danasea.backend.modules.communication")
public class CommunicationExceptionHandler extends LocalizedExceptionHandlerSupport {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error("NOTIFICATION_NOT_FOUND"));
    }
}
