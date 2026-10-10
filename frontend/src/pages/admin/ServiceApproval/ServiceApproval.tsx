import React, { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_SERVICES } from '../../../data/adminMockData';
import type { ServiceStatus } from '../../../types';

type ServiceExtended = typeof MOCK_ADMIN_SERVICES[0];

export function ServiceApproval() {
  const [services, setServices] = useState<ServiceExtended[]>(MOCK_ADMIN_SERVICES);
  const [selectedServiceId, setSelectedServiceId] = useState<string>(MOCK_ADMIN_SERVICES[0]?.id || '');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [adminNote, setAdminNote] = useState<string>("Tuyến dịch vụ biển nằm trong vùng đệm kiểm soát, đã hoàn tất kiểm định trang bị cứu hộ.");
  
  // Modals & Toast
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredServices = useMemo(() => {
    return services.filter(s => {
      const matchStatus = statusFilter === 'all' || s.status === statusFilter;
      const matchCategory = categoryFilter === 'all' || s.categoryId === categoryFilter;
      return matchStatus && matchCategory;
    });
  }, [services, statusFilter, categoryFilter]);

  const selectedService = services.find(s => s.id === selectedServiceId) || services[0];

  const handleApprove = () => {
    if (!selectedService) return;
    setServices(prev =>
      prev.map(s =>
        s.id === selectedService.id
          ? { ...s, status: 'ACTIVE' as ServiceStatus, rejectionReason: '' }
          : s
      )
    );
    setIsApproveModalOpen(false);
    showToast(`Đã phê duyệt xuất bản tour "${selectedService.name}" thành công! Dịch vụ hiện đã mở bán công khai.`);
  };

  const handlePause = () => {
    if (!selectedService) return;
    setServices(prev =>
      prev.map(s =>
        s.id === selectedService.id
          ? { ...s, status: 'PAUSED' as ServiceStatus }
          : s
      )
    );
    showToast(`Đã tạm dừng mở bán tour "${selectedService.name}".`);
  };

  const handleReject = () => {
    if (!selectedService) return;
    setServices(prev =>
      prev.map(s =>
        s.id === selectedService.id
          ? { ...s, status: 'REJECTED' as ServiceStatus, rejectionReason: rejectReason }
          : s
      )
    );
    setIsRejectModalOpen(false);
    showToast(`Đã từ chối duyệt tour "${selectedService.name}".`);
  };

  // Metrics
  const totalCount = services.length;
  const activeCount = services.filter(s => s.status === 'ACTIVE').length;
  const pendingCount = services.filter(s => s.status === 'PENDING_APPROVAL').length;
  const rejectedCount = services.filter(s => s.status === 'REJECTED').length;

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
              Kiểm duyệt Dịch vụ
            </h1>
          </div>
          <div className="flex items-center gap-2 text-xs font-medium">
            <span className="px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-600">
              Tổng: <strong>{totalCount}</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary border border-primary/20">
              Đang mở bán: <strong>{activeCount}</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
              Chờ duyệt: <strong>{pendingCount}</strong>
            </span>
          </div>
        </div>

        {/* Filter Section */}
        <section className="mb-space-lg rounded-xl bg-white p-space-md shadow-xs border border-slate-200">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0">
              <button 
                onClick={() => setStatusFilter('all')}
                className={`px-3.5 py-1.5 rounded-lg text-xs transition-all shrink-0 ${statusFilter === 'all' ? 'bg-primary text-white font-bold shadow-xs' : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200'}`}
              >
                Tất cả ({totalCount})
              </button>
              <button 
                onClick={() => setStatusFilter('PENDING_APPROVAL')}
                className={`px-3.5 py-1.5 rounded-lg text-xs transition-all shrink-0 ${statusFilter === 'PENDING_APPROVAL' ? 'bg-primary text-white font-bold shadow-xs' : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200'}`}
              >
                Chờ duyệt ({pendingCount})
              </button>
              <button 
                onClick={() => setStatusFilter('ACTIVE')}
                className={`px-3.5 py-1.5 rounded-lg text-xs transition-all shrink-0 ${statusFilter === 'ACTIVE' ? 'bg-primary text-white font-bold shadow-xs' : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200'}`}
              >
                Đang mở bán ({activeCount})
              </button>
              <button 
                onClick={() => setStatusFilter('REJECTED')}
                className={`px-3.5 py-1.5 rounded-lg text-xs transition-all shrink-0 ${statusFilter === 'REJECTED' ? 'bg-primary text-white font-bold shadow-xs' : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200'}`}
              >
                Bị từ chối ({rejectedCount})
              </button>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <select 
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="h-9 px-3 rounded-lg bg-slate-50 text-slate-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary cursor-pointer border border-slate-200"
              >
                <option value="all">Tất cả danh mục biển</option>
                <option value="cat-1">Chèo SUP &amp; Kayak</option>
                <option value="cat-2">Cano &amp; Dù bay</option>
                <option value="cat-3">Lặn ngắm san hô</option>
                <option value="cat-4">Mô tô nước (Jetski)</option>
                <option value="cat-5">Du thuyền hoàng hôn</option>
              </select>
            </div>
          </div>
        </section>

        {/* Dual Workspace: Review Queue & Verification Panel */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* Left Column (5 Cols): Review Queue Console */}
          <div className="lg:col-span-5 bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
            <div className="px-4 py-3 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">Hàng Đợi Thẩm Định</span>
              <span className="text-xs text-slate-500 font-medium">{filteredServices.length} tour</span>
            </div>

            <div className="divide-y divide-slate-200 max-h-[820px] overflow-y-auto">
              {filteredServices.map(service => {
                const isSelected = service.id === selectedService?.id;
                return (
                  <div 
                    key={service.id}
                    onClick={() => setSelectedServiceId(service.id)}
                    className={`p-3.5 transition-colors cursor-pointer flex gap-3 ${
                      isSelected ? 'bg-primary/5 border-l-4 border-l-primary' : 'hover:bg-slate-50'
                    }`}
                  >
                    <img 
                      src={service.thumbnailUrl} 
                      alt={service.name}
                      className="w-16 h-16 rounded-lg object-cover shrink-0 border border-slate-200"
                    />
                    <div className="flex flex-col justify-between flex-1 min-w-0">
                      <div>
                        <div className="flex items-center justify-between gap-1 mb-0.5">
                          <span className="text-[11px] font-bold text-primary truncate uppercase">{service.vendorName}</span>
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${
                            service.status === 'ACTIVE'
                              ? 'bg-primary/10 text-primary border border-primary/20'
                              : service.status === 'PENDING_APPROVAL'
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-red-50 text-red-700 border border-red-200'
                          }`}>
                            {service.status === 'ACTIVE' ? 'Đã duyệt' : service.status === 'PENDING_APPROVAL' ? 'Chờ duyệt' : 'Từ chối'}
                          </span>
                        </div>
                        <h4 className="text-xs font-bold text-slate-800 leading-snug truncate">
                          {service.name}
                        </h4>
                        <p className="text-[11px] text-slate-500 truncate">
                          {service.locationName} • {service.durationMinutes} phút
                        </p>
                      </div>

                      <div className="flex items-center justify-between pt-1 text-xs">
                        <span className="font-bold text-primary">
                          {(service.price ?? service.basePrice ?? 0).toLocaleString('vi-VN')} đ
                        </span>
                        <span className="text-[11px] text-slate-400 font-mono">#{service.id}</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right Column (7 Cols) */}
          {selectedService && (
            <div className="lg:col-span-7 flex flex-col gap-space-lg">
              <div className="rounded-xl bg-white p-space-lg shadow-xs border border-slate-200">
                <div className="flex flex-col md:flex-row md:items-start justify-between gap-space-md pb-space-md border-b border-slate-200">
                  <div>
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className="px-2.5 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-xs uppercase font-bold">
                        Mã Tour: #{selectedService.id}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface font-label-sm text-xs font-semibold">
                        {selectedService.categoryName}
                      </span>
                    </div>
                    <h2 className="font-headline-lg text-headline-lg text-on-surface tracking-tight font-black">
                      {selectedService.name}
                    </h2>
                    <p className="font-body-md text-sm text-on-surface-variant italic mt-0.5">
                      {selectedService.nameEn}
                    </p>
                  </div>

                  <div className="rounded-2xl bg-surface-container-low p-space-md text-right shrink-0">
                    <span className="font-label-sm text-on-surface-variant block text-xs font-medium">Đơn giá niêm yết</span>
                    <div className="flex items-baseline justify-end gap-1 mt-0.5">
                      <span className="font-headline-xl text-headline-xl text-primary font-black">
                        {(selectedService.price ?? selectedService.basePrice ?? 0).toLocaleString('vi-VN')}
                      </span>
                      <span className="font-label-lg text-primary font-bold">VNĐ</span>
                    </div>
                    <span className="font-body-sm text-xs text-on-surface-variant block">/ 01 khách</span>
                  </div>
                </div>

                {/* Service Specs */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-space-md rounded-2xl bg-surface-container-low my-space-md">
                  <div>
                    <span className="text-xs text-outline block">Đơn vị vận hành</span>
                    <span className="font-label-md text-xs font-bold text-on-surface">{selectedService.vendorName}</span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Thời lượng tour</span>
                    <span className="font-label-md text-xs font-bold text-on-surface">{selectedService.durationMinutes} phút</span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Định mức phục vụ</span>
                    <span className="font-label-md text-xs font-bold text-on-surface">Tối đa {selectedService.capacityPerSlot} khách/ca</span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Tọa độ GPS</span>
                    <span className="font-mono text-xs font-bold text-primary">{selectedService.latitude}° N, {selectedService.longitude}° E</span>
                  </div>
                </div>

                {/* Safety Evaluation (safety_rule_evaluations) */}
                <div className="p-space-md rounded-2xl bg-surface-container mb-space-md">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-primary"></span>
                      <span className="font-headline-sm text-sm text-on-surface font-bold">Đánh giá An toàn Biển</span>
                    </div>
                    <span className="px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-xs font-semibold">
                      Đạt chuẩn an toàn
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div className="p-2.5 rounded-xl bg-surface-container-lowest">
                      <span className="text-outline block">Sóng tối đa cho phép:</span>
                      <span className="font-bold text-on-surface">≤ {selectedService.maxWaveM} mét (Hiện tại: 0.5m)</span>
                    </div>
                    <div className="p-2.5 rounded-xl bg-surface-container-lowest">
                      <span className="text-outline block">Vận tốc gió gián đoạn:</span>
                      <span className="font-bold text-on-surface">≥ {selectedService.minWindKmh} km/h (Hiện tại: 12 km/h)</span>
                    </div>
                  </div>
                </div>

                {/* Waiver Form */}
                <div className="mb-space-md">
                  <span className="font-label-md text-on-surface font-semibold block mb-1">
                    Cam kết Miễn trừ Trách nhiệm (waiver_content)
                  </span>
                  <div className="p-3 rounded-xl bg-surface-container-low text-xs text-on-surface-variant border-l-2 border-primary leading-relaxed">
                    {selectedService.waiverContent}
                  </div>
                </div>

                {/* Decision Actions */}
                <div className="p-space-md bg-surface-container-low rounded-2xl flex flex-col gap-3">
                  <label className="font-label-md text-xs text-on-surface font-semibold">Ghi chú thẩm định của Quản trị viên:</label>
                  <textarea 
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                    className="w-full p-2.5 rounded-xl bg-surface-container-lowest text-on-surface font-body-sm text-xs focus:outline-none focus:ring-1 focus:ring-primary border border-outline-variant/30 resize-none" 
                    rows={2}
                  />
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
                    <div>
                      {selectedService.status === 'ACTIVE' ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-primary/15 text-primary text-xs font-bold">
                          <span className="material-symbols-outlined text-[16px]">verified</span>
                          <span>Đang mở bán công khai (ACTIVE)</span>
                        </span>
                      ) : selectedService.status === 'REJECTED' ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-secondary/15 text-secondary text-xs font-bold">
                          <span className="material-symbols-outlined text-[16px]">block</span>
                          <span>Đã từ chối thẩm định</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-secondary-container/25 text-on-secondary-container text-xs font-bold animate-pulse">
                          <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                          <span>Hồ sơ đang chờ phê duyệt</span>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      {selectedService.status === 'ACTIVE' ? (
                        <button 
                          onClick={handlePause}
                          className="px-4 py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-xs font-bold transition-colors flex items-center gap-1.5 shadow-xs"
                        >
                          <span className="material-symbols-outlined text-[16px]">pause_circle</span>
                          <span>Tạm ngưng mở bán</span>
                        </button>
                      ) : (
                        <button 
                          onClick={() => setIsRejectModalOpen(true)}
                          className="px-4 py-2.5 rounded-xl bg-secondary-container/20 text-secondary hover:bg-secondary-container font-label-md text-xs font-bold transition-colors flex items-center gap-1.5 shadow-xs"
                        >
                          <span className="material-symbols-outlined text-[16px]">close</span>
                          <span>Từ chối duyệt (REJECT)</span>
                        </button>
                      )}

                      <button 
                        onClick={() => setIsApproveModalOpen(true)}
                        className={`px-5 py-2.5 rounded-xl font-label-md text-xs font-bold transition-all flex items-center gap-1.5 shadow-sm ${
                          selectedService.status === 'ACTIVE'
                            ? 'bg-surface-container text-primary hover:bg-primary/10 border border-primary/30'
                            : 'bg-primary text-on-primary hover:bg-primary-container'
                        }`}
                      >
                        <span className="material-symbols-outlined text-[16px]">done_all</span>
                        <span>{selectedService.status === 'ACTIVE' ? 'Cập nhật phê duyệt lại' : 'Phê duyệt xuất bản (PUBLISH)'}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </section>
      </div>

      {/* APPROVE MODAL (PORTAL TO BODY TO ENSURE PERFECT CENTER ALIGNMENT) */}
      {isApproveModalOpen && selectedService && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl p-space-xl max-w-lg w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-[28px]">verified</span>
              </div>
              <div>
                <h3 className="font-headline-md text-headline-md font-bold text-on-surface">Xác nhận Phê duyệt &amp; Xuất bản Tour</h3>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-surface-container-low flex flex-col gap-2 border border-outline-variant/20">
              <div className="flex justify-between items-start">
                <span className="text-xs text-outline">Tên dịch vụ:</span>
                <span className="text-xs font-bold text-on-surface text-right max-w-xs">{selectedService.name}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-xs text-outline">Đơn vị vận hành:</span>
                <span className="text-xs font-bold text-primary">{selectedService.vendorName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-xs text-outline">Đơn giá niêm yết:</span>
                <span className="text-xs font-black text-on-surface">{(selectedService.price ?? selectedService.basePrice ?? 0).toLocaleString('vi-VN')} đ / khách</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-xs text-outline">Điều kiện thời tiết:</span>
                <span className="text-xs font-semibold text-primary">Sóng ≤ {selectedService.maxWaveM}m • Gió ≥ {selectedService.minWindKmh}km/h</span>
              </div>
            </div>

            <div className="text-xs text-on-surface-variant bg-surface-container-low/40 p-3 rounded-xl border border-outline-variant/10 leading-relaxed">
              <strong>Lưu ý:</strong> Khi bạn xác nhận phê duyệt, tour này sẽ được chuyển ngay sang trạng thái <strong className="text-primary font-mono">ACTIVE</strong> và hiển thị công khai trên Cổng đặt vé cho du khách bắt đầu đặt chỗ.
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button 
                onClick={() => setIsApproveModalOpen(false)}
                className="px-4 py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-xs font-semibold transition-colors"
              >
                Hủy bỏ
              </button>
              <button 
                onClick={handleApprove}
                className="px-5 py-2.5 rounded-xl bg-primary text-on-primary font-label-md text-xs font-bold hover:bg-primary-container transition-colors shadow-sm flex items-center gap-1.5"
              >
                <span className="material-symbols-outlined text-[16px]">check_circle</span>
                <span>Xác nhận Xuất bản (PUBLISH)</span>
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* REJECT MODAL (PORTAL TO BODY TO ENSURE PERFECT CENTER ALIGNMENT) */}
      {isRejectModalOpen && selectedService && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl p-space-xl max-w-md w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-secondary-container/20 text-secondary flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-[28px]">warning</span>
              </div>
              <div>
                <h3 className="font-headline-md text-headline-md font-bold text-on-surface">Từ chối Thẩm định Tour</h3>
                <span className="text-xs text-outline">Gửi lý do phản hồi cho đối tác</span>
              </div>
            </div>

            <p className="font-body-md text-xs text-on-surface-variant">
              Nhập lý do chưa đảm bảo an toàn hải đồ để thông báo tới đơn vị <strong>{selectedService.vendorName}</strong>:
            </p>
            <textarea 
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="VD: Cự ly hải trình vượt quá phao an toàn 350m, hoặc thiếu chứng chỉ cứu sinh..."
              className="w-full h-24 p-3 rounded-xl bg-surface-container-low text-xs text-on-surface focus:outline-none focus:ring-2 focus:ring-secondary border border-outline-variant/30 resize-none"
            />
            <div className="flex justify-end gap-2 pt-1">
              <button 
                onClick={() => setIsRejectModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container hover:bg-surface-container-high text-xs font-semibold transition-colors"
              >
                Hủy
              </button>
              <button 
                onClick={handleReject}
                className="px-5 py-2 rounded-xl bg-secondary text-on-secondary text-xs font-bold hover:bg-secondary-container transition-colors shadow-sm"
              >
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}

export default ServiceApproval;
