package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port trích xuất dữ liệu thô và projection từ cơ sở dữ liệu phục vụ các Use Case của Module
 * Report.
 */
public interface ReportDataPort {

    /**
     * Lấy danh sách SubOrder trong khoảng thời gian TimeRange.
     *
     * @param timeRange khoảng thời gian bắt buộc (chuẩn UTC+7)
     * @param vendorId ID của vendor (nếu null: lấy toàn bộ hệ thống)
     * @return danh sách SubOrderRecord
     */
    List<SubOrderRecord> findSubOrders(TimeRange timeRange, UUID vendorId);

    List<SubOrderRecord> findSubOrdersByMasterOrderIds(Collection<UUID> ids);

    List<SubOrderRecord> findSubOrdersByIds(Collection<UUID> ids);

    /**
     * Lấy danh sách MasterOrder trong khoảng thời gian TimeRange.
     *
     * @param timeRange khoảng thời gian bắt buộc
     * @return danh sách MasterOrderRecord
     */
    List<MasterOrderRecord> findMasterOrders(TimeRange timeRange);

    /** Lấy danh sách MasterOrder theo tập hợp ID. */
    List<MasterOrderRecord> findMasterOrdersByIds(Collection<UUID> masterOrderIds);

    /** Lấy danh sách Payment trong khoảng thời gian TimeRange. */
    List<PaymentRecord> findPayments(TimeRange timeRange);

    /** Lấy danh sách Payment theo tập hợp MasterOrder ID. */
    List<PaymentRecord> findPaymentsByMasterOrderIds(Collection<UUID> masterOrderIds);

    /** Lấy danh sách Refund trong khoảng thời gian TimeRange (hỗ trợ lọc theo vendorId). */
    List<RefundRecord> findRefunds(TimeRange timeRange, UUID vendorId);

    /** Lấy danh sách Refund theo tập hợp SubOrder ID. */
    List<RefundRecord> findRefundsBySubOrderIds(Collection<UUID> subOrderIds);

    /** Thống kê số lượng hoàn tiền phân rã theo RefundReason trong khoảng thời gian TimeRange. */
    Map<RefundReason, Long> countRefundsByReason(TimeRange timeRange, UUID vendorId);

    /** Lấy danh sách ServiceSlot trong khoảng ngày của TimeRange (hỗ trợ lọc theo vendorId). */
    List<ServiceSlotRecord> findServiceSlots(TimeRange timeRange, UUID vendorId);

    /** Lấy danh sách tất cả các Vendor đã được phê duyệt hoạt động (APPROVED). */
    List<VendorRecord> findApprovedVendors();

    /** Lấy thông tin Vendor theo Vendor ID. */
    Optional<VendorRecord> findVendorById(UUID vendorId);

    /** Lấy thông tin Vendor theo User ID (hỗ trợ cơ chế phân giải Vendor Isolation). */
    Optional<VendorRecord> findVendorByUserId(UUID userId);

    /**
     * Lấy danh sách Review đánh giá trong khoảng thời gian TimeRange (hỗ trợ lọc theo vendorId).
     */
    List<ReviewRecord> findReviews(TimeRange timeRange, UUID vendorId);
}
