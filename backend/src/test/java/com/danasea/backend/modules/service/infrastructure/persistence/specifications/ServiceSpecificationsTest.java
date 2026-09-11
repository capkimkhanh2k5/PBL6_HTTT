package com.danasea.backend.modules.service.infrastructure.persistence.specifications;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServiceSpecificationsTest {

    private Root<ServiceJpaEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Predicate mockPredicate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        mockPredicate = mock(Predicate.class);

        Path<Object> path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(mockPredicate);
        when(cb.equal(any(Expression.class), any(Expression.class))).thenReturn(mockPredicate);
        when(cb.conjunction()).thenReturn(mockPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(mockPredicate);
        when(cb.or(any(Predicate[].class))).thenReturn(mockPredicate);
        when(cb.like(any(Expression.class), anyString())).thenReturn(mockPredicate);
        when(cb.lower(any(Expression.class))).thenReturn(mock(Expression.class));
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(mockPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(mockPredicate);
        when(cb.between(any(Expression.class), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(mockPredicate);
        when(path.isNotNull()).thenReturn(mockPredicate);
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mockPredicate);
    }

    @Test
    void isPublished_shouldEnforcePublishedStatus() {
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.isPublished();
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("status");
        verify(cb).equal(any(Expression.class), eq(ServiceStatus.PUBLISHED));
    }

    @Test
    void hasCategory_whenPresent_shouldFilterByCategoryId() {
        UUID categoryId = UUID.randomUUID();
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.hasCategory(categoryId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("categoryId");
        verify(cb).equal(any(Expression.class), eq(categoryId));
    }

    @Test
    void hasCategory_whenNull_shouldReturnConjunction() {
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.hasCategory(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).conjunction();
    }

    @Test
    void hasKeyword_whenPresent_shouldMatchNameNameEnAndDescription() {
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.hasKeyword("diving");
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("name");
        verify(root).get("nameEn");
        verify(root).get("description");
        verify(cb).or(any(Predicate[].class));
    }

    @Test
    void inPriceRange_whenBothPresent_shouldAddBothPredicates() {
        BigDecimal min = BigDecimal.valueOf(100);
        BigDecimal max = BigDecimal.valueOf(500);
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.inPriceRange(min, max);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(min));
        verify(cb).lessThanOrEqualTo(any(Expression.class), eq(max));
    }

    @Test
    void withinLocation_whenValid_shouldApplyBoundingBox() {
        BigDecimal lat = BigDecimal.valueOf(16.0544);
        BigDecimal lng = BigDecimal.valueOf(108.2022);
        Double radiusKm = 10.0;
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.withinLocation(lat, lng, radiusKm);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root, atLeastOnce()).get("latitude");
        verify(root, atLeastOnce()).get("longitude");
    }

    @Test
    void filter_combinesAllCriteriaAndAlwaysEnforcesPublished() {
        UUID categoryId = UUID.randomUUID();
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.filter(
                categoryId,
                "kayak",
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(16.0),
                BigDecimal.valueOf(108.0),
                15.0
        );

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("status");
        verify(cb).equal(any(Expression.class), eq(ServiceStatus.PUBLISHED));
        verify(root).get("categoryId");
        verify(cb).equal(any(Expression.class), eq(categoryId));
        verify(cb, atLeastOnce()).and(any(Predicate[].class));
    }
}
