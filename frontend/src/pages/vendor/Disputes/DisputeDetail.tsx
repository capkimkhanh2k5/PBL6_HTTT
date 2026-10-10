import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MOCK_DISPUTES } from '../../../mockData';

export function DisputeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const dispute = MOCK_DISPUTES.find(d => d.id === id);
  const [evidence, setEvidence] = useState<File[]>([]);

  if (!dispute) {
    return (
      <div className="p-8 text-center text-xs text-error">
        Không tìm thấy khiếu nại.
      </div>
    );
  }

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setEvidence(prev => [...prev, ...Array.from(e.target.files!)]);
    }
  };

  const handleRemoveFile = (index: number) => {
    setEvidence(prev => prev.filter((_, i) => i !== index));
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
            <span>Mới mở (OPEN)</span>
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
            <span>Đang xử lý</span>
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>Đã giải quyết</span>
          </span>
        );
      case 'CLOSED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant text-xs font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-outline"></span>
            <span>Đã đóng</span>
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface text-xs font-medium">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="w-full px-6 py-6 space-y-6">
      {/* Top Navigation & Header */}
      <div className="space-y-2 pb-3 border-b border-outline-variant/20">
        <button 
          onClick={() => navigate('/vendor/disputes')} 
          className="inline-flex items-center gap-1 text-xs text-on-surface-variant hover:text-primary transition-colors cursor-pointer"
          type="button"
        >
          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
          <span>Quay lại danh sách khiếu nại</span>
        </button>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
          <div className="flex items-center gap-3">
            <h1 className="text-xl md:text-2xl font-bold font-mono text-on-surface tracking-tight">
              {dispute.id}
            </h1>
            <span className="text-xs text-on-surface-variant">•</span>
            <span className="text-xs text-on-surface-variant font-medium">
              Đơn hàng: <strong className="font-mono text-on-surface">{dispute.subOrderId}</strong>
            </span>
          </div>
          <div>
            {getStatusBadge(dispute.status)}
          </div>
        </div>
      </div>

      {/* Main 12-Column Console Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* LEFT COLUMN (8 cols): Chronological Dispute Resolution Thread */}
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl border border-outline-variant/20 shadow-xs divide-y divide-outline-variant/15 overflow-hidden">
          {/* Step 1: Customer Report */}
          <div className="p-5 sm:p-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-7 h-7 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
                  <span className="material-symbols-outlined text-[18px]">report_problem</span>
                </span>
                <div>
                  <h2 className="text-sm font-bold text-on-surface">1. Nội dung khiếu nại từ khách hàng</h2>
                  <p className="text-[11px] text-on-surface-variant">Khởi tạo bởi {dispute.raisedBy}</p>
                </div>
              </div>
              <span className="text-xs font-mono text-on-surface-variant">
                {new Date(dispute.createdAt).toLocaleString('vi-VN')}
              </span>
            </div>

            <div className="p-3.5 rounded-lg bg-surface-container-low/40 border border-outline-variant/15 text-xs text-on-surface leading-relaxed">
              <div className="mb-1 text-[11px] font-semibold text-outline">
                Phân loại vi phạm: <span className="text-on-surface">{dispute.category}</span>
              </div>
              <p>{dispute.description}</p>
            </div>
          </div>

          {/* Step 2: Vendor Evidence Submission */}
          <div className="p-5 sm:p-6 space-y-3">
            <div className="flex items-center gap-2">
              <span className="w-7 h-7 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[18px]">upload_file</span>
              </span>
              <div>
                <h2 className="text-sm font-bold text-on-surface">2. Minh chứng đối ứng của Nhà cung cấp</h2>
                <p className="text-[11px] text-on-surface-variant">Tải lên hình ảnh, hóa đơn hoặc văn bản giải trình để Hội đồng xem xét</p>
              </div>
            </div>

            {/* Dropzone */}
            <label className="border border-dashed border-outline-variant/40 rounded-lg p-4 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-surface-container-low/30 transition-colors">
              <span className="material-symbols-outlined text-primary text-[24px] mb-1">cloud_upload</span>
              <span className="text-xs font-semibold text-on-surface">Bấm để chọn tệp hoặc kéo thả vào đây</span>
              <span className="text-[11px] text-on-surface-variant mt-0.5">Hỗ trợ PDF, PNG, JPG (Tối đa 10MB/tệp)</span>
              <input type="file" multiple accept=".pdf,.png,.jpg,.jpeg" className="hidden" onChange={handleFileUpload} />
            </label>

            {/* Selected Evidence List */}
            {evidence.length > 0 && (
              <div className="space-y-2 pt-1">
                <h4 className="text-xs font-semibold text-on-surface">Tệp đã đính kèm ({evidence.length}):</h4>
                <div className="divide-y divide-outline-variant/15 border border-outline-variant/20 rounded-lg overflow-hidden">
                  {evidence.map((file, idx) => (
                    <div key={idx} className="flex items-center justify-between p-2.5 bg-surface-container-low/20 text-xs">
                      <div className="flex items-center gap-2 min-w-0">
                        <span className="material-symbols-outlined text-[16px] text-primary">description</span>
                        <span className="truncate font-medium text-on-surface max-w-[280px]">{file.name}</span>
                        <span className="text-[11px] text-on-surface-variant">({(file.size / 1024).toFixed(1)} KB)</span>
                      </div>
                      <button 
                        type="button" 
                        onClick={() => handleRemoveFile(idx)} 
                        className="text-error hover:bg-error/10 p-1 rounded transition-colors cursor-pointer"
                        title="Xóa tệp"
                      >
                        <span className="material-symbols-outlined text-[16px]">close</span>
                      </button>
                    </div>
                  ))}
                </div>
                <button 
                  type="button"
                  className="px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-bold shadow-xs hover:bg-primary-container transition-colors cursor-pointer"
                >
                  Gửi minh chứng bổ sung
                </button>
              </div>
            )}
          </div>

          {/* Step 3: Admin & Port Authority Resolution */}
          <div className="p-5 sm:p-6 space-y-3">
            <div className="flex items-center gap-2">
              <span className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
                <span className="material-symbols-outlined text-[18px]">gavel</span>
              </span>
              <div>
                <h2 className="text-sm font-bold text-on-surface">3. Phán quyết &amp; Kết luận từ Sàn DANASEA</h2>
                <p className="text-[11px] text-on-surface-variant">Quyết định giải quyết tranh chấp có hiệu lực thi hành</p>
              </div>
            </div>

            {dispute.resolutionNote ? (
              <div className="p-3.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/30 text-emerald-900 dark:text-emerald-200 border border-emerald-200 dark:border-emerald-800 text-xs leading-relaxed">
                <div className="flex items-center gap-1.5 font-bold mb-1">
                  <span className="material-symbols-outlined text-[16px]">verified</span>
                  <span>Kết luận chính thức của Admin:</span>
                </div>
                <p>{dispute.resolutionNote}</p>
              </div>
            ) : (
              <div className="p-4 rounded-lg bg-surface-container-low/30 text-center text-xs text-on-surface-variant border border-outline-variant/15 flex flex-col items-center gap-1.5">
                <span className="material-symbols-outlined text-[24px] text-outline">hourglass_top</span>
                <p className="font-medium">Hồ sơ đang trong quá trình thụ lý và xem xét bởi Hội đồng Cảng vụ DANASEA.</p>
                <span className="text-[11px] text-outline">Thời gian xử lý trung bình: 24h - 48h làm việc.</span>
              </div>
            )}
          </div>
        </div>

        {/* RIGHT COLUMN (4 cols): Sticky Summary Drawer Panel */}
        <div className="lg:col-span-4 sticky top-20">
          <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 shadow-xs p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/15">
              <h3 className="text-sm font-bold text-on-surface">Tóm tắt hồ sơ</h3>
              <span className="text-[11px] font-mono text-primary font-bold">{dispute.id}</span>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-on-surface-variant">Trạng thái</span>
                <div>{getStatusBadge(dispute.status)}</div>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-on-surface-variant">Mã đơn con (SubOrder)</span>
                <span className="font-mono font-bold text-on-surface">{dispute.subOrderId}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-on-surface-variant">Khách hàng khiếu nại</span>
                <span className="font-semibold text-on-surface">{dispute.raisedBy}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-on-surface-variant">Phân loại</span>
                <span className="font-medium text-on-surface">{dispute.category}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-on-surface-variant">Ngày khởi tạo</span>
                <span className="font-mono text-on-surface-variant">
                  {new Date(dispute.createdAt).toLocaleDateString('vi-VN')}
                </span>
              </div>
            </div>

            {/* Policy Notes */}
            <div className="pt-3 border-t border-outline-variant/15">
              <div className="p-3 rounded-lg bg-surface-container-low/40 border border-outline-variant/15 space-y-1 text-[11px] text-on-surface-variant leading-relaxed">
                <span className="font-semibold text-on-surface flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px] text-primary">info</span>
                  Quy chuẩn hòa giải:
                </span>
                <p>
                  Tiền cược đối soát của đơn con liên quan sẽ tạm giữ trên ví bảo chứng ký quỹ cho đến khi có phán quyết cuối cùng từ Admin.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
