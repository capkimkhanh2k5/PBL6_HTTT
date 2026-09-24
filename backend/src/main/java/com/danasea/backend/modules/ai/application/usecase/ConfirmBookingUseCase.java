package com.danasea.backend.modules.ai.application.usecase;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemDto;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CancelBookingHoldCommand;
import com.danasea.backend.modules.booking.application.dtos.CreateBookingHoldCommand;
import com.danasea.backend.modules.booking.application.usecases.CancelBookingHoldUseCase;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("aiConfirmBookingUseCase")
public class ConfirmBookingUseCase {

    private final ConfirmationCardStorePort cardStorePort;
    private final GetPublicServiceDetailUseCase getServiceDetailUseCase;
    private final StringRedisTemplate redisTemplate;
    private final CreateBookingHoldUseCase createBookingHoldUseCase;
    private final CancelBookingHoldUseCase cancelBookingHoldUseCase;
    private final ServiceAvailabilityPort serviceAvailabilityPort;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    private static final String RETRY_KEY_PREFIX = "ai:booking:retries:";
    private static final Duration HOLD_TTL = Duration.ofMinutes(15);
    private static final Duration PROCESSING_LOCK_TTL = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 2;
    private static final String REASON_PRICE_CHANGED = "PRICE_CHANGED";
    private static final String REASON_SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE";

    @Autowired
    public ConfirmBookingUseCase(ConfirmationCardStorePort cardStorePort,
                                 GetPublicServiceDetailUseCase getServiceDetailUseCase,
                                 @Autowired(required = false) StringRedisTemplate redisTemplate,
                                 CreateBookingHoldUseCase createBookingHoldUseCase,
                                 CancelBookingHoldUseCase cancelBookingHoldUseCase,
                                 ServiceAvailabilityPort serviceAvailabilityPort) {
        this.cardStorePort = cardStorePort;
        this.getServiceDetailUseCase = getServiceDetailUseCase;
        this.redisTemplate = redisTemplate;
        this.createBookingHoldUseCase = createBookingHoldUseCase;
        this.cancelBookingHoldUseCase = cancelBookingHoldUseCase;
        this.serviceAvailabilityPort = serviceAvailabilityPort;
    }

    public ConfirmBookingUseCase(ConfirmationCardStorePort cardStorePort,
                                 GetPublicServiceDetailUseCase getServiceDetailUseCase,
                                 StringRedisTemplate redisTemplate) {
        this(cardStorePort, getServiceDetailUseCase, redisTemplate, null, null, null);
    }

    public ConfirmBookingUseCase(ConfirmationCardStorePort cardStorePort,
                                 GetPublicServiceDetailUseCase getServiceDetailUseCase) {
        this(cardStorePort, getServiceDetailUseCase, null, null, null, null);
    }

    public Map<String, Object> execute(String cardId, UUID userId, String sessionId) {
        return execute(cardId, userId, sessionId, null);
    }

    public Map<String, Object> execute(
            String cardId,
            UUID userId,
            String sessionId,
            UUID expectedConversationId) {
        String processingLockToken = null;
        if (createBookingHoldUseCase != null) {
            processingLockToken = cardStorePort.tryAcquireProcessingLock(cardId, PROCESSING_LOCK_TTL)
                    .orElseThrow(() -> new IllegalStateException("Confirmation card is already being processed"));
        }
        try {
            return executeLocked(cardId, userId, sessionId, expectedConversationId);
        } finally {
            if (processingLockToken != null) {
                cardStorePort.releaseProcessingLock(cardId, processingLockToken);
            }
        }
    }

    private Map<String, Object> executeLocked(
            String cardId,
            UUID userId,
            String sessionId,
            UUID expectedConversationId) {
        ConfirmationCard card = cardStorePort.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation card not found or expired"));

        if (expectedConversationId != null && !expectedConversationId.equals(card.getConversationId())) {
            throw new AccessDeniedException("Confirmation card does not belong to this conversation");
        }

        if (!ConfirmationCard.STATUS_PENDING.equals(card.getStatus())) {
            throw new IllegalStateException("Card is not in PENDING state");
        }
        SupportedLanguage language = SupportedLanguage.fromTag(card.getLocale()).orElse(null);

        // Re-validate price and slot via get_service_detail
        ServiceDetailResult serviceDetail = getServiceDetailUseCase.execute(card.getServiceId(), userId, sessionId);

        BigDecimal currentPrice = serviceDetail.getPromotionalPrice() != null 
                ? serviceDetail.getPromotionalPrice() 
                : serviceDetail.getPrice();
        if (currentPrice == null) {
            currentPrice = BigDecimal.ZERO;
        }

        BigDecimal cardPrice = card.getPrice() != null ? card.getPrice() : BigDecimal.ZERO;
        boolean priceChanged = currentPrice.compareTo(cardPrice) != 0;
        
        // Slot validation (if date/slot is in availableSlots)
        boolean hasSlots = serviceDetail.getAvailableSlots() != null && !serviceDetail.getAvailableSlots().isEmpty();
        boolean slotAvailable = hasSlots && card.getDate() != null && serviceDetail.getAvailableSlots().contains(card.getDate());

        // Check if slots are completely exhausted
        if (!hasSlots) {
            card.markCancelled();
            cardStorePort.save(card);
            Map<String, Object> outOfStockResponse = new HashMap<>();
            outOfStockResponse.put("status", "out_of_stock");
            outOfStockResponse.put("message", localizedOrLegacy(
                    "ai.booking.out_of_stock",
                    "The selected slot or service is no longer available. Please choose another date.", language));
            outOfStockResponse.put("cardId", cardId);
            return outOfStockResponse;
        }

        if (priceChanged || !slotAvailable) {
            // Check retry limit from Redis / card (enforce max 2 retries; fail on 3rd attempt)
            int currentRetries = getRetryCount(card);

            if (currentRetries >= MAX_RETRIES) {
                card.markCancelled();
                cardStorePort.save(card);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", localizedOrLegacy(
                        "ai.booking.too_many_changes",
                        "Service details have changed too many times. Please start over.", language));
                return errorResponse;
            }

            int nextRetry = currentRetries + 1;
            recordRetryCount(card, nextRetry);

            // Build a new card and return alternative
            card.markCancelled();
            cardStorePort.save(card); // Update old card status

            String alternativeDate = !slotAvailable && hasSlots ? serviceDetail.getAvailableSlots().get(0) : card.getDate();
            UUID alternativeSlotId = card.getSlotId();
            if (!slotAvailable && serviceAvailabilityPort != null) {
                alternativeSlotId = serviceAvailabilityPort
                        .findAvailableSlotId(card.getServiceId(), alternativeDate)
                        .orElse(null);
            }
            String reason = priceChanged ? REASON_PRICE_CHANGED : REASON_SLOT_UNAVAILABLE;

            ConfirmationCard newCard = ConfirmationCard.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(card.getConversationId())
                    .serviceId(card.getServiceId())
                    .slotId(alternativeSlotId)
                    .price(currentPrice)
                    .date(alternativeDate)
                    .quantity(card.getQuantity())
                    .status(ConfirmationCard.STATUS_PENDING)
                    .createdAt(LocalDateTime.now())
                    .retryCount(nextRetry)
                    .reason(reason)
                    .locale(card.getLocale())
                    .build();

            cardStorePort.save(newCard);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "alternative_needed");
            response.put("message", localizedOrLegacy(
                    "ai.booking.changed",
                    "The price or slot has changed. Please confirm the new details.", language));
            response.put("oldCardId", cardId);
            response.put("newCard", newCard);
            response.put("reason", reason);
            return response;
        }

        BookingHoldResult hold = null;
        if (createBookingHoldUseCase != null) {
            if (userId == null) {
                throw new AccessDeniedException("User is not authenticated");
            }
            if (card.getSlotId() == null) {
                throw new IllegalStateException("Confirmation card does not reference an available service slot");
            }
            hold = createBookingHoldUseCase.execute(new CreateBookingHoldCommand(
                    userId,
                    java.util.List.of(new BookingHoldItemDto(card.getSlotId(), card.getQuantity()))));
        }

        try {
            card.markConfirmed();
            cardStorePort.save(card);
        } catch (RuntimeException exception) {
            if (hold != null && cancelBookingHoldUseCase != null) {
                try {
                    cancelBookingHoldUseCase.execute(new CancelBookingHoldCommand(hold.bookingId(), userId));
                } catch (RuntimeException compensationFailure) {
                    exception.addSuppressed(compensationFailure);
                }
            }
            throw exception;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", localizedOrLegacy("ai.booking.success",
                hold == null
                        ? "Booking confirmation accepted."
                        : "Booking inventory is held. Complete payment before the hold expires.",
                language));
        response.put("cardId", cardId);
        if (hold != null) {
            response.put("bookingId", hold.bookingId());
            response.put("bookingStatus", hold.status());
            response.put("holdExpiresAt", hold.holdExpiresAt());
            response.put("totalAmount", hold.totalAmount());
        }
        return response;
    }

    private int getRetryCount(ConfirmationCard card) {
        if (redisTemplate != null && card.getConversationId() != null) {
            try {
                String val = redisTemplate.opsForValue().get(RETRY_KEY_PREFIX + card.getConversationId());
                if (val != null) {
                    return Integer.parseInt(val);
                }
            } catch (Exception e) {
                log.warn("Failed to get retry count from Redis", e);
            }
        }
        return card.getRetryCount();
    }

    private void recordRetryCount(ConfirmationCard card, int nextRetry) {
        if (redisTemplate != null && card.getConversationId() != null) {
            try {
                redisTemplate.opsForValue().set(RETRY_KEY_PREFIX + card.getConversationId(), String.valueOf(nextRetry), HOLD_TTL);
            } catch (Exception e) {
                log.warn("Failed to record retry count in Redis", e);
            }
        }
    }

    private String localizedOrLegacy(String key, String legacy, SupportedLanguage language) {
        return language == null ? legacy : messages.get(key, language);
    }
}
