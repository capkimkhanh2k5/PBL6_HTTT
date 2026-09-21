package com.danasea.backend.modules.communication;

import com.danasea.backend.configs.RabbitMQConfig;
import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.communication.domain.events.NotificationEmailEvent;
import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.NotificationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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

    @InjectMocks
    private SendNotificationUseCase sendNotificationUseCase;

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
}
