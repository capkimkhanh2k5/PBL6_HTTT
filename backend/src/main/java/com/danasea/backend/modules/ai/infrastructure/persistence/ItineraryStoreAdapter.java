package com.danasea.backend.modules.ai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiItineraryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ItineraryStoreAdapter implements ItineraryStorePort {
    private final JpaAiItineraryRepository repository;
    private final ObjectMapper mapper;

    @Override
    @Transactional
    public Saved create(UUID ownerId, ItineraryPlan plan) {
        AiItineraryJpaEntity entity = new AiItineraryJpaEntity();
        entity.setOwnerId(ownerId);
        entity.setPlanJson(serialize(plan));
        return toSaved(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Saved> find(UUID id, UUID ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId).map(this::toSaved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Saved> list(UUID ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId, PageRequest.of(0, 50)).stream().map(this::toSaved).toList();
    }

    @Override
    @Transactional
    public Saved replace(UUID id, UUID ownerId, long expectedVersion, ItineraryPlan plan) {
        AiItineraryJpaEntity entity = repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new AiResourceNotFoundException("Itinerary not found"));
        if (entity.getVersion() != expectedVersion) throw new ObjectOptimisticLockingFailureException(AiItineraryJpaEntity.class, id);
        entity.setPlanJson(serialize(plan));
        return toSaved(repository.saveAndFlush(entity));
    }

    private String serialize(ItineraryPlan plan) {
        try { return mapper.writeValueAsString(plan); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize itinerary", exception); }
    }

    private Saved toSaved(AiItineraryJpaEntity entity) {
        try {
            return new Saved(entity.getId(), entity.getOwnerId(), entity.getVersion(),
                    mapper.readValue(entity.getPlanJson(), ItineraryPlan.class), entity.getCreatedAt(), entity.getUpdatedAt());
        } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read itinerary", exception); }
    }
}
