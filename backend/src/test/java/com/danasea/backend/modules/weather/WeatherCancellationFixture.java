package com.danasea.backend.modules.weather;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.order.application.services.RescheduleSupport;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.application.services.WeatherBookingCancellationService;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

final class WeatherCancellationFixture {
    static void install(
            Object coordinator,
            JpaSubOrderRepository subs,
            JpaRefundRepository refunds,
            JpaServiceSlotRepository slots,
            UUID slotId) {
        var support = mock(RescheduleSupport.class);
        lenient()
                .when(support.lock(any()))
                .thenAnswer(
                        inv -> {
                            UUID id = inv.getArgument(0);
                            for (var sub : subs.findBySlotId(slotId)) {
                                if (id.equals(sub.getId())) {
                                    var master = new MasterOrderJpaEntity();
                                    master.setPaymentStatus(PaymentOrderStatus.PAID);
                                    return new RescheduleSupport.LockedOrder(master, sub);
                                }
                            }
                            throw new IllegalStateException("Booking fixture not found");
                        });
        lenient()
                .when(slots.findByIdForUpdate(any()))
                .thenReturn(
                        Optional.of(
                                new com.danasea.backend.modules.service.infrastructure.persistence
                                        .entities.ServiceSlotJpaEntity()));
        ReflectionTestUtils.setField(
                coordinator,
                "weatherCancellation",
                new WeatherBookingCancellationService(support, subs, refunds, slots));
    }
}
