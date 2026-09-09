package com.danasea.backend.security.authentication.application.port;

import com.danasea.backend.security.authentication.domain.event.UserRegisteredEvent;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserRegisteredEvent event);
}
