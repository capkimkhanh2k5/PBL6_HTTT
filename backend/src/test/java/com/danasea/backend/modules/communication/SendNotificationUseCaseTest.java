package com.danasea.backend.modules.communication;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.communication.application.dtos.NotificationCommand;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.events.NotificationEmailEvent;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendNotificationUseCase Tests")
class SendNotificationUseCaseTest {

    @Mock
    private JpaNotificationRepository notificationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AccountInternalApi accountInternalApi;

    private SendNotificationUseCase sendNotificationUseCase;

    @BeforeEach
    void setUp() {
        sendNotificationUseCase = new SendNotificationUseCase(
                notificationRepository,
                rabbitTemplate,
                accountInternalApi,
                LocalizedMessageService.standalone());
    }

    @Test
    @DisplayName("Gửi thông báo IN_APP: Lưu DB với trạng thái SENT")
    void sendNotification_inApp_savesWithSentStatus() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(NotificationJpaEntity.class))).thenAnswer(invocation -> {
            NotificationJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        NotificationJpaEntity result = sendNotificationUseCase.execute(
                userId,
                "WEATHER_WARNING",
                NotificationChannel.IN_APP,
                "Cảnh báo bão",
                "Thời tiết không an toàn cho chuyến đi",
                "SERVICE_SLOT",
                UUID.randomUUID(),
                null
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertEquals("WEATHER_WARNING", result.getType());
        verify(notificationRepository, times(1)).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Gửi thông báo EMAIL: Lưu DB và đẩy event vào RabbitMQ")
    void sendNotification_email_enqueuesRabbitEvent() {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.save(any(NotificationJpaEntity.class))).thenAnswer(invocation -> {
            NotificationJpaEntity entity = invocation.getArgument(0);
            entity.setId(notifId);
            return entity;
        });

        NotificationJpaEntity result = sendNotificationUseCase.execute(
                userId,
                "REFUND_SUCCESS",
                NotificationChannel.EMAIL,
                "Xác nhận hoàn tiền 100%",
                "Đơn hàng của bạn đã được hoàn tiền do thời tiết",
                "REFUND",
                UUID.randomUUID(),
                "customer@example.com"
        );

        assertNotNull(result);
        ArgumentCaptor<NotificationEmailEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEmailEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE), eventCaptor.capture());
        NotificationEmailEvent event = eventCaptor.getValue();
        assertEquals(notifId, event.notificationId());
        assertEquals("customer@example.com", event.toEmail());
        assertEquals("Xác nhận hoàn tiền 100%", event.subject());
    }

    @Test
    @DisplayName("Snapshots notification and email text independently for English and Vietnamese recipients")
    void sendLocalizedNotifications_snapshotsEachRecipientLocale() {
        UUID englishUserId = UUID.randomUUID();
        UUID vietnameseUserId = UUID.randomUUID();
        User englishUser = new User();
        englishUser.setLocale("en-US");
        User vietnameseUser = new User();
        vietnameseUser.setLocale("vi-VN");
        when(accountInternalApi.findUserById(englishUserId)).thenReturn(Optional.of(englishUser));
        when(accountInternalApi.findUserById(vietnameseUserId)).thenReturn(Optional.of(vietnameseUser));
        when(notificationRepository.save(any(NotificationJpaEntity.class))).thenAnswer(invocation -> {
            NotificationJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LocalizedMessageRef title = new LocalizedMessageRef(
                "notification.weather.red.vendor.title", new Object[]{"Ocean Tour"});
        LocalizedMessageRef body = new LocalizedMessageRef(
                "notification.weather.red.vendor.body", new Object[]{"high waves"});

        NotificationJpaEntity english = sendNotificationUseCase.execute(new NotificationCommand(
                englishUserId, "WEATHER_WARNING", NotificationChannel.EMAIL, title, body,
                "SERVICE_SLOT", UUID.randomUUID(), "english@example.com"));
        NotificationJpaEntity vietnamese = sendNotificationUseCase.execute(new NotificationCommand(
                vietnameseUserId, "WEATHER_WARNING", NotificationChannel.EMAIL, title, body,
                "SERVICE_SLOT", UUID.randomUUID(), "vietnamese@example.com"));

        assertEquals("en", english.getLocale());
        assertEquals("Dangerous weather alert for service: Ocean Tour", english.getTitle());
        assertEquals("vi", vietnamese.getLocale());
        assertEquals("Cảnh báo thời tiết nguy hiểm cho dịch vụ: Ocean Tour", vietnamese.getTitle());

        ArgumentCaptor<NotificationEmailEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEmailEvent.class);
        verify(rabbitTemplate, times(2))
                .convertAndSend(eq(RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE), eventCaptor.capture());
        List<NotificationEmailEvent> events = eventCaptor.getAllValues();
        assertEquals("en", events.get(0).locale());
        assertEquals(english.getTitle(), events.get(0).subject());
        assertEquals("vi", events.get(1).locale());
        assertEquals(vietnamese.getTitle(), events.get(1).subject());
    }
}
