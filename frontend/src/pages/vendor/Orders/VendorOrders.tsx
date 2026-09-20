import { useState } from "react";
import { createPortal } from "react-dom";

export type SubOrderStatus = 
  | "PENDING" 
  | "CONFIRMED" 
  | "REJECTED" 
  | "COMPLETED" 
  | "CANCELLED" 
  | "REFUNDED";

export interface RefundDetail {
  subOrderId: string;
  amount: number;
  refund_percentage: number;
  reason: string;
  processed_at: string | null;
  status: "PENDING" | "PROCESSED" | "FAILED";
}

export interface OrderItem {
  id: string; // Sub-order ID (e.g. SUB-89412-01)
  orderNumber: string; // Master order ID (e.g. DNS-89412)
  serviceTitle: string;
  customerName: string;
  customerPhone: string;
  timeSlot: string;
  dateStr: string;
  guestCount: number;
  unitPrice: number;
  totalAmount: number;
  commissionRate: number; // e.g. 10 or 12
  commissionAmount: number;
  payoutAmount: number;
  status: SubOrderStatus;
  checked_in_at: string | null; // check-in recorded as timestamp, not separate order status
  departurePoint: string;
  image: string;
  refundInfo?: RefundDetail;
}

const INITIAL_ORDERS: OrderItem[] = [
  {
    id: "SUB-89412-01",
    orderNumber: "DNS-89412",
    serviceTitle: "Chèo SUP ngắm bình minh Mỹ Khê",
    customerName: "Nguyễn Văn An",
    customerPhone: "0912 345 678",
    timeSlot: "05:30 - 07:30",
    dateStr: "Hôm nay (24/10)",
    guestCount: 2,
    unitPrice: 280000,
    totalAmount: 560000,
    commissionRate: 10,
    commissionAmount: 56000,
    payoutAmount: 504000,
    status: "CONFIRMED",
    checked_in_at: null,
    departurePoint: "Bãi tắm Phạm Văn Đồng (Bến 02)",
    image: "https://lh3.googleusercontent.com/aida-public/AB6AXuByxMflsX8bEwAT9idQDNojY6FPPTpO_FiceT_mF054Df9U9b4iBAvsWbzwH3vUrZBldKBHzRnkIP2XEHt1okhPuFRzuUqi9yImTdjKtsY0Z8k0aius6X7rNM7Gva9Y8zh52yopRcbW00egyRdaa5KjOJchScc_vUpXETrcWLqQ3_pJjY6TBt1e8Go8_ZVwOFSsDmzAzwbp4R3lmdGweV0ok29BtYkFCmM8QifDbM02dlsM_PfOJoXXDg",
  },
  {
    id: "SUB-89415-03",
    orderNumber: "DNS-89415",
    serviceTitle: "Lặn ngắm san hô Bãi Bụt (Cano khứ hồi)",
    customerName: "Trần Thu Hà",
    customerPhone: "0988 765 432",
    timeSlot: "08:30 - 11:30",
    dateStr: "Hôm nay (24/10)",
    guestCount: 4,
    unitPrice: 520000,
    totalAmount: 2080000,
    commissionRate: 12,
    commissionAmount: 249600,
    payoutAmount: 1830400,
    status: "PENDING",
    checked_in_at: null,
    departurePoint: "Sơn Trà Pier B",
    image: "https://lh3.googleusercontent.com/aida-public/AB6AXuC7VrVbNfJYyaQ3DRZ62-ZAMxqDxthXVRRRY032c4SQOXIzjHHNFtCXodvcv2W8BONR62Aaw3QlmiK7Wan6A2HYN_aQmd6lm9f0gGW9yZBOrjLxe_lWPLjoNlfH2lRqF38xP9KUgUP31e3X_4vitkKUZgDabObtM4PP6v46ejcjzcoG-TGPQkhM7LDHS13Zz4rb3hwx2YoteNUBwUyK5BKUuSFC8Tttp6fYqmJLCe6kycUaKfe_BTVeqg",
  },
  {
    id: "SUB-89422-04",
    orderNumber: "DNS-89422",
    serviceTitle: "Mô tô nước Jetski cảm giác mạnh",
    customerName: "Hoàng Tuấn Vũ",
    customerPhone: "0934 999 888",
    timeSlot: "14:00 - 14:45",
    dateStr: "Hôm qua (23/10)",
    guestCount: 2,
    unitPrice: 600000,
    totalAmount: 1200000,
    commissionRate: 12,
    commissionAmount: 144000,
    payoutAmount: 1056000,
    status: "REJECTED",
    checked_in_at: null,
    departurePoint: "Bãi tắm Mỹ Khê",
    image: "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=600&q=80",
  },
  {
    id: "SUB-89425-05",
    orderNumber: "DNS-89425",
    serviceTitle: "Tour SUP Bãi Bụt ngắm hoàng hôn",
    customerName: "Đinh Phương Thảo",
    customerPhone: "0977 123 456",
    timeSlot: "16:30 - 18:30",
    dateStr: "22/10/2024",
    guestCount: 2,
    unitPrice: 350000,
    totalAmount: 700000,
    commissionRate: 10,
    commissionAmount: 70000,
    payoutAmount: 630000,
    status: "CANCELLED",
    checked_in_at: null,
    departurePoint: "Bến Bãi Bụt",
    image: "https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=600&q=80",
  },
  {
    id: "SUB-89428-06",
    orderNumber: "DNS-89428",
    serviceTitle: "Lặn biển bình khí chuyên sâu",
    customerName: "Vũ Hải Nam",
    customerPhone: "0905 444 333",
    timeSlot: "09:00 - 12:00",
    dateStr: "21/10/2024",
    guestCount: 1,
    unitPrice: 950000,
    totalAmount: 950000,
    commissionRate: 12,
    commissionAmount: 114000,
    payoutAmount: 836000,
    status: "REFUNDED",
    checked_in_at: null,
    departurePoint: "Trạm Hòn Chảo",
    image: "https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=600&q=80",
    refundInfo: {
      subOrderId: "SUB-89428-06",
      amount: 950000,
      refund_percentage: 100,
      reason: "Cảng vụ phát lệnh cấm biển do biển động sóng lớn cấp 6",
      processed_at: "21/10/2024 15:30",
      status: "PROCESSED"
    }
  },
  {
    id: "SUB-89430-07",
    orderNumber: "DNS-89430",
    serviceTitle: "Chèo SUP đón bình minh Mỹ Khê",
    customerName: "Lê Minh Trí",
    customerPhone: "0913 222 111",
    timeSlot: "05:30 - 07:30",
    dateStr: "20/10/2024",
    guestCount: 2,
    unitPrice: 280000,
    totalAmount: 560000,
    commissionRate: 10,
    commissionAmount: 56000,
    payoutAmount: 504000,
    status: "COMPLETED",
    checked_in_at: "20/10/2024 lúc 05:25",
    departurePoint: "Bãi tắm Phạm Văn Đồng (Bến 02)",
    image: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80",
  }
];

export function VendorOrders() {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const [activeFilter, setActiveFilter] = useState<"ALL" | SubOrderStatus>("ALL");
  const [showFinancials, setShowFinancials] = useState(false);
  const [orders, setOrders] = useState<OrderItem[]>(INITIAL_ORDERS);

  // Terminal state
  const [isTerminalOpen, setIsTerminalOpen] = useState(false);
  const [manualCode, setManualCode] = useState("");
  const [scannerResult, setScannerResult] = useState<{
    type: "SUCCESS" | "USED" | "INVALID" | "WRONG_VENDOR" | null;
    order?: OrderItem;
    message?: string;
  }>({ type: null });

  // Reject dialog
  const [rejectOrderId, setRejectOrderId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("Lệnh cấm biển khẩn cấp từ Cảng vụ / Thời tiết xấu");

  // Refund detail view modal
  const [selectedRefundOrder, setSelectedRefundOrder] = useState<OrderItem | null>(null);

  // QR Terminal Ticket Validation logic based on explicit test suite
  const handleVerifyCode = (codeToVerify?: string) => {
    const code = (codeToVerify || manualCode).trim().toUpperCase();
    if (!code) {
      setScannerResult({ type: "INVALID", message: "Vui lòng nhập mã vé hợp lệ." });
      return;
    }

    // Case 1: Wrong vendor
    if (code === "SUB-OTHER-999" || code === "WRONG" || code.includes("OTHER")) {
      setScannerResult({
        type: "WRONG_VENDOR",
        message: "Vé không thuộc quyền phục vụ của Danang Ocean Club (Thuộc Nhà cung cấp Sơn Trà Diving Co.)."
      });
      return;
    }

    // Match order by Sub-order ID or Master order ID
    const matched = orders.find(
      (o) => o.id.toUpperCase() === code || o.orderNumber.toUpperCase() === code
    );

    if (!matched) {
      setScannerResult({
        type: "INVALID",
        message: `Mã vé "${code}" không tồn tại trên hệ thống Cảng vụ.`
      });
      return;
    }

    // Case 2: Already checked in
    if (matched.checked_in_at !== null) {
      setScannerResult({
        type: "USED",
        order: matched,
        message: `Vé #${matched.id} của khách ${matched.customerName} đã được check-in vào lúc ${matched.checked_in_at}. Không được check-in lặp lại!`
      });
      return;
    }

    // Case 3: PENDING
    if (matched.status === "PENDING") {
      setScannerResult({
        type: "INVALID",
        order: matched,
        message: `Đơn hàng #${matched.id} chưa được Vendor xác nhận, chưa thể thực hiện check-in!`
      });
      return;
    }

    // Case 4: REJECTED
    if (matched.status === "REJECTED") {
      setScannerResult({
        type: "INVALID",
        order: matched,
        message: `Đơn hàng #${matched.id} đã bị từ chối phục vụ.`
      });
      return;
    }

    // Case 5: CANCELLED
    if (matched.status === "CANCELLED") {
      setScannerResult({
        type: "INVALID",
        order: matched,
        message: `Đơn hàng #${matched.id} đã bị khách hàng hủy trước thời điểm xuất bến.`
      });
      return;
    }

    // Case 6: REFUNDED
    if (matched.status === "REFUNDED") {
      setScannerResult({
        type: "INVALID",
        order: matched,
        message: `Đơn hàng #${matched.id} đã được hoàn tiền do hủy chuyến, vé không còn giá trị sử dụng.`
      });
      return;
    }

    // Case 7: SUCCESS (CONFIRMED and not checked in)
    setScannerResult({
      type: "SUCCESS",
      order: matched,
      message: `Vé hợp lệ! Khách: ${matched.customerName} (${matched.guestCount} khách). Sẵn sàng xuất bến!`
    });
  };

  // Perform admission and record checked_in_at
  const handleConfirmAdmit = (orderId: string) => {
    const checkinTime = new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }) + " " + new Date().toLocaleDateString("vi-VN");
    setOrders((prev) =>
      prev.map((o) =>
        o.id === orderId
          ? { ...o, status: "COMPLETED", checked_in_at: checkinTime }
          : o
      )
    );
    setScannerResult({
      type: "USED",
      message: `Đã check-in thành công và cấp phát phao bảo hộ cho đơn #${orderId}! (${checkinTime})`
    });
    showToast(`Check-in thành công đơn #${orderId}! Đã ghi nhận thời gian xuất bến.`);
  };

  // Action: Confirm pending order
  const handleConfirmOrder = (orderId: string) => {
    setOrders((prev) =>
      prev.map((o) => (o.id === orderId ? { ...o, status: "CONFIRMED" } : o))
    );
    showToast(`Đã duyệt xác nhận đơn #${orderId}! Khách có thể đến xuất bến.`);
  };

  // Action: Reject order
  const handleRejectOrder = () => {
    if (!rejectOrderId) return;
    setOrders((prev) =>
      prev.map((o) => (o.id === rejectOrderId ? { ...o, status: "REJECTED" } : o))
    );
    showToast(`Đã từ chối đơn #${rejectOrderId} với lý do: ${rejectReason}.`);
    setRejectOrderId(null);
  };

  // Filtered list
  const filteredOrders = orders.filter((o) => {
    if (activeFilter === "ALL") return true;
    return o.status === activeFilter;
  });

  const countPending = orders.filter((o) => o.status === "PENDING").length;
  const countConfirmed = orders.filter((o) => o.status === "CONFIRMED").length;
  const countCompleted = orders.filter((o) => o.status === "COMPLETED").length;
  const countRejected = orders.filter((o) => o.status === "REJECTED").length;
  const countCancelled = orders.filter((o) => o.status === "CANCELLED").length;
  const countRefunded = orders.filter((o) => o.status === "REFUNDED").length;

  return (
    <div className="w-full pt-4 pb-20 bg-background flex-1">
      {/* Top Banner */}
      <div className="relative w-full overflow-hidden px-space-lg py-space-lg lg:px-space-xl bg-surface border-b border-outline-variant/20">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-space-md">
          <div className="space-y-1">
            <div className="flex items-center gap-space-sm">
              <span className="px-2.5 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-[11px] font-bold">
                QUẢN TRỊ BÁN HÀNG &amp; VÉ
              </span>
              <span className="px-2 py-0.5 rounded-full bg-surface-container-high text-[11px] font-medium text-on-surface-variant">
                Dữ liệu demo
              </span>
            </div>
            <h1 className="font-headline-lg text-headline-lg font-bold text-on-surface">
              Đơn hàng &amp; Cổng Soát vé QR
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-2xl">
              Xác thực vé xuất bến qua QR, tiếp nhận đơn đặt trước và đối soát chiết khấu thực nhận.
            </p>
          </div>

          <div className="flex items-center gap-space-sm">
            <button
              onClick={() => setShowFinancials(!showFinancials)}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-on-surface font-label-md shadow-sm hover:bg-surface-container transition-all"
            >
              <span className="material-symbols-outlined text-[20px] text-primary">analytics</span>
              <span>{showFinancials ? "Ẩn tài chính" : "Chi tiết chiết khấu"}</span>
            </button>
            <button
              onClick={() => {
                setScannerResult({ type: null });
                setManualCode("");
                setIsTerminalOpen(true);
              }}
              className="inline-flex items-center gap-2 px-space-lg py-2.5 rounded-xl bg-primary text-on-primary font-label-md font-bold shadow-md hover:bg-primary-container transition-all"
            >
              <span className="material-symbols-outlined text-[20px]">qr_code_scanner</span>
              <span>Terminal Quét QR Bến</span>
            </button>
          </div>
        </div>

        {/* Filter Tabs (All 6 statuses distinct, Cancelled & Refunded separated) */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 mt-6 text-nowrap scrollbar-none">
          <button
            onClick={() => setActiveFilter("ALL")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "ALL"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span>Tất cả</span>
            <span className="px-1.5 py-0.5 rounded-full bg-surface-container text-[11px] font-bold">
              {orders.length}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("PENDING")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "PENDING"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-amber-500"></span>
            <span>Cần duyệt (PENDING)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-800 text-[11px] font-bold">
              {countPending}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("CONFIRMED")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "CONFIRMED"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-blue-500"></span>
            <span>Đã xác nhận (CONFIRMED)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-blue-100 text-blue-800 text-[11px] font-bold">
              {countConfirmed}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("COMPLETED")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "COMPLETED"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>Đã đi tour (COMPLETED)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[11px] font-bold">
              {countCompleted}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("REJECTED")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "REJECTED"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-red-400"></span>
            <span>Từ chối (REJECTED)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-red-100 text-red-800 text-[11px] font-bold">
              {countRejected}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("CANCELLED")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "CANCELLED"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-slate-400"></span>
            <span>Đã hủy (CANCELLED)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-slate-200 text-slate-700 text-[11px] font-bold">
              {countCancelled}
            </span>
          </button>

          <button
            onClick={() => setActiveFilter("REFUNDED")}
            className={`px-4 py-2 rounded-xl font-label-md transition-all flex items-center gap-1.5 ${
              activeFilter === "REFUNDED"
                ? "bg-primary text-on-primary font-bold shadow-sm"
                : "bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low"
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-purple-500"></span>
            <span>Đã hoàn tiền (REFUNDED)</span>
            <span className="px-1.5 py-0.5 rounded-full bg-purple-100 text-purple-800 text-[11px] font-bold">
              {countRefunded}
            </span>
          </button>
        </div>
      </div>

      {/* Orders List Container */}
      <div
        key={activeFilter}
        className="px-space-lg py-space-lg lg:px-space-xl max-w-[1360px] mx-auto w-full flex flex-col gap-space-md animate-fade-in-up"
      >
        {filteredOrders.length > 0 ? (
          filteredOrders.map((order) => (
            <div
              key={order.id}
              className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/30 flex flex-col lg:flex-row lg:items-center justify-between gap-space-md hover:border-primary/40 transition-all"
            >
              {/* Order Basic Info */}
              <div className="flex items-start gap-space-md">
                <div className="w-16 h-16 rounded-xl overflow-hidden bg-surface-container-high shrink-0 shadow-sm">
                  <img src={order.image} alt={order.serviceTitle} className="w-full h-full object-cover" />
                </div>
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-label-md font-bold text-primary">#{order.id}</span>
                    <span className="text-[12px] text-on-surface-variant">
                      (Đơn tổng: <strong className="text-on-surface">{order.orderNumber}</strong>)
                    </span>

                    {/* Status Badges */}
                    {order.status === "PENDING" && (
                      <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 text-[11px] font-bold">
                        CHỜ XÁC NHẬN
                      </span>
                    )}
                    {order.status === "CONFIRMED" && (
                      <span className="px-2 py-0.5 rounded-full bg-blue-50 text-blue-800 text-[11px] font-bold">
                        ĐÃ XÁC NHẬN
                      </span>
                    )}
                    {order.status === "COMPLETED" && (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800 text-[11px] font-bold">
                        HOÀN THÀNH
                      </span>
                    )}
                    {order.status === "REJECTED" && (
                      <span className="px-2 py-0.5 rounded-full bg-red-50 text-red-800 text-[11px] font-bold">
                        TỪ CHỐI
                      </span>
                    )}
                    {order.status === "CANCELLED" && (
                      <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 text-[11px] font-bold">
                        ĐÃ HỦY
                      </span>
                    )}
                    {order.status === "REFUNDED" && (
                      <span className="px-2 py-0.5 rounded-full bg-purple-50 text-purple-800 text-[11px] font-bold">
                        ĐÃ HOÀN TIỀN
                      </span>
                    )}

                    {/* Checked-in timestamp */}
                    {order.checked_in_at ? (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-900 text-[10px] font-semibold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[12px]">done_all</span>
                        Check-in: {order.checked_in_at}
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant text-[10px]">
                        Chưa check-in
                      </span>
                    )}
                  </div>

                  <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface mt-1">
                    {order.serviceTitle}
                  </h3>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-on-surface-variant font-body-sm text-[13px] mt-1">
                    <span className="flex items-center gap-1">
                      <span className="material-symbols-outlined text-[15px]">person</span>
                      {order.customerName} ({order.customerPhone})
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="material-symbols-outlined text-[15px]">schedule</span>
                      {order.timeSlot} • {order.dateStr}
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="material-symbols-outlined text-[15px]">pin_drop</span>
                      {order.departurePoint}
                    </span>
                  </div>
                </div>
              </div>

              {/* Financial snapshot & Actions */}
              <div className="flex items-center justify-between lg:justify-end gap-space-lg pt-2 lg:pt-0 border-t lg:border-t-0 border-outline-variant/20">
                <div className="flex flex-col text-right">
                  <span className="text-[12px] text-on-surface-variant">
                    {order.guestCount} khách × {order.unitPrice.toLocaleString("vi-VN")} đ
                  </span>
                  <span className="font-headline-sm font-bold text-on-surface">
                    {order.totalAmount.toLocaleString("vi-VN")} đ
                  </span>
                  {showFinancials && (
                    <span className="text-[11px] text-primary font-semibold mt-0.5">
                      Thực nhận (Net): {order.payoutAmount.toLocaleString("vi-VN")} đ (-{order.commissionRate}%)
                    </span>
                  )}
                </div>

                {/* Actions per status */}
                <div className="flex items-center gap-2">
                  {order.status === "PENDING" && (
                    <>
                      <button
                        onClick={() => handleConfirmOrder(order.id)}
                        className="px-3.5 py-2 rounded-xl bg-primary text-on-primary font-label-sm font-bold hover:bg-primary-container shadow-sm"
                      >
                        Xác nhận
                      </button>
                      <button
                        onClick={() => setRejectOrderId(order.id)}
                        className="px-3 py-2 rounded-xl bg-surface-container-high text-error font-label-sm font-bold hover:bg-error/10"
                      >
                        Từ chối
                      </button>
                    </>
                  )}

                  {order.status === "CONFIRMED" && (
                    <button
                      onClick={() => {
                        setManualCode(order.id);
                        setIsTerminalOpen(true);
                        handleVerifyCode(order.id);
                      }}
                      className="px-3.5 py-2 rounded-xl bg-primary text-on-primary font-label-sm font-bold hover:bg-primary-container shadow-sm flex items-center gap-1"
                    >
                      <span className="material-symbols-outlined text-[16px]">qr_code_scanner</span>
                      Soát vé &amp; Check-in
                    </button>
                  )}

                  {order.status === "REFUNDED" && order.refundInfo && (
                    <button
                      onClick={() => setSelectedRefundOrder(order)}
                      className="px-3 py-2 rounded-xl bg-purple-50 text-purple-800 font-label-sm font-bold hover:bg-purple-100"
                    >
                      Xem chi tiết hoàn tiền
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="p-12 text-center bg-surface-container-lowest rounded-2xl border border-outline-variant/30 text-on-surface-variant">
            <span className="material-symbols-outlined text-4xl mb-2 text-outline">receipt_long</span>
            <p className="font-body-md">Không có đơn hàng nào thuộc trạng thái này.</p>
          </div>
        )}
      </div>

      {/* Terminal Check-in QR Modal with Full Test Suite (Portaled to body) */}
      {isTerminalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface rounded-2xl max-w-lg w-full p-space-xl shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">qr_code_scanner</span>
                <h3 className="font-headline-sm font-bold text-on-surface">Terminal Soát vé Bến bãi VITA</h3>
              </div>
              <button onClick={() => setIsTerminalOpen(false)} className="text-on-surface-variant hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            {/* Test Suite Shortcut Chips */}
            <div className="flex flex-col gap-1.5 p-3 rounded-xl bg-surface-container-low text-[12px]">
              <span className="font-bold text-on-surface">Bộ vé mock thử nghiệm nghiệp vụ:</span>
              <div className="flex flex-wrap gap-1.5">
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89412-01"); handleVerifyCode("SUB-89412-01"); }}
                  className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-900 font-medium hover:bg-emerald-200"
                >
                  ✓ Vé hợp lệ
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89415-03"); handleVerifyCode("SUB-89415-03"); }}
                  className="px-2 py-0.5 rounded bg-amber-100 text-amber-900 font-medium hover:bg-amber-200"
                >
                  ⚠ Chưa xác nhận
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89422-04"); handleVerifyCode("SUB-89422-04"); }}
                  className="px-2 py-0.5 rounded bg-red-100 text-red-900 font-medium hover:bg-red-200"
                >
                  ✗ Đã từ chối
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89425-05"); handleVerifyCode("SUB-89425-05"); }}
                  className="px-2 py-0.5 rounded bg-slate-200 text-slate-800 font-medium hover:bg-slate-300"
                >
                  ✗ Đã hủy
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89428-06"); handleVerifyCode("SUB-89428-06"); }}
                  className="px-2 py-0.5 rounded bg-purple-100 text-purple-900 font-medium hover:bg-purple-200"
                >
                  ✗ Đã hoàn tiền
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-89430-07"); handleVerifyCode("SUB-89430-07"); }}
                  className="px-2 py-0.5 rounded bg-blue-100 text-blue-900 font-medium hover:bg-blue-200"
                >
                  ✗ Đã check-in rồi
                </button>
                <button
                  type="button"
                  onClick={() => { setManualCode("SUB-OTHER-999"); handleVerifyCode("SUB-OTHER-999"); }}
                  className="px-2 py-0.5 rounded bg-red-100 text-red-900 font-medium hover:bg-red-200"
                >
                  ✗ Sai Vendor
                </button>
              </div>
            </div>

            {/* High-Tech Animated Scanner Viewfinder */}
            <div className="relative w-full h-48 rounded-2xl bg-neutral-950/80 border border-primary/30 flex items-center justify-center overflow-hidden shadow-inner group">
              {/* Corner Reticle Brackets */}
              <div className="absolute top-3 left-3 w-6 h-6 border-t-2 border-l-2 border-primary"></div>
              <div className="absolute top-3 right-3 w-6 h-6 border-t-2 border-r-2 border-primary"></div>
              <div className="absolute bottom-3 left-3 w-6 h-6 border-b-2 border-l-2 border-primary"></div>
              <div className="absolute bottom-3 right-3 w-6 h-6 border-b-2 border-r-2 border-primary"></div>

              {/* Sweeping Laser Scan Line */}
              <div className="absolute left-6 right-6 h-0.5 bg-gradient-to-r from-transparent via-primary-container to-transparent shadow-[0_0_15px_#00646f] animate-scan-line pointer-events-none"></div>

              {/* Viewfinder Center Target */}
              <div className="relative z-10 flex flex-col items-center justify-center text-white/40 group-hover:text-white/60 transition-colors">
                <span className="material-symbols-outlined text-[64px] animate-pulse">qr_code_scanner</span>
                <span className="text-[11px] uppercase tracking-widest font-mono text-primary-fixed mt-1">
                  ĐANG CHỜ TÍN HIỆU QR TỪ DU KHÁCH
                </span>
              </div>
            </div>

            {/* Input & Scanner Viewfinder */}
            <div className="flex gap-2">
              <input
                type="text"
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                placeholder="Nhập mã đơn con (ví dụ: SUB-89412-01)..."
                className="flex-1 px-space-md py-2.5 rounded-xl border border-outline-variant bg-surface text-on-surface font-body-md focus:outline-primary"
                onKeyDown={(e) => { if (e.key === "Enter") handleVerifyCode(); }}
              />
              <button
                onClick={() => handleVerifyCode()}
                className="px-4 py-2.5 rounded-xl bg-primary text-on-primary font-bold hover:bg-primary-container shadow-sm cursor-pointer"
              >
                Kiểm tra
              </button>
            </div>

            {/* Scanner Result Display */}
            {scannerResult.type && (
              <div
                className={`p-space-md rounded-xl text-body-sm flex flex-col gap-2 ${
                  scannerResult.type === "SUCCESS"
                    ? "bg-emerald-50 text-emerald-900 border border-emerald-300"
                    : "bg-red-50 text-red-900 border border-red-300"
                }`}
              >
                <div className="flex items-center gap-2 font-bold">
                  <span className="material-symbols-outlined">
                    {scannerResult.type === "SUCCESS" ? "check_circle" : "error"}
                  </span>
                  <span>
                    {scannerResult.type === "SUCCESS"
                      ? "XÁC THỰC VÉ THÀNH CÔNG"
                      : scannerResult.type === "USED"
                      ? "VÉ ĐÃ QUA SỬ DỤNG"
                      : scannerResult.type === "WRONG_VENDOR"
                      ? "SAI ĐỐI TÁC PHỤC VỤ"
                      : "VÉ KHÔNG HỢP LỆ"}
                  </span>
                </div>
                <p>{scannerResult.message}</p>

                {scannerResult.type === "SUCCESS" && scannerResult.order && (
                  <button
                    onClick={() => handleConfirmAdmit(scannerResult.order!.id)}
                    className="mt-2 w-full py-2.5 rounded-xl bg-emerald-600 text-white font-bold hover:bg-emerald-700 shadow-md flex items-center justify-center gap-1.5"
                  >
                    <span className="material-symbols-outlined">how_to_reg</span>
                    Xác nhận Khách Xuất Bến &amp; Cấp Áo Phao
                  </button>
                )}
              </div>
            )}

            {/* Disclaimer on Security & Offline limitation */}
            <p className="text-[11px] text-on-surface-variant leading-relaxed">
              * <em>Lưu ý an toàn:</em> Xác minh QR và chống lặp thực tế cần máy chủ backend đối soát chữ ký điện tử; giao diện không công khai khóa bí mật (qr_secret) và không hỗ trợ nhận diện offline.
            </p>
          </div>
        </div>,
        document.body
      )}

      {/* Reject Order Modal (Portaled to body) */}
      {rejectOrderId && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface rounded-2xl max-w-md w-full p-space-lg shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
            <h3 className="font-headline-sm font-bold text-on-surface">Từ chối đơn #{rejectOrderId}</h3>
            <div className="flex flex-col gap-1.5">
              <label className="font-label-md font-semibold text-on-surface">Lý do an toàn hàng hải</label>
              <select
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                className="p-2.5 rounded-xl border border-outline-variant bg-surface text-on-surface font-body-sm"
              >
                <option value="Lệnh cấm biển khẩn cấp từ Cảng vụ / Thời tiết xấu">
                  Lệnh cấm biển khẩn cấp từ Cảng vụ / Thời tiết xấu
                </option>
                <option value="Đã đạt giới hạn thiết bị phao an toàn kiểm định">
                  Đã đạt giới hạn thiết bị phao an toàn kiểm định
                </option>
                <option value="Không đủ điều kiện sức khỏe thể lực theo quy chuẩn VITA">
                  Không đủ điều kiện sức khỏe thể lực theo quy chuẩn VITA
                </option>
              </select>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setRejectOrderId(null)}
                className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md"
              >
                Hủy
              </button>
              <button
                onClick={handleRejectOrder}
                className="px-4 py-2 rounded-xl bg-error text-white font-label-md font-bold"
              >
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Refund Details Modal (Portaled to body) */}
      {selectedRefundOrder && selectedRefundOrder.refundInfo && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface rounded-2xl max-w-md w-full p-space-xl shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-purple-600">currency_exchange</span>
                <h3 className="font-headline-sm font-bold text-on-surface">Chi tiết Hoàn tiền (Refund)</h3>
              </div>
              <button onClick={() => setSelectedRefundOrder(null)} className="text-on-surface-variant hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="flex flex-col gap-2 font-body-sm">
              <div className="flex justify-between py-1 border-b border-outline-variant/20">
                <span className="text-on-surface-variant">Mã đơn con:</span>
                <span className="font-bold text-on-surface">{selectedRefundOrder.id}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-outline-variant/20">
                <span className="text-on-surface-variant">Số tiền hoàn (amount):</span>
                <span className="font-bold text-purple-700">
                  {selectedRefundOrder.refundInfo.amount.toLocaleString("vi-VN")} đ ({selectedRefundOrder.refundInfo.refund_percentage}%)
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-outline-variant/20">
                <span className="text-on-surface-variant">Trạng thái refund:</span>
                <span className="px-2 py-0.5 rounded-full bg-purple-100 text-purple-900 font-bold text-[11px]">
                  {selectedRefundOrder.refundInfo.status}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-outline-variant/20">
                <span className="text-on-surface-variant">Thời gian xử lý:</span>
                <span className="text-on-surface">{selectedRefundOrder.refundInfo.processed_at}</span>
              </div>
              <div className="flex flex-col py-1">
                <span className="text-on-surface-variant font-semibold">Lý do hoàn tiền:</span>
                <p className="mt-1 p-2 rounded-lg bg-surface-container-low text-on-surface text-[13px]">
                  {selectedRefundOrder.refundInfo.reason}
                </p>
              </div>
            </div>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedRefundOrder(null)}
                className="px-4 py-2 rounded-xl bg-surface-container-high text-on-surface font-label-md"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
          <span className="font-label-md text-label-md">{toastMessage}</span>
        </div>
      )}
    </div>
  );
}
