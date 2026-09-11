package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPublicServiceDetailUseCaseTest {

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @Mock
    private RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    @InjectMocks
    private GetPublicServiceDetailUseCase getServiceDetailUseCase;

    @Test
    void execute_ShouldReturnDetailAndIncrementViewCount() {
        UUID serviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String sessionId = "session123";
        
        Service service = new Service();
        service.setId(serviceId);
        service.setName("Detail Service");
        service.setViewCount(5);

        when(serviceRepositoryPort.findPublishedById(serviceId)).thenReturn(Optional.of(service));

        ServiceDetailResult result = getServiceDetailUseCase.execute(serviceId, userId, sessionId);

        assertThat(result.getName()).isEqualTo("Detail Service");
        assertThat(result.getViewCount()).isEqualTo(6); // Incremented logic in mapping
        
        verify(serviceRepositoryPort, times(1)).incrementViewCount(serviceId, ServiceStatus.PUBLISHED);
        verify(recordRecentlyViewedUseCase, times(1)).execute(serviceId, userId, sessionId);
    }

    @Test
    void execute_WhenDraftService_ShouldThrow404() {
        UUID serviceId = UUID.randomUUID();
        when(serviceRepositoryPort.findPublishedById(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getServiceDetailUseCase.execute(serviceId, null, null))
                .isInstanceOf(ServiceNotFoundException.class);
                
        verify(serviceRepositoryPort, never()).incrementViewCount(any(), any());
        verify(recordRecentlyViewedUseCase, never()).execute(any(), any(), any());
    }
}
