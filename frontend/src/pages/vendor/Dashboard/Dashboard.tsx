import { useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";

interface UrgentOrder {
  id: string;
  serviceName: string;
  customerName: string;
  timeSlot: string;
  dateStr: string;
  guestCount: number;
  guestLabel: string;
  totalAmount: number;
  paymentNote: string;
  tag: string;
  tagType: "urgent" | "vip" | "pending";
  status: "PENDING" | "CONFIRMED" | "REJECTED";
}

interface OperationSlot {
  id: string;
  time: string;
  period: string;
  title: string;
  location: string;
  captain: string;
  booked: number;
  capacity: number;
  status: "FULL" | "AVAILABLE" | "HALF";
}

export function Dashboard() {
  const navigate = useNavigate();

  // Toast notification
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // State 1: Urgent orders list
  const [orders, setOrders] = useState<UrgentOrder[]>([
    {
      id: "SUB-89412-01",
      serviceName: "Chèo SUP ngắm bình minh Mỹ Khê",
      customerName: "Nguyễn Văn An",
      timeSlot: "05:30 - 07:30",
      dateStr: "Ngày mai (24/10)",
      guestCount: 2,
      guestLabel: "người lớn",
      totalAmount: 700000,
      paymentNote: "Đã thanh toán ví",
      tag: "Chờ duyệt gấp",
      tagType: "urgent",
      status: "PENDING",
    },
    {
      id: "SUB-89415-03",
      serviceName: "Lặn ngắm san hô Bãi Bụt (Cano riêng)",
      customerName: "Trần Thu Hà",
      timeSlot: "08:30 - 11:30",
      dateStr: "Hôm nay (23/10)",
      guestCount: 4,
      guestLabel: "khách",
      totalAmount: 3200000,
      paymentNote: "Đã cọc 100%",
      tag: "VIP Tour",
      tagType: "vip",
      status: "PENDING",
    },
    {
      id: "SUB-89420-02",
      serviceName: "Trượt phao chuối & Moto nước Bãi Rạng",
      customerName: "Lê Hoàng Long",
      timeSlot: "15:00 - 16:30",
      dateStr: "Hôm nay (23/10)",
      guestCount: 6,
      guestLabel: "khách đoàn",
      totalAmount: 1800000,
      paymentNote: "Đã thanh toán",
      tag: "Đang xét",
      tagType: "pending",
      status: "PENDING",
    },
  ]);

  // Handle Order Confirm / Reject
  const handleOrderAction = (id: string, action: "CONFIRMED" | "REJECTED") => {
    setOrders((prev) =>
      prev.map((o) => (o.id === id ? { ...o, status: action } : o))
    );
    if (action === "CONFIRMED") {
      showToast(`Đã duyệt đơn #${id}. Hệ thống đã gửi vé QR và thông báo cho khách.`);
    } else {
      showToast(`Đã từ chối đơn #${id}. Thông báo hoàn tiền được kích hoạt.`);
    }
  };

  // State 2: Today operation slots
  const [slots, setSlots] = useState<OperationSlot[]>([
    {
      id: "slot-01",
      time: "05:30",
      period: "Sáng sớm",
      title: "Chèo SUP đón mặt trời mọc",
      location: "Bãi tắm Phạm Văn Đồng",
      captain: "Hoàng Nam (PADI Pro)",
      booked: 12,
      capacity: 12,
      status: "FULL",
    },
    {
      id: "slot-02",
      time: "08:30",
      period: "Ca sáng",
      title: "Lặn bình khí & Đi bộ dưới biển Bãi Bụt",
      location: "Cầu cảng CT15 Bán đảo Sơn Trà",
      captain: "Văn Tuấn",
      booked: 8,
      capacity: 10,
      status: "AVAILABLE",
    },
    {
      id: "slot-03",
      time: "14:00",
      period: "Ca chiều",
      title: "Tour Cano lướt sóng & Ngắm hoàng hôn Hải Đăng",
      location: "Trạm cứu hộ số 2 Mỹ Khê",
      captain: "Quốc Hùng",
      booked: 6,
      capacity: 12,
      status: "HALF",
    },
  ]);

  // Modal: Tạo ca khẩn cấp (Emergency slot)
  const [isSlotModalOpen, setIsSlotModalOpen] = useState(false);
  const [newSlotTime, setNewSlotTime] = useState("16:00");
  const [newSlotTitle, setNewSlotTitle] = useState("Chèo SUP ngắm hoàng hôn cấp cứu biển");
  const [newSlotCapacity, setNewSlotCapacity] = useState(10);
  const [newSlotCaptain, setNewSlotCaptain] = useState("Đội trưởng cứu hộ Mỹ Khê");

  const handleCreateEmergencySlot = (e: React.FormEvent) => {
    e.preventDefault();
    const newSlot: OperationSlot = {
      id: `slot-${Date.now()}`,
      time: newSlotTime,
      period: "Ca khẩn cấp",
      title: newSlotTitle,
      location: "Trạm cứu hộ số 1 Mỹ Khê",
      captain: newSlotCaptain,
      booked: 0,
      capacity: Number(newSlotCapacity),
      status: "AVAILABLE",
    };
    setSlots((prev) => [...prev, newSlot]);
    setIsSlotModalOpen(false);
    showToast(`Đã tạo thành công ca khẩn cấp lúc ${newSlotTime} (${newSlotCapacity} chỗ)!`);
  };

  // State: Reply to customer review
  const [replyText, setReplyText] = useState("");
  const [showReplyBox, setShowReplyBox] = useState(false);
  const [reviewsList, setReviewsList] = useState([
    {
      id: "rev-1",
      author: "Thảo Linh",
      tour: "Tour Chèo SUP Mỹ Khê",
      time: "35 phút trước",
      rating: 5,
      content:
        "Trải nghiệm tuyệt vời! Huấn luyện viên hỗ trợ rất nhiệt tình, chụp ảnh bằng flycam siêu nét, thiết bị áo phao chuẩn xịn.",
      reply: null as string | null,
    },
  ]);

  const handleSendReply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyText.trim()) return;
    setReviewsList((prev) =>
      prev.map((r) =>
        r.id === "rev-1" ? { ...r, reply: replyText } : r
      )
    );
    setReplyText("");
    setShowReplyBox(false);
    showToast("Đã gửi phản hồi chính thức từ câu lạc bộ đến khách hàng!");
  };

  // Quick export CSV
  const handleExportReport = () => {
    const csvContent =
      "data:text/csv;charset=utf-8," +
      "Ma_Don,Dich_Vu,Khach_Hang,Gio,So_Khach,Tong_Tien,Trang_Thai\n" +
      orders
        .map(
          (o) =>
            `${o.id},"${o.serviceName}","${o.customerName}",${o.timeSlot},${o.guestCount},${o.totalAmount},${o.status}`
        )
        .join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Bao_cao_ca_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("Đã xuất file báo cáo ca làm việc định dạng CSV!");
  };

  const pendingOrdersCount = orders.filter((o) => o.status === "PENDING").length;

  return (
    <div className="w-full pt-6 pb-16 bg-background flex-1">
      {/* Toast alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-on-surface text-surface px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-outline-variant/30 animate-bounce">
          <span className="material-symbols-outlined text-primary text-[22px]">check_circle</span>
          <span className="font-label-md text-label-md font-medium">{toastMessage}</span>
        </div>
      )}

      <div className="w-full max-w-[1280px] mx-auto px-space-lg space-y-space-xl">
        {/* Top Welcome & Verification Header Card */}
        <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm relative overflow-hidden border border-outline-variant/20">
          <div className="absolute -right-12 -top-12 w-64 h-64 rounded-full bg-primary/5 pointer-events-none blur-3xl"></div>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-space-md relative z-10">
            <div className="space-y-space-xs">
              <div className="flex flex-wrap items-center gap-space-sm">
                <span className="font-label-sm text-label-sm uppercase tracking-widest text-primary font-bold">
                  Trung tâm Điều phối Cảng Vụ
                </span>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-semibold">
                  <span className="material-symbols-outlined text-[14px]">verified</span>
                  Huy hiệu xác thực (VERIFIED)
                </span>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-surface-container-high text-on-surface font-label-sm text-label-sm">
                  <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
                  HỒ SƠ: ĐÃ PHÊ DUYỆT (APPROVED)
                </span>
              </div>
              <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">
                Xin chào, Danang Ocean Club!
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Giám sát và vận hành các tour thể thao biển tại bán đảo Sơn Trà & bãi biển Mỹ Khê hôm nay.
              </p>
            </div>

            <div className="flex items-center gap-space-sm">
              <button
                onClick={handleExportReport}
                className="px-space-md py-2.5 rounded-xl bg-surface-container-low hover:bg-surface-container text-on-surface font-label-lg text-label-lg transition-colors flex items-center gap-space-xs shadow-sm cursor-pointer"
                type="button"
              >
                <span className="material-symbols-outlined text-body-lg">download</span>
                Xuất báo cáo ca
              </button>
              <button
                onClick={() => setIsSlotModalOpen(true)}
                className="px-space-md py-2.5 rounded-xl bg-primary text-on-primary hover:bg-primary-container font-label-lg text-label-lg transition-all shadow-sm flex items-center gap-space-xs cursor-pointer"
                type="button"
              >
                <span className="material-symbols-outlined text-body-lg">add_task</span>
                Tạo ca khẩn cấp
              </button>
            </div>
          </div>

          {/* Compliance Alert / Progress Strip */}
          <div className="mt-space-lg p-space-md rounded-xl bg-surface-container-low flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
            <div className="flex items-start sm:items-center gap-space-md">
              <div className="w-10 h-10 rounded-xl bg-secondary-container/20 text-secondary flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-headline-sm">security_update_warning</span>
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="font-label-md text-label-md text-on-surface font-bold">
                    Hồ sơ đối tác đạt 90% chuẩn vận hành biển
                  </span>
                  <span className="text-secondary font-label-sm text-label-sm font-semibold">
                    Cần cập nhật trước 30/11
                  </span>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  Vui lòng nộp bản kiểm định định kỳ "Giấy chứng nhận an toàn kỹ thuật thiết bị lặn biển & bình khí nén" cho Đội Thanh tra Cảng vụ.
                </p>
              </div>
            </div>
            <div className="flex items-center gap-space-md sm:w-80 shrink-0">
              <div className="flex-1 space-y-1">
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Tiến trình chuẩn hóa</span>
                  <span className="text-primary font-bold">90%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-surface-container-highest overflow-hidden">
                  <div className="h-full bg-primary rounded-full transition-all duration-500 w-[90%]"></div>
                </div>
              </div>
              <button
                onClick={() => navigate("/vendor/profile")}
                className="px-space-sm py-1.5 rounded-lg bg-surface-container-lowest text-primary hover:bg-surface-bright font-label-sm text-label-sm transition-colors shadow-sm shrink-0 cursor-pointer font-bold"
                type="button"
              >
                Tải lên hồ sơ
              </button>
            </div>
          </div>
        </div>

        {/* 4 Main KPI Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-md">
          {/* KPI 1: Doanh thu tạm tính */}
          <div
            onClick={() => navigate("/vendor/revenue")}
            className="card-hover group bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between space-y-space-md cursor-pointer border border-outline-variant/20 hover:border-primary/40 transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
                Doanh thu tạm tính tháng này
              </span>
              <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center group-hover:scale-110 group-hover:bg-primary group-hover:text-on-primary transition-all duration-300">
                <span className="material-symbols-outlined text-body-lg">payments</span>
              </div>
            </div>
            <div>
              <div className="font-display-lg text-[32px] leading-tight font-headline-xl text-on-surface tracking-tight font-bold">
                48.650.000 <span className="text-headline-sm font-headline-sm text-on-surface-variant font-normal">đ</span>
              </div>
              <div className="flex items-center gap-1.5 mt-2">
                <span className="inline-flex items-center font-label-sm text-label-sm text-primary font-bold">
                  <span className="material-symbols-outlined text-body-sm">trending_up</span> +14%
                </span>
                <span className="font-body-sm text-body-sm text-on-surface-variant">so với tháng trước</span>
              </div>
            </div>
            <p className="font-body-sm text-body-sm text-tertiary">Doanh thu ghi nhận từ sub_orders thực tế sau dịch vụ.</p>
          </div>

          {/* KPI 2: Đơn cần xác nhận ngay */}
          <div
            onClick={() => navigate("/vendor/orders")}
            className="card-hover group bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between space-y-space-md relative overflow-hidden cursor-pointer border border-outline-variant/20 hover:border-secondary/40 transition-all"
          >
            <div className="absolute top-0 right-0 w-1.5 h-full bg-secondary"></div>
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant flex items-center">
                <span className="relative flex h-2 w-2 mr-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-secondary opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-secondary"></span>
                </span>
                Đơn cần xác nhận ngay
              </span>
              <span className="px-2 py-0.5 rounded-full bg-secondary-container/15 text-secondary font-label-sm text-label-sm font-bold">
                Xử lý ≤ 30p
              </span>
            </div>
            <div>
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-[32px] leading-tight font-headline-xl text-secondary font-bold">
                  {String(pendingOrdersCount).padStart(2, "0")}
                </span>
                <span className="font-headline-sm text-headline-sm text-on-surface">đơn mới</span>
              </div>
              <div className="flex items-center gap-1.5 mt-2">
                <span className="material-symbols-outlined text-secondary text-body-sm">timer</span>
                <span className="font-body-sm text-body-sm text-on-surface-variant">2 đơn cận giờ khởi hành sáng</span>
              </div>
            </div>
            <p className="font-body-sm text-body-sm text-secondary font-medium">Cần bấm xác nhận để giữ điều phối cano & HLV.</p>
          </div>

          {/* KPI 3: Khách trải nghiệm hôm nay */}
          <div
            onClick={() => navigate("/vendor/schedule")}
            className="card-hover group bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between space-y-space-md cursor-pointer border border-outline-variant/20 hover:border-primary/40 transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
                Khách trải nghiệm hôm nay
              </span>
              <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center group-hover:scale-110 group-hover:bg-primary group-hover:text-on-primary transition-all duration-300">
                <span className="material-symbols-outlined text-body-lg">surfing</span>
              </div>
            </div>
            <div>
              <div className="font-display-lg text-[32px] leading-tight font-headline-xl text-on-surface font-bold">
                26 <span className="text-headline-sm font-headline-sm text-on-surface-variant font-normal">lượt khách</span>
              </div>
              <div className="flex items-center gap-2 mt-2 font-body-sm text-body-sm text-on-surface-variant">
                <span className="inline-flex items-center gap-1 font-medium text-on-surface">
                  <span className="w-2 h-2 rounded-full bg-primary"></span>03 ca sáng
                </span>
                <span className="text-outline-variant">•</span>
                <span className="inline-flex items-center gap-1 font-medium text-on-surface">
                  <span className="w-2 h-2 rounded-full bg-tertiary"></span>02 ca chiều
                </span>
              </div>
            </div>
            <div className="w-full bg-surface-container-high h-1.5 rounded-full overflow-hidden flex">
              <div className="bg-primary h-full w-[65%] transition-all duration-700 ease-out"></div>
              <div className="bg-tertiary h-full w-[35%] transition-all duration-700 ease-out"></div>
            </div>
          </div>

          {/* KPI 4: Tỷ lệ đánh giá chất lượng */}
          <div
            onClick={() => navigate("/vendor/reviews")}
            className="card-hover group bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between space-y-space-md cursor-pointer border border-outline-variant/20 hover:border-primary/40 transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
                Đánh giá chất lượng
              </span>
              <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center group-hover:scale-110 group-hover:bg-primary group-hover:text-on-primary transition-all duration-300">
                <span className="material-symbols-outlined text-body-lg">star</span>
              </div>
            </div>
            <div>
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-[32px] leading-tight font-headline-xl text-on-surface font-bold">
                  4.92
                </span>
                <span className="font-headline-sm text-headline-sm text-on-surface-variant">/ 5.0</span>
              </div>
              <div className="flex items-center gap-1.5 mt-2">
                <span className="material-symbols-outlined text-amber-500 text-body-sm" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                <span className="font-body-sm text-body-sm text-on-surface-variant">147 đánh giá hợp lệ</span>
              </div>
            </div>
            <p className="font-body-sm text-body-sm text-emerald-700 font-semibold flex items-center gap-1">
              <span className="material-symbols-outlined text-[16px]">verified</span> 99% phản hồi tích cực
            </p>
          </div>
        </div>

        {/* Core Layout 2 Columns: Left 8 cols, Right 4 cols */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* Left Column (65%) */}
          <div className="lg:col-span-8 space-y-space-lg">
            {/* Urgent Sub-orders Table Card */}
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm mb-space-md">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="font-headline-md text-headline-md text-on-surface font-bold">
                      Đơn trải nghiệm cần xử lý gấp
                    </h2>
                    {pendingOrdersCount > 0 && (
                      <span className="px-2 py-0.5 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm font-bold">
                        {pendingOrdersCount} Chờ duyệt
                      </span>
                    )}
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">
                    Cần phản hồi nhanh trong 30 phút để chốt slot cano và huấn luyện viên cứu hộ.
                  </p>
                </div>
                <button
                  onClick={() => navigate("/vendor/orders")}
                  className="font-label-md text-label-md text-primary hover:underline font-semibold inline-flex items-center gap-1 cursor-pointer"
                >
                  Xem toàn bộ đơn hàng
                  <span className="material-symbols-outlined text-body-md">arrow_forward</span>
                </button>
              </div>

              {/* Table Container */}
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-surface-container-low text-on-surface-variant font-label-sm text-label-sm uppercase">
                      <th className="py-3 px-space-md rounded-l-xl">Mã Sub-order</th>
                      <th className="py-3 px-space-md">Dịch vụ & Khách hàng</th>
                      <th className="py-3 px-space-md">Khung giờ</th>
                      <th className="py-3 px-space-md text-center">Số khách</th>
                      <th className="py-3 px-space-md text-right">Tổng tiền</th>
                      <th className="py-3 px-space-md rounded-r-xl text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant/20">
                    {orders.map((o) => (
                      <tr key={o.id} className="group hover:bg-surface-container-low/50 transition-colors">
                        <td className="py-3.5 px-space-md">
                          <div className="font-label-md text-label-md font-bold text-on-surface">#{o.id}</div>
                          {o.status === "PENDING" ? (
                            <span className="text-[10px] uppercase tracking-wider text-secondary font-bold px-1.5 py-0.5 bg-secondary-container/30 rounded">
                              {o.tag}
                            </span>
                          ) : o.status === "CONFIRMED" ? (
                            <span className="text-[10px] uppercase tracking-wider text-primary font-bold px-1.5 py-0.5 bg-primary/10 rounded">
                              ĐÃ XÁC NHẬN
                            </span>
                          ) : (
                            <span className="text-[10px] uppercase tracking-wider text-error font-bold px-1.5 py-0.5 bg-error-container/30 rounded">
                              TỪ CHỐI
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-space-md">
                          <div className="font-label-md text-label-md font-semibold text-on-surface">
                            {o.serviceName}
                          </div>
                          <div className="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px]">person</span>
                            {o.customerName}
                          </div>
                        </td>
                        <td className="py-3.5 px-space-md">
                          <span className="px-2 py-1 rounded-md bg-surface-container font-label-sm text-label-sm text-on-surface font-medium whitespace-nowrap">
                            {o.timeSlot}
                          </span>
                          <div className="font-body-sm text-[11px] text-tertiary mt-0.5">{o.dateStr}</div>
                        </td>
                        <td className="py-3.5 px-space-md text-center">
                          <span className="font-label-md text-label-md font-bold text-on-surface">
                            {String(o.guestCount).padStart(2, "0")}
                          </span>
                          <span className="text-body-sm text-[11px] text-on-surface-variant block">
                            {o.guestLabel}
                          </span>
                        </td>
                        <td className="py-3.5 px-space-md text-right">
                          <div className="font-label-md text-label-md font-bold text-primary">
                            {o.totalAmount.toLocaleString("vi-VN")} đ
                          </div>
                          <span className="text-[10px] text-outline">{o.paymentNote}</span>
                        </td>
                        <td className="py-3.5 px-space-md text-center">
                          {o.status === "PENDING" ? (
                            <div className="flex items-center justify-center gap-1.5">
                              <button
                                onClick={() => handleOrderAction(o.id, "CONFIRMED")}
                                className="px-3 py-1.5 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-semibold hover:bg-primary-container shadow-sm transition-all cursor-pointer"
                                type="button"
                              >
                                Xác nhận ngay
                              </button>
                              <button
                                onClick={() => handleOrderAction(o.id, "REJECTED")}
                                className="px-2.5 py-1.5 rounded-lg bg-surface-container hover:bg-error-container/20 text-on-surface-variant hover:text-error font-label-sm text-label-sm transition-colors cursor-pointer"
                                type="button"
                              >
                                Từ chối
                              </button>
                            </div>
                          ) : (
                            <span className="font-label-sm text-[12px] text-on-surface-variant">
                              {o.status === "CONFIRMED" ? "Đã duyệt" : "Đã từ chối"}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Today Operations Schedule Card */}
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm mb-space-lg">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="font-headline-md text-headline-md text-on-surface font-bold">
                      Lịch vận hành thực địa hôm nay
                    </h2>
                    <span className="w-2.5 h-2.5 rounded-full bg-primary animate-ping"></span>
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">
                    Kiểm soát tỷ lệ lấp đầy sức chứa ca lặn, SUP và phân bổ hướng dẫn viên an toàn.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="px-3 py-1 rounded-lg bg-surface-container font-label-sm text-label-sm text-on-surface font-semibold">
                    Hôm nay: 23/10/2024
                  </span>
                  <button
                    onClick={() => navigate("/vendor/schedule")}
                    className="p-1.5 rounded-lg bg-surface-container-low hover:bg-surface-container text-primary font-label-sm text-label-sm transition-colors cursor-pointer"
                    title="Mở toàn màn hình lịch"
                  >
                    <span className="material-symbols-outlined text-[18px]">open_in_new</span>
                  </button>
                </div>
              </div>

              {/* Schedule Grid Slots */}
              <div className="space-y-space-md">
                {slots.map((slot) => (
                  <div
                    key={slot.id}
                    className="p-space-md rounded-xl bg-surface-container-low flex flex-col sm:flex-row sm:items-center justify-between gap-space-md"
                  >
                    <div className="flex items-center gap-space-md">
                      <div className="w-16 h-16 rounded-xl bg-primary-container text-on-primary-container flex flex-col items-center justify-center shrink-0">
                        <span className="font-headline-sm text-headline-sm leading-none font-bold">
                          {slot.time}
                        </span>
                        <span className="text-[10px] tracking-wider uppercase mt-1">
                          {slot.period}
                        </span>
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                            {slot.title}
                          </h3>
                          {slot.status === "FULL" ? (
                            <span className="px-2 py-0.5 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm font-bold">
                              Đầy chỗ
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                              Đang nhận
                            </span>
                          )}
                        </div>
                        <p className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                          Địa điểm: {slot.location} • Đội trưởng: {slot.captain}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-space-lg sm:w-60 justify-between sm:justify-end">
                      <div className="text-right">
                        <div className="font-headline-sm text-headline-sm text-on-surface font-bold">
                          {slot.booked} / {slot.capacity}{" "}
                          <span className="text-body-sm text-on-surface-variant font-normal">khách</span>
                        </div>
                        <div className="font-body-sm text-[11px] text-primary font-semibold">
                          {slot.capacity - slot.booked === 0
                            ? "100% Sức chứa"
                            : `Còn ${slot.capacity - slot.booked} chỗ trống`}
                        </div>
                      </div>
                      <div className="w-20 bg-surface-container-highest h-2 rounded-full overflow-hidden">
                        <div
                          className="bg-primary h-full rounded-full transition-all duration-500"
                          style={{
                            width: `${Math.min(100, (slot.booked / slot.capacity) * 100)}%`,
                          }}
                        ></div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right Column (35%) */}
          <div className="lg:col-span-4 space-y-space-lg">
            {/* Weather & Maritime Safety Card */}
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm relative overflow-hidden border border-outline-variant/20">
              <div className="flex items-center justify-between mb-space-md">
                <div>
                  <span className="font-label-sm text-label-sm uppercase tracking-wider text-primary font-bold">
                    Dữ liệu Cảng vụ Đà Nẵng
                  </span>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    Thời tiết Biển & Hải trình
                  </h3>
                </div>
                <div className="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center text-on-primary-fixed">
                  <span className="material-symbols-outlined text-body-lg">air</span>
                </div>
              </div>

              {/* Status Indicator Pill */}
              <div className="p-space-sm rounded-xl bg-primary/10 text-primary flex items-center gap-2 mb-space-md">
                <span className="w-2.5 h-2.5 rounded-full bg-primary animate-pulse"></span>
                <span className="font-label-sm text-label-sm font-bold">Điều kiện an toàn - Cho phép xuất bến</span>
              </div>

              {/* Weather Metric Items */}
              <div className="grid grid-cols-3 gap-2 p-space-sm rounded-xl bg-surface-container-low mb-space-md text-center">
                <div className="p-2">
                  <span className="material-symbols-outlined text-primary text-body-lg">waves</span>
                  <div className="font-headline-sm text-headline-sm text-on-surface font-bold mt-1">0.5m</div>
                  <span className="font-label-sm text-[11px] text-on-surface-variant block">Độ cao sóng</span>
                </div>
                <div className="p-2">
                  <span className="material-symbols-outlined text-primary text-body-lg">navigation</span>
                  <div className="font-headline-sm text-headline-sm text-on-surface font-bold mt-1">12 km/h</div>
                  <span className="font-label-sm text-[11px] text-on-surface-variant block">Gió Đông Nam</span>
                </div>
                <div className="p-2">
                  <span className="material-symbols-outlined text-primary text-body-lg">water_drop</span>
                  <div className="font-headline-sm text-headline-sm text-on-surface font-bold mt-1">0 mm</div>
                  <span className="font-label-sm text-[11px] text-on-surface-variant block">Lượng mưa</span>
                </div>
              </div>

              {/* Visual Safety Barometer Graph */}
              <div className="space-y-1.5">
                <div className="flex justify-between font-body-sm text-body-sm">
                  <span className="text-on-surface-variant">Độ êm mặt biển</span>
                  <span className="text-primary font-bold">Cực tốt (Cấp 1-2)</span>
                </div>
                <div className="w-full h-1.5 rounded-full bg-surface-container-highest overflow-hidden">
                  <div className="h-full bg-primary rounded-full w-[85%]"></div>
                </div>
                <p className="text-[11px] text-on-surface-variant">Cảm biến phao biển cập nhật: 10 phút trước</p>
              </div>
            </div>

            {/* Latest Customer Reviews Card with Reply Box */}
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20">
              <div className="flex items-center justify-between mb-space-md">
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">Đánh giá mới nhất</h3>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">Khách vừa trải nghiệm</p>
                </div>
                <button
                  onClick={() => navigate("/vendor/reviews")}
                  className="font-label-sm text-label-sm text-primary font-semibold hover:underline cursor-pointer"
                >
                  Tất cả (128)
                </button>
              </div>

              {reviewsList.map((rev) => (
                <div key={rev.id} className="p-space-md rounded-xl bg-surface-container-low space-y-space-sm">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-space-xs">
                      <div className="w-8 h-8 rounded-full bg-primary/20 text-primary flex items-center justify-center font-bold text-label-sm">
                        TL
                      </div>
                      <div>
                        <div className="font-label-sm text-label-sm font-bold text-on-surface">{rev.author}</div>
                        <div className="text-[10px] text-on-surface-variant">{rev.tour} • {rev.time}</div>
                      </div>
                    </div>
                    <div className="flex text-amber-500">
                      {[...Array(rev.rating)].map((_, i) => (
                        <span key={i} className="material-symbols-outlined text-[15px]">
                          star
                        </span>
                      ))}
                    </div>
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface italic">
                    "{rev.content}"
                  </p>

                  {rev.reply && (
                    <div className="p-2.5 rounded-lg bg-surface-container-lowest border-l-2 border-primary mt-2">
                      <div className="text-[11px] font-bold text-primary">Phản hồi từ Danang Ocean Club:</div>
                      <div className="text-body-sm text-[12px] text-on-surface mt-0.5">{rev.reply}</div>
                    </div>
                  )}

                  {!rev.reply && (
                    <div className="pt-2 flex items-center gap-2">
                      <button
                        onClick={() => setShowReplyBox(!showReplyBox)}
                        className="flex-1 py-1.5 px-3 rounded-lg bg-surface-container-lowest text-primary hover:bg-surface-bright font-label-sm text-label-sm font-semibold transition-colors flex items-center justify-center gap-1 cursor-pointer"
                        type="button"
                      >
                        <span className="material-symbols-outlined text-body-sm">reply</span>
                        {showReplyBox ? "Hủy phản hồi" : "Phản hồi khách"}
                      </button>
                    </div>
                  )}

                  {showReplyBox && !rev.reply && (
                    <form onSubmit={handleSendReply} className="mt-2 space-y-2">
                      <textarea
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        placeholder="Nhập lời cảm ơn hoặc giải đáp của câu lạc bộ..."
                        className="w-full p-2 text-body-sm rounded-lg border border-outline-variant/30 bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
                        rows={2}
                      ></textarea>
                      <button
                        type="submit"
                        className="w-full py-1.5 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-bold hover:bg-primary-container transition-all cursor-pointer"
                      >
                        Gửi phản hồi
                      </button>
                    </form>
                  )}
                </div>
              ))}
            </div>

            {/* Maritime Safety Support Card */}
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20">
              <div className="flex items-center gap-space-sm mb-space-sm">
                <div className="w-10 h-10 rounded-xl bg-tertiary/10 text-tertiary flex items-center justify-center">
                  <span className="material-symbols-outlined text-body-xl">support_agent</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    BQL Bán Đảo Sơn Trà & Cảng Vụ
                  </h3>
                  <p className="font-body-sm text-[11px] text-on-surface-variant">Kênh hỗ trợ cứu hộ khẩn cấp 24/7</p>
                </div>
              </div>
              <div className="space-y-space-sm mt-space-md">
                <div className="p-space-sm rounded-xl bg-surface-container-low flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary text-body-lg">emergency</span>
                    <span className="font-label-md text-label-md text-on-surface font-semibold">Hotline Cứu nạn:</span>
                  </div>
                  <a className="font-headline-sm text-headline-sm text-secondary font-bold hover:underline" href="tel:1900889922">
                    1900 8899 22
                  </a>
                </div>
                <div className="p-space-sm rounded-xl bg-surface-container-low flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-primary text-body-lg">radio</span>
                    <span className="font-label-sm text-label-sm text-on-surface">Kênh bộ đàm VHF:</span>
                  </div>
                  <span className="font-label-md text-label-md text-primary font-bold">CH 16 (156.8 MHz)</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL: TẠO CA KHẨN CẤP (Portaled to body) */}
      {isSlotModalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-lg w-full p-space-xl shadow-2xl border border-outline-variant/30 space-y-space-md">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">add_task</span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">Tạo ca biển khẩn cấp</h3>
              </div>
              <button
                onClick={() => setIsSlotModalOpen(false)}
                className="p-1 rounded-lg text-on-surface-variant hover:bg-surface-container cursor-pointer"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleCreateEmergencySlot} className="space-y-4">
              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Tên dịch vụ / Ca cứu hộ
                </label>
                <input
                  type="text"
                  required
                  value={newSlotTitle}
                  onChange={(e) => setNewSlotTitle(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Giờ khởi hành
                  </label>
                  <input
                    type="time"
                    required
                    value={newSlotTime}
                    onChange={(e) => setNewSlotTime(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Sức chứa (Khách)
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={100}
                    required
                    value={newSlotCapacity}
                    onChange={(e) => setNewSlotCapacity(Number(e.target.value))}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Người phụ trách / Hướng dẫn viên
                </label>
                <input
                  type="text"
                  required
                  value={newSlotCaptain}
                  onChange={(e) => setNewSlotCaptain(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsSlotModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md cursor-pointer hover:bg-surface-container-high transition-colors"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold cursor-pointer hover:bg-primary-container transition-all shadow-md"
                >
                  Tạo ca ngay
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
