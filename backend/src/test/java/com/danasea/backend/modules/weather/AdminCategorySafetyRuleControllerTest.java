package com.danasea.backend.modules.weather;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.danasea.backend.modules.weather.application.dtos.CategorySafetyRuleResponse;
import com.danasea.backend.modules.weather.application.dtos.UpdateCategorySafetyRuleRequest;
import com.danasea.backend.modules.weather.domain.exceptions.CategorySafetyRuleNotFoundException;
import com.danasea.backend.modules.weather.domain.exceptions.InvalidSafetyRuleThresholdException;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.CategorySafetyRuleService;
import com.danasea.backend.modules.weather.presentation.controllers.AdminCategorySafetyRuleController;
import com.danasea.backend.modules.weather.presentation.handlers.WeatherExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCategorySafetyRuleController Tests")
class AdminCategorySafetyRuleControllerTest {

    @Mock
    private CategorySafetyRuleService safetyRuleService;

    @InjectMocks
    private AdminCategorySafetyRuleController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID categoryId;
    private CategorySafetyRule sampleRule;
    private CategorySafetyRuleResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new WeatherExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        categoryId = UUID.randomUUID();

        sampleRule = CategorySafetyRule.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .categorySlug("cheo-sup-kayak")
                .categoryName("Chèo SUP & Kayak")
                .cautionWaveHeightM(0.50)
                .maxWaveHeightM(0.80)
                .cautionWindSpeedKmh(12.0)
                .maxWindSpeedKmh(20.0)
                .maxWindGustKmh(28.0)
                .maxOceanCurrentMs(0.30)
                .minVisibilityM(2000.0)
                .fatalThunderstormCodes("95,96,99")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        sampleResponse = CategorySafetyRuleResponse.builder()
                .id(sampleRule.getId())
                .categoryId(sampleRule.getCategoryId())
                .categorySlug(sampleRule.getCategorySlug())
                .categoryName(sampleRule.getCategoryName())
                .cautionWaveHeightM(sampleRule.getCautionWaveHeightM())
                .maxWaveHeightM(sampleRule.getMaxWaveHeightM())
                .cautionWindSpeedKmh(sampleRule.getCautionWindSpeedKmh())
                .maxWindSpeedKmh(sampleRule.getMaxWindSpeedKmh())
                .maxWindGustKmh(sampleRule.getMaxWindGustKmh())
                .maxOceanCurrentMs(sampleRule.getMaxOceanCurrentMs())
                .minVisibilityM(sampleRule.getMinVisibilityM())
                .fatalThunderstormCodes(sampleRule.getFatalThunderstormCodes())
                .createdAt(sampleRule.getCreatedAt())
                .updatedAt(sampleRule.getUpdatedAt())
                .build();
    }

    @Nested
    @DisplayName("GET /api/admin/category-safety-rules")
    class GetAllRulesTests {

        @Test
        @DisplayName("Admin lấy danh sách toàn bộ quy chuẩn an toàn thành công")
        void getAllCategorySafetyRules_success() throws Exception {
            when(safetyRuleService.getAllRules()).thenReturn(List.of(sampleRule));
            when(safetyRuleService.mapToResponse(sampleRule)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/admin/category-safety-rules")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].categorySlug", is("cheo-sup-kayak")))
                    .andExpect(jsonPath("$[0].categoryName", is("Chèo SUP & Kayak")))
                    .andExpect(jsonPath("$[0].cautionWaveHeightM", is(0.50)))
                    .andExpect(jsonPath("$[0].maxWaveHeightM", is(0.80)))
                    .andExpect(jsonPath("$[0].cautionWindSpeedKmh", is(12.0)))
                    .andExpect(jsonPath("$[0].maxWindSpeedKmh", is(20.0)));
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/category-safety-rules/{categoryId}")
    class UpdateRuleTests {

        @Test
        @DisplayName("Admin cập nhật ngưỡng an toàn thành công -> Trả về 200 OK")
        void updateCategorySafetyRule_success() throws Exception {
            UpdateCategorySafetyRuleRequest request = UpdateCategorySafetyRuleRequest.builder()
                    .cautionWaveHeightM(0.60)
                    .maxWaveHeightM(0.90)
                    .cautionWindSpeedKmh(14.0)
                    .maxWindSpeedKmh(22.0)
                    .build();

            CategorySafetyRule updatedRule = CategorySafetyRule.builder()
                    .id(sampleRule.getId())
                    .categoryId(categoryId)
                    .categorySlug("cheo-sup-kayak")
                    .categoryName("Chèo SUP & Kayak")
                    .cautionWaveHeightM(0.60)
                    .maxWaveHeightM(0.90)
                    .cautionWindSpeedKmh(14.0)
                    .maxWindSpeedKmh(22.0)
                    .maxWindGustKmh(28.0)
                    .maxOceanCurrentMs(0.30)
                    .minVisibilityM(2000.0)
                    .fatalThunderstormCodes("95,96,99")
                    .build();

            CategorySafetyRuleResponse updatedResponse = CategorySafetyRuleResponse.builder()
                    .id(sampleRule.getId())
                    .categoryId(categoryId)
                    .categorySlug("cheo-sup-kayak")
                    .categoryName("Chèo SUP & Kayak")
                    .cautionWaveHeightM(0.60)
                    .maxWaveHeightM(0.90)
                    .cautionWindSpeedKmh(14.0)
                    .maxWindSpeedKmh(22.0)
                    .build();

            when(safetyRuleService.updateRule(eq(categoryId), any(UpdateCategorySafetyRuleRequest.class)))
                    .thenReturn(updatedRule);
            when(safetyRuleService.mapToResponse(updatedRule))
                    .thenReturn(updatedResponse);

            mockMvc.perform(patch("/api/admin/category-safety-rules/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId", is(categoryId.toString())))
                    .andExpect(jsonPath("$.cautionWaveHeightM", is(0.60)))
                    .andExpect(jsonPath("$.maxWaveHeightM", is(0.90)))
                    .andExpect(jsonPath("$.cautionWindSpeedKmh", is(14.0)))
                    .andExpect(jsonPath("$.maxWindSpeedKmh", is(22.0)));
        }

        @Test
        @DisplayName("Validation Error: caution > max -> Trả về 400 Bad Request")
        void updateCategorySafetyRule_cautionExceedsMax_returns400() throws Exception {
            UpdateCategorySafetyRuleRequest request = UpdateCategorySafetyRuleRequest.builder()
                    .cautionWaveHeightM(1.50)
                    .maxWaveHeightM(0.80)
                    .build();

            when(safetyRuleService.updateRule(eq(categoryId), any(UpdateCategorySafetyRuleRequest.class)))
                    .thenThrow(new InvalidSafetyRuleThresholdException("Ngưỡng sóng vàng (1.50m) không được vượt quá ngưỡng sóng đỏ (0.80m)"));

            mockMvc.perform(patch("/api/admin/category-safety-rules/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_SAFETY_RULE_THRESHOLD")))
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("không được vượt quá ngưỡng sóng đỏ")));
        }

        @Test
        @DisplayName("Validation Error: Giá trị âm -> Trả về 400 Bad Request")
        void updateCategorySafetyRule_negativeValue_returns400() throws Exception {
            UpdateCategorySafetyRuleRequest request = UpdateCategorySafetyRuleRequest.builder()
                    .maxWaveHeightM(-5.0)
                    .build();

            mockMvc.perform(patch("/api/admin/category-safety-rules/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_INPUT")));
        }

        @Test
        @DisplayName("Not Found Error: categoryId không tồn tại -> Trả về 404 Not Found")
        void updateCategorySafetyRule_notFound_returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            UpdateCategorySafetyRuleRequest request = UpdateCategorySafetyRuleRequest.builder()
                    .maxWaveHeightM(1.0)
                    .build();

            when(safetyRuleService.updateRule(eq(unknownId), any(UpdateCategorySafetyRuleRequest.class)))
                    .thenThrow(new CategorySafetyRuleNotFoundException("Không tìm thấy quy chuẩn an toàn cho danh mục với ID: " + unknownId));

            mockMvc.perform(patch("/api/admin/category-safety-rules/{categoryId}", unknownId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("CATEGORY_SAFETY_RULE_NOT_FOUND")))
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Không tìm thấy")));
        }
    }
}
