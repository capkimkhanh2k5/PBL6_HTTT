package com.danasea.backend.security.authentication.application.port;

import com.danasea.backend.security.authentication.domain.event.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.event.OtpEmailRequestedEvent;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserRegisteredEvent event);
    void publishOtpRequestedEvent(OtpEmailRequestedEvent event);
}
