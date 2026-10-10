package com.danasea.backend.modules.communication.infrastructure.adapters;

import com.danasea.backend.modules.communication.domain.ports.PushNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component("pushNotificationPort")
public class LoggingPushNotificationAdapter implements PushNotificationPort {

    @Override
    public boolean sendPush(UUID userId, String title, String body, Map<String, Object> data) {
        log.info("[DEV PUSH PREVIEW] No push provider configured. Preview for userId: {}, title: {}, body: {}, data: {}",
                userId, title, body, data);
        return false;
    }
}
