package com.danasea.backend.modules.order.application.services;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescheduleSupport {
    private final JpaSubOrderRepository subOrders;
    private final JpaMasterOrderRepository orders;
    private final EntityManager entityManager;

    public record LockedOrder(MasterOrderJpaEntity master, SubOrderJpaEntity sub) {}

    public LockedOrder lock(UUID id) {
        UUID orderId =
                subOrders
                        .findMasterOrderIdById(id)
                        .orElseThrow(() -> new OrderNotFoundException("Sub-order not found."));
        MasterOrderJpaEntity master =
                orders.findByIdForUpdate(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found."));
        SubOrderJpaEntity sub =
                subOrders
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new OrderNotFoundException("Sub-order not found."));
        entityManager.refresh(master);
        entityManager.refresh(sub);
        return new LockedOrder(master, sub);
    }

    public static void owner(MasterOrderJpaEntity master, UUID userId) {
        if (!Objects.equals(master.getCustomerId(), userId)) {
            throw new AccessDeniedException("User does not own this order.");
        }
    }

    public static void eligible(MasterOrderJpaEntity master, SubOrderJpaEntity sub) {
        if (master.getPaymentStatus() != PaymentOrderStatus.PAID
                || (master.getStatus() != MasterOrderStatus.PAID
                        && master.getStatus() != MasterOrderStatus.PARTIALLY_COMPLETED)
                || sub.getStatus() != SubOrderStatus.CONFIRMED
                || sub.getCheckedInAt() != null) {
            throw conflict("Only paid, confirmed orders without check-in can be rescheduled.");
        }
    }

    public static boolean upcoming(ServiceSlotJpaEntity slot) {
        return slot.getDate()
                .atTime(slot.getStartTime())
                .isAfter(LocalDateTime.now(Booking.VIETNAM_ZONE));
    }

    public static boolean weatherAlert(SafetyRuleEvaluationJpaEntity alert) {
        return alert != null
                && !String.valueOf(alert.getStatus()).startsWith("RESOLVED_")
                && !"AUTO_CANCELLED_FOR_SAFETY".equals(alert.getStatus())
                && (Boolean.FALSE.equals(alert.getIsSafe())
                        || "RED".equals(alert.getAlertLevel())
                        || "YELLOW".equals(alert.getAlertLevel()));
    }

    public static int maxPax(BookingItemJpaEntity item) {
        int original =
                item.getMaxPaxPerPackage() == null
                        ? item.getAllocations().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsPrivateLock()))
                                .mapToInt(a -> a.getAllocatedSeats())
                                .max()
                                .orElse(1)
                        : item.getMaxPaxPerPackage();
        int participants = item.getParticipantsCount() == null ? 1 : item.getParticipantsCount();
        return Math.max(original, (participants + item.getQuantity() - 1) / item.getQuantity());
    }

    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
