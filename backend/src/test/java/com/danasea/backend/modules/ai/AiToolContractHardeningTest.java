package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.application.port.ServiceSearchPort;
import com.danasea.backend.modules.ai.application.tool.GetSafetyAlertTool;
import com.danasea.backend.modules.ai.application.tool.GetWeatherForecastTool;
import com.danasea.backend.modules.ai.application.tool.RequestBookingConfirmationTool;
import com.danasea.backend.modules.ai.application.tool.SearchServiceTool;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CreateBookingHoldCommand;
import com.danasea.backend.modules.booking.application.usecases.CancelBookingHoldUseCase;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiToolContractHardeningTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bookingToolCreatesConversationOwnedConfirmationCardWithCurrentSnapshot() throws Exception {
        ConfirmationCardStorePort store = mock(ConfirmationCardStorePort.class);
        GetPublicServiceDetailUseCase detailUseCase = mock(GetPublicServiceDetailUseCase.class);
        ServiceAvailabilityPort availabilityPort = mock(ServiceAvailabilityPort.class);
        UUID conversationId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        String slot = "2026-09-25T08:00:00+07:00";
        when(detailUseCase.execute(serviceId, null, null)).thenReturn(ServiceDetailResult.builder()
                .id(serviceId)
                .price(new BigDecimal("500000"))
                .promotionalPrice(new BigDecimal("450000"))
                .availableSlots(List.of(slot))
                .build());
        when(availabilityPort.findAvailableSlotId(serviceId, slot)).thenReturn(java.util.Optional.of(slotId));
        RequestBookingConfirmationTool tool = new RequestBookingConfirmationTool(
                objectMapper, store, detailUseCase, availabilityPort);

        var response = objectMapper.readTree(tool.execute("{\"service_id\":\"" + serviceId
                + "\",\"date\":\"" + slot + "\",\"participants\":2}", conversationId));

        assertThat(response.get("status").asText()).isEqualTo("CONFIRMATION_PENDING");
        assertThat(response.get("cardId").asText()).isNotBlank();
        assertThat(response.get("price").decimalValue()).isEqualByComparingTo("450000");
        ArgumentCaptor<ConfirmationCard> cardCaptor = ArgumentCaptor.forClass(ConfirmationCard.class);
        verify(store).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getConversationId()).isEqualTo(conversationId);
        assertThat(cardCaptor.getValue().getSlotId()).isEqualTo(slotId);
        assertThat(cardCaptor.getValue().getStatus()).isEqualTo(ConfirmationCard.STATUS_PENDING);
        assertThat(cardCaptor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void bookingToolRejectsUnavailableSlotWithoutCreatingCard() throws Exception {
        ConfirmationCardStorePort store = mock(ConfirmationCardStorePort.class);
        GetPublicServiceDetailUseCase detailUseCase = mock(GetPublicServiceDetailUseCase.class);
        ServiceAvailabilityPort availabilityPort = mock(ServiceAvailabilityPort.class);
        UUID serviceId = UUID.randomUUID();
        when(detailUseCase.execute(serviceId, null, null)).thenReturn(ServiceDetailResult.builder()
                .id(serviceId)
                .price(BigDecimal.TEN)
                .availableSlots(List.of("another-slot"))
                .build());
        RequestBookingConfirmationTool tool = new RequestBookingConfirmationTool(
                objectMapper, store, detailUseCase, availabilityPort);

        var response = objectMapper.readTree(tool.execute("{\"service_id\":\"" + serviceId
                + "\",\"date\":\"requested-slot\",\"participants\":1}", UUID.randomUUID()));

        assertThat(response.get("error").asText()).isEqualTo("SLOT_UNAVAILABLE");
        verify(store, never()).save(any());
    }

    @Test
    void confirmingAiCardCreatesRealInventoryHoldAndReturnsBookingId() {
        ConfirmationCardStorePort store = mock(ConfirmationCardStorePort.class);
        GetPublicServiceDetailUseCase detailUseCase = mock(GetPublicServiceDetailUseCase.class);
        CreateBookingHoldUseCase createHold = mock(CreateBookingHoldUseCase.class);
        CancelBookingHoldUseCase cancelHold = mock(CancelBookingHoldUseCase.class);
        ServiceAvailabilityPort availabilityPort = mock(ServiceAvailabilityPort.class);
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String slot = "2026-09-25T08:00:00";
        ConfirmationCard card = ConfirmationCard.builder()
                .id("card-1")
                .conversationId(conversationId)
                .serviceId(serviceId)
                .slotId(slotId)
                .price(new BigDecimal("450000"))
                .date(slot)
                .quantity(2)
                .status(ConfirmationCard.STATUS_PENDING)
                .build();
        when(store.tryAcquireProcessingLock(eq("card-1"), any())).thenReturn(Optional.of("lock-token"));
        when(store.findById("card-1")).thenReturn(Optional.of(card));
        when(detailUseCase.execute(serviceId, userId, "session-1")).thenReturn(ServiceDetailResult.builder()
                .id(serviceId)
                .price(new BigDecimal("450000"))
                .availableSlots(List.of(slot))
                .build());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(15);
        when(createHold.execute(any(CreateBookingHoldCommand.class))).thenReturn(new BookingHoldResult(
                bookingId, userId, BookingStatus.HOLD, new BigDecimal("900000"), expiresAt, List.of(), OffsetDateTime.now()));
        ConfirmBookingUseCase useCase = new ConfirmBookingUseCase(
                store, detailUseCase, null, createHold, cancelHold, availabilityPort);

        var response = useCase.execute("card-1", userId, "session-1", conversationId);

        assertThat(response.get("bookingId")).isEqualTo(bookingId);
        assertThat(response.get("bookingStatus")).isEqualTo(BookingStatus.HOLD);
        assertThat(card.getStatus()).isEqualTo(ConfirmationCard.STATUS_CONFIRMED);
        ArgumentCaptor<CreateBookingHoldCommand> commandCaptor = ArgumentCaptor.forClass(CreateBookingHoldCommand.class);
        verify(createHold).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(userId);
        assertThat(commandCaptor.getValue().items().getFirst().slotId()).isEqualTo(slotId);
        verify(store).releaseProcessingLock("card-1", "lock-token");
    }

    @Test
    void searchToolRejectsInvalidPriceRangeAndDoesNotExposeProviderErrors() throws Exception {
        ServiceSearchPort searchPort = mock(ServiceSearchPort.class);
        SearchServiceTool tool = new SearchServiceTool(searchPort, objectMapper);

        var invalid = objectMapper.readTree(tool.execute("{\"min_price\":20,\"max_price\":10}"));
        assertThat(invalid.get("error").asText()).isEqualTo("INVALID_ARGUMENTS");
        verify(searchPort, never()).exactAndFilterSearch(any(), any(), any(), any());

        when(searchPort.exactAndFilterSearch(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/private"));
        String unavailable = tool.execute("{\"query\":\"kayak\"}");
        assertThat(unavailable).contains("SEARCH_UNAVAILABLE").doesNotContain("secret-host");
    }

    @Test
    void futureForecastUsesRequestedCoordinatesAndDate() throws Exception {
        GetWeatherInfoUseCase weatherUseCase = mock(GetWeatherInfoUseCase.class);
        WeatherProviderPort provider = mock(WeatherProviderPort.class);
        LocalDate date = LocalDate.now().plusDays(2);
        when(provider.getTimeWindowForecast(eq(16.1), eq(108.2), eq(date), eq(LocalTime.MIN), eq(LocalTime.MAX)))
                .thenReturn(WeatherInfoDto.TimeWindowForecast.builder().peakWaveHeight(1.2).peakWindSpeed(18.0).build());
        GetWeatherForecastTool tool = new GetWeatherForecastTool(weatherUseCase, null, objectMapper, provider);

        var response = objectMapper.readTree(tool.execute("{\"location\":\"Custom point\",\"date\":\""
                + date + "\",\"latitude\":16.1,\"longitude\":108.2}"));

        assertThat(response.get("available").asBoolean()).isTrue();
        assertThat(response.get("peakWaveHeight").asDouble()).isEqualTo(1.2);
        verify(provider).getTimeWindowForecast(16.1, 108.2, date, LocalTime.MIN, LocalTime.MAX);
        verify(weatherUseCase, never()).execute(anyDouble(), anyDouble());
    }

    @Test
    void safetyToolUsesCoordinatesResolvedFromRequestedLocation() throws Exception {
        GetWeatherInfoUseCase weatherUseCase = mock(GetWeatherInfoUseCase.class);
        when(weatherUseCase.execute(16.1067, 108.2774)).thenReturn(WeatherInfoDto.builder()
                .marine(WeatherInfoDto.MarineData.builder().waveHeight(0.5).build())
                .build());
        GetSafetyAlertTool tool = new GetSafetyAlertTool(
                weatherUseCase, null, objectMapper, new WeatherRuleEngine());

        var response = objectMapper.readTree(tool.execute("{\"location\":\"Son Tra\"}"));

        assertThat(response.get("latitude").asDouble()).isEqualTo(16.1067);
        assertThat(response.get("isSafe").asBoolean()).isTrue();
        verify(weatherUseCase).execute(16.1067, 108.2774);
    }
}
