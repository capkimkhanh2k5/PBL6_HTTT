package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import com.danasea.backend.modules.ai.application.port.DecisionModelPort;
import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.application.port.RateLimiterPort;
import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.application.usecase.PlanItineraryUseCase;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.infrastructure.jobs.ItinerarySourceMonitoringJob;
import com.danasea.backend.modules.service.application.api.AiItinerarySlotReadApi;
import com.danasea.backend.modules.weather.application.api.AiItineraryAlertReadApi;
import com.danasea.backend.security.authorization.BaseSecurityIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@Transactional
class ItineraryLifecycleHttpIntegrationTest extends BaseSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired ItineraryStorePort store;
    @Autowired PlanItineraryUseCase planner;
    @Autowired AiItinerarySlotReadApi slotSignals;
    @Autowired AiItineraryAlertReadApi alertSignals;
    @MockitoBean DecisionModelPort decisions;
    @MockitoBean TravelWeatherPort weather;
    @MockitoBean RateLimiterPort limiter;
    private UUID owner, other, firstSlot, secondSlot;
    private final LocalDate date = LocalDate.now(TravelContext.ZONE).plusDays(1);

    @BeforeEach void setup() {
        owner = account(); other = account();
        firstSlot = service("First coastal activity", 9);
        secondSlot = service("Second coastal activity", 12);
        when(decisions.decide(any(), any())).thenAnswer(call -> DecisionResult.unavailable(call.getArgument(0), "TEST_OFFLINE"));
        when(weather.assess(any(), any())).thenReturn(new TravelWeatherPort.Assessment("NOT_REQUIRED", true, false, null, null, null));
        when(limiter.isAllowed(any(), any(), any())).thenReturn(true);
    }

    private UUID account() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into users(id,email,role,full_name,is_email_verified,is_locked,created_at,updated_at) values(?,?,'CUSTOMER','AI test',true,false,now(),now())", id, id + "@example.test");
        return id;
    }

    private UUID service(String name, int hour) {
        UUID serviceId = UUID.randomUUID(), optionId = UUID.randomUUID(), slotId = UUID.randomUUID();
        jdbc.update("insert into services(id,name,description,status,weather_sensitive,duration_minutes,latitude,longitude,created_at,updated_at) values(?,?,'Coastal trip','PUBLISHED',false,60,16.1,108.2,now(),now())", serviceId, name);
        jdbc.update("insert into service_options(id,service_id,name,option_type,pricing_unit,price,status) values(?,?,'Standard','SHARED','PER_PERSON',100000,'ACTIVE')", optionId, serviceId);
        jdbc.update("insert into service_slots(id,service_id,date,start_time,end_time,capacity,booked_count,status,inventory_type,created_at,updated_at) values(?,?,?,?,?,10,0,'OPEN','PERSON_LIMIT',now(),now())", slotId, serviceId, date, LocalTime.of(hour,0), LocalTime.of(hour+1,0));
        return slotId;
    }

    private String criteria() { return "{\"query\":\"\",\"partySize\":1,\"from\":\"" + date + "\",\"to\":\"" + date + "\",\"maxActivities\":2}"; }
    private JsonNode plan() throws Exception {
        return mapper.readTree(mvc.perform(post("/api/ai/itineraries").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(criteria())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void previewSaveIsPrivateIdempotentAndRequiresAnExplicitAlternative() throws Exception {
        JsonNode preview = mapper.readTree(mvc.perform(post("/api/ai/itineraries/preview").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(criteria())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String route = "/api/ai/itineraries/previews/" + preview.path("id").asText() + "/save";
        String choice = "{\"alternativeId\":\"alternative-1\"}";
        mvc.perform(post(route).with(user(other.toString()).roles("CUSTOMER")).header("Idempotency-Key","private-save-1")
                .contentType(MediaType.APPLICATION_JSON).content(choice)).andExpect(status().isNotFound());
        mvc.perform(post(route).with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(choice)).andExpect(status().isBadRequest());
        JsonNode saved = mapper.readTree(mvc.perform(post(route).with(user(owner.toString()).roles("CUSTOMER")).header("Idempotency-Key","private-save-1")
                .contentType(MediaType.APPLICATION_JSON).content(choice)).andExpect(status().isOk()).andExpect(jsonPath("$.lifecycle").value("DRAFT"))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post(route).with(user(owner.toString()).roles("CUSTOMER")).header("Idempotency-Key","private-save-1")
                .contentType(MediaType.APPLICATION_JSON).content(choice)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saved.path("id").asText()));
        mvc.perform(post(route).with(user(owner.toString()).roles("CUSTOMER")).header("Idempotency-Key","private-save-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"alternativeId\":\"alternative-2\"}")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from ai_itineraries where owner_id=?", Integer.class, owner)).isEqualTo(1);
    }

    @Test void acceptingProposalUsesCasAndRecordsRevisionWithoutCreatingBookings() throws Exception {
        JsonNode saved = plan();
        String route = "/api/ai/itineraries/" + saved.path("id").asText();
        mvc.perform(post(route + "/accept").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycle").value("ACCEPTED")).andExpect(jsonPath("$.version").value(1));
        JsonNode proposed = mapper.readTree(mvc.perform(post(route + "/replan").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1,\"trigger\":\"WEATHER_RED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itinerary.version").value(1))
                .andExpect(jsonPath("$.proposal.trigger").value("CUSTOMER_REQUEST"))
                .andReturn().getResponse().getContentAsString());
        String accept = route + "/proposals/" + proposed.path("proposal").path("id").asText() + "/accept";
        mvc.perform(post(accept).with(user(other.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}")).andExpect(status().isNotFound());
        mvc.perform(post(accept).with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.bookingChanged").value(false));
        mvc.perform(post(accept).with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}")).andExpect(status().isConflict());
        mvc.perform(get(route + "/revisions").with(user(owner.toString()).roles("CUSTOMER"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test void persistedSlotEventMarksStaleAndCreatesOneProposalPreservingUnaffectedItems() throws Exception {
        JsonNode saved = plan();
        UUID id = UUID.fromString(saved.path("id").asText());
        planner.accept(id, owner, 0, com.danasea.backend.shared.i18n.SupportedLanguage.VI);
        var original = planner.get(id, owner).plan();
        UUID orderId = UUID.randomUUID();
        jdbc.update("insert into master_orders(id,customer_id,total_amount,status,created_at,updated_at) values(?,?,200000,'PAID',now(),now())", orderId, owner);
        jdbc.update("update service_slots set status='CLOSED',updated_at=now() where id=?", firstSlot);
        ItinerarySourceMonitoringJob job = new ItinerarySourceMonitoringJob(store, slotSignals, alertSignals, planner);
        job.check();
        var stale = planner.get(id, owner);
        assertThat(stale.lifecycle()).isEqualTo("STALE");
        assertThat(stale.version()).isEqualTo(2);
        assertThat(stale.plan()).isEqualTo(original);
        var proposed = planner.proposals(id, owner);
        assertThat(proposed).hasSize(1);
        assertThat(proposed.getFirst().trigger()).isEqualTo("CANONICAL_SLOT_UNAVAILABLE");
        assertThat(proposed.getFirst().removedSlotIds()).containsExactly(firstSlot);
        assertThat(proposed.getFirst().plan().items()).containsExactly(original.items().stream().filter(item -> item.slotId().equals(secondSlot)).findFirst().orElseThrow());
        job.check();
        assertThat(planner.proposals(id, owner)).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id=? and type='AI_ITINERARY_SOURCE_CHANGED'", Integer.class, owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from master_orders where id=?", String.class, orderId)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select total_amount from master_orders where id=?", java.math.BigDecimal.class, orderId)).isEqualByComparingTo("200000");
        assertThat(jdbc.queryForObject("select count(*) from ai_itinerary_items where itinerary_id=?", Integer.class, id)).isEqualTo(2);
    }

    @Test void archiveStopsMonitoringAndRejectsLaterStateChanges() throws Exception {
        JsonNode saved = plan();
        String route = "/api/ai/itineraries/" + saved.path("id").asText();
        mvc.perform(post(route + "/archive").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycle").value("ARCHIVED"));
        mvc.perform(post(route + "/accept").with(user(owner.toString()).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}")).andExpect(status().isConflict());
        assertThat(store.trackedSlotIds(0, 100)).doesNotContain(firstSlot, secondSlot);
    }
}
