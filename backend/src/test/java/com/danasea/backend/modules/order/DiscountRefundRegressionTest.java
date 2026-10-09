package com.danasea.backend.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.BookingCancellationFinancialAdapter;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.settlement.domain.models.SubOrderCalculationContext;
import com.danasea.backend.modules.settlement.domain.services.SettlementCalculationEngine;

class DiscountRefundRegressionTest {
    @Test
    void bookingCancellationRefundsCustomerNetAmount() {
        var orders = mock(JpaMasterOrderRepository.class);
        var subs = mock(JpaSubOrderRepository.class);
        var refunds = mock(JpaRefundRepository.class);
        var slots = mock(JpaServiceSlotRepository.class);
        UUID booking = UUID.randomUUID();
        var order = new MasterOrderJpaEntity();
        order.setId(UUID.randomUUID());
        order.setStatus(MasterOrderStatus.PAID);
        var sub = new SubOrderJpaEntity();
        sub.setId(UUID.randomUUID());
        sub.setSlotId(UUID.randomUUID());
        sub.setSubtotalAmount(new BigDecimal("100000"));
        sub.setFinalAmount(new BigDecimal("80000"));
        var slot = new ServiceSlotJpaEntity();
        slot.setDate(LocalDate.now().plusDays(4));
        slot.setStartTime(LocalTime.NOON);
        when(orders.findByBookingIdForUpdate(booking)).thenReturn(Optional.of(order));
        when(subs.findByMasterOrderId(order.getId())).thenReturn(List.of(sub));
        when(slots.findById(sub.getSlotId())).thenReturn(Optional.of(slot));
        when(refunds.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var adapter = new BookingCancellationFinancialAdapter(orders, subs, refunds, slots, new RefundPolicyEngine());
        var result = adapter.requestRefund(booking, UUID.randomUUID(), "discount-refund", OffsetDateTime.now());
        assertThat(result.refundAmount()).isEqualByComparingTo("80000");
    }

    @Test
    void fullRefundRecordsActualCashAndContributesToTotalRefunds() {
        var context = discountedContext(SubOrderStatus.REFUNDED, "80000");
        var result = new SettlementCalculationEngine().calculateSettlement(UUID.randomUUID(), LocalDate.now(),
                LocalDate.now(), List.of(context), new BigDecimal("0.10"));
        assertThat(result.getLineItems().get(0).getRefundAmount()).isEqualByComparingTo("80000");
        assertThat(result.getTotalRefundAmount()).isEqualByComparingTo("80000");
        assertThat(result.getTotalNetPayout()).isZero();
    }

    @Test
    void partialRefundReversesPlatformFundingInSameProportion() {
        var context = discountedContext(SubOrderStatus.PARTIALLY_REFUNDED, "40000");
        var line = new SettlementCalculationEngine().calculateLineItem(context);
        assertThat(line.getRefundAmount()).isEqualByComparingTo("40000");
        assertThat(line.getGrossAmount()).isEqualByComparingTo("50000");
        assertThat(line.getCommissionAmount()).isEqualByComparingTo("5000");
        assertThat(line.getNetAmount()).isEqualByComparingTo("45000");
    }

    @Test
    void fullNetRefundIsDetectedEvenIfStatusUpdateHasNotArrived() {
        var line = new SettlementCalculationEngine().calculateLineItem(discountedContext(SubOrderStatus.COMPLETED, "80000"));
        assertThat(line.isExcluded()).isTrue();
        assertThat(line.getNetAmount()).isZero();
        assertThat(line.getRefundAmount()).isEqualByComparingTo("80000");
    }

    private SubOrderCalculationContext discountedContext(SubOrderStatus status, String refund) {
        return SubOrderCalculationContext.builder().subOrderId(UUID.randomUUID()).status(status)
                .subtotalAmount(new BigDecimal("100000")).finalAmount(new BigDecimal("80000"))
                .platformDiscountAmount(new BigDecimal("20000")).commissionBasisAmount(new BigDecimal("100000"))
                .commissionRate(new BigDecimal("0.10")).refundAmount(new BigDecimal(refund)).build();
    }
}
