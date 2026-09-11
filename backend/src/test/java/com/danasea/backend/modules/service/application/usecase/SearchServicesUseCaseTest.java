package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServicesUseCaseTest {

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @InjectMocks
    private SearchServicesUseCase searchServicesUseCase;

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
        service.setId(UUID.randomUUID());
        service.setName("Test Service");
        service.setPrice(BigDecimal.valueOf(100));
    }

    @Test
    void execute_ShouldReturnServiceSummaries() {
        SearchServicesCriteria criteria = SearchServicesCriteria.builder().page(0).size(20).build();
        
        when(serviceRepositoryPort.searchPublishedServices(
                any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)
        )).thenReturn(List.of(service));

        List<ServiceSummaryResult> results = searchServicesUseCase.execute(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Test Service");
    }
}
