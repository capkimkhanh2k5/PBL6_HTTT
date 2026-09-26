package com.danasea.backend.modules.order.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.CreateOrderCommand;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.exceptions.BookingNotEligibleForOrderException;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.BookingLookupPort;
import com.danasea.backend.modules.order.domain.ports.BookingOrderView;
import com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort;
import com.danasea.backend.modules.order.domain.ports.CommissionPolicyPort;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@Service
public class CreateOrderUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final BookingLookupPort bookingLookupPort;
    private final BookingStatusUpdatePort bookingStatusUpdatePort;
    private final CommissionPolicyPort commissionPolicyPort;
    private final OrderEventPublisherPort orderEventPublisherPort;

    public CreateOrderUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            BookingLookupPort bookingLookupPort,
            BookingStatusUpdatePort bookingStatusUpdatePort,
            CommissionPolicyPort commissionPolicyPort,
            OrderEventPublisherPort orderEventPublisherPort) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.bookingLookupPort = bookingLookupPort;
        this.bookingStatusUpdatePort = bookingStatusUpdatePort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
    }

    @Transactional
    public MasterOrderDetailResult execute(CreateOrderCommand command) {
        if (command == null || command.customerId() == null || command.bookingId() == null) {
            throw new IllegalArgumentException("Customer ID and booking ID are required.");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required.");
        }

        // 1. Kiểm tra Idempotency theo bookingId
        Optional<MasterOrder> existingByBooking = masterOrderRepository.findByBookingId(command.bookingId());
        if (existingByBooking.isPresent()) {
            MasterOrder order = existingByBooking.get();
            order.setSubOrders(subOrderRepository.findByMasterOrderId(order.getId()));
            return MasterOrderDetailResult.fromDomain(order);
        }

        // 2. Kiểm tra Idempotency theo (customerId, idempotencyKey)
        Optional<MasterOrder> existingByKey = masterOrderRepository.findByCustomerIdAndIdempotencyKey(
                command.customerId(), command.idempotencyKey());
        if (existingByKey.isPresent()) {
            MasterOrder order = existingByKey.get();
            if (!command.bookingId().equals(order.getBookingId())) {
                throw new InvalidOrderStateException("The idempotency key was already used for another booking.");
            }
            order.setSubOrders(subOrderRepository.findByMasterOrderId(order.getId()));
            return MasterOrderDetailResult.fromDomain(order);
        }

        // 3. Tra cứu thông tin Booking qua Port
        BookingOrderView booking = bookingLookupPort.findBookingForOrder(command.bookingId())
                .orElseThrow(() -> new OrderNotFoundException("Booking not found with id: " + command.bookingId()));

        // 4. Kiểm tra quyền sở hữu (chặn IDOR 403)
        if (!booking.customerId().equals(command.customerId())) {
            throw new UnauthorizedOrderAccessException(command.bookingId(), command.customerId());
        }

        // 5. Kiểm tra trạng thái Booking: chỉ cho phép từ trạng thái HOLD
        if (!"HOLD".equalsIgnoreCase(booking.status())) {
            throw new BookingNotEligibleForOrderException(
                    command.bookingId(), "Booking is not in HOLD status (current: " + booking.status() + ")");
        }

        // 6. Kiểm tra TTL giữ chỗ
        OffsetDateTime now = OffsetDateTime.now();
        if (booking.holdExpiresAt() != null && now.isAfter(booking.holdExpiresAt())) {
            throw new BookingNotEligibleForOrderException(
                    command.bookingId(), "Booking hold expired at " + booking.holdExpiresAt());
        }

        // 7. Kiểm tra danh sách Booking Items
        if (booking.items() == null || booking.items().isEmpty()) {
            throw new BookingNotEligibleForOrderException(command.bookingId(), "Booking contains no items.");
        }

        // 8. Khởi tạo MasterOrder
        MasterOrder order = MasterOrder.createFromBooking(
                command.customerId(),
                command.bookingId(),
                booking.totalAmount(),
                booking.holdExpiresAt(),
                command.idempotencyKey()
        );
        order = masterOrderRepository.save(order);

        // 9. Tách Sub-Orders theo quy tắc 1 SubOrder = 1 BookingItem (Mục 9.2.1)
        List<SubOrder> subOrders = new ArrayList<>();
        for (BookingOrderView.BookingItemOrderView item : booking.items()) {
            BigDecimal rate = commissionPolicyPort.getCommissionRate(item.vendorId());
            if (rate == null) {
                rate = CommissionPolicyPort.DEFAULT_COMMISSION_RATE;
            }
            BigDecimal subtotal = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            BigDecimal commission = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal vendorPayout = subtotal.subtract(commission);

            SubOrder subOrder = new SubOrder();
            subOrder.setBookingItemId(item.bookingItemId());
            subOrder.setMasterOrderId(order.getId());
            subOrder.setVendorId(item.vendorId());
            subOrder.setServiceId(item.serviceId());
            subOrder.setSlotId(item.slotId());
            subOrder.setQuantity(item.quantity());
            subOrder.setUnitPrice(item.price());
            subOrder.setSubtotalAmount(subtotal);
            subOrder.setCommissionRate(rate);
            subOrder.setCommissionAmount(commission);
            subOrder.setVendorPayoutAmount(vendorPayout);
            subOrder.setStatus(SubOrderStatus.PENDING);
            subOrder.setWaiverAccepted(false);
            subOrders.add(subOrder);
        }
        subOrders = subOrderRepository.saveAll(subOrders);
        order.setSubOrders(subOrders);

        // 10. Chuyển trạng thái Booking sang PENDING_PAYMENT (trong cùng Transaction)
        bookingStatusUpdatePort.updateStatusToPendingPayment(command.bookingId());

        // 11. Bắn sự kiện Order Created
        orderEventPublisherPort.publishOrderCreatedEvent(order);

        return MasterOrderDetailResult.fromDomain(order);
    }
}
