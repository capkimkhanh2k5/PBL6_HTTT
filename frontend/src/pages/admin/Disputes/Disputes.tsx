import { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_DISPUTES } from '../../../data/adminMockData';
import type { DisputeStatus } from '../../../types';

type DisputeExtended = typeof MOCK_ADMIN_DISPUTES[0];

export function Disputes() {
  const [disputes, setDisputes] = useState<DisputeExtended[]>(MOCK_ADMIN_DISPUTES);
  const [selectedDisputeId, setSelectedDisputeId] = useState<string>(MOCK_ADMIN_DISPUTES[0]?.id || '');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [resolutionNote, setResolutionNote] = useState<string>(
    MOCK_ADMIN_DISPUTES[0]?.resolutionNote ||
    "Xác minh hiện trường: Nhà cung cấp chưa đảm bảo tiêu chuẩn trang bị an toàn mặt nước. Đề xuất hoàn 50% chi phí dịch vụ cho khách hàng."
  );

  // Refund Modal State
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundPercentage, setRefundPercentage] = useState<number>(50);
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredDisputes = useMemo(() => {
    return disputes.filter(d => statusFilter === 'all' || d.status === statusFilter);
  }, [disputes, statusFilter]);

  const selectedDispute = disputes.find(d => d.id === selectedDisputeId) || disputes[0];

  const handleSelectDispute = (disp: DisputeExtended) => {
    setSelectedDisputeId(disp.id);
    setResolutionNote(disp.resolutionNote || "Đã xác minh thông tin hai chiều giữa du khách và đối tác.");
  };

  const handleExecuteRefund = () => {
    if (!selectedDispute) return;
    const calcAmount = Math.round(((selectedDispute.orderAmount || 1000000) * refundPercentage) / 100);

    setDisputes(prev =>
      prev.map(d =>
        d.id === selectedDispute.id
          ? {
              ...d,
              status: 'RESOLVED' as DisputeStatus,
              resolutionNote: `${resolutionNote} (Đã phát lệnh hoàn tiền ${refundPercentage}% = ${calcAmount.toLocaleString('vi-VN')} đ)`,
              resolvedBy: 'Vũ Hải Đăng',
              resolvedAt: new Date().toISOString()
            }
          : d
      )
    );

    setIsRefundModalOpen(false);
    showToast(`Đã phát lệnh hoàn ${calcAmount.toLocaleString('vi-VN')} đ cho du khách ${selectedDispute.customerName}!`);
  };

  const handleDismissDispute = () => {
    if (!selectedDispute) return;
    setDisputes(prev =>
      prev.map(d =>
        d.id === selectedDispute.id
          ? {
              ...d,
              status: 'RESOLVED' as DisputeStatus,
              resolutionNote: "Bác bỏ khiếu nại: Đối tác đã cung cấp đầy đủ nhật trình GPS và chứng minh lỗi phát sinh ngoài thẩm quyền.",
              resolvedBy: 'Vũ Hải Đăng',
              resolvedAt: new Date().toISOString()
            }
          : d
      )
    );
    showToast(`Đã bác bỏ khiếu nại #${selectedDispute.id}.`);
  };

  // Metrics
  const openCount = disputes.filter(d => d.status === 'OPEN' || d.status === 'IN_PROGRESS').length;
  const resolvedCount = disputes.filter(d => d.status === 'RESOLVED').length;

  return (
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full animate-fade-in-up">
        {/* Toast */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 mb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">
              Quản lý Khiếu nại &amp; Tranh chấp
            </h1>
          </div>
          <div className="flex items-center gap-2 text-xs font-medium">
            <span className="px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
              Chờ xử lý: <strong>{openCount}</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary border border-primary/20">
              Đã giải quyết: <strong>{resolvedCount}</strong>
            </span>
          </div>
        </div>

        {/* 2-Column Split Workspace */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN: Disputes List (5 cols) */}
          <div className="lg:col-span-5 flex flex-col gap-space-md">
            <div className="p-space-md bg-white rounded-xl shadow-xs flex items-center justify-between border border-slate-200">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">filter_list</span>
                <span className="font-bold text-slate-800 text-sm">Danh sách tranh chấp</span>
              </div>
              <div className="flex gap-1.5 p-1 bg-slate-100 rounded-xl border border-slate-200">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'all' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  Tất cả
                </button>
                <button
                  onClick={() => setStatusFilter('OPEN')}
                  className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'OPEN' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  Chờ xử lý
                </button>
                <button
                  onClick={() => setStatusFilter('RESOLVED')}
                  className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'RESOLVED' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  Đã xong
                </button>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              {filteredDisputes.map(disp => {
                const isSelected = disp.id === selectedDispute?.id;
                return (
                  <div 
                    key={disp.id}
                    onClick={() => handleSelectDispute(disp)}
                    className={`p-space-md rounded-xl shadow-xs transition-all cursor-pointer border ${
                      isSelected ? 'border-primary ring-2 ring-primary/20 bg-primary/5' : 'border-slate-200 bg-white hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1.5">
                      <span className="font-mono text-xs font-bold text-primary">#{disp.id}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        disp.status === 'OPEN'
                          ? 'bg-amber-50 text-amber-700 border border-amber-200'
                          : disp.status === 'IN_PROGRESS'
                          ? 'bg-blue-50 text-blue-700 border border-blue-200'
                          : 'bg-primary/10 text-primary border border-primary/20'
                      }`}>
                        {disp.status}
                      </span>
                    </div>

                    <h4 className="font-bold text-sm text-slate-800 line-clamp-1">{disp.serviceName}</h4>
                    <p className="text-xs text-slate-500 line-clamp-2 mt-1">{disp.description}</p>

                    <div className="flex items-center justify-between pt-2 mt-2 border-t border-slate-200 text-xs">
                      <span className="font-semibold text-slate-700">{disp.customerName}</span>
                      <span className="font-bold text-slate-900">{disp.orderAmount?.toLocaleString('vi-VN')} đ</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* RIGHT COLUMN: Dispute Adjudication (7 cols) */}
          {selectedDispute && (
            <div className="lg:col-span-7 flex flex-col gap-space-lg">
              <div className="p-space-lg rounded-xl bg-white shadow-xs flex flex-col gap-4 border border-slate-200">
                <div className="flex items-start justify-between pb-3 border-b border-slate-200">
                  <div>
                    <span className="font-label-sm text-xs font-medium text-primary">Hồ sơ tranh chấp #{selectedDispute.id}</span>
                    <h2 className="text-lg font-bold text-slate-800 mt-0.5">{selectedDispute.serviceName}</h2>
                    <span className="text-xs text-slate-500">Đơn con liên quan: {selectedDispute.subOrderId}</span>
                  </div>
                  <span className="px-3 py-1 rounded-full bg-slate-100 border border-slate-200 text-slate-700 font-bold text-xs">
                    {selectedDispute.category}
                  </span>
                </div>

                {/* Parties Details */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
                    <span className="text-xs text-slate-500 block font-bold">Du khách phản ánh</span>
                    <span className="font-bold text-sm text-slate-800 block mt-0.5">{selectedDispute.customerName}</span>
                    <span className="text-xs text-slate-500">{selectedDispute.customerPhone}</span>
                  </div>

                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
                    <span className="text-xs text-slate-500 block font-bold">Đối tác vận hành</span>
                    <span className="font-bold text-sm text-slate-800 block mt-0.5">{selectedDispute.vendorName}</span>
                    <span className="text-xs text-primary font-semibold">Giá trị đơn: {selectedDispute.orderAmount?.toLocaleString('vi-VN')} đ</span>
                  </div>
                </div>

                {/* Customer Claim */}
                <div>
                  <span className="text-xs font-semibold text-slate-800 block mb-1">Nội dung phản ánh:</span>
                  <div className="p-3 rounded-xl bg-slate-50 text-xs text-slate-800 leading-relaxed border-l-2 border-primary">
                    {selectedDispute.description}
                  </div>
                </div>

                {/* Evidence Images */}
                {selectedDispute.evidenceImages && selectedDispute.evidenceImages.length > 0 && (
                  <div>
                    <span className="text-xs font-semibold text-slate-800 block mb-1.5">Hình ảnh bằng chứng đính kèm:</span>
                    <div className="flex gap-3">
                      {selectedDispute.evidenceImages.map((img, idx) => (
                        <img 
                          key={idx}
                          src={img} 
                          alt="Bằng chứng hiện trường"
                          onClick={() => setPreviewImage(img)}
                          className="w-32 h-20 rounded-xl object-cover border border-slate-200 cursor-pointer hover:opacity-90"
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Port Authority Field Log */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex flex-col gap-1">
                  <span className="font-bold text-primary flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">radar</span>
                    Đối soát hải đồ &amp; Camera bến bãi
                  </span>
                  <p>
                    Dữ liệu AIS ghi nhận phương tiện xuất bến lúc sự cố diễn ra. Đối tác có nghĩa vụ bồi thường nếu thiết bị an toàn không đạt chuẩn.
                  </p>
                </div>

                {/* Resolution Note & Decision */}
                <div className="p-space-md bg-slate-50 border border-slate-200 rounded-xl flex flex-col gap-3">
                  <label className="text-xs font-bold text-slate-800">Kết luận thẩm định &amp; Biên bản giải quyết:</label>
                  <textarea 
                    value={resolutionNote}
                    onChange={(e) => setResolutionNote(e.target.value)}
                    className="w-full p-2.5 rounded-xl bg-white text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary border border-slate-200 resize-none"
                    rows={3}
                  />

                  <div className="flex items-center justify-end gap-2 pt-1">
                    <button 
                      onClick={handleDismissDispute}
                      className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 font-semibold text-xs transition-colors"
                    >
                      Bác bỏ khiếu nại
                    </button>
                    <button 
                      onClick={() => setIsRefundModalOpen(true)}
                      className="px-5 py-2 rounded-xl bg-primary text-white font-semibold text-xs hover:bg-primary-container flex items-center gap-1.5 shadow-xs transition-colors"
                    >
                      <span className="material-symbols-outlined text-[18px]">currency_exchange</span>
                      <span>Chấp thuận &amp; Tạo lệnh hoàn tiền</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* REFUND MODAL (Portaled to body) */}
      {isRefundModalOpen && selectedDispute && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/50 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-3xl p-space-xl max-w-lg w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <h3 className="font-headline-md font-bold text-on-surface">Lệnh Hoàn Tiền Bảo Vệ Du Khách</h3>
            <p className="text-xs text-on-surface-variant">
              Tiền sẽ hoàn tự động qua cổng <strong>SEPAY</strong> về tài khoản của du khách <strong>{selectedDispute.customerName}</strong>.
            </p>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-bold text-on-surface">Chọn tỷ lệ hoàn tiền:</label>
              <div className="grid grid-cols-4 gap-2">
                {[20, 30, 50, 100].map(pct => (
                  <button
                    key={pct}
                    type="button"
                    onClick={() => setRefundPercentage(pct)}
                    className={`py-2 rounded-xl text-xs font-bold transition-all ${
                      refundPercentage === pct
                        ? 'bg-primary text-on-primary shadow-xs'
                        : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                    }`}
                  >
                    {pct}%
                  </button>
                ))}
              </div>
            </div>

            <div className="p-3 rounded-xl bg-surface-container-low flex items-center justify-between">
              <span className="text-xs text-outline">Số tiền thanh toán hoàn trả:</span>
              <span className="font-headline-md font-black text-secondary">
                {Math.round(((selectedDispute.orderAmount || 1000000) * refundPercentage) / 100).toLocaleString('vi-VN')} đ
              </span>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button 
                onClick={() => setIsRefundModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container text-xs font-semibold"
              >
                Hủy
              </button>
              <button 
                onClick={handleExecuteRefund}
                className="px-5 py-2.5 rounded-xl bg-primary text-on-primary text-xs font-bold shadow-sm"
              >
                Phát lệnh chuyển tiền ngay
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* IMAGE PREVIEW MODAL (Portaled to body) */}
      {previewImage && createPortal(
        <div 
          onClick={() => setPreviewImage(null)}
          className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/80 p-4 animate-scale-in cursor-pointer"
        >
          <img 
            src={previewImage} 
            alt="Phóng to bằng chứng" 
            className="max-w-2xl max-h-[80vh] rounded-2xl object-contain shadow-2xl"
          />
        </div>,
        document.body
      )}
    </main>
  );
}

export default Disputes;
