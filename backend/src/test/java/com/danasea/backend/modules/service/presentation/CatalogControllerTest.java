package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
import com.danasea.backend.modules.service.application.usecase.GetPublicServiceDetailUseCase;
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
    private GetPublicServiceDetailUseCase getServiceDetailUseCase;

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

        mockMvc.perform(get("/api/v1/catalog")
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

        mockMvc.perform(get("/api/v1/catalog/" + id)
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

        mockMvc.perform(get("/api/v1/catalog/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }
}
