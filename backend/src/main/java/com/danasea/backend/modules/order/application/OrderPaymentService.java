package com.danasea.backend.modules.order.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.application.dtos.ConfirmBookingCommand;
import com.danasea.backend.modules.booking.application.usecases.ConfirmBookingUseCase;
import com.danasea.backend.modules.booking.domain.exceptions.BookingHoldExpiredException;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.InvalidWebhookException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaPaymentRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.presentation.dtos.OrderPageResponse;
import com.danasea.backend.modules.order.presentation.dtos.OrderResponse;
import com.danasea.backend.modules.order.presentation.dtos.PaymentIntentResponse;
import com.danasea.backend.modules.order.presentation.dtos.PaymentWebhookRequest;
import com.danasea.backend.modules.order.presentation.dtos.PaymentWebhookResponse;
import com.danasea.backend.modules.order.presentation.dtos.RefundResponse;
import com.danasea.backend.modules.order.presentation.dtos.RefundWebhookRequest;
import com.danasea.backend.modules.order.presentation.dtos.RefundWebhookResponse;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderResponse;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderPaymentService {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,100}");
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.15");

    private final JpaBookingRepository bookingRepository;
    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaPaymentRepository paymentRepository;
    private final JpaRefundRepository refundRepository;
    private final JpaServiceSlotRepository serviceSlotRepository;
    private final VendorInternalApi vendorInternalApi;
    private final RefundPolicyEngine refundPolicyEngine;
    private final PaymentWebhookSigner webhookSigner;
    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final ObjectMapper objectMapper;

    public OrderPaymentService(
            JpaBookingRepository bookingRepository,
            JpaMasterOrderRepository masterOrderRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaPaymentRepository paymentRepository,
            JpaRefundRepository refundRepository,
            JpaServiceSlotRepository serviceSlotRepository,
            VendorInternalApi vendorInternalApi,
            RefundPolicyEngine refundPolicyEngine,
            PaymentWebhookSigner webhookSigner,
            ConfirmBookingUseCase confirmBookingUseCase,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.vendorInternalApi = vendorInternalApi;
        this.refundPolicyEngine = refundPolicyEngine;
        this.webhookSigner = webhookSigner;
        this.confirmBookingUseCase = confirmBookingUseCase;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(UUID customerId, UUID bookingId, String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        if (customerId == null || bookingId == null) {
            throw new IllegalArgumentException("Customer ID and booking ID are required.");
        }

        BookingJpaEntity booking = bookingRepository.findByIdWithItemsForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        assertOwner(booking.getCustomerId(), customerId);

        var existingForBooking = masterOrderRepository.findByBookingId(bookingId);
        if (existingForBooking.isPresent()) {
            return toOrderResponse(existingForBooking.get());
        }
        var existingForKey = masterOrderRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (existingForKey.isPresent()) {
            if (!bookingId.equals(existingForKey.get().getBookingId())) {
                throw new InvalidOrderStateException("The idempotency key was already used for another booking.");
            }
            return toOrderResponse(existingForKey.get());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (!BookingStatus.HOLD.equals(booking.getStatus())) {
            throw new InvalidBookingStateException("Only a booking in HOLD status can be converted to an order.");
        }
        if (booking.getHoldExpiresAt() != null && now.isAfter(booking.getHoldExpiresAt())) {
            throw new BookingHoldExpiredException(bookingId);
        }
        if (booking.getItems() == null || booking.getItems().isEmpty()) {
            throw new InvalidBookingStateException("A booking must contain at least one item.");
        }

        MasterOrderJpaEntity order = new MasterOrderJpaEntity();
        order.setBookingId(bookingId);
        order.setCustomerId(customerId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(booking.getTotalAmount());
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        order.setIdempotencyKey(idempotencyKey);
        order = masterOrderRepository.save(order);

        List<SubOrderJpaEntity> subOrders = new ArrayList<>();
        for (BookingItemJpaEntity item : booking.getItems()) {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal commission = subtotal.multiply(DEFAULT_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setBookingItemId(item.getId());
            subOrder.setMasterOrderId(order.getId());
            subOrder.setVendorId(item.getVendorId());
            subOrder.setServiceId(item.getServiceId());
            subOrder.setSlotId(item.getSlotId());
            subOrder.setQuantity(item.getQuantity());
            subOrder.setUnitPrice(item.getPrice());
            subOrder.setSubtotalAmount(subtotal);
            subOrder.setCommissionRate(DEFAULT_COMMISSION_RATE);
            subOrder.setCommissionAmount(commission);
            subOrder.setVendorPayoutAmount(subtotal.subtract(commission));
            subOrder.setStatus(SubOrderStatus.PENDING);
            subOrder.setWaiverAccepted(false);
            subOrders.add(subOrder);
        }
        subOrderRepository.saveAll(subOrders);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        bookingRepository.save(booking);
        return toOrderResponse(order, subOrders);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, UUID orderId, boolean admin) {
        MasterOrderJpaEntity order = findOrder(orderId);
        if (!admin) {
            assertOwner(order.getCustomerId(), userId);
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse<OrderResponse> getCustomerOrders(UUID customerId, int page, int size) {
        validatePage(page, size);
        Page<MasterOrderJpaEntity> result = masterOrderRepository.findByCustomerId(
                customerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new OrderPageResponse<>(result.getContent().stream().map(this::toOrderResponse).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderPageResponse<SubOrderResponse> getVendorOrders(UUID userId, int page, int size) {
        validatePage(page, size);
        UUID vendorId = vendorInternalApi.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found."))
                .getId();
        Page<SubOrderJpaEntity> result = subOrderRepository.findByVendorId(
                vendorId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new OrderPageResponse<>(result.getContent().stream().map(this::toSubOrderResponse).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public SubOrderResponse rejectVendorBooking(
            UUID userId,
            UUID bookingItemId,
            String reason,
            String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new IllegalArgumentException("A rejection reason of at most 500 characters is required.");
        }
        UUID vendorId = vendorInternalApi.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found."))
                .getId();
        SubOrderJpaEntity subOrder = subOrderRepository.findByBookingItemIdForUpdate(bookingItemId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Vendor booking item not found with id: " + bookingItemId));
        if (!vendorId.equals(subOrder.getVendorId())) {
            throw new AccessDeniedException("Vendor does not own this booking item.");
        }
        if (subOrder.getStatus() == SubOrderStatus.REJECTED) {
            return toSubOrderResponse(subOrder);
        }
        if (subOrder.getStatus() != SubOrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Only a confirmed booking item can be rejected.");
        }

        var existingRefund = refundRepository.findBySubOrderIdAndIdempotencyKey(
                subOrder.getId(), idempotencyKey);
        if (existingRefund.isEmpty()) {
            RefundJpaEntity refund = new RefundJpaEntity();
            refund.setSubOrderId(subOrder.getId());
            refund.setAmount(subOrder.getSubtotalAmount());
            refund.setRefundPercentage(BigDecimal.valueOf(100));
            refund.setReason(RefundReason.VENDOR_FAULT);
            refund.setStatus(RefundStatus.PENDING);
            refund.setRequestedBy(userId);
            refund.setIdempotencyKey(idempotencyKey);
            refundRepository.save(refund);
        }

        subOrder.setStatus(SubOrderStatus.REJECTED);
        subOrderRepository.save(subOrder);
        if (subOrder.getSlotId() != null && subOrder.getQuantity() != null && subOrder.getQuantity() > 0) {
            serviceSlotRepository.decrementBookedCount(subOrder.getSlotId(), subOrder.getQuantity());
        }
        return toSubOrderResponse(subOrder);
    }

    @Transactional
    public PaymentIntentResponse createPaymentIntent(
            UUID customerId,
            UUID orderId,
            PaymentProvider provider,
            String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        if (provider == null) {
            throw new IllegalArgumentException("Payment provider is required.");
        }

        MasterOrderJpaEntity order = masterOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        assertOwner(order.getCustomerId(), customerId);

        var existing = paymentRepository.findByMasterOrderIdAndIdempotencyKey(orderId, idempotencyKey);
        if (existing.isPresent()) {
            if (existing.get().getProvider() != provider) {
                throw new InvalidOrderStateException("The idempotency key was already used with another provider.");
            }
            return toPaymentIntentResponse(existing.get());
        }
        if (!MasterOrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new InvalidOrderStateException("A payment intent can only be created for a pending order.");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderStateException("The order amount must be positive.");
        }

        PaymentJpaEntity payment = new PaymentJpaEntity();
        payment.setMasterOrderId(orderId);
        payment.setProvider(provider);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        return toPaymentIntentResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentWebhookResponse processWebhook(
            PaymentProvider provider,
            String rawPayload,
            String signature) {
        if (!webhookSigner.isValid(rawPayload, signature)) {
            throw new InvalidWebhookException("Invalid webhook signature.");
        }
        PaymentWebhookRequest request = parseWebhook(rawPayload);
        validateWebhookRequest(request);

        var alreadyProcessed = paymentRepository.findByProviderAndWebhookEventId(provider, request.eventId());
        if (alreadyProcessed.isPresent()) {
            PaymentJpaEntity payment = alreadyProcessed.get();
            return new PaymentWebhookResponse(request.eventId(), payment.getId(), payment.getStatus(), true);
        }

        PaymentJpaEntity payment = paymentRepository.findByIdForUpdate(request.paymentId())
                .orElseThrow(() -> new InvalidWebhookException("Unknown payment."));
        if (payment.getProvider() != provider) {
            throw new InvalidWebhookException("The webhook provider does not match the payment provider.");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(request.amount()) != 0) {
            throw new InvalidWebhookException("The webhook amount does not match the payment amount.");
        }
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new InvalidOrderStateException("The payment has already reached a terminal state.");
        }

        payment.setWebhookEventId(request.eventId());
        payment.setProviderTransactionId(request.providerTransactionId());
        payment.setRawWebhookPayload(rawPayload);
        payment.setStatus(request.status());
        paymentRepository.save(payment);

        if (PaymentStatus.SUCCESS.equals(request.status())) {
            MasterOrderJpaEntity order = masterOrderRepository.findByIdForUpdate(payment.getMasterOrderId())
                    .orElseThrow(() -> new InvalidWebhookException("Payment order not found."));
            if (!MasterOrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
                throw new InvalidOrderStateException("The order is not awaiting payment.");
            }
            if (order.getTotalAmount().compareTo(payment.getAmount()) != 0) {
                throw new InvalidWebhookException("The paid amount does not match the order total.");
            }

            order.setStatus(MasterOrderStatus.PAID);
            masterOrderRepository.save(order);
            List<SubOrderJpaEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
            subOrders.forEach(subOrder -> subOrder.setStatus(SubOrderStatus.CONFIRMED));
            subOrderRepository.saveAll(subOrders);

            confirmBookingUseCase.execute(new ConfirmBookingCommand(order.getBookingId(), order.getCustomerId()));
        }

        return new PaymentWebhookResponse(request.eventId(), payment.getId(), payment.getStatus(), false);
    }

    @Transactional
    public List<RefundResponse> requestRefund(
            UUID customerId,
            UUID orderOrSubOrderId,
            RefundReason requestedReason,
            String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        RefundReason reason = requestedReason == null ? RefundReason.CUSTOMER_REQUEST : requestedReason;
        if (reason != RefundReason.CUSTOMER_REQUEST && reason != RefundReason.CUSTOMER_CANCEL) {
            throw new IllegalArgumentException("Customers may only request a customer cancellation refund.");
        }

        MasterOrderJpaEntity order;
        List<SubOrderJpaEntity> subOrders;
        var master = masterOrderRepository.findById(orderOrSubOrderId);
        if (master.isPresent()) {
            order = master.get();
            subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        } else {
            SubOrderJpaEntity subOrder = subOrderRepository.findById(orderOrSubOrderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderOrSubOrderId));
            order = findOrder(subOrder.getMasterOrderId());
            subOrders = List.of(subOrder);
        }
        assertOwner(order.getCustomerId(), customerId);
        if (!MasterOrderStatus.PAID.equals(order.getStatus())
                && !MasterOrderStatus.PARTIALLY_COMPLETED.equals(order.getStatus())
                && !MasterOrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new InvalidOrderStateException("Refunds can only be requested for a paid order.");
        }

        List<RefundResponse> responses = new ArrayList<>();
        for (SubOrderJpaEntity subOrder : subOrders) {
            var existing = refundRepository.findBySubOrderIdAndIdempotencyKey(subOrder.getId(), idempotencyKey);
            if (existing.isPresent()) {
                responses.add(toRefundResponse(existing.get()));
                continue;
            }
            if (!isRefundableStatus(subOrder.getStatus())) {
                throw new InvalidOrderStateException("Sub-order " + subOrder.getId() + " is not refundable.");
            }

            LocalDateTime departure = serviceSlotRepository.findById(subOrder.getSlotId())
                    .filter(slot -> slot.getDate() != null && slot.getStartTime() != null)
                    .map(slot -> LocalDateTime.of(slot.getDate(), slot.getStartTime()))
                    .orElse(null);
            RefundEvaluationResult evaluation = refundPolicyEngine.evaluate(
                    reason, departure, LocalDateTime.now(), subOrder.getSubtotalAmount());
            if (evaluation.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOrderStateException("The cancellation policy does not allow a refund for sub-order "
                        + subOrder.getId() + ".");
            }

            RefundJpaEntity refund = new RefundJpaEntity();
            refund.setSubOrderId(subOrder.getId());
            refund.setAmount(evaluation.refundAmount());
            refund.setRefundPercentage(evaluation.refundPercentage());
            refund.setReason(reason);
            refund.setStatus(RefundStatus.PENDING);
            refund.setRequestedBy(customerId);
            refund.setIdempotencyKey(idempotencyKey);
            responses.add(toRefundResponse(refundRepository.save(refund)));
        }
        return responses;
    }

    @Transactional
    public RefundWebhookResponse processRefundWebhook(
            PaymentProvider provider,
            String rawPayload,
            String signature) {
        if (!webhookSigner.isValid(rawPayload, signature)) {
            throw new InvalidWebhookException("Invalid webhook signature.");
        }
        RefundWebhookRequest request = parseRefundWebhook(rawPayload);
        validateRefundWebhookRequest(request);

        var processedEvent = refundRepository.findByProviderAndWebhookEventId(provider, request.eventId());
        if (processedEvent.isPresent()) {
            RefundJpaEntity refund = processedEvent.get();
            return new RefundWebhookResponse(request.eventId(), refund.getId(), refund.getStatus(), true);
        }

        RefundJpaEntity refund = refundRepository.findByIdForUpdate(request.refundId())
                .orElseThrow(() -> new InvalidWebhookException("Unknown refund."));
        if (!RefundStatus.PENDING.equals(refund.getStatus())) {
            throw new InvalidOrderStateException("The refund has already reached a terminal state.");
        }
        if (refund.getAmount() == null || refund.getAmount().compareTo(request.amount()) != 0) {
            throw new InvalidWebhookException("The webhook amount does not match the refund amount.");
        }

        SubOrderJpaEntity subOrder = subOrderRepository.findById(refund.getSubOrderId())
                .orElseThrow(() -> new InvalidWebhookException("Refund sub-order not found."));
        PaymentJpaEntity originalPayment = paymentRepository
                .findByMasterOrderIdOrderByCreatedAtDesc(subOrder.getMasterOrderId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS
                        || payment.getStatus() == PaymentStatus.REFUNDED)
                .findFirst()
                .orElseThrow(() -> new InvalidWebhookException("No successful original payment was found."));
        if (originalPayment.getProvider() != provider) {
            throw new InvalidWebhookException("Refund provider must match the original payment provider.");
        }

        refund.setProvider(provider);
        refund.setWebhookEventId(request.eventId());
        refund.setProviderRefundId(request.providerRefundId());
        refund.setStatus(request.status());
        refund.setProcessedAt(RefundStatus.PROCESSED.equals(request.status()) ? OffsetDateTime.now() : null);
        refundRepository.save(refund);

        if (RefundStatus.PROCESSED.equals(request.status())) {
            subOrder.setStatus(refund.getRefundPercentage().compareTo(BigDecimal.valueOf(100)) == 0
                    ? SubOrderStatus.REFUNDED
                    : SubOrderStatus.PARTIALLY_REFUNDED);
            subOrderRepository.save(subOrder);

            List<UUID> subOrderIds = subOrderRepository.findByMasterOrderId(subOrder.getMasterOrderId()).stream()
                    .map(SubOrderJpaEntity::getId)
                    .toList();
            BigDecimal processedTotal = refundRepository
                    .findBySubOrderIdInAndStatus(subOrderIds, RefundStatus.PROCESSED).stream()
                    .map(RefundJpaEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (processedTotal.compareTo(originalPayment.getAmount()) >= 0) {
                originalPayment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(originalPayment);
            }
        }

        return new RefundWebhookResponse(request.eventId(), refund.getId(), refund.getStatus(), false);
    }

    private PaymentWebhookRequest parseWebhook(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, PaymentWebhookRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new InvalidWebhookException("Malformed webhook payload.");
        }
    }

    private RefundWebhookRequest parseRefundWebhook(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, RefundWebhookRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new InvalidWebhookException("Malformed refund webhook payload.");
        }
    }

    private void validateRefundWebhookRequest(RefundWebhookRequest request) {
        if (request == null || request.eventId() == null || request.eventId().isBlank()
                || request.eventId().length() > 150 || request.refundId() == null
                || request.providerRefundId() == null || request.providerRefundId().isBlank()
                || request.providerRefundId().length() > 150 || request.status() == null
                || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWebhookException("Missing or invalid refund webhook fields.");
        }
        if (request.status() != RefundStatus.PROCESSED && request.status() != RefundStatus.FAILED) {
            throw new InvalidWebhookException("Refund webhook status must be PROCESSED or FAILED.");
        }
    }

    private void validateWebhookRequest(PaymentWebhookRequest request) {
        if (request == null || request.eventId() == null || request.eventId().isBlank()
                || request.eventId().length() > 150 || request.paymentId() == null
                || request.providerTransactionId() == null || request.providerTransactionId().isBlank()
                || request.providerTransactionId().length() > 255 || request.status() == null
                || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWebhookException("Missing or invalid webhook fields.");
        }
        if (request.status() != PaymentStatus.SUCCESS && request.status() != PaymentStatus.FAILED) {
            throw new InvalidWebhookException("Webhook status must be SUCCESS or FAILED.");
        }
    }

    private MasterOrderJpaEntity findOrder(UUID id) {
        return masterOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse toOrderResponse(MasterOrderJpaEntity order) {
        return toOrderResponse(order, subOrderRepository.findByMasterOrderId(order.getId()));
    }

    private OrderResponse toOrderResponse(MasterOrderJpaEntity order, List<SubOrderJpaEntity> subOrders) {
        return new OrderResponse(order.getId(), order.getBookingId(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), order.getDiscountAmount(),
                subOrders.stream().map(this::toSubOrderResponse).toList(),
                order.getCreatedAt(), order.getUpdatedAt());
    }

    private SubOrderResponse toSubOrderResponse(SubOrderJpaEntity subOrder) {
        return new SubOrderResponse(subOrder.getId(), subOrder.getVendorId(), subOrder.getServiceId(),
                subOrder.getSlotId(), subOrder.getQuantity(), subOrder.getUnitPrice(),
                subOrder.getSubtotalAmount(), subOrder.getStatus());
    }

    private PaymentIntentResponse toPaymentIntentResponse(PaymentJpaEntity payment) {
        return new PaymentIntentResponse(payment.getId(), payment.getMasterOrderId(), payment.getProvider(),
                payment.getAmount(), payment.getStatus(), payment.getId().toString(), payment.getCreatedAt());
    }

    private RefundResponse toRefundResponse(RefundJpaEntity refund) {
        return new RefundResponse(refund.getId(), refund.getSubOrderId(), refund.getAmount(),
                refund.getRefundPercentage(), refund.getReason(), refund.getStatus(), refund.getCreatedAt());
    }

    private void assertOwner(UUID ownerId, UUID userId) {
        if (ownerId == null || userId == null || !ownerId.equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this order.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain 8 to 100 letters, digits, dots, underscores, colons, or hyphens.");
        }
    }

    private boolean isRefundableStatus(SubOrderStatus status) {
        return status == SubOrderStatus.CONFIRMED || status == SubOrderStatus.CHECKED_IN
                || status == SubOrderStatus.IN_PROGRESS || status == SubOrderStatus.COMPLETED;
    }
}
