package com.danasea.backend.security.authentication.application.ports;

import com.danasea.backend.security.authentication.domain.events.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserRegisteredEvent event);
    void publishOtpRequestedEvent(OtpEmailRequestedEvent event);
}
