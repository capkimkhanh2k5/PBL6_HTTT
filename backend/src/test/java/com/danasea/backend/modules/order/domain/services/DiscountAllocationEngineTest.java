package com.danasea.backend.modules.order.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.order.domain.models.DiscountCode;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import com.danasea.backend.modules.order.domain.services.DiscountAllocationEngine.AllocationResult;
import com.danasea.backend.modules.order.domain.services.DiscountAllocationEngine.CandidateItem;
import com.danasea.backend.modules.order.domain.services.DiscountAllocationEngine.ItemAllocation;

@DisplayName("DiscountAllocationEngine Tests")
class DiscountAllocationEngineTest {

    private DiscountAllocationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DiscountAllocationEngine();
    }

    @Nested
    @DisplayName("Largest Remainder (Hare-Niemeyer) Algorithm Tests")
    class LargestRemainderTests {

        @Test
        @DisplayName("Phân bổ pro-rata chính xác với phần dư lớn nhất (Ví dụ 100k trên 100k + 200k)")
        void testProRataLargestRemainderAllocation() {
            UUID v1 = UUID.randomUUID();
            UUID v2 = UUID.randomUUID();
            UUID v3 = UUID.randomUUID();

            CandidateItem itemA = new CandidateItem(UUID.randomUUID(), v1, UUID.randomUUID(), new BigDecimal("100000.00"), new BigDecimal("0.10"));
            CandidateItem itemB = new CandidateItem(UUID.randomUUID(), v2, UUID.randomUUID(), new BigDecimal("200000.00"), new BigDecimal("0.10"));

            DiscountCode code = new DiscountCode();
            code.setId(UUID.randomUUID());
            code.setCode("DANASEA100K");
            code.setScope(DiscountScope.PLATFORM);
            code.setSponsorType(DiscountSponsorType.PLATFORM);
            code.setDiscountType(DiscountType.FIXED);
            code.setDiscountValue(new BigDecimal("100000.00"));
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(itemA, itemB), OffsetDateTime.now(), 0);

            assertThat(result.valid()).isTrue();
            assertThat(result.totalDiscountAmount()).isEqualByComparingTo("100000.00");
            assertThat(result.finalPayableAmount()).isEqualByComparingTo("200000.00");

            ItemAllocation allocA = result.itemAllocations().stream().filter(a -> a.itemId().equals(itemA.itemId())).findFirst().orElseThrow();
            ItemAllocation allocB = result.itemAllocations().stream().filter(a -> a.itemId().equals(itemB.itemId())).findFirst().orElseThrow();

            // 100,000 / 300,000 = 33,333.33 -> floor 33,333.33, remainder .333...
            // 200,000 / 300,000 = 66,666.67 -> floor 66,666.66, remainder .666...
            // B has largest remainder, so B gets +0.01
            assertThat(allocA.totalDiscountAmount()).isEqualByComparingTo("33333.33");
            assertThat(allocB.totalDiscountAmount()).isEqualByComparingTo("66666.67");
            assertThat(allocA.totalDiscountAmount().add(allocB.totalDiscountAmount())).isEqualByComparingTo("100000.00");

            // Sàn tài trợ: cơ sở hoa hồng giữ nguyên giá gốc, vendor nhận đủ payout
            assertThat(allocA.platformDiscountAmount()).isEqualByComparingTo("33333.33");
            assertThat(allocA.vendorDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(allocA.commissionBasisAmount()).isEqualByComparingTo("100000.00");
            assertThat(allocA.commissionAmount()).isEqualByComparingTo("10000.00");
            assertThat(allocA.vendorPayoutAmount()).isEqualByComparingTo("90000.00");
            assertThat(allocA.finalSubtotal()).isEqualByComparingTo("66666.67");
        }

        @Test
        @DisplayName("Voucher do Vendor tài trợ: cơ sở hoa hồng giảm tương ứng với giảm giá")
        void testVendorSponsoredVoucher() {
            UUID v1 = UUID.randomUUID();

            CandidateItem item = new CandidateItem(UUID.randomUUID(), v1, UUID.randomUUID(), new BigDecimal("1000000.00"), new BigDecimal("0.10"));

            DiscountCode code = new DiscountCode();
            code.setId(UUID.randomUUID());
            code.setCode("VENDOR100");
            code.setScope(DiscountScope.VENDOR);
            code.setVendorId(v1);
            code.setSponsorType(DiscountSponsorType.VENDOR);
            code.setDiscountType(DiscountType.FIXED);
            code.setDiscountValue(new BigDecimal("100000.00"));
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(item), OffsetDateTime.now(), 0);

            assertThat(result.valid()).isTrue();
            assertThat(result.totalDiscountAmount()).isEqualByComparingTo("100000.00");
            assertThat(result.finalPayableAmount()).isEqualByComparingTo("900000.00");

            ItemAllocation alloc = result.itemAllocations().get(0);
            assertThat(alloc.vendorDiscountAmount()).isEqualByComparingTo("100000.00");
            assertThat(alloc.platformDiscountAmount()).isEqualByComparingTo("0.00");
            // Cơ sở hoa hồng = 1,000,000 - 100,000 = 900,000
            assertThat(alloc.commissionBasisAmount()).isEqualByComparingTo("900000.00");
            // Hoa hồng = 900,000 * 10% = 90,000
            assertThat(alloc.commissionAmount()).isEqualByComparingTo("90000.00");
            // Vendor nhận = 900,000 - 90,000 = 810,000
            assertThat(alloc.vendorPayoutAmount()).isEqualByComparingTo("810000.00");
        }

        @Test
        @DisplayName("Voucher phần trăm có Max Discount Cap")
        void testPercentageVoucherWithMaxCap() {
            UUID v1 = UUID.randomUUID();
            CandidateItem item = new CandidateItem(UUID.randomUUID(), v1, UUID.randomUUID(), new BigDecimal("1000000.00"), new BigDecimal("0.10"));

            DiscountCode code = new DiscountCode();
            code.setId(UUID.randomUUID());
            code.setCode("SALE20");
            code.setScope(DiscountScope.PLATFORM);
            code.setSponsorType(DiscountSponsorType.PLATFORM);
            code.setDiscountType(DiscountType.PERCENTAGE);
            code.setDiscountValue(new BigDecimal("20.00")); // 20% của 1,000,000 = 200,000
            code.setMaxDiscountAmount(new BigDecimal("50000.00")); // Nhưng cap tối đa 50,000
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(item), OffsetDateTime.now(), 0);

            assertThat(result.valid()).isTrue();
            assertThat(result.totalDiscountAmount()).isEqualByComparingTo("50000.00");
            assertThat(result.finalPayableAmount()).isEqualByComparingTo("950000.00");
        }
    }

    @Nested
    @DisplayName("Validation & Ineligibility Tests")
    class ValidationTests {

        @Test
        @DisplayName("Mã chưa đến ngày hoặc đã hết hạn")
        void testValidityDates() {
            DiscountCode code = new DiscountCode();
            code.setCode("EXPIRED");
            code.setValidTo(OffsetDateTime.now().minusDays(1));
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(), OffsetDateTime.now(), 0);
            assertThat(result.valid()).isFalse();
            assertThat(result.errorCode()).isEqualTo("DISCOUNT_EXPIRED");
        }

        @Test
        @DisplayName("Vượt quá lượt dùng toàn hệ thống (Quota Exceeded)")
        void testMaxUsesExceeded() {
            DiscountCode code = new DiscountCode();
            code.setCode("QUOTAFULL");
            code.setMaxUses(10);
            code.setUsedCount(10);
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(), OffsetDateTime.now(), 0);
            assertThat(result.valid()).isFalse();
            assertThat(result.errorCode()).isEqualTo("DISCOUNT_QUOTA_EXCEEDED");
        }

        @Test
        @DisplayName("Vượt quá giới hạn của người dùng (Per User Limit)")
        void testUserLimitExceeded() {
            DiscountCode code = new DiscountCode();
            code.setCode("USERLIMIT");
            code.setMaxUsesPerUser(1);
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(), OffsetDateTime.now(), 1);
            assertThat(result.valid()).isFalse();
            assertThat(result.errorCode()).isEqualTo("DISCOUNT_USER_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("Đơn hàng chưa đạt giá trị tối thiểu (Min Order Amount)")
        void testMinOrderAmount() {
            UUID v1 = UUID.randomUUID();
            CandidateItem item = new CandidateItem(UUID.randomUUID(), v1, UUID.randomUUID(), new BigDecimal("150000.00"), new BigDecimal("0.10"));

            DiscountCode code = new DiscountCode();
            code.setCode("MIN300K");
            code.setMinOrderAmount(new BigDecimal("300000.00"));
            code.setDiscountType(DiscountType.FIXED);
            code.setDiscountValue(new BigDecimal("50000.00"));
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(item), OffsetDateTime.now(), 0);
            assertThat(result.valid()).isFalse();
            assertThat(result.errorCode()).isEqualTo("DISCOUNT_MIN_AMOUNT_NOT_MET");
        }

        @Test
        @DisplayName("Phạm vi Vendor khác với vendor trong đơn hàng")
        void testVendorScopeMismatch() {
            UUID v1 = UUID.randomUUID();
            UUID v2 = UUID.randomUUID();
            CandidateItem item = new CandidateItem(UUID.randomUUID(), v1, UUID.randomUUID(), new BigDecimal("200000.00"), new BigDecimal("0.10"));

            DiscountCode code = new DiscountCode();
            code.setCode("VENDOR2ONLY");
            code.setScope(DiscountScope.VENDOR);
            code.setVendorId(v2); // Vendor khác
            code.setDiscountType(DiscountType.FIXED);
            code.setDiscountValue(new BigDecimal("50000.00"));
            code.setIsActive(true);

            AllocationResult result = engine.evaluateAndAllocate(code, List.of(item), OffsetDateTime.now(), 0);
            assertThat(result.valid()).isFalse();
            assertThat(result.errorCode()).isEqualTo("DISCOUNT_SCOPE_MISMATCH");
        }
    }
}
