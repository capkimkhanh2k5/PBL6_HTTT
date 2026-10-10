import { useState } from "react";
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
  status: "PENDING" | "CONFIRMED" | "REJECTED";
}

interface OperationSlot {
  id: string;
  time: string;
  period: string;
  title: string;
  location: string;
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
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Urgent orders list
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
      status: "PENDING",
    },
  ]);

  // Handle Order Confirm / Reject
  const handleOrderAction = (id: string, action: "CONFIRMED" | "REJECTED") => {
    setOrders((prev) =>
      prev.map((o) => (o.id === id ? { ...o, status: action } : o))
    );
    if (action === "CONFIRMED") {
      showToast(`Đã xác nhận đơn #${id}.`);
    } else {
      showToast(`Đã từ chối đơn #${id}.`);
    }
  };

  // Today operation slots
  const [slots] = useState<OperationSlot[]>([
    {
      id: "slot-01",
      time: "05:30",
      period: "Sáng sớm",
      title: "Chèo SUP đón mặt trời mọc",
      location: "Bãi tắm Phạm Văn Đồng",
      booked: 12,
      capacity: 12,
      status: "FULL",
    },
    {
      id: "slot-02",
      time: "08:30",
      period: "Sáng",
      title: "Lặn biển Bãi Bụt Sơn Trà",
      location: "Cảng Bến 02 Mỹ Khê",
      booked: 16,
      capacity: 20,
      status: "AVAILABLE",
    },
    {
      id: "slot-03",
      time: "15:00",
      period: "Chiều",
      title: "Trượt phao chuối cảm giác mạnh",
      location: "Bãi tắm Mỹ Khê 2",
      booked: 10,
      capacity: 15,
      status: "AVAILABLE",
    },
  ]);

  // Recent reviews
  const [reviewsList, setReviewsList] = useState([
    {
      id: "rev-1",
      author: "Hoàng Minh Quân",
      tour: "Chèo SUP ngắm bình minh",
      time: "35 phút trước",
      rating: 5,
      content:
        "Trải nghiệm tuyệt vời! Hướng dẫn viên hỗ trợ nhiệt tình, trang bị áo phao đầy đủ và an toàn.",
      reply: null as string | null,
    },
  ]);

  const [replyText, setReplyText] = useState("");
  const [showReplyBox, setShowReplyBox] = useState(false);

  const handleSendReply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyText.trim()) return;
    setReviewsList((prev) =>
      prev.map((r) => (r.id === "rev-1" ? { ...r, reply: replyText } : r))
    );
    setReplyText("");
    setShowReplyBox(false);
    showToast("Đã gửi phản hồi đánh giá!");
  };

  const pendingOrders = orders.filter((o) => o.status === "PENDING");

  return (
    <div className="w-full px-6 py-4 space-y-3.5 bg-background min-h-screen">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed top-6 right-6 z-50 flex items-center gap-2 px-4 py-2.5 rounded-lg bg-black text-white shadow-xl border border-slate-600">
          <span className="material-symbols-outlined text-[18px] text-emerald-400">check_circle</span>
          <span className="text-xs font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <h1 className="text-xl md:text-2xl font-bold text-black tracking-tight">
            Tổng quan
          </h1>
          <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 text-xs font-bold border border-emerald-300">
            Đã duyệt
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/vendor/services/new')}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-container transition-colors cursor-pointer shadow-xs"
          >
            <span className="material-symbols-outlined text-[16px]">add</span>
            <span>Tạo dịch vụ</span>
          </button>
          <button
            onClick={() => navigate('/vendor/schedule')}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 text-xs font-medium border border-slate-200 transition-colors cursor-pointer shadow-xs"
          >
            <span className="material-symbols-outlined text-[16px]">calendar_month</span>
            <span>Lịch khởi hành</span>
          </button>
        </div>
      </div>

      {/* Compact Inline Metric Strip */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 rounded-xl bg-white border border-slate-200 shadow-xs">
        <div className="flex flex-wrap items-center gap-5 text-xs">
          <button
            onClick={() => navigate('/vendor/orders')}
            className="flex items-center gap-1.5 text-slate-700 hover:text-primary transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px] text-primary">receipt_long</span>
            <span>Đơn cần duyệt:</span>
            <strong className="text-slate-900 font-mono font-bold text-sm ml-0.5">{pendingOrders.length}</strong>
            <span className="text-slate-500 font-normal">yêu cầu</span>
          </button>
          <span className="text-slate-300">•</span>
          <button
            onClick={() => navigate('/vendor/schedule')}
            className="flex items-center gap-1.5 text-slate-700 hover:text-primary transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px] text-primary">schedule</span>
            <span>Ca hôm nay:</span>
            <strong className="text-slate-900 font-mono font-bold text-sm ml-0.5">{slots.length}</strong>
            <span className="text-slate-500 font-normal">khung giờ</span>
          </button>
          <span className="text-slate-300">•</span>
          <button
            onClick={() => navigate('/vendor/revenue')}
            className="flex items-center gap-1.5 text-slate-700 hover:text-primary transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px] text-primary">payments</span>
            <span>Doanh thu kỳ này:</span>
            <strong className="text-slate-900 font-mono font-bold text-sm ml-0.5">14.800.000 đ</strong>
          </button>
          <span className="text-slate-300">•</span>
          <button
            onClick={() => navigate('/vendor/reviews')}
            className="flex items-center gap-1.5 text-slate-700 hover:text-primary transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px] text-amber-500">star</span>
            <span>Đánh giá sàn:</span>
            <strong className="text-slate-900 font-mono font-bold text-sm ml-0.5">4.9/5.0</strong>
            <span className="text-slate-500 font-normal">(86 lượt)</span>
          </button>
        </div>
      </div>

      {/* 2-Column Main Operation Workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3.5 items-start">
        {/* Column 1: Pending Orders to Review (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-3.5">
          <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center justify-between pb-2.5 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-primary"></span>
                <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Đơn đặt tour chờ xác nhận</h2>
              </div>
              <button 
                onClick={() => navigate('/vendor/orders')}
                className="text-xs text-primary font-semibold hover:underline cursor-pointer"
              >
                Xem tất cả ({orders.length})
              </button>
            </div>

            {pendingOrders.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-500">
                Hiện không có đơn hàng nào đang chờ duyệt.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 text-xs">
                {pendingOrders.map((order) => (
                  <div key={order.id} className="py-2.5 px-2 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 hover:bg-slate-50 transition-colors rounded">
                    <div>
                      <div className="font-bold text-slate-900 flex items-center gap-2">
                        <span>{order.serviceName}</span>
                        <span className="px-1.5 py-0.2 bg-slate-100 text-slate-800 text-[11px] font-mono font-semibold rounded border border-slate-200">
                          {order.totalAmount.toLocaleString('vi-VN')} đ
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-600 mt-0.5 flex flex-wrap items-center gap-x-2">
                        <span>Khách: <strong className="text-slate-800">{order.customerName}</strong> ({order.guestCount} {order.guestLabel})</span>
                        <span>•</span>
                        <span className="font-mono">{order.timeSlot} • {order.dateStr}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <button
                        onClick={() => handleOrderAction(order.id, "REJECTED")}
                        className="px-2.5 py-1 text-xs font-medium text-slate-700 bg-white hover:bg-slate-100 rounded border border-slate-200 transition-colors cursor-pointer shadow-xs"
                      >
                        Từ chối
                      </button>
                      <button
                        onClick={() => handleOrderAction(order.id, "CONFIRMED")}
                        className="px-2.5 py-1 text-xs font-semibold text-white bg-primary hover:bg-primary-container rounded transition-colors cursor-pointer shadow-xs"
                      >
                        Xác nhận
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Customer Review Snippet */}
          <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center justify-between pb-2.5 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[16px] text-amber-500">reviews</span>
                <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Đánh giá gần đây</h2>
              </div>
              <button 
                onClick={() => navigate('/vendor/reviews')}
                className="text-xs text-primary font-semibold hover:underline cursor-pointer"
              >
                Tất cả đánh giá
              </button>
            </div>

            {reviewsList.map((rev) => (
              <div key={rev.id} className="py-2.5 px-2 flex flex-col gap-1.5 text-xs hover:bg-slate-50 transition-colors rounded">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900">{rev.author}</span>
                    <span className="text-slate-500 font-medium">• {rev.tour}</span>
                  </div>
                  <div className="flex items-center gap-0.5 text-amber-500">
                    <span>{"★".repeat(rev.rating)}</span>
                    <span className="text-slate-400 text-[10px] ml-1 font-mono">{rev.time}</span>
                  </div>
                </div>
                <p className="text-slate-700 text-xs leading-relaxed">{rev.content}</p>

                {rev.reply && (
                  <div className="mt-1 p-2 rounded bg-slate-50 text-xs text-slate-700 border-l-4 border-l-primary">
                    <span className="font-semibold text-slate-900 block mb-0.5">Phản hồi từ Đối tác:</span>
                    <span>{rev.reply}</span>
                  </div>
                )}

                {!rev.reply && (
                  <div>
                    {!showReplyBox ? (
                      <button
                        onClick={() => setShowReplyBox(true)}
                        className="text-xs text-primary font-semibold hover:underline mt-0.5 cursor-pointer"
                      >
                        + Viết phản hồi cho du khách
                      </button>
                    ) : (
                      <form onSubmit={handleSendReply} className="mt-2 flex flex-col gap-2">
                        <textarea
                          value={replyText}
                          onChange={(e) => setReplyText(e.target.value)}
                          placeholder="Nhập nội dung cảm ơn hoặc giải đáp thắc mắc..."
                          rows={2}
                          className="w-full p-2 text-xs rounded-lg bg-white border border-slate-200 text-slate-800 focus:outline-none focus:border-primary"
                        />
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => setShowReplyBox(false)}
                            className="px-2.5 py-1 text-xs text-slate-600 border border-slate-200 rounded hover:bg-slate-100 cursor-pointer"
                          >
                            Hủy
                          </button>
                          <button
                            type="submit"
                            className="px-3 py-1 text-xs font-semibold bg-primary text-white rounded hover:bg-primary-container cursor-pointer shadow-xs"
                          >
                            Gửi phản hồi
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Column 2: Today Schedule Slots (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-3.5">
          <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center justify-between pb-2.5 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-primary"></span>
                <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Lịch khởi hành hôm nay</h2>
              </div>
              <button 
                onClick={() => navigate('/vendor/schedule')}
                className="text-xs text-primary font-semibold hover:underline cursor-pointer"
              >
                Chi tiết
              </button>
            </div>

            <div className="divide-y divide-slate-100 text-xs">
              {slots.map((slot) => (
                <div key={slot.id} className="py-2.5 px-2 flex items-center justify-between gap-3 hover:bg-slate-50 transition-colors rounded">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 font-mono text-xs">{slot.time}</span>
                      <span className="font-bold text-slate-900 text-xs">{slot.title}</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">{slot.location}</p>
                  </div>

                  <div className="text-right flex-shrink-0">
                    <span className="text-xs font-bold text-slate-900 font-mono block">
                      {slot.booked}/{slot.capacity} khách
                    </span>
                    <span className={`text-[10px] font-semibold px-1.5 py-0.2 rounded border ${
                      slot.status === 'FULL' 
                        ? 'bg-amber-50 text-amber-700 border-amber-200' 
                        : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    }`}>
                      {slot.status === 'FULL' ? 'Hết chỗ' : 'Còn chỗ'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Navigation Panel */}
          <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col gap-2.5">
            <span className="text-xs font-bold text-slate-900 uppercase tracking-wider">Lối tắt quản lý</span>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <button 
                onClick={() => navigate('/vendor/services')}
                className="p-2 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-left font-medium text-slate-700 flex items-center gap-2 transition-colors cursor-pointer shadow-xs"
              >
                <span className="material-symbols-outlined text-[16px] text-primary">sailing</span>
                <span>Dịch vụ ({orders.length})</span>
              </button>
              <button 
                onClick={() => navigate('/vendor/orders')}
                className="p-2 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-left font-medium text-slate-700 flex items-center gap-2 transition-colors cursor-pointer shadow-xs"
              >
                <span className="material-symbols-outlined text-[16px] text-primary">receipt</span>
                <span>Đơn hàng</span>
              </button>
              <button 
                onClick={() => navigate('/vendor/revenue')}
                className="p-2 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-left font-medium text-slate-700 flex items-center gap-2 transition-colors cursor-pointer shadow-xs"
              >
                <span className="material-symbols-outlined text-[16px] text-primary">account_balance_wallet</span>
                <span>Doanh thu</span>
              </button>
              <button 
                onClick={() => navigate('/vendor/profile')}
                className="p-2 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-left font-medium text-slate-700 flex items-center gap-2 transition-colors cursor-pointer shadow-xs"
              >
                <span className="material-symbols-outlined text-[16px] text-primary">store</span>
                <span>Hồ sơ bến</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}

export default Dashboard;
