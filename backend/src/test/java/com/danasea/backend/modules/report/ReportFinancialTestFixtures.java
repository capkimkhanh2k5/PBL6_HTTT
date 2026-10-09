package com.danasea.backend.modules.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.report.application.ports.PaymentRecord;
import com.danasea.backend.modules.report.application.ports.ReportDataPort;
import com.danasea.backend.modules.report.application.ports.SubOrderRecord;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Explicit successful-payment fixtures for pre-existing analytics scenarios. */
public final class ReportFinancialTestFixtures {
    private ReportFinancialTestFixtures() {}

    public static void paidOrders(ReportDataPort port, List<SubOrderRecord> orders) {
        lenient().when(port.findSubOrdersByMasterOrderIds(any())).thenReturn(orders);
        lenient().when(port.findSubOrdersByIds(any())).thenReturn(orders);
        lenient()
                .doAnswer(
                        invocation -> {
                            TimeRange range = invocation.getArgument(0);
                            var masters = port.findMasterOrders(range);
                            var groups =
                                    orders.stream()
                                            .collect(
                                                    Collectors.groupingBy(
                                                            SubOrderRecord::masterOrderId));
                            return groups.entrySet().stream()
                                    .map(
                                            group -> {
                                                var first = group.getValue().getFirst();
                                                var gross =
                                                        group.getValue().stream()
                                                                .map(SubOrderRecord::subtotalAmount)
                                                                .filter(Objects::nonNull)
                                                                .reduce(
                                                                        BigDecimal.ZERO,
                                                                        BigDecimal::add);
                                                var discount =
                                                        masters.stream()
                                                                .filter(
                                                                        m ->
                                                                                m.id().equals(
                                                                                                group
                                                                                                        .getKey()))
                                                                .map(
                                                                        m ->
                                                                                m.discountAmount()
                                                                                                == null
                                                                                        ? BigDecimal
                                                                                                .ZERO
                                                                                        : m
                                                                                                .discountAmount())
                                                                .findFirst()
                                                                .orElse(BigDecimal.ZERO);
                                                return new PaymentRecord(
                                                        UUID.randomUUID(),
                                                        group.getKey(),
                                                        gross.subtract(discount),
                                                        PaymentStatus.SUCCESS,
                                                        PaymentProvider.VNPAY,
                                                        first.createdAt(),
                                                        first.createdAt(),
                                                        first.createdAt());
                                            })
                                    .toList();
                        })
                .when(port)
                .findPayments(any());
    }
}
