package com.danasea.backend.security.authentication.infrastructure.messaging;

import com.danasea.backend.config.RabbitMQConfig;
import com.danasea.backend.security.authentication.application.port.AuthEventPublisher;
import com.danasea.backend.security.authentication.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQAuthEventPublisher implements AuthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for user: {}", event.email());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.USER_REGISTERED_ROUTING_KEY, event, message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }
}
