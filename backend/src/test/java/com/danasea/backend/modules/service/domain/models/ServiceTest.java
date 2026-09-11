package com.danasea.backend.modules.service.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceImagesRequiredException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.exceptions.WeatherRequirementsMissingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    @Nested
    @DisplayName("Status Query Methods")
    class StatusQueryTests {
        @Test
        void shouldIdentifyCorrectStatus() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            assertTrue(draft.isDraft());
            assertFalse(draft.isPendingReview());
            assertFalse(draft.isPublished());
            assertFalse(draft.isPaused());
            assertFalse(draft.isRejected());

            Service pending = Service.builder().status(ServiceStatus.PENDING_REVIEW).build();
            assertTrue(pending.isPendingReview());

            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            assertTrue(published.isPublished());

            Service paused = Service.builder().status(ServiceStatus.PAUSED).build();
            assertTrue(paused.isPaused());

            Service rejected = Service.builder().status(ServiceStatus.REJECTED).build();
            assertTrue(rejected.isRejected());
        }
    }

    @Nested
    @DisplayName("Ownership Validation")
    class OwnershipTests {
        @Test
        void isOwnedBy_shouldReturnTrueForMatchingVendorId() {
            UUID vendorId = UUID.randomUUID();
            Service service = Service.builder().vendorId(vendorId).build();

            assertTrue(service.isOwnedBy(vendorId));
            assertFalse(service.isOwnedBy(UUID.randomUUID()));
            assertFalse(service.isOwnedBy(null));
        }

        @Test
        void validateOwnership_shouldThrowWhenVendorDoesNotMatch() {
            UUID ownerId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Service service = Service.builder().id(UUID.randomUUID()).vendorId(ownerId).build();

            service.validateOwnership(ownerId); // should not throw

            assertThrows(UnauthorizedServiceAccessException.class, () -> service.validateOwnership(otherId));
        }
    }

    @Nested
    @DisplayName("Weather Requirements Validation")
    class WeatherRequirementsTests {
        @Test
        void shouldPassWhenNotWeatherSensitive() {
            Service service = Service.builder().weatherSensitive(false).build();
            service.validateWeatherRequirements();

            Service serviceNull = Service.builder().weatherSensitive(null).build();
            serviceNull.validateWeatherRequirements();
        }

        @Test
        void shouldPassWhenWeatherSensitiveAndRequirementsProvided() {
            Service service = Service.builder()
                    .weatherSensitive(true)
                    .minWindKmh(BigDecimal.valueOf(10))
                    .maxWaveM(BigDecimal.valueOf(2.5))
                    .build();

            service.validateWeatherRequirements();
        }

        @Test
        void shouldThrowWhenWeatherSensitiveAndRequirementsMissing() {
            Service missingWind = Service.builder()
                    .weatherSensitive(true)
                    .maxWaveM(BigDecimal.valueOf(2.5))
                    .build();
            assertThrows(WeatherRequirementsMissingException.class, missingWind::validateWeatherRequirements);

            Service missingWave = Service.builder()
                    .weatherSensitive(true)
                    .minWindKmh(BigDecimal.valueOf(10))
                    .build();
            assertThrows(WeatherRequirementsMissingException.class, missingWave::validateWeatherRequirements);
        }
    }

    @Nested
    @DisplayName("Submit For Review")
    class SubmitForReviewTests {
        @Test
        void shouldTransitionToPendingReviewFromDraftOrRejected() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            draft.submitForReview(true);
            assertEquals(ServiceStatus.PENDING_REVIEW, draft.getStatus());

            Service rejected = Service.builder().status(ServiceStatus.REJECTED).build();
            rejected.submitForReview(true);
            assertEquals(ServiceStatus.PENDING_REVIEW, rejected.getStatus());
        }

        @Test
        void shouldThrowWhenImagesMissing() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            assertThrows(ServiceImagesRequiredException.class, () -> draft.submitForReview(false));
        }

        @Test
        void shouldThrowWhenStateNotDraftOrRejected() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            assertThrows(InvalidServiceStateException.class, () -> published.submitForReview(true));

            Service pending = Service.builder().status(ServiceStatus.PENDING_REVIEW).build();
            assertThrows(InvalidServiceStateException.class, () -> pending.submitForReview(true));
        }
    }

    @Nested
    @DisplayName("Approve Service")
    class ApproveTests {
        @Test
        void shouldTransitionToPublishedAndClearRejectionReason() {
            Service service = Service.builder()
                    .status(ServiceStatus.PENDING_REVIEW)
                    .rejectionReason("Previous reason")
                    .build();

            service.approve();
            assertEquals(ServiceStatus.PUBLISHED, service.getStatus());
            assertNull(service.getRejectionReason());
        }

        @Test
        void shouldThrowWhenNotPendingReview() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            assertThrows(InvalidServiceStateException.class, draft::approve);
        }
    }

    @Nested
    @DisplayName("Reject Service")
    class RejectTests {
        @Test
        void shouldTransitionToRejectedWithReason() {
            Service service = Service.builder().status(ServiceStatus.PENDING_REVIEW).build();

            service.reject("Insufficient documentation");
            assertEquals(ServiceStatus.REJECTED, service.getStatus());
            assertEquals("Insufficient documentation", service.getRejectionReason());
        }

        @Test
        void shouldThrowWhenReasonIsBlankOrNull() {
            Service service = Service.builder().status(ServiceStatus.PENDING_REVIEW).build();

            assertThrows(IllegalArgumentException.class, () -> service.reject(null));
            assertThrows(IllegalArgumentException.class, () -> service.reject("   "));
        }

        @Test
        void shouldThrowWhenNotPendingReview() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            assertThrows(InvalidServiceStateException.class, () -> published.reject("Reason"));
        }
    }

    @Nested
    @DisplayName("Pause and Resume")
    class PauseResumeTests {
        @Test
        void shouldPausePublishedService() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            published.pause();
            assertEquals(ServiceStatus.PAUSED, published.getStatus());
        }

        @Test
        void shouldResumePausedService() {
            Service paused = Service.builder().status(ServiceStatus.PAUSED).build();
            paused.resume();
            assertEquals(ServiceStatus.PUBLISHED, paused.getStatus());
        }

        @Test
        void shouldThrowWhenPausingNonPublishedService() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            assertThrows(InvalidServiceStateException.class, draft::pause);
        }

        @Test
        void shouldThrowWhenResumingNonPausedService() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            assertThrows(InvalidServiceStateException.class, published::resume);
        }
    }

    @Nested
    @DisplayName("Delete Validation")
    class DeleteValidationTests {
        @Test
        void shouldAllowDeleteWhenDraft() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            draft.validateDeletable(); // should not throw
        }

        @Test
        void shouldThrowWhenNotDraft() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            assertThrows(InvalidServiceStateException.class, published::validateDeletable);

            Service paused = Service.builder().status(ServiceStatus.PAUSED).build();
            assertThrows(InvalidServiceStateException.class, paused::validateDeletable);
        }
    }

    @Nested
    @DisplayName("Transition On Update")
    class TransitionOnUpdateTests {
        @Test
        void shouldTransitionPublishedToPendingReview() {
            Service published = Service.builder().status(ServiceStatus.PUBLISHED).build();
            published.transitionOnUpdate();
            assertEquals(ServiceStatus.PENDING_REVIEW, published.getStatus());
        }

        @Test
        void shouldNotChangeStatusWhenDraftOrRejected() {
            Service draft = Service.builder().status(ServiceStatus.DRAFT).build();
            draft.transitionOnUpdate();
            assertEquals(ServiceStatus.DRAFT, draft.getStatus());

            Service rejected = Service.builder().status(ServiceStatus.REJECTED).build();
            rejected.transitionOnUpdate();
            assertEquals(ServiceStatus.REJECTED, rejected.getStatus());
        }
    }
}
