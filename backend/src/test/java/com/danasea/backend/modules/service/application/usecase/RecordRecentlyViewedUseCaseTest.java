package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordRecentlyViewedUseCaseTest {

    @Mock
    private RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @InjectMocks
    private RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    @Test
    void execute_WithExistingRecord_ShouldUpdateViewedAt() {
        UUID serviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Service service = new Service();
        service.setId(serviceId);
        
        RecentlyViewed rv = new RecentlyViewed();
        rv.setId(UUID.randomUUID());

        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(service));
        when(recentlyViewedRepositoryPort.findFirstByUserIdAndServiceId(userId, serviceId))
                .thenReturn(Optional.of(rv));

        recordRecentlyViewedUseCase.execute(serviceId, userId, null);

        verify(recentlyViewedRepositoryPort, times(1)).save(rv);
    }

    @Test
    void execute_WithNewRecord_ShouldInsert() {
        UUID serviceId = UUID.randomUUID();
        String sessionId = "sess1";
        
        Service service = new Service();
        service.setId(serviceId);

        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(service));
        when(recentlyViewedRepositoryPort.findFirstBySessionIdAndServiceId(sessionId, serviceId))
                .thenReturn(Optional.empty());

        recordRecentlyViewedUseCase.execute(serviceId, null, sessionId);

        verify(recentlyViewedRepositoryPort, times(1)).save(any(RecentlyViewed.class));
    }
}
