package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
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

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private ServiceImageRepositoryPort serviceImageRepositoryPort;

    @Mock
    private ServiceAvailabilityPort serviceAvailabilityPort;

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
        UUID categoryId = UUID.randomUUID();
        service.setCategoryId(categoryId);

        Category category = new Category();
        category.setName("Diving");
        ServiceImage image = new ServiceImage();
        image.setUrl("https://cdn.example/service.jpg");

        when(serviceRepositoryPort.findPublishedById(serviceId)).thenReturn(Optional.of(service));
        when(categoryRepositoryPort.findById(categoryId)).thenReturn(Optional.of(category));
        when(serviceImageRepositoryPort.findByServiceId(serviceId)).thenReturn(List.of(image));
        when(serviceAvailabilityPort.findAvailableSlots(serviceId))
                .thenReturn(List.of("2026-09-24T08:00:00"));

        ServiceDetailResult result = getServiceDetailUseCase.execute(serviceId, userId, sessionId);

        assertThat(result.getName()).isEqualTo("Detail Service");
        assertThat(result.getViewCount()).isEqualTo(6); // Incremented logic in mapping
        assertThat(result.getCategoryName()).isEqualTo("Diving");
        assertThat(result.getImageUrls()).containsExactly("https://cdn.example/service.jpg");
        assertThat(result.getAvailableSlots()).containsExactly("2026-09-24T08:00:00");
        
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
