package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.ai.application.port.CustomerSupportNoticePort;
import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort.Request;
import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerSupportNoticeAdapter implements CustomerSupportNoticePort {
    private final AccountInternalApi accounts;
    private final SendNotificationUseCase notifications;
    @Override public void created(Request request) {
        send(request.ownerId(), request, "notification.ai.support.received.title", "notification.ai.support.received.body", request.id());
        int page = 0;
        while (true) {
            var admins = accounts.findUsers(PageRequest.of(page, 100), Role.ADMIN, false, null);
            admins.getContent().forEach(admin -> send(admin.getId(), request, "notification.ai.support.queue.title", "notification.ai.support.queue.body", request.id()));
            if (!admins.hasNext()) break;
            page++;
        }
    }
    @Override public void updated(Request request) {
        send(request.ownerId(), request, "notification.ai.support.updated.title", "notification.ai.support.updated.body", request.id(), request.status());
    }
    private void send(UUID recipient, Request request, String title, String body, Object... args) {
        notifications.execute(new NotificationCommand(recipient, "AI_SUPPORT_REQUEST", NotificationChannel.IN_APP,
                LocalizedMessageRef.of(title), LocalizedMessageRef.of(body, args), "AI_CUSTOMER_SUPPORT_REQUEST", request.id(), null));
    }
}
