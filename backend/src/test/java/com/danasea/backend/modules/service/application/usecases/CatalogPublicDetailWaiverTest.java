package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.shared.i18n.LocalizedContentSelector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog Public Detail Waiver Acceptance Tests")
class CatalogPublicDetailWaiverTest {

    @Mock private ServiceRepositoryPort serviceRepositoryPort;
    @Mock private RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ServiceImageRepositoryPort serviceImageRepositoryPort;
    @Mock private ServiceAvailabilityPort serviceAvailabilityPort;
    @Mock private ServiceOptionRepositoryPort serviceOptionRepositoryPort;

    private LocalizedContentSelector localizedContentSelector;
    private GetPublicServiceDetailUseCase useCase;

    private final UUID serviceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        localizedContentSelector = new LocalizedContentSelector();
        useCase =
                new GetPublicServiceDetailUseCase(
                        serviceRepositoryPort,
                        recordRecentlyViewedUseCase,
                        categoryRepositoryPort,
                        serviceImageRepositoryPort,
                        serviceAvailabilityPort,
                        localizedContentSelector,
                        serviceOptionRepositoryPort,
                        null,
                        null,
                        null);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Service createMockService(boolean required, int version, String vi, String en) {
        Service s = new Service();
        s.setId(serviceId);
        s.setName("Lặn biển ngắm san hô");
        s.setNameEn("Coral Reef Diving");
        s.setCategoryId(categoryId);
        s.setViewCount(10);
        s.setWaiverRequired(required);
        s.setWaiverVersion(version);
        s.setWaiverContent(vi);
        s.setWaiverContentEn(en);
        return s;
    }

    private void stubCommonDependencies(Service s) {
        when(serviceRepositoryPort.findPublishedById(serviceId)).thenReturn(Optional.of(s));
        Category cat = new Category();
        cat.setName("Thể thao biển");
        when(categoryRepositoryPort.findById(categoryId)).thenReturn(Optional.of(cat));
        when(serviceImageRepositoryPort.findByServiceId(serviceId)).thenReturn(List.of());
        when(serviceAvailabilityPort.findAvailableSlots(serviceId)).thenReturn(List.of());
        when(serviceOptionRepositoryPort.findByServiceIdAndStatus(serviceId, OptionStatus.ACTIVE))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName(
            "Khách xem bằng tiếng Việt: nhận nội dung VI, waiverLanguage=VI, fallbackUsed=false")
    void viewServiceInVietnamese() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        Service s = createMockService(true, 2, "Bản cam kết tiếng Việt", "English Waiver Content");
        stubCommonDependencies(s);

        ServiceDetailResult result = useCase.execute(serviceId, null, "session-1");

        assertThat(result.getWaiverRequired()).isTrue();
        assertThat(result.getWaiverVersion()).isEqualTo(2);
        assertThat(result.getWaiverContent()).isEqualTo("Bản cam kết tiếng Việt");
        assertThat(result.getWaiverLanguage()).isEqualTo("VI");
        assertThat(result.getWaiverFallbackUsed()).isFalse();
    }

    @Test
    @DisplayName(
            "Khách xem bằng tiếng Anh: nhận nội dung EN, waiverLanguage=EN, fallbackUsed=false")
    void viewServiceInEnglish() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Service s = createMockService(true, 2, "Bản cam kết tiếng Việt", "English Waiver Content");
        stubCommonDependencies(s);

        ServiceDetailResult result = useCase.execute(serviceId, null, "session-1");

        assertThat(result.getWaiverRequired()).isTrue();
        assertThat(result.getWaiverVersion()).isEqualTo(2);
        assertThat(result.getWaiverContent()).isEqualTo("English Waiver Content");
        assertThat(result.getWaiverLanguage()).isEqualTo("EN");
        assertThat(result.getWaiverFallbackUsed()).isFalse();
    }

    @Test
    @DisplayName(
            "Khách xem bằng tiếng Anh nhưng bản dịch EN khuyết: fallback sang VI,"
                    + " fallbackUsed=true")
    void viewServiceInEnglishWithFallbackToVietnamese() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Service s = createMockService(true, 3, "Chỉ có bản cam kết tiếng Việt", null);
        stubCommonDependencies(s);

        ServiceDetailResult result = useCase.execute(serviceId, null, "session-1");

        assertThat(result.getWaiverRequired()).isTrue();
        assertThat(result.getWaiverVersion()).isEqualTo(3);
        assertThat(result.getWaiverContent()).isEqualTo("Chỉ có bản cam kết tiếng Việt");
        assertThat(result.getWaiverLanguage()).isEqualTo("VI");
        assertThat(result.getWaiverFallbackUsed()).isTrue();
    }

    @Test
    @DisplayName("Dịch vụ không yêu cầu cam kết: waiverRequired=false, waiverContent=null")
    void viewServiceWithoutWaiverRequirement() {
        Service s = createMockService(false, 1, null, null);
        stubCommonDependencies(s);

        ServiceDetailResult result = useCase.execute(serviceId, null, "session-1");

        assertThat(result.getWaiverRequired()).isFalse();
        assertThat(result.getWaiverContent()).isNull();
    }
}
