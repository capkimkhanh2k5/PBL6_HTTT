package com.danasea.backend.modules.service.infrastructure.mapper;

import java.util.Collections;
import java.util.List;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    public Service toDomain(ServiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Service domain = new Service();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setVendorId(entity.getVendorId());
        domain.setCategoryId(entity.getCategoryId());
        domain.setName(entity.getName());
        domain.setNameEn(entity.getNameEn());
        domain.setSlug(entity.getSlug());
        domain.setDescription(entity.getDescription());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setPrice(entity.getPrice());
        domain.setDurationMinutes(entity.getDurationMinutes());
        domain.setCapacityPerSlot(entity.getCapacityPerSlot());
        domain.setLocationName(entity.getLocationName());
        domain.setAddress(entity.getAddress());
        domain.setLatitude(entity.getLatitude());
        domain.setLongitude(entity.getLongitude());
        domain.setStatus(entity.getStatus());
        domain.setWaiverContent(entity.getWaiverContent());
        domain.setWeatherSensitive(entity.getWeatherSensitive());
        domain.setMinWindKmh(entity.getMinWindKmh());
        domain.setMaxWaveM(entity.getMaxWaveM());
        domain.setAvgRating(entity.getAvgRating());
        domain.setRatingCount(entity.getRatingCount());
        domain.setViewCount(entity.getViewCount());
        return domain;
    }

    public ServiceJpaEntity toEntity(Service domain) {
        if (domain == null) {
            return null;
        }
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVendorId(domain.getVendorId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setName(domain.getName());
        entity.setNameEn(domain.getNameEn());
        entity.setSlug(domain.getSlug());
        entity.setDescription(domain.getDescription());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setPrice(domain.getPrice());
        entity.setDurationMinutes(domain.getDurationMinutes());
        entity.setCapacityPerSlot(domain.getCapacityPerSlot());
        entity.setLocationName(domain.getLocationName());
        entity.setAddress(domain.getAddress());
        entity.setLatitude(domain.getLatitude());
        entity.setLongitude(domain.getLongitude());
        entity.setStatus(domain.getStatus());
        entity.setWaiverContent(domain.getWaiverContent());
        entity.setWeatherSensitive(domain.getWeatherSensitive());
        entity.setMinWindKmh(domain.getMinWindKmh());
        entity.setMaxWaveM(domain.getMaxWaveM());
        entity.setAvgRating(domain.getAvgRating());
        entity.setRatingCount(domain.getRatingCount());
        entity.setViewCount(domain.getViewCount());
        return entity;
    }

    public List<Service> toDomainList(List<ServiceJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).toList();
    }
}
