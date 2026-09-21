package com.danasea.backend.modules.booking.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.danasea.backend.modules.booking.application.dtos.BookingCancelResult;
import com.danasea.backend.modules.booking.application.dtos.CancelBookingCommand;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelBookingUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final ServiceSlotPort serviceSlotPort;
    private final InventoryLockPort inventoryLockPort;

    public BookingCancelResult execute(CancelBookingCommand command) {
        if (command == null || command.bookingId() == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findByIdWithItems(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        // Chống IDOR: Khách hàng chỉ được hủy booking của chính mình (ngoại trừ ADMIN)
        if (!command.isAdmin()) {
            booking.validateOwner(command.customerId());
        }

        // Kiểm tra tính hợp lệ của trạng thái hiện tại
        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new InvalidBookingStateException("Booking is already cancelled.");
        }

        if (!BookingStatus.HOLD.equals(booking.getStatus())
                && !BookingStatus.CONFIRMED.equals(booking.getStatus())
                && !BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            throw new InvalidBookingStateException("Cannot cancel booking in status " + booking.getStatus());
        }

        String reason = (command.reason() != null && !command.reason().isBlank())
                ? command.reason().trim()
                : "Customer requested cancellation";

        OffsetDateTime now = OffsetDateTime.now();
        Optional<OffsetDateTime> earliestServiceTimeOpt = booking.findEarliestServiceStartTime();
        OffsetDateTime earliestServiceTime = earliestServiceTimeOpt.orElse(null);

        boolean refundEligible;
        int refundPercentage;
        BigDecimal refundAmount;
        String message;

        if (BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            // Tính toán chính sách hoàn tiền 24h đối với Booking đã thanh toán xác nhận
            refundEligible = booking.isEligibleForFullRefund(now);
            refundPercentage = refundEligible ? 100 : 0;
            refundAmount = refundEligible ? booking.getTotalAmount() : BigDecimal.ZERO;
            message = refundEligible
                    ? "Hủy thành công. Bạn đủ điều kiện hoàn 100% tiền theo chính sách trước 24h."
                    : "Hủy thành công. Theo chính sách hủy trễ dưới 24h, bạn không được hoàn tiền.";

            // Hoàn trả số lượng đã đặt vào các slot dịch vụ trong PostgreSQL
            serviceSlotPort.releaseCapacityBatch(booking.getItems());
        } else {
            // Trạng thái HOLD hoặc PENDING_PAYMENT: Chưa trừ tiền nên không hoàn tiền, giải phóng Redis lock
            refundEligible = false;
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
            message = "Hủy giữ chỗ thành công.";

            if (booking.getItems() != null && !booking.getItems().isEmpty()) {
                List<InventoryLockItem> lockItems = booking.getItems().stream()
                        .map(item -> InventoryLockItem.of(item.getSlotId(), item.getQuantity(), 0))
                        .toList();
                inventoryLockPort.releaseHolds(booking.getId(), lockItems);
            }
        }

        booking.cancel(now);
        Booking saved = bookingRepository.save(booking);

        return new BookingCancelResult(
                saved.getId(),
                saved.getCustomerId(),
                saved.getStatus(),
                saved.getUpdatedAt(),
                earliestServiceTime,
                refundEligible,
                refundPercentage,
                refundAmount,
                reason,
                message
        );
    }
}
