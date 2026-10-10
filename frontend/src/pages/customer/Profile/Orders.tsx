import { useState, useMemo } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { FEATURED_SERVICES, MOCK_IMAGES, MOCK_SLOTS, MOCK_MASTER_ORDERS, MOCK_SUB_ORDERS } from "../../../mockData";
import type { SubOrder } from "../../../types";

export function Orders() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState("all");
  const [selectedQrOrder, setSelectedQrOrder] = useState<SubOrder | null>(null);

  // Review & Rating Modal state
  const [reviewingOrder, setReviewingOrder] = useState<SubOrder | null>(null);
  const [ratingStars, setRatingStars] = useState(5);
  const [reviewComment, setReviewComment] = useState("");
  const [submittedReviews, setSubmittedReviews] = useState<Record<string, { rating: number; comment: string }>>({});

  // Dispute Modal state
  const [disputingOrder, setDisputingOrder] = useState<SubOrder | null>(null);
  const [disputeReason, setDisputeReason] = useState("");
  const [submittedDisputes, setSubmittedDisputes] = useState<Record<string, string>>({});

  const filteredMasterOrders = useMemo(() => {
    if (filter === "all") return MOCK_MASTER_ORDERS;
    if (filter === "upcoming") return MOCK_MASTER_ORDERS.filter(o => o.status === 'CONFIRMED');
    if (filter === "completed") return MOCK_MASTER_ORDERS.filter(o => o.status === 'CANCELLED' || o.status === 'COMPLETED');
    return MOCK_MASTER_ORDERS;
  }, [filter]);

  const handleOpenReview = (subOrder: SubOrder) => {
    setReviewingOrder(subOrder);
    setRatingStars(5);
    setReviewComment("");
  };

  const handleSubmitReview = () => {
    if (!reviewingOrder) return;
    setSubmittedReviews(prev => ({
      ...prev,
      [reviewingOrder.id]: { rating: ratingStars, comment: reviewComment }
    }));
    setReviewingOrder(null);
  };

  const handleOpenDispute = (subOrder: SubOrder) => {
    setDisputingOrder(subOrder);
    setDisputeReason("");
  };

  const handleSubmitDispute = () => {
    if (!disputingOrder || !disputeReason.trim()) return;
    setSubmittedDisputes(prev => ({
      ...prev,
      [disputingOrder.id]: disputeReason
    }));
    setDisputingOrder(null);
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1">
      <div className="flex flex-col w-full">
        <div className="relative w-full overflow-hidden border-b border-slate-200 bg-white">
          <div className="max-w-[1440px] w-full mx-auto px-4 sm:px-8 lg:px-12 py-6">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Trải nghiệm của tôi</h1>
                <p className="text-sm text-slate-500 mt-1">Quản lý các tour đã đặt, mã vé điện tử QR xuất bến và gửi đánh giá dịch vụ.</p>
              </div>
              
              <div className="flex items-center gap-3">
                <div className="px-3.5 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-center">
                  <span className="block text-base font-bold text-primary">02</span>
                  <span className="text-[11px] text-slate-500 font-medium">Sắp diễn ra</span>
                </div>
                <div className="px-3.5 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-center">
                  <span className="block text-base font-bold text-slate-800">01</span>
                  <span className="text-[11px] text-slate-500 font-medium">Đã đi</span>
                </div>
              </div>
            </div>

            <div className="mt-4 flex items-center gap-2">
              <button onClick={() => setFilter("all")} className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${filter === 'all' ? 'bg-primary text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`} type="button">
                Tất cả ({MOCK_MASTER_ORDERS.length})
              </button>
              <button onClick={() => setFilter("upcoming")} className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${filter === 'upcoming' ? 'bg-primary text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`} type="button">
                Sắp diễn ra ({MOCK_MASTER_ORDERS.filter(o => o.status === 'CONFIRMED').length})
              </button>
              <button onClick={() => setFilter("completed")} className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${filter === 'completed' ? 'bg-primary text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`} type="button">
                Đã hoàn thành ({MOCK_MASTER_ORDERS.filter(o => o.status === 'CANCELLED' || o.status === 'COMPLETED').length})
              </button>
            </div>
          </div>
        </div>

        <div className="max-w-[1440px] w-full mx-auto px-4 sm:px-8 lg:px-12 py-8">
          {/* Full-width safety guarantee banner */}
          <div className="w-full bg-cyan-50/80 border border-cyan-200/80 rounded-2xl p-4 flex items-center justify-between gap-4 mb-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-cyan-600 text-white flex items-center justify-center font-bold text-lg flex-shrink-0">
                <span className="material-symbols-outlined text-[20px]">verified_user</span>
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Chính sách bảo trợ &amp; An toàn biển</h3>
                <p className="text-xs text-slate-600">Tất cả các dịch vụ (Sub-orders) được xử lý qua hệ thống DANASEA đều được áp dụng bảo hiểm du lịch biển và chính sách bảo trợ an toàn tự động.</p>
              </div>
            </div>
          </div>

          <div key={filter} className="w-full flex flex-col gap-6 animate-fade-in-up">
            {filteredMasterOrders.map(masterOrder => {
              const subOrders = MOCK_SUB_ORDERS.filter(sub => sub.masterOrderId === masterOrder.id);
              return (
                <article key={masterOrder.id} className="order-card bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-slate-100 flex flex-col gap-6 transition-all duration-300 hover:shadow-md w-full">
                  <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-100">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container flex-shrink-0">
                        <span className="material-symbols-outlined text-[20px]">receipt_long</span>
                      </div>
                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Master Order:</span>
                          <span className="text-base font-bold text-slate-900">#{masterOrder.id.toUpperCase()}</span>
                          <span className="px-2.5 py-0.5 rounded-full bg-secondary-fixed text-on-secondary-fixed text-xs font-bold">
                            {masterOrder.status === 'CONFIRMED' ? 'PAID (Đã thanh toán)' : 'COMPLETED'}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">Ngày tạo: {new Date(masterOrder.createdAt || '').toLocaleDateString('vi-VN')} · Phương thức thanh toán: QR Pay</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="text-xs text-slate-500 uppercase tracking-wider block font-medium">Tổng thanh toán Master</span>
                      <span className="text-xl text-primary font-bold">{masterOrder.totalAmount.toLocaleString('vi-VN')}đ</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[18px] text-secondary">layers</span>
                      Danh sách đơn con (Sub Orders)
                    </span>
                    <span className="text-xs font-semibold text-slate-500">{subOrders.length} dịch vụ</span>
                  </div>

                  {subOrders.length === 0 ? (
                    <div className="p-4 rounded-xl bg-slate-50 text-center text-xs text-slate-500">
                      Không có đơn con nào trong đơn hàng này.
                    </div>
                  ) : (
                    subOrders.map(subOrder => {
                      const service = FEATURED_SERVICES.find(s => s.id === subOrder.serviceId);
                      const slot = MOCK_SLOTS.find(s => s.id === subOrder.slotId);
                      const image = MOCK_IMAGES.find(i => i.serviceId === subOrder.serviceId && i.isPrimary)?.imageUrl;
                      
                      return (
                        <div key={subOrder.id} className="bg-surface-container-low rounded-xl p-4 flex flex-col md:flex-row gap-4 w-full">
                          <div className="relative w-full md:w-56 h-36 flex-shrink-0 rounded-xl overflow-hidden shadow-sm">
                            <img src={image || 'https://via.placeholder.com/400x300'} className="w-full h-full object-cover" alt={service?.name} />
                            <span className="absolute top-2 left-2 px-2.5 py-0.5 rounded-full bg-secondary text-on-secondary text-xs font-semibold flex items-center gap-1 shadow-sm">
                              <span className="w-1.5 h-1.5 rounded-full bg-secondary-fixed animate-ping"></span>
                              {slot?.startTime} {new Date(slot?.date || '').toLocaleDateString('vi-VN')}
                            </span>
                          </div>
                          <div className="flex flex-col justify-between flex-1 min-w-0">
                            <div>
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="flex items-center gap-2">
                                    <span className="font-mono font-bold text-xs text-secondary">#{subOrder.id.toUpperCase()}</span>
                                    <span className="text-xs text-slate-500">· NCC: <strong>DANASEA Partner</strong></span>
                                  </div>
                                  <h3 className="text-base font-bold text-slate-900 mt-1 hover:text-primary cursor-pointer transition-colors" onClick={() => navigate(`/tour/${service?.id}`)}>
                                    {service?.name}
                                  </h3>
                                </div>
                                <div className="flex flex-col items-end">
                                  <span className="text-base text-primary font-bold">{subOrder.subtotalAmount.toLocaleString('vi-VN')}đ</span>
                                  <span className="text-xs text-slate-500">SL: {subOrder.quantity} người</span>
                                </div>
                              </div>
                              <div className="flex items-center gap-2 mt-2">
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-secondary-container/30 text-on-secondary-container text-xs">
                                  <span className="material-symbols-outlined text-[14px]">map</span>
                                  {service?.locationName}
                                </span>
                              </div>
                            </div>
                            
                            <div className="flex flex-wrap items-center justify-between gap-3 pt-3 mt-3 border-t border-slate-200">
                              <div className="flex items-center gap-2">
                                <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                                  subOrder.status === 'CONFIRMED' 
                                    ? 'bg-blue-50 text-blue-700 border border-blue-200' 
                                    : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                }`}>
                                  {subOrder.status === 'CONFIRMED' ? 'ĐÃ XÁC NHẬN' : 'HOÀN THÀNH'}
                                </span>
                                {submittedReviews[subOrder.id] && (
                                  <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 text-xs font-medium">
                                    ★ Đã đánh giá ({submittedReviews[subOrder.id].rating} sao)
                                  </span>
                                )}
                                {submittedDisputes[subOrder.id] && (
                                  <span className="px-2 py-0.5 rounded-full bg-red-50 text-red-700 border border-red-200 text-xs font-medium">
                                    Đang xử lý khiếu nại
                                  </span>
                                )}
                              </div>
                              <div className="flex items-center gap-2">
                                {subOrder.status === 'CONFIRMED' ? (
                                  <>
                                    <button 
                                      className="px-3.5 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold flex items-center gap-1 shadow-sm transition-transform hover:scale-105 active:scale-95 cursor-pointer" 
                                      type="button" 
                                      onClick={() => setSelectedQrOrder(subOrder)}
                                    >
                                      <span className="material-symbols-outlined text-[15px]">qr_code_2</span>
                                      <span>Mã QR Check-in</span>
                                    </button>
                                    <button 
                                      onClick={() => handleOpenDispute(subOrder)}
                                      className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium transition-colors cursor-pointer" 
                                      type="button"
                                    >
                                      Hỗ trợ / Khiếu nại
                                    </button>
                                  </>
                                ) : (
                                  <>
                                    {!submittedReviews[subOrder.id] && (
                                      <button 
                                        onClick={() => handleOpenReview(subOrder)}
                                        className="px-3.5 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-600 text-white text-xs font-semibold flex items-center gap-1 shadow-sm transition-transform hover:scale-105 active:scale-95 cursor-pointer" 
                                        type="button"
                                      >
                                        <span className="material-symbols-outlined text-[15px]">rate_review</span>
                                        <span>Đánh giá dịch vụ</span>
                                      </button>
                                    )}
                                    {!submittedDisputes[subOrder.id] && (
                                      <button 
                                        onClick={() => handleOpenDispute(subOrder)}
                                        className="px-3 py-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs font-medium transition-colors cursor-pointer" 
                                        type="button"
                                      >
                                        Gửi khiếu nại
                                      </button>
                                    )}
                                  </>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </article>
              );
            })}
          </div>
        </div>
      </div>

      {/* Portaled QR Code Modal */}
      {selectedQrOrder &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-scale-in">
            <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-3xl p-6 max-w-sm w-full shadow-2xl relative text-center">
              <button
                onClick={() => setSelectedQrOrder(null)}
                className="absolute top-4 right-4 p-1.5 rounded-full text-on-surface-variant hover:bg-surface-container cursor-pointer transition-colors"
              >
                <span className="material-symbols-outlined">close</span>
              </button>

              <div className="w-12 h-12 rounded-2xl bg-secondary-container text-on-secondary-container flex items-center justify-center mx-auto mb-3">
                <span className="material-symbols-outlined text-[28px]">qr_code_scanner</span>
              </div>

              <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                Mã QR Check-in Vé Biển
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                Đưa mã này cho HDV hoặc Quầy vé tại bến cảng để quét nhận ca
              </p>

              <div className="my-5 p-4 rounded-2xl bg-white inline-block shadow-md border border-outline-variant/20">
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(
                    selectedQrOrder.qrSecret || selectedQrOrder.id
                  )}`}
                  alt="QR Check-in"
                  className="w-44 h-44 mx-auto rounded-lg"
                />
              </div>

              <div className="bg-surface-container-low rounded-xl p-3 text-left space-y-1 mb-4">
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Mã vé (Sub-order):</span>
                  <span className="font-mono font-bold text-secondary">#{selectedQrOrder.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Trạng thái:</span>
                  <span className="text-green-600 font-bold">Hợp lệ / Sẵn sàng</span>
                </div>
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Số lượng khách:</span>
                  <span className="text-on-surface font-bold">{selectedQrOrder.quantity} người</span>
                </div>
              </div>

              <button
                onClick={() => setSelectedQrOrder(null)}
                className="w-full py-2.5 rounded-full bg-secondary text-on-secondary font-label-md font-semibold hover:opacity-90 transition-opacity cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>,
          document.body
        )}

      {/* Portaled Review & Rating Modal */}
      {reviewingOrder &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl relative space-y-4">
              <div className="flex items-center justify-between border-b pb-3">
                <h3 className="text-lg font-bold text-slate-900">Đánh giá trải nghiệm</h3>
                <button
                  onClick={() => setReviewingOrder(null)}
                  className="text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>

              <div className="space-y-1">
                <span className="text-xs text-slate-500 font-medium">Đơn hàng #{reviewingOrder.id.toUpperCase()}</span>
                <p className="text-sm font-semibold text-slate-800">
                  {FEATURED_SERVICES.find(s => s.id === reviewingOrder.serviceId)?.name}
                </p>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1.5">Mức độ hài lòng</label>
                <div className="flex items-center gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRatingStars(star)}
                      className={`text-2xl cursor-pointer transition-transform hover:scale-110 ${
                        star <= ratingStars ? "text-amber-400" : "text-slate-300"
                      }`}
                    >
                      ★
                    </button>
                  ))}
                  <span className="text-xs font-bold text-slate-600 ml-2">
                    {ratingStars === 5 ? "Tuyệt vời" : ratingStars === 4 ? "Tốt" : ratingStars === 3 ? "Bình thường" : "Cần cải thiện"}
                  </span>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1.5">Nhận xét chi tiết</label>
                <textarea
                  rows={4}
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  placeholder="Chia sẻ cảm nhận của bạn về hướng dẫn viên, trang thiết bị, thời gian..."
                  className="w-full p-3 rounded-xl border border-slate-200 text-sm text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setReviewingOrder(null)}
                  className="px-4 py-2 rounded-xl text-slate-600 hover:bg-slate-100 text-xs font-semibold cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  onClick={handleSubmitReview}
                  className="px-4 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-dark cursor-pointer shadow-sm"
                >
                  Gửi đánh giá
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}

      {/* Portaled Dispute Modal */}
      {disputingOrder &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl relative space-y-4">
              <div className="flex items-center justify-between border-b pb-3">
                <h3 className="text-lg font-bold text-slate-900">Gửi yêu cầu khiếu nại</h3>
                <button
                  onClick={() => setDisputingOrder(null)}
                  className="text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>

              <div className="space-y-1">
                <span className="text-xs text-slate-500 font-medium">Mã đơn: #{disputingOrder.id.toUpperCase()}</span>
                <p className="text-sm font-semibold text-slate-800">
                  {FEATURED_SERVICES.find(s => s.id === disputingOrder.serviceId)?.name}
                </p>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1.5">Lý do khiếu nại / Hoàn tiền</label>
                <textarea
                  rows={4}
                  value={disputeReason}
                  onChange={(e) => setDisputeReason(e.target.value)}
                  placeholder="Mô tả sự cố bạn gặp phải (ví dụ: đối tác hủy ca không báo trước, thời tiết xấu hoãn ca, chất lượng không đúng cam kết...)"
                  className="w-full p-3 rounded-xl border border-slate-200 text-sm text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                />
              </div>

              <p className="text-xs text-slate-500">
                Đội ngũ hỗ trợ DANASEA sẽ liên hệ xác minh với đối tác và phản hồi trong vòng 24 giờ.
              </p>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setDisputingOrder(null)}
                  className="px-4 py-2 rounded-xl text-slate-600 hover:bg-slate-100 text-xs font-semibold cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={!disputeReason.trim()}
                  onClick={handleSubmitDispute}
                  className="px-4 py-2 rounded-xl bg-red-600 text-white text-xs font-semibold hover:bg-red-700 cursor-pointer shadow-sm disabled:opacity-50"
                >
                  Xác nhận gửi khiếu nại
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </main>
  );
}
