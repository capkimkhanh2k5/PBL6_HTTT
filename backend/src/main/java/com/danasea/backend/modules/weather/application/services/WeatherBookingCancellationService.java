package com.danasea.backend.modules.weather.application.services;

import com.danasea.backend.modules.order.application.services.RescheduleSupport;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherBookingCancellationService {
    private final RescheduleSupport support;
    private final JpaSubOrderRepository subOrders;
    private final JpaRefundRepository refunds;
    private final JpaServiceSlotRepository slots;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancel(UUID subOrderId, UUID expectedSlotId, UUID evaluationId) {
        var locked = support.lock(subOrderId);
        var sub = locked.sub();
        if (!Objects.equals(sub.getSlotId(), expectedSlotId)
                || sub.getStatus() != SubOrderStatus.CONFIRMED
                || sub.getCheckedInAt() != null
                || locked.master().getPaymentStatus() != PaymentOrderStatus.PAID) return false;
        slots.findByIdForUpdate(expectedSlotId)
                .orElseThrow(() -> RescheduleSupport.conflict("Weather slot not found."));
        sub.setStatus(SubOrderStatus.CANCELLED);
        subOrders.saveAndFlush(sub);
        if (sub.getBookingItemId() != null)
            slots.releaseBookingItemCapacity(sub.getBookingItemId());
        String key = "weather-" + evaluationId;
        if (sub.getFinalAmount().signum() > 0
                && refunds.findBySubOrderIdAndIdempotencyKey(subOrderId, key).isEmpty()) {
            var refund = new RefundJpaEntity();
            refund.setSubOrderId(subOrderId);
            refund.setAmount(sub.getFinalAmount());
            refund.setRefundPercentage(BigDecimal.valueOf(100.0));
            refund.setReason(RefundReason.WEATHER);
            refund.setStatus(RefundStatus.PENDING);
            refund.setRequestedBy(SecurityUtils.getCurrentUserId().orElse(null));
            refund.setIdempotencyKey(key);
            refunds.save(refund);
        }
        return true;
    }
}
