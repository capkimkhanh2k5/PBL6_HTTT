package com.danasea.backend.modules.booking.infrastructure.jobs;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.events.BookingHoldExpiredEvent;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingHoldCleanupJob {

    private final BookingRepositoryPort bookingRepository;
    private final InventoryLockPort inventoryLockPort;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${app.scheduler.booking-cleanup-delay:1m}")
    public void cleanExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Booking> expiredHolds = bookingRepository.findExpiredHolds(now);

        if (expiredHolds == null || expiredHolds.isEmpty()) {
            return;
        }

        log.info("Found {} expired booking holds to clean up", expiredHolds.size());

        for (Booking booking : expiredHolds) {
            try {
                booking.cancel(now);

                List<InventoryLockItem> lockItems = booking.getItems().stream()
                        .map(item -> InventoryLockItem.of(item.getSlotId(), item.getQuantity(), 0))
                        .toList();
                inventoryLockPort.releaseHolds(booking.getId(), lockItems);

                bookingRepository.save(booking);
                eventPublisher.publishEvent(new BookingHoldExpiredEvent(booking.getId(), now));
                log.info("Successfully cancelled and released expired booking hold {}", booking.getId());
            } catch (Exception e) {
                log.error("Failed to clean up expired booking hold {}", booking.getId(), e);
            }
        }
    }
}
