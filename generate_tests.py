import os

base_dir = "backend/src/test/java/com/danasea/backend/modules/service"
os.makedirs(f"{base_dir}/application/usecase", exist_ok=True)
os.makedirs(f"{base_dir}/presentation", exist_ok=True)

files = {}

files["application/usecase/SearchServicesUseCaseTest.java"] = """package com.danasea.backend.modules.service.application.usecase;

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
"""

files["application/usecase/GetServiceDetailUseCaseTest.java"] = """package com.danasea.backend.modules.service.application.usecase;

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
class GetServiceDetailUseCaseTest {

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @Mock
    private RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    @InjectMocks
    private GetServiceDetailUseCase getServiceDetailUseCase;

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
"""

files["application/usecase/RecordRecentlyViewedUseCaseTest.java"] = """package com.danasea.backend.modules.service.application.usecase;

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
        when(recentlyViewedRepositoryPort.findByUserIdOrSessionIdAndServiceId(userId, null, serviceId))
                .thenReturn(Optional.of(rv));

        recordRecentlyViewedUseCase.execute(serviceId, userId, null);

        verify(recentlyViewedRepositoryPort, times(1)).save(rv); // Updates timestamp
    }

    @Test
    void execute_WithNewRecord_ShouldInsert() {
        UUID serviceId = UUID.randomUUID();
        String sessionId = "sess1";
        
        Service service = new Service();
        service.setId(serviceId);

        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(service));
        when(recentlyViewedRepositoryPort.findByUserIdOrSessionIdAndServiceId(null, sessionId, serviceId))
                .thenReturn(Optional.empty());

        recordRecentlyViewedUseCase.execute(serviceId, null, sessionId);

        verify(recentlyViewedRepositoryPort, times(1)).save(any(RecentlyViewed.class));
    }
}
"""

files["application/usecase/WishlistUseCaseTest.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.WishlistItemResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.WishlistRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistUseCaseTest {

    @Mock
    private WishlistRepositoryPort wishlistRepositoryPort;

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @InjectMocks
    private WishlistUseCase wishlistUseCase;

    @Test
    void addWishlist_ShouldSave_WhenNotExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        
        when(serviceRepositoryPort.existsById(serviceId)).thenReturn(true);
        when(wishlistRepositoryPort.existsByUserIdAndServiceId(userId, serviceId)).thenReturn(false);
        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(new Service()));

        wishlistUseCase.addWishlist(userId, serviceId);

        verify(wishlistRepositoryPort, times(1)).save(any(Wishlist.class));
    }

    @Test
    void addWishlist_ShouldBeIdempotent_WhenExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        
        when(serviceRepositoryPort.existsById(serviceId)).thenReturn(true);
        when(wishlistRepositoryPort.existsByUserIdAndServiceId(userId, serviceId)).thenReturn(true);

        wishlistUseCase.addWishlist(userId, serviceId);

        verify(wishlistRepositoryPort, never()).save(any(Wishlist.class));
    }

    @Test
    void addWishlist_ShouldThrow_WhenServiceNotFound() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        
        when(serviceRepositoryPort.existsById(serviceId)).thenReturn(false);

        assertThatThrownBy(() -> wishlistUseCase.addWishlist(userId, serviceId))
                .isInstanceOf(ServiceNotFoundException.class);
    }
}
"""

files["presentation/CatalogControllerTest.java"] = """package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
import com.danasea.backend.modules.service.application.usecase.GetServiceDetailUseCase;
import com.danasea.backend.modules.service.application.usecase.SearchServicesUseCase;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SearchServicesUseCase searchServicesUseCase;

    @Mock
    private GetServiceDetailUseCase getServiceDetailUseCase;

    @InjectMocks
    private CatalogController catalogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController)
                .setControllerAdvice(new CatalogExceptionHandler())
                .build();
    }

    @Test
    void searchServices_ShouldReturn200() throws Exception {
        ServiceSummaryResult result = ServiceSummaryResult.builder()
                .id(UUID.randomUUID())
                .name("Public Service")
                .build();

        when(searchServicesUseCase.execute(any())).thenReturn(List.of(result));
        when(searchServicesUseCase.count(any())).thenReturn(1L);

        mockMvc.perform(get("/api/services")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Public Service"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getServiceDetail_ShouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        ServiceDetailResult result = ServiceDetailResult.builder()
                .id(id)
                .name("Service Name")
                .build();

        when(getServiceDetailUseCase.execute(eq(id), any(), any())).thenReturn(result);

        mockMvc.perform(get("/api/services/" + id)
                .header("X-Session-Id", "sess-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Service Name"));
    }

    @Test
    void getServiceDetail_WhenNotFound_ShouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        
        when(getServiceDetailUseCase.execute(eq(id), any(), any()))
                .thenThrow(new ServiceNotFoundException("Service not found"));

        mockMvc.perform(get("/api/services/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }
}
"""

for path, content in files.items():
    with open(f"{base_dir}/{path}", "w") as f:
        f.write(content)

print("Generated Tests layer")
