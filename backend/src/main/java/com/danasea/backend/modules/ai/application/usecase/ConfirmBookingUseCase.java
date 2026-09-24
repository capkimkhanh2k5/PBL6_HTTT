package com.danasea.backend.modules.ai.application.usecase;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@Slf4j
@Service("aiConfirmBookingUseCase")
public class ConfirmBookingUseCase {

    private final ConfirmationCardStorePort cardStorePort;
    private final GetPublicServiceDetailUseCase getServiceDetailUseCase;
    private final StringRedisTemplate redisTemplate;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    private static final String RETRY_KEY_PREFIX = "ai:booking:retries:";
    private static final Duration HOLD_TTL = Duration.ofMinutes(15);
    private static final int MAX_RETRIES = 2;
    private static final String REASON_PRICE_CHANGED = "PRICE_CHANGED";
    private static final String REASON_SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE";

    @Autowired
    public ConfirmBookingUseCase(ConfirmationCardStorePort cardStorePort,
                                 GetPublicServiceDetailUseCase getServiceDetailUseCase,
                                 @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.cardStorePort = cardStorePort;
        this.getServiceDetailUseCase = getServiceDetailUseCase;
        this.redisTemplate = redisTemplate;
    }

    public ConfirmBookingUseCase(ConfirmationCardStorePort cardStorePort,
                                 GetPublicServiceDetailUseCase getServiceDetailUseCase) {
        this(cardStorePort, getServiceDetailUseCase, null);
    }

    public Map<String, Object> execute(String cardId, UUID userId, String sessionId) {
        ConfirmationCard card = cardStorePort.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Confirmation card not found or expired"));

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
                    "Khung giờ hoặc dịch vụ đã hết chỗ. Vui lòng chọn ngày khác.", language));
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
            String reason = priceChanged ? REASON_PRICE_CHANGED : REASON_SLOT_UNAVAILABLE;

            ConfirmationCard newCard = ConfirmationCard.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(card.getConversationId())
                    .serviceId(card.getServiceId())
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

        // If no change, proceed to hold()
        card.markConfirmed();
        cardStorePort.save(card);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", localizedOrLegacy(
                "ai.booking.success", "Booking confirmed and held successfully.", language));
        response.put("cardId", cardId);
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
