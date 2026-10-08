package com.danasea.backend.security.authentication.application.ports;

import com.danasea.backend.security.authentication.domain.events.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;
import com.danasea.backend.security.authentication.domain.events.PasswordResetRequestedEvent;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserRegisteredEvent event);
    void publishOtpRequestedEvent(OtpEmailRequestedEvent event);
    void publishPasswordResetRequestedEvent(PasswordResetRequestedEvent event);
}
