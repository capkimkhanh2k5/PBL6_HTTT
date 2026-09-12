package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.domain.events.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQAuthEventPublisher implements AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQAuthEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for user: {}", event.email());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.USER_REGISTERED_ROUTING_KEY, event);
    }

    @Override
    public void publishOtpRequestedEvent(OtpEmailRequestedEvent event) {
        log.info("Publishing OtpEmailRequestedEvent for user: {}", event.email());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.USER_OTP_REQUESTED_ROUTING_KEY,
                event);
    }
}
