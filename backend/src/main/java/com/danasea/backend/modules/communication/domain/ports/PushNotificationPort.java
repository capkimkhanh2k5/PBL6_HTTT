package com.danasea.backend.modules.communication.domain.ports;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationPort {
    boolean sendPush(UUID userId, String title, String body, Map<String, Object> data);
}
