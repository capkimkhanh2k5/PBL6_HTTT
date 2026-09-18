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

        {/* Top Header */}
        <div className="mb-space-lg flex flex-col md:flex-row md:items-center justify-between gap-space-md">
          <div>
            <div className="flex items-center gap-space-xs text-primary font-label-md text-label-md uppercase tracking-wider mb-1 font-bold">
              <span className="material-symbols-outlined text-[16px]">verified_user</span>
              <span>Hội đồng Phân xử &amp; Giám sát Du lịch Biển Đà Nẵng</span>
            </div>
            <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black">
              Thẩm định Khiếu nại &amp; Quản trị Hoàn tiền Dịch vụ Biển
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl mt-1">
              Tiếp nhận tranh chấp du khách - đối tác và thực hiện lệnh hoàn tiền về phương thức gốc theo chuẩn quy chế Cảng vụ.
            </p>
          </div>

          <div className="flex items-center gap-space-sm self-start md:self-auto">
            <div className="px-space-md py-2 bg-surface-container rounded-xl flex items-center gap-space-sm shadow-sm">
              <span className="w-2.5 h-2.5 rounded-full bg-primary animate-ping"></span>
              <div className="text-left">
                <span className="block font-label-sm text-xs text-outline font-semibold">Ca trực vận hành</span>
                <span className="font-label-md text-xs text-on-surface font-bold">Hải Đăng #04</span>
              </div>
            </div>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-space-md mb-space-xl">
          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-start justify-between">
              <div>
                <span className="font-label-sm text-outline uppercase tracking-wider font-bold">Hồ sơ chờ xử lý</span>
                <h3 className="font-display-lg font-black text-secondary mt-1">{openCount}</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-secondary/10 text-secondary flex items-center justify-center">
                <span className="material-symbols-outlined text-[26px]">pending_actions</span>
              </div>
            </div>
            <span className="text-xs text-secondary font-semibold mt-2">Cần thẩm định biên bản</span>
          </div>

          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-start justify-between">
              <div>
                <span className="font-label-sm text-outline uppercase tracking-wider font-bold">Đã giải quyết</span>
                <h3 className="font-display-lg font-black text-primary mt-1">{resolvedCount}</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[26px]">task_alt</span>
              </div>
            </div>
            <span className="text-xs text-primary font-semibold mt-2">100% hồ sơ minh bạch</span>
          </div>

          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-start justify-between">
              <div>
                <span className="font-label-sm text-outline uppercase tracking-wider font-bold">Tỷ lệ hài lòng</span>
                <h3 className="font-display-lg font-black text-on-surface mt-1">96.2%</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-primary-container/10 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[26px]">sentiment_very_satisfied</span>
              </div>
            </div>
            <div className="mt-2 w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: "96.2%" }}></div>
            </div>
          </div>

          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-start justify-between">
              <div>
                <span className="font-label-sm text-outline uppercase tracking-wider font-bold">Hoàn tiền bảo vệ khách</span>
                <h3 className="font-headline-xl font-black text-primary mt-1">18.420.000 đ</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[26px]">account_balance_wallet</span>
              </div>
            </div>
            <span className="text-xs text-outline font-semibold mt-2">Hoàn về cổng SEPAY gốc</span>
          </div>
        </div>

        {/* 2-Column Split Workspace */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN: Disputes List (5 cols) */}
          <div className="lg:col-span-5 flex flex-col gap-space-md">
            <div className="p-space-md bg-surface-container-lowest rounded-2xl shadow-sm flex items-center justify-between border border-outline-variant/20">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">filter_list</span>
                <span className="font-label-lg font-bold text-on-surface">Danh sách tranh chấp</span>
              </div>
              <div className="flex gap-1">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-2 py-1 rounded text-xs font-bold ${statusFilter === 'all' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}
                >
                  Tất cả
                </button>
                <button
                  onClick={() => setStatusFilter('OPEN')}
                  className={`px-2 py-1 rounded text-xs font-bold ${statusFilter === 'OPEN' ? 'bg-secondary text-on-secondary' : 'bg-surface-container text-on-surface-variant'}`}
                >
                  Mới
                </button>
                <button
                  onClick={() => setStatusFilter('RESOLVED')}
                  className={`px-2 py-1 rounded text-xs font-bold ${statusFilter === 'RESOLVED' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}
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
                    className={`p-space-md rounded-2xl shadow-sm transition-all cursor-pointer border ${
                      isSelected ? 'border-primary ring-2 ring-primary/20 bg-primary/5' : 'border-outline-variant/20 bg-surface-container-lowest hover:bg-surface-container-low/40'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1.5">
                      <span className="font-mono text-xs font-bold text-primary">#{disp.id}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        disp.status === 'OPEN'
                          ? 'bg-secondary text-on-secondary'
                          : disp.status === 'IN_PROGRESS'
                          ? 'bg-secondary-container text-on-secondary-container'
                          : 'bg-primary/10 text-primary'
                      }`}>
                        {disp.status}
                      </span>
                    </div>

                    <h4 className="font-bold text-sm text-on-surface line-clamp-1">{disp.serviceName}</h4>
                    <p className="text-xs text-outline line-clamp-2 mt-1">{disp.description}</p>

                    <div className="flex items-center justify-between pt-2 mt-2 border-t border-outline-variant/10 text-xs">
                      <span className="font-semibold text-on-surface">{disp.customerName}</span>
                      <span className="font-black text-secondary">{disp.orderAmount?.toLocaleString('vi-VN')} đ</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* RIGHT COLUMN: Dispute Adjudication (7 cols) */}
          {selectedDispute && (
            <div className="lg:col-span-7 flex flex-col gap-space-lg">
              <div className="p-space-lg rounded-3xl bg-surface-container-lowest shadow-sm flex flex-col gap-4 border border-outline-variant/20">
                <div className="flex items-start justify-between pb-3 border-b border-outline-variant/20">
                  <div>
                    <span className="text-xs font-bold text-primary uppercase tracking-wider">Hồ sơ tranh chấp #{selectedDispute.id}</span>
                    <h2 className="font-headline-md font-black text-on-surface mt-0.5">{selectedDispute.serviceName}</h2>
                    <span className="text-xs text-outline">Đơn con liên quan: {selectedDispute.subOrderId}</span>
                  </div>
                  <span className="px-3 py-1 rounded-full bg-secondary/10 text-secondary font-bold text-xs">
                    {selectedDispute.category}
                  </span>
                </div>

                {/* Parties Details */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-xl bg-surface-container-low">
                    <span className="text-xs text-outline block font-bold">Du khách phản ánh</span>
                    <span className="font-bold text-sm text-on-surface block mt-0.5">{selectedDispute.customerName}</span>
                    <span className="text-xs text-outline">{selectedDispute.customerPhone}</span>
                  </div>

                  <div className="p-3 rounded-xl bg-surface-container-low">
                    <span className="text-xs text-outline block font-bold">Đối tác vận hành</span>
                    <span className="font-bold text-sm text-on-surface block mt-0.5">{selectedDispute.vendorName}</span>
                    <span className="text-xs text-primary font-semibold">Giá trị đơn: {selectedDispute.orderAmount?.toLocaleString('vi-VN')} đ</span>
                  </div>
                </div>

                {/* Customer Claim */}
                <div>
                  <span className="text-xs font-bold text-outline uppercase tracking-wider block mb-1">Nội dung phản ánh:</span>
                  <div className="p-3 rounded-xl bg-surface-container-low text-xs text-on-surface leading-relaxed border-l-2 border-secondary">
                    {selectedDispute.description}
                  </div>
                </div>

                {/* Evidence Images */}
                {selectedDispute.evidenceImages && selectedDispute.evidenceImages.length > 0 && (
                  <div>
                    <span className="text-xs font-bold text-outline uppercase tracking-wider block mb-1.5">Hình ảnh bằng chứng đính kèm:</span>
                    <div className="flex gap-3">
                      {selectedDispute.evidenceImages.map((img, idx) => (
                        <img 
                          key={idx}
                          src={img} 
                          alt="Bằng chứng hiện trường"
                          onClick={() => setPreviewImage(img)}
                          className="w-32 h-20 rounded-xl object-cover border border-outline-variant cursor-pointer hover:opacity-90"
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Port Authority Field Log */}
                <div className="p-3 rounded-xl bg-surface-container text-xs text-on-surface-variant flex flex-col gap-1">
                  <span className="font-bold text-primary flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">radar</span>
                    Đối soát hải đồ &amp; Camera bến bãi VITA
                  </span>
                  <p>
                    Dữ liệu AIS ghi nhận phương tiện xuất bến lúc sự cố diễn ra. Quy chế Cảng vụ điều 14: Đối tác có nghĩa vụ bồi thường nếu thiết bị an toàn không đạt chuẩn.
                  </p>
                </div>

                {/* Resolution Note & Decision */}
                <div className="p-space-md bg-surface-container-low rounded-2xl flex flex-col gap-3">
                  <label className="text-xs font-bold text-on-surface">Kết luận thẩm định &amp; Biên bản giải quyết:</label>
                  <textarea 
                    value={resolutionNote}
                    onChange={(e) => setResolutionNote(e.target.value)}
                    className="w-full p-2.5 rounded-xl bg-surface-container-lowest text-xs text-on-surface focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30 resize-none"
                    rows={3}
                  />

                  <div className="flex items-center justify-end gap-2 pt-1">
                    <button 
                      onClick={handleDismissDispute}
                      className="px-4 py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-bold text-xs"
                    >
                      Bác bỏ khiếu nại
                    </button>
                    <button 
                      onClick={() => setIsRefundModalOpen(true)}
                      className="px-5 py-2.5 rounded-xl bg-secondary text-on-secondary font-bold text-xs hover:bg-secondary-container flex items-center gap-1.5 shadow-sm"
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
