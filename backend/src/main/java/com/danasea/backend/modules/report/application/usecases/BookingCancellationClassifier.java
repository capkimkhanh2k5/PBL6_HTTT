package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.report.application.ports.RefundRecord;
import com.danasea.backend.modules.report.application.ports.ReportDataPort;
import com.danasea.backend.modules.report.application.ports.SubOrderRecord;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BookingCancellationClassifier {
    private BookingCancellationClassifier() {}

    static Map<UUID, RefundReason> reasons(ReportDataPort port, List<SubOrderRecord> orders) {
        Map<UUID, RefundReason> result = new HashMap<>();
        port.findRefundsBySubOrderIds(orders.stream().map(SubOrderRecord::id).toList()).stream()
                .filter(r -> r.reason() != null)
                .sorted(
                        Comparator.comparing(
                                        RefundRecord::createdAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(r -> r.id().toString()))
                .forEach(r -> result.putIfAbsent(r.subOrderId(), r.reason()));
        for (SubOrderRecord order : orders) {
            if (order.cancellationReason() != null)
                result.put(order.id(), order.cancellationReason());
            else if (order.status() == SubOrderStatus.REJECTED)
                result.put(order.id(), RefundReason.VENDOR_FAULT);
        }
        return result;
    }

    static boolean isCancelled(SubOrderRecord order, Map<UUID, RefundReason> reasons) {
        if (order.cancellationReason() != null
                || order.status() == SubOrderStatus.CANCELLED
                || order.status() == SubOrderStatus.REJECTED) return true;
        RefundReason reason = reasons.get(order.id());
        if (order.status() == SubOrderStatus.REFUNDED) {
            return reason != RefundReason.COMPENSATION && reason != RefundReason.DISPUTE;
        }
        return order.status() == SubOrderStatus.PARTIALLY_REFUNDED
                && reason != null
                && reason != RefundReason.COMPENSATION
                && reason != RefundReason.DISPUTE;
    }
}
