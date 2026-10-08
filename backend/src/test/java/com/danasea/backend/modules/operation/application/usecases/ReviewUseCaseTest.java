package com.danasea.backend.modules.operation.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.operation.domain.exceptions.DuplicateReviewException;
import com.danasea.backend.modules.operation.domain.exceptions.InvalidReviewSubOrderStateException;
import com.danasea.backend.modules.operation.domain.exceptions.ReviewNotFoundException;
import com.danasea.backend.modules.operation.domain.exceptions.ReviewPeriodExpiredException;
import com.danasea.backend.modules.operation.domain.exceptions.UnauthorizedReviewAccessException;
import com.danasea.backend.modules.operation.domain.services.ReviewRatingService;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;
import com.danasea.backend.modules.operation.infrastructure.persistence.mappers.ReviewMapper;
import com.danasea.backend.modules.operation.infrastructure.persistence.repositories.JpaReviewRepository;
import com.danasea.backend.modules.operation.presentation.dtos.*;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review & Rating UseCase Unit Tests")
class ReviewUseCaseTest {

    @Mock
    private JpaReviewRepository reviewRepository;

    @Mock
    private JpaSubOrderRepository subOrderRepository;

    @Mock
    private JpaMasterOrderRepository masterOrderRepository;

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private JpaVendorRepository vendorRepository;

    private ReviewMapper reviewMapper;
    private ReviewRatingService reviewRatingService;

    private CreateReviewUseCase createReviewUseCase;
    private UpdateReviewUseCase updateReviewUseCase;
    private FlagReviewUseCase flagReviewUseCase;
    private GetServiceReviewsUseCase getServiceReviewsUseCase;
    private GetVendorReviewsUseCase getVendorReviewsUseCase;
    private ReplyVendorReviewUseCase replyVendorReviewUseCase;
    private GetAdminReviewsUseCase getAdminReviewsUseCase;
    private UpdateReviewVisibilityUseCase updateReviewVisibilityUseCase;

    private UUID customerId;
    private UUID vendorId;
    private UUID serviceId;
    private UUID masterOrderId;
    private UUID subOrderId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        masterOrderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        ObjectMapper objectMapper = new ObjectMapper();
        reviewMapper = new ReviewMapper(objectMapper, mock(com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository.class));
        reviewRatingService = new ReviewRatingService(reviewRepository, serviceRepository, vendorRepository);

        createReviewUseCase = new CreateReviewUseCase(reviewRepository, subOrderRepository, masterOrderRepository, reviewMapper, reviewRatingService);
        updateReviewUseCase = new UpdateReviewUseCase(reviewRepository, reviewMapper, reviewRatingService);
        flagReviewUseCase = new FlagReviewUseCase(reviewRepository, reviewMapper);
        getServiceReviewsUseCase = new GetServiceReviewsUseCase(reviewRepository, reviewMapper);
        getVendorReviewsUseCase = new GetVendorReviewsUseCase(reviewRepository, vendorRepository, reviewMapper);
        replyVendorReviewUseCase = new ReplyVendorReviewUseCase(reviewRepository, vendorRepository, reviewMapper);
        getAdminReviewsUseCase = new GetAdminReviewsUseCase(reviewRepository, reviewMapper);
        updateReviewVisibilityUseCase = new UpdateReviewVisibilityUseCase(reviewRepository, reviewMapper, reviewRatingService);
    }

    @Nested
    @DisplayName("CreateReviewUseCase Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should successfully create review when sub-order is completed and owned by customer")
        void shouldCreateReviewSuccessfully() {
            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setServiceId(serviceId);
            subOrder.setVendorId(vendorId);
            subOrder.setStatus(SubOrderStatus.COMPLETED);

            MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(customerId);

            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
            when(reviewRepository.existsBySubOrderId(subOrderId)).thenReturn(false);

            ReviewJpaEntity savedReview = ReviewJpaEntity.builder()
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .vendorId(vendorId)
                    .serviceId(serviceId)
                    .rating((short) 5)
                    .comment("Trải nghiệm tuyệt vời!")
                    .isVisible(true)
                    .isFlagged(false)
                    .build();
            savedReview.setId(reviewId);
            savedReview.setCreatedAt(OffsetDateTime.now());

            when(reviewRepository.save(any(ReviewJpaEntity.class))).thenReturn(savedReview);

            // Mock rating recalculation queries
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(new ServiceJpaEntity()));
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(new VendorJpaEntity()));
            when(reviewRepository.countByServiceIdAndIsVisibleTrue(serviceId)).thenReturn(1L);
            when(reviewRepository.getAvgRatingByServiceId(serviceId)).thenReturn(5.0);
            when(reviewRepository.countByVendorIdAndIsVisibleTrue(vendorId)).thenReturn(1L);
            when(reviewRepository.getAvgRatingByVendorId(vendorId)).thenReturn(5.0);

            CreateReviewRequest request = new CreateReviewRequest((short) 5, "Trải nghiệm tuyệt vời!", List.of("https://image1.jpg"));
            ReviewResponse response = createReviewUseCase.execute(subOrderId, request, customerId);

            assertThat(response).isNotNull();
            assertThat(response.rating()).isEqualTo((short) 5);
            assertThat(response.comment()).isEqualTo("Trải nghiệm tuyệt vời!");

            verify(reviewRepository).save(any(ReviewJpaEntity.class));
            verify(serviceRepository).save(any(ServiceJpaEntity.class));
            verify(vendorRepository).save(any(VendorJpaEntity.class));
        }

        @Test
        @DisplayName("Should reject review if sub-order is not completed")
        void shouldRejectIfNotCompleted() {
            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setStatus(SubOrderStatus.CONFIRMED);

            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));

            CreateReviewRequest request = new CreateReviewRequest((short) 5, "Comment", null);

            assertThatThrownBy(() -> createReviewUseCase.execute(subOrderId, request, customerId))
                    .isInstanceOf(InvalidReviewSubOrderStateException.class)
                    .hasMessageContaining("Only completed sub-orders can be reviewed");
        }

        @Test
        @DisplayName("Should reject review if customer is not owner")
        void shouldRejectIfNotOwner() {
            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setStatus(SubOrderStatus.COMPLETED);

            MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(UUID.randomUUID()); // different customer

            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));

            CreateReviewRequest request = new CreateReviewRequest((short) 5, "Comment", null);

            assertThatThrownBy(() -> createReviewUseCase.execute(subOrderId, request, customerId))
                    .isInstanceOf(UnauthorizedReviewAccessException.class)
                    .hasMessageContaining("User is not the owner");
        }

        @Test
        @DisplayName("Should reject duplicate review for same sub-order")
        void shouldRejectDuplicateReview() {
            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setMasterOrderId(masterOrderId);
            subOrder.setStatus(SubOrderStatus.COMPLETED);

            MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
            masterOrder.setId(masterOrderId);
            masterOrder.setCustomerId(customerId);

            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));
            when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
            when(reviewRepository.existsBySubOrderId(subOrderId)).thenReturn(true);

            CreateReviewRequest request = new CreateReviewRequest((short) 5, "Comment", null);

            assertThatThrownBy(() -> createReviewUseCase.execute(subOrderId, request, customerId))
                    .isInstanceOf(DuplicateReviewException.class)
                    .hasMessageContaining("already been submitted");
        }
    }

    @Nested
    @DisplayName("UpdateReviewUseCase Tests")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should successfully edit review within 7 days")
        void shouldUpdateReviewSuccessfully() {
            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .serviceId(serviceId)
                    .vendorId(vendorId)
                    .rating((short) 3)
                    .comment("Bình thường")
                    .isVisible(true)
                    .build();
            review.setId(reviewId);
            review.setCreatedAt(OffsetDateTime.now().minusDays(2)); // 2 days ago

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(ReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

            // Mock rating recalculation
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(new ServiceJpaEntity()));
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(new VendorJpaEntity()));

            UpdateReviewRequest request = new UpdateReviewRequest((short) 5, "Sau khi hỗ trợ thì rất ưng ý!", null);
            ReviewResponse response = updateReviewUseCase.execute(reviewId, request, customerId);

            assertThat(response.rating()).isEqualTo((short) 5);
            assertThat(response.comment()).isEqualTo("Sau khi hỗ trợ thì rất ưng ý!");
            verify(reviewRepository).save(review);
        }

        @Test
        @DisplayName("Should reject edit if 7 days have passed")
        void shouldRejectEditAfter7Days() {
            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .rating((short) 3)
                    .build();
            review.setId(reviewId);
            review.setCreatedAt(OffsetDateTime.now().minusDays(8)); // 8 days ago

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            UpdateReviewRequest request = new UpdateReviewRequest((short) 4, "Updated", null);

            assertThatThrownBy(() -> updateReviewUseCase.execute(reviewId, request, customerId))
                    .isInstanceOf(ReviewPeriodExpiredException.class)
                    .hasMessageContaining("within 7 days");
        }

        @Test
        @DisplayName("Should reject edit if user is not author")
        void shouldRejectEditIfNotAuthor() {
            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .subOrderId(subOrderId)
                    .customerId(UUID.randomUUID()) // different author
                    .build();
            review.setId(reviewId);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            UpdateReviewRequest request = new UpdateReviewRequest((short) 4, "Updated", null);

            assertThatThrownBy(() -> updateReviewUseCase.execute(reviewId, request, customerId))
                    .isInstanceOf(UnauthorizedReviewAccessException.class)
                    .hasMessageContaining("not the author");
        }
    }

    @Nested
    @DisplayName("Vendor Reply Tests")
    class VendorReplyTests {

        @Test
        @DisplayName("Vendor should successfully reply to own review")
        void shouldReplySuccessfully() {
            UUID vendorUserId = UUID.randomUUID();
            VendorJpaEntity vendor = new VendorJpaEntity();
            vendor.setId(vendorId);
            vendor.setUserId(vendorUserId);

            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .vendorId(vendorId)
                    .serviceId(serviceId)
                    .customerId(customerId)
                    .rating((short) 5)
                    .build();
            review.setId(reviewId);

            when(vendorRepository.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(ReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

            VendorReplyRequest request = new VendorReplyRequest("Cảm ơn quý khách đã trải nghiệm dịch vụ!");
            ReviewResponse response = replyVendorReviewUseCase.execute(reviewId, request, vendorUserId);

            assertThat(response.vendorReply()).isEqualTo("Cảm ơn quý khách đã trải nghiệm dịch vụ!");
            assertThat(response.vendorRepliedAt()).isNotNull();
        }

        @Test
        @DisplayName("Vendor cannot reply to review of another vendor")
        void shouldRejectReplyToOtherVendorReview() {
            UUID vendorUserId = UUID.randomUUID();
            VendorJpaEntity vendor = new VendorJpaEntity();
            vendor.setId(UUID.randomUUID()); // Different vendor
            vendor.setUserId(vendorUserId);

            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .vendorId(vendorId)
                    .build();
            review.setId(reviewId);

            when(vendorRepository.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            VendorReplyRequest request = new VendorReplyRequest("Phản hồi");

            assertThatThrownBy(() -> replyVendorReviewUseCase.execute(reviewId, request, vendorUserId))
                    .isInstanceOf(UnauthorizedReviewAccessException.class)
                    .hasMessageContaining("Vendor does not own the service");
        }
    }

    @Nested
    @DisplayName("Admin Visibility & Rating Recalculation Tests")
    class AdminVisibilityTests {

        @Test
        @DisplayName("Hiding review should recalculate service and vendor ratings, and adjust badge")
        void shouldRecalculateWhenReviewHidden() {
            ReviewJpaEntity review = ReviewJpaEntity.builder()
                    .serviceId(serviceId)
                    .vendorId(vendorId)
                    .customerId(customerId)
                    .rating((short) 5)
                    .isVisible(true)
                    .build();
            review.setId(reviewId);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(ReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

            // Setup entities for recalculation
            ServiceJpaEntity service = new ServiceJpaEntity();
            service.setId(serviceId);
            service.setAvgRating(new BigDecimal("5.00"));
            service.setRatingCount(1);
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

            VendorJpaEntity vendor = new VendorJpaEntity();
            vendor.setId(vendorId);
            vendor.setRatingAvg(new BigDecimal("5.00"));
            vendor.setRatingCount(1);
            vendor.setBadgeTier(BadgeTier.VERIFIED);
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

            // After hiding, counts drop to 0
            when(reviewRepository.countByServiceIdAndIsVisibleTrue(serviceId)).thenReturn(0L);
            when(reviewRepository.getAvgRatingByServiceId(serviceId)).thenReturn(null);
            when(reviewRepository.countByVendorIdAndIsVisibleTrue(vendorId)).thenReturn(0L);
            when(reviewRepository.getAvgRatingByVendorId(vendorId)).thenReturn(null);

            UpdateReviewVisibilityRequest request = new UpdateReviewVisibilityRequest(false, "Nội dung vi phạm tiêu chuẩn");
            ReviewResponse response = updateReviewVisibilityUseCase.execute(reviewId, request);

            assertThat(response.isVisible()).isFalse();
            assertThat(response.flagReason()).isEqualTo("Nội dung vi phạm tiêu chuẩn");

            // Verify service score updated to 0
            ArgumentCaptor<ServiceJpaEntity> serviceCaptor = ArgumentCaptor.forClass(ServiceJpaEntity.class);
            verify(serviceRepository).save(serviceCaptor.capture());
            assertThat(serviceCaptor.getValue().getAvgRating()).isEqualTo(BigDecimal.ZERO);
            assertThat(serviceCaptor.getValue().getRatingCount()).isEqualTo(0);

            // Verify vendor score updated to 0 and badge NONE
            ArgumentCaptor<VendorJpaEntity> vendorCaptor = ArgumentCaptor.forClass(VendorJpaEntity.class);
            verify(vendorRepository).save(vendorCaptor.capture());
            assertThat(vendorCaptor.getValue().getRatingAvg()).isEqualTo(BigDecimal.ZERO);
            assertThat(vendorCaptor.getValue().getRatingCount()).isEqualTo(0);
            assertThat(vendorCaptor.getValue().getBadgeTier()).isEqualTo(BadgeTier.NONE);
        }

        @Test
        @DisplayName("Badge should become TOP_RATED when count >= 5 and ratingAvg >= 4.0")
        void shouldDeriveTopRatedBadge() {
            VendorJpaEntity vendor = new VendorJpaEntity();
            vendor.setId(vendorId);
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

            when(reviewRepository.countByVendorIdAndIsVisibleTrue(vendorId)).thenReturn(5L);
            when(reviewRepository.getAvgRatingByVendorId(vendorId)).thenReturn(4.6);

            reviewRatingService.recalculateVendorRating(vendorId);

            ArgumentCaptor<VendorJpaEntity> captor = ArgumentCaptor.forClass(VendorJpaEntity.class);
            verify(vendorRepository).save(captor.capture());
            assertThat(captor.getValue().getBadgeTier()).isEqualTo(BadgeTier.TOP_RATED);
            assertThat(captor.getValue().getRatingAvg()).isEqualTo(new BigDecimal("4.60"));
            assertThat(captor.getValue().getRatingCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Badge should become VERIFIED when count >= 1 and not TOP_RATED")
        void shouldDeriveVerifiedBadge() {
            VendorJpaEntity vendor = new VendorJpaEntity();
            vendor.setId(vendorId);
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

            when(reviewRepository.countByVendorIdAndIsVisibleTrue(vendorId)).thenReturn(2L);
            when(reviewRepository.getAvgRatingByVendorId(vendorId)).thenReturn(3.5);

            reviewRatingService.recalculateVendorRating(vendorId);

            ArgumentCaptor<VendorJpaEntity> captor = ArgumentCaptor.forClass(VendorJpaEntity.class);
            verify(vendorRepository).save(captor.capture());
            assertThat(captor.getValue().getBadgeTier()).isEqualTo(BadgeTier.VERIFIED);
        }
    }
}
