package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.application.port.ConfirmationOutcomePort;
import com.danasea.backend.modules.ai.application.tool.RequestBookingConfirmationTool;
import com.danasea.backend.modules.ai.application.tool.ToolExecutionContext;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemDto;
import com.danasea.backend.modules.booking.application.dtos.CreateBookingHoldCommand;
import com.danasea.backend.modules.booking.application.usecases.CancelBookingHoldUseCase;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi;
import com.danasea.backend.security.authorization.BaseSecurityIntegrationTest;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.scheduler.enabled=false")
class DurableAiConfirmationIntegrationTest extends BaseSecurityIntegrationTest {
    public static final class UnserializableOutcome {
        public String getValue() { throw new IllegalStateException("Simulated outcome serialization failure"); }
    }
    @Autowired @Qualifier("aiConfirmBookingUseCase") ConfirmBookingUseCase confirm;
    @Autowired RequestBookingConfirmationTool requests;
    @Autowired ConfirmationOutcomePort outcomes;
    @Autowired CreateBookingHoldUseCase nativeHolds;
    @Autowired CancelBookingHoldUseCase nativeCancel;
    @Autowired AiCatalogReadApi catalog;
    @Autowired JdbcTemplate jdbc;
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper mapper;
    @MockitoBean ConfirmationCardStorePort cache;
    private final Map<String, ConfirmationCard> cards = new ConcurrentHashMap<>();
    private UUID owner, vendor, conversation, service, option, slot;
    private String cardId;
    private final LocalDate date = LocalDate.now(TravelContext.ZONE).plusDays(1);

    @BeforeEach void setup() throws Exception {
        owner = account("CUSTOMER"); vendor = account("VENDOR");
        conversation = UUID.randomUUID(); service = UUID.randomUUID(); option = UUID.randomUUID(); slot = UUID.randomUUID();
        jdbc.update("insert into ai_conversations(id,user_id,locale,started_at,created_at,updated_at) values(?,?,'en',now(),now(),now())", conversation, owner);
        jdbc.update("insert into services(id,vendor_id,name,description,status,weather_sensitive,duration_minutes,created_at,updated_at) values(?,?,'Private kayak','Native confirmation evidence','PUBLISHED',false,60,now(),now())", service, vendor);
        jdbc.update("insert into service_options(id,service_id,name,option_type,pricing_unit,price,max_pax_per_package,status) values(?,?,'Private two-person package','PRIVATE','PER_PACKAGE',300000,2,'ACTIVE')", option, service);
        jdbc.update("insert into service_slots(id,service_id,date,start_time,end_time,capacity,booked_count,status,inventory_type,created_at,updated_at) values(?,?,?,'09:00','10:00',6,0,'OPEN','SHARED_CAPACITY_UNITS',now(),now())", slot, service, date);
        for (int unit = 1; unit <= 3; unit++) jdbc.update("insert into service_slot_units(id,slot_id,unit_number,capacity,booked_count) values(?,?,?,2,0)", UUID.randomUUID(), slot, unit);
        when(cache.findById(anyString())).thenAnswer(call -> Optional.ofNullable(cards.get(call.getArgument(0))).map(this::copy));
        // Intentionally grant every cache lease: PostgreSQL must serialize without Redis fencing.
        when(cache.tryAcquireProcessingLock(anyString(), any())).thenAnswer(call -> Optional.of(UUID.randomUUID().toString()));
        doAnswer(call -> { ConfirmationCard card = call.getArgument(0); cards.put(card.getId(), copy(card)); return null; }).when(cache).save(any());
        var result = mapper.readTree(requests.execute("""
                {"service_id":"%s","option_id":"%s","slot_id":"%s","date":"%s","participants":3}
                """.formatted(service, option, slot, date), new ToolExecutionContext(SupportedLanguage.EN, conversation, owner)));
        assertThat(result.path("status").asText()).isEqualTo("CONFIRMATION_PENDING");
        assertThat(result.path("participantsPerPackage").toString()).isEqualTo("[2,1]");
        assertThat(result.path("groupingRequiresConfirmation").asBoolean()).isTrue();
        cardId = result.path("cardId").asText();
    }

    private UUID account(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into users(id,email,role,full_name,is_email_verified,is_locked,created_at,updated_at) values(?,?,?,'Confirmation test',true,false,now(),now())", id, id + "@confirmation.test", role);
        return id;
    }

    private ConfirmationCard copy(ConfirmationCard card) {
        try { return mapper.readValue(mapper.writeValueAsString(card), ConfirmationCard.class); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private int bookings() { return jdbc.queryForObject("select count(*) from bookings where customer_id=?", Integer.class, owner); }
    private int ledgers() { return jdbc.queryForObject("select count(*) from ai_confirmation_outcomes where card_id=?", Integer.class, cardId); }
    private long redisHolds() { return redis.opsForZSet().zCard("inventory:slot:" + slot + ":holds"); }
    private Map<String, Object> confirm() { return confirm.execute(cardId, owner, "test-session", conversation); }

    @Test void explicitPackageGroupingUsesRealNativeRulesAndPersistsOneAtomicOutcome() {
        var result = confirm();
        UUID booking = UUID.fromString(result.get("bookingId").toString());
        assertThat(result.get("status")).isEqualTo("success");
        assertThat(bookings()).isEqualTo(1); assertThat(ledgers()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select state from ai_confirmation_outcomes where card_id=?", String.class, cardId)).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("select total_amount from bookings where id=?", BigDecimal.class, booking)).isEqualByComparingTo("600000");
        assertThat(jdbc.queryForList("select participants_count from booking_items where booking_id=? order by participants_count desc", Integer.class, booking)).containsExactly(2, 1);
        assertThat(jdbc.queryForObject("select sum(quantity) from booking_items where booking_id=?", Integer.class, booking)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(distinct a.unit_number) from booking_item_allocations a join booking_items i on i.id=a.booking_item_id where i.booking_id=?", Integer.class, booking)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from booking_item_allocations a join booking_items i on i.id=a.booking_item_id join service_slot_units u on u.slot_id=a.slot_id and u.unit_number=a.unit_number where i.booking_id=? and a.allocated_seats>u.capacity", Integer.class, booking)).isZero();
    }

    @Test void fiftyParticipantsUseExplicitPackagesWithinNativeItemLimit() throws Exception {
        jdbc.update("update service_slots set capacity=50 where id=?", slot);
        for (int unit=4; unit<=25; unit++) jdbc.update("insert into service_slot_units(id,slot_id,unit_number,capacity,booked_count) values(?,?,?,2,0)", UUID.randomUUID(), slot, unit);
        var pending = mapper.readTree(requests.execute("""
                {"service_id":"%s","option_id":"%s","slot_id":"%s","date":"%s","participants":50}
                """.formatted(service, option, slot, date), new ToolExecutionContext(SupportedLanguage.EN, conversation, owner)));
        assertThat(pending.path("status").asText()).isEqualTo("CONFIRMATION_PENDING");
        assertThat(pending.path("participantsPerPackage")).hasSize(25);
        cardId = pending.path("cardId").asText();
        var result = confirm();
        UUID booking = UUID.fromString(result.get("bookingId").toString());
        assertThat(jdbc.queryForObject("select total_amount from bookings where id=?", BigDecimal.class, booking)).isEqualByComparingTo("7500000");
        assertThat(jdbc.queryForObject("select count(*) from booking_items where booking_id=?", Integer.class, booking)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select sum(quantity*participants_count) from booking_items where booking_id=?", Integer.class, booking)).isEqualTo(50);
        assertThat(jdbc.queryForObject("select count(distinct a.unit_number) from booking_item_allocations a join booking_items i on i.id=a.booking_item_id where i.booking_id=?", Integer.class, booking)).isEqualTo(25);
    }

    @Test void cacheLossAndNewApplicationInstanceReplayOriginalOutcomeWithoutExtendingHold() throws Exception {
        var first = confirm(); UUID booking = UUID.fromString(first.get("bookingId").toString());
        OffsetDateTime expiry = jdbc.queryForObject("select hold_expires_at from bookings where id=?", OffsetDateTime.class, booking);
        cards.clear();
        jdbc.update("update service_options set price=900000 where id=?", option);
        jdbc.update("update service_slots set status='CLOSED' where id=?", slot);
        var restarted = new ConfirmBookingUseCase(cache, null, null, nativeHolds, nativeCancel, null, catalog);
        restarted.setDurableOutcomes(outcomes);
        var replay = restarted.execute(cardId, owner, "another-session", conversation);
        assertThat(mapper.readTree(mapper.writeValueAsString(replay))).isEqualTo(mapper.readTree(mapper.writeValueAsString(first)));
        assertThat(bookings()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select hold_expires_at from bookings where id=?", OffsetDateTime.class, booking)).isEqualTo(expiry);
    }

    @Test void concurrentConfirmationsRemainSingleHoldWhenEveryRedisLeaseIsGranted() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return confirm(); });
            var second = executor.submit(() -> { start.await(); return confirm(); });
            start.countDown();
            var one = first.get(20, TimeUnit.SECONDS); var two = second.get(20, TimeUnit.SECONDS);
            assertThat(one.get("bookingId").toString()).isEqualTo(two.get("bookingId").toString());
        }
        assertThat(bookings()).isEqualTo(1); assertThat(ledgers()).isEqualTo(1);
    }

    @Test void durableReplayRejectsOtherCustomerConversationAndChangedCardFingerprint() {
        confirm();
        assertThatThrownBy(() -> confirm.execute(cardId, UUID.randomUUID(), "test", conversation)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> confirm.execute(cardId, owner, "test", UUID.randomUUID())).isInstanceOf(AccessDeniedException.class);
        ConfirmationCard changed = copy(cards.get(cardId)); changed.setPrice(new BigDecimal("1"));
        assertThatThrownBy(() -> outcomes.execute(changed, owner, conversation, () -> Map.of("status", "success")))
                .isInstanceOf(AiStateConflictException.class);
        assertThat(bookings()).isEqualTo(1);
    }

    @Test void cacheWriteFailureRollsBackHoldAndLedgerAndReleasesNativeRedisInventory() {
        AtomicBoolean failOnce = new AtomicBoolean(true);
        doAnswer(call -> {
            ConfirmationCard card = call.getArgument(0);
            if (ConfirmationCard.STATUS_CONFIRMED.equals(card.getStatus()) && failOnce.getAndSet(false))
                throw new IllegalStateException("Simulated confirmation cache outage");
            cards.put(card.getId(), copy(card)); return null;
        }).when(cache).save(any());
        assertThatThrownBy(this::confirm).isInstanceOf(IllegalStateException.class);
        assertThat(bookings()).isZero(); assertThat(ledgers()).isZero(); assertThat(redisHolds()).isZero();
        assertThat(confirm().get("status")).isEqualTo("success"); assertThat(bookings()).isEqualTo(1);
    }

    @Test void staleConfirmedCacheWithoutDurableSuccessCreatesNoHold() {
        var cached = cards.get(cardId); cached.markConfirmed();
        cached.setConfirmedBy(owner); cached.setConfirmationOutcome(Map.of("status", "success", "bookingId", UUID.randomUUID()));
        assertThatThrownBy(this::confirm).isInstanceOf(AiStateConflictException.class);
        assertThat(bookings()).isZero(); assertThat(ledgers()).isZero(); assertThat(redisHolds()).isZero();
    }

    @Test void missingOrIncorrectProposedGroupingRequiresFreshCustomerConfirmation() {
        cards.get(cardId).setParticipantsPerPackage(List.of(2, 2));
        assertThat(confirm().get("status")).isEqualTo("refresh_required");
        assertThat(bookings()).isZero(); assertThat(redisHolds()).isZero();
        assertThat(jdbc.queryForObject("select state from ai_confirmation_outcomes where card_id=?", String.class, cardId)).isEqualTo("NO_HOLD");
    }

    @Test void ledgerSerializationFailureRollsBackActualNativeHoldInSameTransaction() {
        ConfirmationCard card = copy(cards.get(cardId));
        assertThatThrownBy(() -> outcomes.execute(card, owner, conversation, () -> {
            var hold = nativeHolds.execute(new CreateBookingHoldCommand(owner, List.of(
                    new BookingHoldItemDto(slot, 1, option, 2), new BookingHoldItemDto(slot, 1, option, 1))));
            return Map.of("status", "success", "bookingId", hold.bookingId(), "unserializable", new UnserializableOutcome());
        })).isInstanceOf(IllegalStateException.class);
        assertThat(bookings()).isZero(); assertThat(ledgers()).isZero(); assertThat(redisHolds()).isZero();
    }
}
