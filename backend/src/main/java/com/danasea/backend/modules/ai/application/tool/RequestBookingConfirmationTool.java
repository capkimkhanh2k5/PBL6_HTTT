package com.danasea.backend.modules.ai.application.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RequestBookingConfirmationTool implements ConversationAwareToolExecutor {

    private static final String STATUS_CONFIRMATION_PENDING = "CONFIRMATION_PENDING";
    private static final int MAX_PARTICIPANTS = 100;

    private final ObjectMapper objectMapper;
    private final ConfirmationCardStorePort cardStorePort;
    private final GetPublicServiceDetailUseCase serviceDetailUseCase;
    private final ServiceAvailabilityPort serviceAvailabilityPort;
    private final AiCatalogReadApi catalog;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    @Autowired
    public RequestBookingConfirmationTool(ObjectMapper objectMapper,
                                          ConfirmationCardStorePort cardStorePort,
                                          GetPublicServiceDetailUseCase serviceDetailUseCase,
                                          ServiceAvailabilityPort serviceAvailabilityPort,
                                          AiCatalogReadApi catalog) {
        this.objectMapper = objectMapper;
        this.cardStorePort = cardStorePort;
        this.serviceDetailUseCase = serviceDetailUseCase;
        this.serviceAvailabilityPort = serviceAvailabilityPort;
        this.catalog = catalog;
    }

    public RequestBookingConfirmationTool(ObjectMapper mapper, ConfirmationCardStorePort store,
                                          GetPublicServiceDetailUseCase details, ServiceAvailabilityPort slots) {
        this(mapper, store, details, slots, null);
    }

    public RequestBookingConfirmationTool(ObjectMapper objectMapper,
                                          ConfirmationCardStorePort cardStorePort,
                                          GetPublicServiceDetailUseCase serviceDetailUseCase) {
        this(objectMapper, cardStorePort, serviceDetailUseCase, null);
    }

    public RequestBookingConfirmationTool(ObjectMapper objectMapper) {
        this(objectMapper, null, null, null);
    }

    public RequestBookingConfirmationTool() {
        this(new ObjectMapper());
    }

    @Override
    public String getName() {
        return "request_booking_confirmation";
    }

    @Override
    public String execute(String argumentsJson) {
        return error("CONVERSATION_CONTEXT_REQUIRED", "A conversation context is required to create a confirmation card");
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        SupportedLanguage language = context == null || context.language() == null
                ? SupportedLanguage.VI
                : context.language();
        return executeInternal(argumentsJson, context == null ? null : context.conversationId(), language);
    }

    @Override
    public String execute(String argumentsJson, UUID conversationId) {
        return executeInternal(argumentsJson, conversationId, SupportedLanguage.VI);
    }

    private String executeInternal(String argumentsJson, UUID conversationId, SupportedLanguage language) {
        if (conversationId == null || cardStorePort == null || catalog == null && (serviceDetailUseCase == null
                || serviceAvailabilityPort == null)) {
            return error("CONFIRMATION_UNAVAILABLE", "Booking confirmation is temporarily unavailable");
        }

        try {
            JsonNode arguments = objectMapper.readTree(argumentsJson);
            if (arguments == null || !arguments.isObject()) {
                return error("INVALID_ARGUMENTS", "arguments must be a JSON object");
            }

            UUID serviceId = parseServiceId(arguments);
            String date = requiredText(arguments, "date", 100);
            int participants = requiredParticipants(arguments);
            if (catalog != null) return nativeConfirmation(arguments, conversationId, language, serviceId, participants, date);
            ServiceDetailResult service = serviceDetailUseCase.execute(serviceId, null, null);
            List<String> availableSlots = service.getAvailableSlots() == null ? List.of() : service.getAvailableSlots();
            if (!availableSlots.contains(date)) {
                return objectMapper.writeValueAsString(Map.of(
                        "error", "SLOT_UNAVAILABLE",
                        "message", "The requested slot is not available",
                        "availableSlots", availableSlots.stream().limit(10).toList()
                ));
            }
            UUID slotId = serviceAvailabilityPort.findAvailableSlotId(serviceId, date).orElse(null);
            if (slotId == null) {
                return error("SLOT_UNAVAILABLE", "The requested slot is no longer available");
            }

            BigDecimal price = service.getPromotionalPrice() != null
                    ? service.getPromotionalPrice()
                    : service.getPrice();
            if (price == null || price.signum() < 0) {
                return error("PRICE_UNAVAILABLE", "The service price is unavailable");
            }

            ConfirmationCard card = ConfirmationCard.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(conversationId)
                    .serviceId(serviceId)
                    .slotId(slotId)
                    .price(price)
                    .date(date)
                    .quantity(participants)
                    .status(ConfirmationCard.STATUS_PENDING)
                    .createdAt(LocalDateTime.now())
                    .locale(language.code())
                    .build();
            cardStorePort.save(card);

            return objectMapper.writeValueAsString(Map.of(
                    "status", STATUS_CONFIRMATION_PENDING,
                    "cardId", card.getId(),
                    "serviceId", serviceId,
                    "date", date,
                    "participants", participants,
                    "price", price,
                    "expiresInSeconds", 900,
                    "message", messages.get("ai.booking.confirmation", language)
            ));
        } catch (ServiceNotFoundException exception) {
            return error("SERVICE_NOT_FOUND", "Service not found or not published");
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ARGUMENTS", exception.getMessage());
        } catch (Exception exception) {
            return error("CONFIRMATION_UNAVAILABLE", "Booking confirmation is temporarily unavailable");
        }
    }

    private String nativeConfirmation(JsonNode args, UUID conversationId, SupportedLanguage language,
                                      UUID serviceId, int participants, String requestedDate) throws Exception {
        if (!args.hasNonNull("option_id") || !args.hasNonNull("slot_id")) {
            return error("OPTION_AND_SLOT_REQUIRED", "Choose an active option and a current slot before requesting confirmation");
        }
        if (participants > 50) throw new IllegalArgumentException("participants must be between 1 and 50");
        UUID optionId = UUID.fromString(requiredText(args, "option_id", 36));
        UUID slotId = UUID.fromString(requiredText(args, "slot_id", 36));
        if (requestedDate.length() < 10) throw new IllegalArgumentException("date must contain an ISO travel date");
        LocalDate from = LocalDate.parse(requestedDate.substring(0, 10));
        if (from.isBefore(LocalDate.now(TravelContext.ZONE))) throw new IllegalArgumentException("date cannot be in the past");
        var service = catalog.find(serviceId, new AiCatalogReadApi.Query(null, null, from, from, participants,
                null, null, null, 1, language));
        var option = service.options().stream().filter(item -> item.id().equals(optionId)).findFirst().orElse(null);
        if (option == null) return error("OPTION_UNAVAILABLE", "The selected option is not active for this service");
        var slot = option.slots().stream().filter(item -> item.id().equals(slotId)).findFirst().orElse(null);
        if (slot == null) return error("SLOT_UNAVAILABLE", "The selected slot is not currently bookable for the party");
        String canonicalDate = LocalDateTime.of(slot.date(), slot.start()).toString();
        ConfirmationCard card = ConfirmationCard.builder().id(UUID.randomUUID().toString()).conversationId(conversationId)
                .serviceId(serviceId).optionId(optionId).slotId(slotId).participantsCount(participants)
                .price(option.unitPrice()).quantity(option.quantity()).date(canonicalDate)
                .status(ConfirmationCard.STATUS_PENDING).createdAt(LocalDateTime.now()).locale(language.code()).build();
        cardStorePort.save(card);
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("status", STATUS_CONFIRMATION_PENDING), Map.entry("cardId", card.getId()),
                Map.entry("serviceId", serviceId), Map.entry("optionId", optionId), Map.entry("slotId", slotId),
                Map.entry("date", canonicalDate), Map.entry("participants", participants), Map.entry("quantity", option.quantity()),
                Map.entry("unitPrice", option.unitPrice()), Map.entry("price", option.unitPrice()),
                Map.entry("partyTotal", option.partyTotal()), Map.entry("expiresInSeconds", 900),
                Map.entry("message", messages.get("ai.booking.confirmation", language))));
    }

    private UUID parseServiceId(JsonNode arguments) {
        String value = requiredText(arguments, "service_id", 36);
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("service_id must be a valid UUID");
        }
    }

    private String requiredText(JsonNode arguments, String field, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.asText().trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized;
    }

    private int requiredParticipants(JsonNode arguments) {
        JsonNode value = arguments.get("participants");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException("participants must be an integer");
        }
        int participants = value.asInt();
        if (participants < 1 || participants > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("participants must be between 1 and 100");
        }
        return participants;
    }

    private String error(String code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", code, "message", message));
        } catch (Exception exception) {
            return "{\"error\":\"" + code + "\"}";
        }
    }
}
