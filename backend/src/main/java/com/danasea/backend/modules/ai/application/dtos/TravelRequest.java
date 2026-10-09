package com.danasea.backend.modules.ai.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.domain.services.TravelQueryParser;

public record TravelRequest(String query, UUID categoryId, BigDecimal totalBudget, Integer partySize,
                            LocalDate from, LocalDate to, LocalTime dayStart, LocalTime dayEnd,
                            Double latitude, Double longitude, Double radiusKm, List<String> interests,
                            Integer limit, Integer maxActivities, Boolean weatherSafeOnly) {
    public TravelContext context() {
        Integer parsedParty = TravelQueryParser.partySize(query);
        LocalDate parsedDate = TravelQueryParser.date(query);
        TravelContext context = new TravelContext(query, categoryId, totalBudget, partySize != null ? partySize : parsedParty == null ? 1 : parsedParty,
                from != null ? from : parsedDate, to != null ? to : from == null && parsedDate != null ? parsedDate : null,
                dayStart, dayEnd, latitude, longitude, radiusKm, interests,
                limit == null ? 10 : limit, maxActivities == null ? 3 : maxActivities,
                Boolean.TRUE.equals(weatherSafeOnly));
        context.validateCurrentDates();
        return context;
    }
}
