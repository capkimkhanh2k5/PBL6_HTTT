package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.domain.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTransactionalEventListener {

    private final AuthEventPublisher rabbitMqEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Transaction committed. Publishing UserRegisteredEvent (eventId: {}) to RabbitMQ", event.eventId());
        rabbitMqEventPublisher.publishUserRegisteredEvent(event);
    }
}
