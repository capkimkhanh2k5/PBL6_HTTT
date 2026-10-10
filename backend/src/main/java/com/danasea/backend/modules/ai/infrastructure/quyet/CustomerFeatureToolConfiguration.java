package com.danasea.backend.modules.ai.infrastructure.quyet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.modules.ai.application.tool.CustomerFeatureTool;
import com.danasea.backend.modules.ai.application.usecase.CompareServicesUseCase;
import com.danasea.backend.modules.ai.application.usecase.CustomerSupportUseCase;
import com.danasea.backend.modules.ai.application.usecase.DiscoverServicesUseCase;
import com.danasea.backend.modules.ai.application.usecase.PlanItineraryUseCase;
import com.danasea.backend.modules.ai.application.usecase.ReviewSummaryUseCase;
import com.danasea.backend.modules.ai.application.usecase.WeatherAwareUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CustomerFeatureToolConfiguration {
    private final DiscoverServicesUseCase discovery;
    private final CompareServicesUseCase comparisons;
    private final PlanItineraryUseCase plans;
    private final ReviewSummaryUseCase reviews;
    private final WeatherAwareUseCase weather;
    private final CustomerSupportUseCase support;
    private final ObjectMapper mapper;

    private CustomerFeatureTool tool(String name) { return new CustomerFeatureTool(name, discovery, comparisons, plans, reviews, weather, support, mapper); }
    @Bean CustomerFeatureTool aiSmartSearchTool() { return tool("ai_smart_search"); }
    @Bean CustomerFeatureTool aiRecommendationTool() { return tool("ai_recommend_services"); }
    @Bean CustomerFeatureTool aiComparisonTool() { return tool("ai_compare_services"); }
    @Bean CustomerFeatureTool aiPlannerTool() { return tool("ai_plan_itinerary"); }
    @Bean CustomerFeatureTool aiReplanTool() { return tool("ai_replan_itinerary"); }
    @Bean CustomerFeatureTool aiReviewSummaryTool() { return tool("ai_review_summary"); }
    @Bean CustomerFeatureTool aiNearbyTool() { return tool("ai_nearby_services"); }
    @Bean CustomerFeatureTool aiCustomerSupportTool() { return tool("ai_customer_support"); }
    @Bean CustomerFeatureTool aiWeatherSlotTool() { return tool("ai_weather_slot"); }
}
