package com.danasea.backend.modules.order.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.danasea.backend.modules.order.application.OrderPaymentService;

class InternalPaymentWebhookProfileTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(OrderPaymentService.class, () -> mock(OrderPaymentService.class))
            .withUserConfiguration(InternalPaymentWebhookController.class);

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", "production"})
    void simulatedWebhookDoesNotExistOutsideTest(String profile) {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> assertThat(context).doesNotHaveBean(InternalPaymentWebhookController.class));
    }

    @Test
    void simulatedWebhookDoesNotExistWithDefaultProfile() {
        runner.run(context -> assertThat(context).doesNotHaveBean(InternalPaymentWebhookController.class));
    }

    @Test
    void simulatedWebhookIsAvailableOnlyInExplicitTestProfile() {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .run(context -> assertThat(context).hasSingleBean(InternalPaymentWebhookController.class));
    }
}
