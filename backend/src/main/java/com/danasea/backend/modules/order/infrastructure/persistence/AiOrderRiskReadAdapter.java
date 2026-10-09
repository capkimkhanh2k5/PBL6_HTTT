package com.danasea.backend.modules.order.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.api.AiOrderRiskReadApi;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiOrderRiskReadAdapter implements AiOrderRiskReadApi {
    private final MasterOrderRepositoryPort orders;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Facts read(UUID orderId) {
        var order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long failed = count("""
                select count(*) from payments p join master_orders o on o.id = p.master_order_id
                where o.customer_id = :customer and p.status = 'FAILED' and p.created_at >= :since
                """, order.getCustomerId(), now.minusDays(1));
        long recent = count("select count(*) from master_orders where customer_id = :customer and created_at >= :since",
                order.getCustomerId(), now.minusHours(1));
        long refunds = count("""
                select count(distinct r.id) from refunds r join sub_orders s on s.id = r.sub_order_id
                join master_orders o on o.id = s.master_order_id
                where o.customer_id = :customer and r.created_at >= :since
                """, order.getCustomerId(), now.minusDays(7));
        return new Facts(order.getId(), order.getCustomerId(), failed, recent, refunds,
                order.getPaymentStatus() == null ? "UNKNOWN" : order.getPaymentStatus().name());
    }

    private long count(String sql, UUID customerId, OffsetDateTime since) {
        return ((Number) entityManager.createNativeQuery(sql).setParameter("customer", customerId)
                .setParameter("since", since).getSingleResult()).longValue();
    }
}
