package com.danasea.backend.modules.order.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.mappers.MasterOrderMapper;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;

@Component
public class MasterOrderRepositoryAdapter implements MasterOrderRepositoryPort {

    private final JpaMasterOrderRepository jpaMasterOrderRepository;
    private final MasterOrderMapper mapper;

    public MasterOrderRepositoryAdapter(
            JpaMasterOrderRepository jpaMasterOrderRepository,
            MasterOrderMapper mapper) {
        this.jpaMasterOrderRepository = jpaMasterOrderRepository;
        this.mapper = mapper;
    }

    @Override
    public MasterOrder save(MasterOrder order) {
        MasterOrderJpaEntity entity = mapper.toEntity(order);
        MasterOrderJpaEntity saved = jpaMasterOrderRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MasterOrder> findById(UUID id) {
        return jpaMasterOrderRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MasterOrder> findByIdForUpdate(UUID id) {
        return jpaMasterOrderRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MasterOrder> findByBookingId(UUID bookingId) {
        return jpaMasterOrderRepository.findByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public Optional<MasterOrder> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey) {
        return jpaMasterOrderRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public OrderPagedResult<MasterOrder> findByCustomerId(UUID customerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MasterOrderJpaEntity> entityPage = jpaMasterOrderRepository.findByCustomerId(customerId, pageRequest);

        List<MasterOrder> content = entityPage.getContent().stream()
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
