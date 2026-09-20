import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MOCK_DISPUTES } from '../../../mockData';

export function DisputeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const dispute = MOCK_DISPUTES.find(d => d.id === id);
  const [evidence, setEvidence] = useState<File[]>([]);

  if (!dispute) {
    return <div className="p-space-lg text-center font-body-lg text-error">Không tìm thấy khiếu nại.</div>;
  }

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setEvidence(prev => [...prev, ...Array.from(e.target.files!)]);
    }
  };

  const handleRemoveFile = (index: number) => {
    setEvidence(prev => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="p-space-lg w-full max-w-[900px] mx-auto flex flex-col min-h-full">
      <button onClick={() => navigate('/vendor/disputes')} className="self-start flex items-center gap-1 text-on-surface-variant hover:text-primary mb-space-lg font-label-md transition-colors">
        <span className="material-symbols-outlined text-[18px]">arrow_back</span>
        Quay lại danh sách
      </button>

      <div className="flex items-center justify-between mb-space-xl">
        <div className="flex flex-col gap-1">
          <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest">Chi tiết khiếu nại</span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface">{dispute.id}</h1>
        </div>
        <div className="px-space-md py-1.5 rounded-full bg-primary-container text-on-primary-container font-label-sm font-bold flex items-center gap-2">
          <span className="material-symbols-outlined text-[16px]">gavel</span>
          {dispute.status}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-space-lg mb-space-lg">
        {/* Basic Info */}
        <div className="bg-surface-container-lowest p-space-lg rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-sm">
          <h3 className="font-label-lg text-label-lg text-on-surface border-b border-outline-variant/30 pb-space-sm mb-space-xs">Thông tin chung</h3>
          <div className="flex justify-between items-center">
            <span className="text-on-surface-variant font-body-sm">Mã đơn con (SubOrder)</span>
            <span className="font-label-md font-bold text-on-surface">{dispute.subOrderId}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-on-surface-variant font-body-sm">Người khiếu nại</span>
            <span className="font-label-md font-bold text-on-surface">{dispute.raisedBy}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-on-surface-variant font-body-sm">Ngày tạo</span>
            <span className="font-label-md font-bold text-on-surface">{new Date(dispute.createdAt).toLocaleString('vi-VN')}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-on-surface-variant font-body-sm">Phân loại</span>
            <span className="font-label-md font-bold text-on-surface">{dispute.category}</span>
          </div>
        </div>

        {/* Resolution Info */}
        <div className="bg-surface-container-lowest p-space-lg rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-sm">
          <h3 className="font-label-lg text-label-lg text-on-surface border-b border-outline-variant/30 pb-space-sm mb-space-xs">Kết luận của Admin</h3>
          {dispute.resolutionNote ? (
            <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
              {dispute.resolutionNote}
            </p>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-on-surface-variant/60 gap-2 my-space-md">
              <span className="material-symbols-outlined text-[32px]">pending_actions</span>
              <p className="font-body-sm text-center">Chưa có kết luận từ Admin.<br/>Đang trong quá trình xem xét.</p>
            </div>
          )}
        </div>
      </div>

      {/* Description */}
      <div className="bg-surface-container-lowest p-space-lg rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-sm mb-space-lg">
        <h3 className="font-label-lg text-label-lg text-on-surface flex items-center gap-2">
          <span className="material-symbols-outlined text-[18px]">description</span> Nội dung phản ánh
        </h3>
        <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed p-space-md bg-surface-container-low/30 rounded-xl">
          {dispute.description}
        </p>
      </div>

      {/* Vendor Evidence */}
      <div className="bg-surface-container-lowest p-space-lg rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-sm">
        <h3 className="font-label-lg text-label-lg text-on-surface flex items-center gap-2 mb-space-xs">
          <span className="material-symbols-outlined text-[18px]">upload_file</span> Cung cấp minh chứng
        </h3>
        <p className="font-body-sm text-body-sm text-on-surface-variant mb-space-sm">
          Vendor có thể tải lên tài liệu, hình ảnh (PDF, JPG, PNG) để cung cấp minh chứng cho Admin xem xét. (Đây là bản xem trước, tệp chưa được lưu lên máy chủ).
        </p>

        {/* Upload Box */}
        <label className="border-2 border-dashed border-outline-variant/50 rounded-xl p-space-xl flex flex-col items-center justify-center text-center cursor-pointer hover:bg-surface-container-low/50 transition-colors">
          <span className="material-symbols-outlined text-primary text-[32px] mb-2">cloud_upload</span>
          <span className="font-label-md text-on-surface mb-1">Bấm để tải tệp lên hoặc kéo thả vào đây</span>
          <span className="font-body-sm text-on-surface-variant">Hỗ trợ .pdf, .jpg, .png (Tối đa 10MB)</span>
          <input type="file" multiple accept=".pdf,.png,.jpg,.jpeg" className="hidden" onChange={handleFileUpload} />
        </label>

        {/* Evidence List */}
        {evidence.length > 0 && (
          <div className="mt-space-md flex flex-col gap-space-xs">
            <h4 className="font-label-sm text-on-surface">Tệp đã chọn:</h4>
            {evidence.map((file, idx) => (
              <div key={idx} className="flex items-center justify-between p-space-sm rounded-lg bg-surface-container border border-outline-variant/20">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px] text-tertiary">draft</span>
                  <span className="font-body-sm text-on-surface truncate max-w-[300px]">{file.name}</span>
                  <span className="text-[10px] text-on-surface-variant">({(file.size / 1024).toFixed(1)} KB)</span>
                </div>
                <button type="button" onClick={() => handleRemoveFile(idx)} className="text-error hover:bg-error/10 p-1 rounded-full flex items-center justify-center transition-colors">
                  <span className="material-symbols-outlined text-[16px]">close</span>
                </button>
              </div>
            ))}
            <button className="mt-space-sm self-start px-space-xl py-space-sm bg-primary text-on-primary rounded-full font-label-md shadow-md hover:bg-primary/90 transition-colors">
              Gửi minh chứng
            </button>
          </div>
        )}
      </div>

    </div>
  );
}
