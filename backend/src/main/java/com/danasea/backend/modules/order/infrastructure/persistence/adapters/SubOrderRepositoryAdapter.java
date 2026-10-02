package com.danasea.backend.modules.order.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.mappers.SubOrderMapper;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;

@Component
public class SubOrderRepositoryAdapter implements SubOrderRepositoryPort {

    private final JpaSubOrderRepository jpaSubOrderRepository;
    private final SubOrderMapper mapper;

    public SubOrderRepositoryAdapter(
            JpaSubOrderRepository jpaSubOrderRepository,
            SubOrderMapper mapper) {
        this.jpaSubOrderRepository = jpaSubOrderRepository;
        this.mapper = mapper;
    }

    @Override
    public SubOrder save(SubOrder subOrder) {
        SubOrderJpaEntity entity = mapper.toEntity(subOrder);
        SubOrderJpaEntity saved = jpaSubOrderRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<SubOrder> saveAll(List<SubOrder> subOrders) {
        List<SubOrderJpaEntity> entities = subOrders.stream().map(mapper::toEntity).toList();
        List<SubOrderJpaEntity> saved = jpaSubOrderRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SubOrder> findById(UUID id) {
        return jpaSubOrderRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SubOrder> findByBookingItemId(UUID bookingItemId) {
        return jpaSubOrderRepository.findByBookingItemId(bookingItemId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubOrder> findByBookingItemIdForUpdate(UUID bookingItemId) {
        return jpaSubOrderRepository.findByBookingItemIdForUpdate(bookingItemId).map(mapper::toDomain);
    }

    @Override
    public List<SubOrder> findByMasterOrderId(UUID masterOrderId) {
        return jpaSubOrderRepository.findByMasterOrderId(masterOrderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public OrderPagedResult<SubOrder> findByVendorId(UUID vendorId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SubOrderJpaEntity> entityPage = jpaSubOrderRepository.findByVendorId(vendorId, pageRequest);

        List<SubOrder> content = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new OrderPagedResult<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}
