import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';

interface ServiceImageItem {
  id: string;
  url: string;
  sort_order: number;
  caption?: string;
}

export function ServiceForm() {
  const navigate = useNavigate();

  // Basic info
  const [nameVi, setNameVi] = useState('Chèo SUP đón bình minh biển Mỹ Khê');
  const [nameEn, setNameEn] = useState('Sunrise Stand-up Paddleboarding at My Khe Beach');
  const [category, setCategory] = useState('Chèo SUP & Kayak');
  const [basePrice, setBasePrice] = useState(280000);
  const [durationMinutes, setDurationMinutes] = useState(120);
  const [capacity, setCapacity] = useState(12);

  // Maritime safety fields strictly adhering to DATABASE_AUDIT.md:
  // min_wind_kmh, max_wave_m, weather_sensitive, waiver_content
  const [min_wind_kmh, setMinWindKmh] = useState<number>(10);
  const [max_wave_m, setMaxWaveM] = useState<number>(1.2);
  const [weather_sensitive, setWeatherSensitive] = useState<boolean>(true);
  const [waiver_content, setWaiverContent] = useState<string>(
    'Du khách cam kết đủ sức khỏe tham gia hoạt động biển, không mắc bệnh tim mạch cấp tính, tuân thủ mặc áo phao cứu sinh đạt chuẩn VITA trong suốt quá trình chèo trên mặt nước và chấp hành hiệu lệnh của huấn luyện viên/cứu hộ bãi biển.'
  );

  // Service images with sort_order
  const [images, setImages] = useState<ServiceImageItem[]>([
    {
      id: 'img-1',
      url: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC7CA5tOzJev3TShmxMJ4QbD1KAtw2Nt4zcS-OGJysAF35iVA3yiq1cn7LzS5avIoYwSZtPPYOK8Uy4sAiKi81ovlXynWiu-ABcA7jMJ4VLmNNaKQnR9XjELpnzI_DKus9UZnuzQy55BJ1ZQa3wNf8-in9TALBABjR7nBMIJYRSaUKp1dthyttkxPw5Z6gKOQz6AO3zsLYbigGjjmP7NTtVdChlPTxTxv31Xsc_IzrAeN1uHkN-vqkS_Q',
      sort_order: 1,
      caption: 'Đoàn khách chèo SUP lúc 05:30 đón bình minh'
    },
    {
      id: 'img-2',
      url: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=600&q=80',
      sort_order: 2,
      caption: 'Thiết bị ván chèo và áo phao trợ nổi kiểm định'
    },
    {
      id: 'img-3',
      url: 'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=600&q=80',
      sort_order: 3,
      caption: 'Góc chụp flycam lưu niệm miễn phí'
    }
  ]);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Reordering images by sort_order
  const handleMoveImage = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIndex = direction === 'UP' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= images.length) return;

    const newArr = [...images];
    const temp = newArr[index];
    newArr[index] = newArr[targetIndex];
    newArr[targetIndex] = temp;

    // update sort_order
    const updated = newArr.map((img, idx) => ({ ...img, sort_order: idx + 1 }));
    setImages(updated);
    showToast(`Đã thay đổi thứ tự ảnh thành công.`);
  };

  const handleAddImage = () => {
    const newImg: ServiceImageItem = {
      id: 'img-' + Date.now(),
      url: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80',
      sort_order: images.length + 1,
      caption: 'Ảnh hoạt động trải nghiệm bổ sung'
    };
    setImages(prev => [...prev, newImg]);
    showToast('Đã thêm 1 ảnh vào thư viện.');
  };

  const handleRemoveImage = (id: string) => {
    if (images.length <= 1) {
      alert('Dịch vụ cần tối thiểu 1 hình ảnh đại diện.');
      return;
    }
    const filtered = images.filter(img => img.id !== id).map((img, idx) => ({ ...img, sort_order: idx + 1 }));
    setImages(filtered);
  };

  // Action: Save Draft (status = DRAFT)
  const handleSaveDraft = () => {
    showToast('Đã lưu dịch vụ ở trạng thái BẢN NHÁP (DRAFT)!');
    setTimeout(() => navigate('/vendor/services'), 1200);
  };

  // Action: Submit for approval (status = PENDING_APPROVAL, NOT self-published)
  const handleSubmitApproval = () => {
    if (!nameVi.trim()) {
      alert('Vui lòng nhập Tên dịch vụ (Tiếng Việt).');
      return;
    }
    if (basePrice <= 0) {
      alert('Đơn giá niêm yết phải lớn hơn 0đ.');
      return;
    }
    if (!waiver_content.trim()) {
      alert('Vui lòng nhập nội dung điều khoản miễn trừ trách nhiệm (waiver_content).');
      return;
    }

    showToast('Hồ sơ đã gửi tới Ban Quản lý Cảng vụ ở trạng thái CHỜ DUYỆT (PENDING_APPROVAL). Vendor không tự duyệt dịch vụ sang PUBLISHED.');
    setTimeout(() => navigate('/vendor/services'), 1500);
  };

  return (
    <main className="w-full bg-background flex-1">
      <div className="flex flex-col w-full">
        {/* Header Bar for Service Editor */}
        <div className="w-full bg-surface-container-lowest border-b border-outline-variant/30 px-space-lg py-space-md flex flex-wrap items-center justify-between gap-space-md shadow-xs">
          <div className="flex flex-col">
            <nav className="flex items-center gap-space-xs text-on-surface-variant font-label-sm text-label-sm">
              <span onClick={() => navigate('/vendor/services')} className="hover:text-primary transition-colors cursor-pointer">
                Dịch vụ &amp; Trải nghiệm
              </span>
              <span className="material-symbols-outlined text-[14px]">chevron_right</span>
              <span className="text-primary font-semibold">Soạn dịch vụ mới</span>
            </nav>
            <div className="flex items-center gap-space-sm mt-0.5">
              <h1 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                {nameVi || 'Chưa đặt tên dịch vụ'}
              </h1>
              <span className="px-2.5 py-0.5 rounded-full bg-secondary-container/15 text-secondary font-label-sm text-label-sm font-semibold flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>Bản nháp (DRAFT)
              </span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-space-sm">
            <button 
              onClick={handleSaveDraft}
              className="px-3.5 py-2 rounded-lg bg-surface-container-highest hover:bg-surface-container text-on-surface text-sm font-medium transition-colors flex items-center gap-1.5" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">save</span>
              <span>Lưu nháp</span>
            </button>
            <button 
              onClick={() => setPreviewOpen(true)}
              className="px-3.5 py-2 rounded-lg bg-surface-container-lowest hover:bg-surface-container-low text-primary text-sm font-medium border border-outline-variant/30 transition-colors flex items-center gap-1.5" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">visibility</span>
              <span>Xem trước</span>
            </button>
            <button 
              onClick={handleSubmitApproval}
              className="px-4 py-2 rounded-lg bg-primary hover:bg-primary-container text-on-primary text-sm font-bold transition-colors flex items-center gap-1.5 shadow-xs" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">send</span>
              <span>Gửi duyệt Cảng vụ</span>
            </button>
          </div>
        </div>

        {/* Main Content Layout */}
        <div className="w-full px-6 py-6">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
            {/* LEFT COLUMN: Consolidated Service Form Panel */}
            <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl border border-outline-variant/20 shadow-xs divide-y divide-outline-variant/20 overflow-hidden">
              {/* 1. Thông tin cơ bản */}
              <section className="p-6 space-y-5">
                <div className="flex items-center justify-between pb-3 border-b border-outline-variant/15">
                  <div className="flex items-center gap-2">
                    <span className="w-7 h-7 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">tune</span>
                    </span>
                    <h2 className="font-bold text-base text-on-surface">1. Thông tin cơ bản</h2>
                  </div>
                  <span className="text-xs text-primary font-semibold">Bắt buộc</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">
                      Tên dịch vụ (Tiếng Việt) <span className="text-error font-bold">*</span>
                    </label>
                    <input 
                      type="text" 
                      value={nameVi}
                      onChange={(e) => setNameVi(e.target.value)}
                      className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary" 
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">
                      Service Name (English)
                    </label>
                    <input 
                      type="text" 
                      value={nameEn}
                      onChange={(e) => setNameEn(e.target.value)}
                      className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary" 
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">Danh mục hoạt động</label>
                    <select 
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary cursor-pointer"
                    >
                      <option value="Chèo SUP & Kayak">Chèo SUP &amp; Kayak</option>
                      <option value="Mô tô nước & Jetski">Mô tô nước &amp; Jetski</option>
                      <option value="Lặn biển & San hô">Lặn biển &amp; San hô</option>
                      <option value="Cano cao tốc">Cano cao tốc</option>
                      <option value="Dù bay Parasailing">Dù bay Parasailing</option>
                    </select>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">
                      Đơn giá niêm yết trọn gói (base_price) <span className="text-error font-bold">*</span>
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={basePrice}
                        onChange={(e) => setBasePrice(Number(e.target.value))}
                        className="w-full pl-3.5 pr-20 py-2.5 rounded-lg bg-surface-container-low text-on-surface font-mono font-bold text-primary border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary text-sm" 
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-xs font-medium">đ / người</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">Thời lượng (duration_minutes)</label>
                    <div className="relative flex items-center">
                      <input 
                        type="number" 
                        value={durationMinutes}
                        onChange={(e) => setDurationMinutes(Number(e.target.value))}
                        className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary" 
                      />
                      <span className="absolute right-3 text-on-surface-variant text-xs">phút</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface">Sức chứa tối đa / ca (capacity)</label>
                    <div className="relative flex items-center">
                      <input 
                        type="number" 
                        value={capacity}
                        onChange={(e) => setCapacity(Number(e.target.value))}
                        className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary" 
                      />
                      <span className="absolute right-3 text-on-surface-variant text-xs">khách / slot</span>
                    </div>
                  </div>
                </div>
              </section>

              {/* 2. Quy chuẩn An toàn Hàng hải & Điều kiện thời tiết (Schema Strict) */}
              <section className="p-6 space-y-5">
                <div className="flex items-center justify-between pb-3 border-b border-outline-variant/15">
                  <div className="flex items-center gap-2">
                    <span className="w-7 h-7 rounded-lg bg-secondary/10 text-secondary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">air</span>
                    </span>
                    <h2 className="font-bold text-base text-on-surface">
                      2. Tiêu chuẩn An toàn Biển &amp; Thời tiết
                    </h2>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full bg-secondary-container/20 text-secondary text-xs font-semibold">
                    Quy chuẩn Cảng vụ
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {/* min_wind_kmh */}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface flex items-center gap-1">
                      <span>Gió tối thiểu (min_wind_kmh)</span>
                      <span className="material-symbols-outlined text-outline text-[14px]" title="Độ nhạy gió tối thiểu cần để vận hành">help</span>
                    </label>
                    <div className="relative flex items-center">
                      <input 
                        type="number"
                        value={min_wind_kmh}
                        onChange={(e) => setMinWindKmh(Number(e.target.value))}
                        className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary"
                      />
                      <span className="absolute right-3 text-on-surface-variant text-xs">km/h</span>
                    </div>
                  </div>

                  {/* max_wave_m */}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-semibold text-on-surface flex items-center gap-1">
                      <span>Sóng tối đa (max_wave_m)</span>
                      <span className="material-symbols-outlined text-outline text-[14px]" title="Ngưỡng sóng biển tối đa cho phép xuất bến an toàn">waves</span>
                    </label>
                    <div className="relative flex items-center">
                      <input 
                        type="number"
                        step="0.1"
                        value={max_wave_m}
                        onChange={(e) => setMaxWaveM(Number(e.target.value))}
                        className="w-full px-3.5 py-2.5 rounded-lg bg-surface-container-low text-on-surface text-sm border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary"
                      />
                      <span className="absolute right-3 text-on-surface-variant text-xs">mét</span>
                    </div>
                  </div>

                  {/* weather_sensitive toggle */}
                  <div className="flex flex-col gap-1.5 justify-center">
                    <span className="text-xs font-semibold text-on-surface">Nhạy cảm thời tiết (weather_sensitive)</span>
                    <label className="flex items-center gap-2 cursor-pointer mt-1">
                      <input 
                        type="checkbox"
                        checked={weather_sensitive}
                        onChange={(e) => setWeatherSensitive(e.target.checked)}
                        className="w-4 h-4 rounded text-primary focus:ring-primary"
                      />
                      <span className="text-xs text-on-surface">Tự động cảnh báo khi biển động</span>
                    </label>
                  </div>
                </div>

                {/* waiver_content */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-on-surface flex items-center justify-between">
                    <span>Điều khoản miễn trừ trách nhiệm hàng hải (waiver_content)</span>
                    <span className="text-error font-bold text-[11px]">* Bắt buộc</span>
                  </label>
                  <textarea 
                    rows={3}
                    value={waiver_content}
                    onChange={(e) => setWaiverContent(e.target.value)}
                    placeholder="Nhập nội dung cam kết an toàn, chấp thuận mang phao và tuân thủ quy chế an toàn biển..."
                    className="w-full p-3 rounded-lg bg-surface-container-low text-on-surface text-xs border border-outline-variant/30 focus:outline-none focus:ring-1 focus:ring-primary leading-relaxed"
                  />
                  <p className="text-[11px] text-on-surface-variant">
                    Du khách bắt buộc phải đọc và tích chọn chấp thuận cam kết an toàn trước khi thanh toán.
                  </p>
                </div>
              </section>

              {/* 3. Thư viện hình ảnh có sort_order */}
              <section className="p-6 space-y-5">
                <div className="flex items-center justify-between pb-3 border-b border-outline-variant/15">
                  <div className="flex items-center gap-2">
                    <span className="w-7 h-7 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">photo_library</span>
                    </span>
                    <h2 className="font-bold text-base text-on-surface">
                      3. Hình ảnh trải nghiệm (quản lý theo sort_order)
                    </h2>
                  </div>
                  <button 
                    onClick={handleAddImage}
                    className="px-3 py-1.5 rounded-lg bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors flex items-center gap-1 cursor-pointer"
                    type="button"
                  >
                    <span className="material-symbols-outlined text-[16px]">add_photo_alternate</span>
                    <span>Thêm ảnh</span>
                  </button>
                </div>

                <div className="border border-outline-variant/20 rounded-lg divide-y divide-outline-variant/15 overflow-hidden">
                  {images.map((img, idx) => (
                    <div key={img.id} className="flex items-center justify-between p-3 bg-surface-container-lowest hover:bg-surface-container-low/20 transition-colors gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <span className="w-6 h-6 rounded bg-surface-container-high flex items-center justify-center font-mono font-bold text-xs text-primary shrink-0">
                          #{img.sort_order}
                        </span>
                        <div className="w-14 h-10 rounded-md overflow-hidden shrink-0 bg-surface-container-high">
                          <img src={img.url} alt={img.caption} className="w-full h-full object-cover" />
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="text-xs font-bold text-on-surface truncate">
                            {idx === 0 ? 'Ảnh đại diện chính (Cover)' : `Ảnh chi tiết #${img.sort_order}`}
                          </span>
                          <span className="text-[11px] text-on-surface-variant truncate">{img.caption}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1 shrink-0">
                        <button 
                          onClick={() => handleMoveImage(idx, 'UP')}
                          disabled={idx === 0}
                          title="Di chuyển lên trước"
                          className="p-1.5 rounded hover:bg-surface-container disabled:opacity-20 transition-colors cursor-pointer"
                          type="button"
                        >
                          <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
                        </button>
                        <button 
                          onClick={() => handleMoveImage(idx, 'DOWN')}
                          disabled={idx === images.length - 1}
                          title="Di chuyển xuống sau"
                          className="p-1.5 rounded hover:bg-surface-container disabled:opacity-20 transition-colors cursor-pointer"
                          type="button"
                        >
                          <span className="material-symbols-outlined text-[16px]">arrow_downward</span>
                        </button>
                        <button 
                          onClick={() => handleRemoveImage(img.id)}
                          title="Xóa ảnh"
                          className="p-1.5 rounded text-error hover:bg-error/10 transition-colors cursor-pointer"
                          type="button"
                        >
                          <span className="material-symbols-outlined text-[16px]">delete</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </div>

            {/* RIGHT COLUMN: Compliance Checklist Panel */}
            <div className="lg:col-span-4 sticky top-20">
              <div className="bg-surface-container-lowest rounded-xl p-5 border border-outline-variant/20 shadow-xs space-y-4">
                <div className="flex items-center gap-2 text-primary font-bold text-sm">
                  <span className="material-symbols-outlined text-[20px]">gavel</span>
                  <span>Quy trình Xét duyệt Cảng vụ</span>
                </div>
                <p className="text-xs text-on-surface-variant leading-relaxed">
                  Dịch vụ mới tạo sẽ mang trạng thái <strong>PENDING_APPROVAL</strong>. Ban Quản lý Cảng vụ sẽ kiểm định tọa độ xuất bến, giới hạn thời tiết an toàn và điều khoản miễn trừ trước khi duyệt sang <strong>PUBLISHED</strong>.
                </p>

                <div className="pt-3 border-t border-outline-variant/15 flex flex-col gap-2">
                  <button 
                    onClick={handleSubmitApproval}
                    className="w-full py-2.5 rounded-lg bg-primary text-on-primary text-sm font-bold shadow-xs hover:bg-primary-container transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                    type="button"
                  >
                    <span className="material-symbols-outlined text-[18px]">send</span>
                    <span>Gửi Cảng vụ xét duyệt</span>
                  </button>
                  <button 
                    onClick={handleSaveDraft}
                    className="w-full py-2 rounded-lg bg-surface-container-high text-on-surface text-xs font-semibold hover:bg-surface-container-highest transition-colors cursor-pointer"
                    type="button"
                  >
                    Lưu bản nháp (DRAFT)
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
          <span className="font-label-md text-label-md">{toastMessage}</span>
        </div>
      )}

      {/* Preview Modal */}
      {previewOpen && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="font-bold text-slate-900 text-lg">Xem trước thông tin dịch vụ</h3>
              <button onClick={() => setPreviewOpen(false)} className="text-slate-400 hover:text-slate-600 cursor-pointer">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <span className="text-xs text-slate-500 font-medium">{category}</span>
                <h4 className="font-bold text-slate-900 text-base">{nameVi || 'Chưa đặt tên'}</h4>
                <p className="text-xs text-slate-500">{nameEn}</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-xl space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-600">Đơn giá:</span>
                  <span className="font-bold text-primary">{basePrice.toLocaleString('vi-VN')} đ / người</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Thời lượng:</span>
                  <span className="font-semibold text-slate-800">{durationMinutes} phút</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Sức chứa mỗi ca:</span>
                  <span className="font-semibold text-slate-800">{capacity} người</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Giới hạn sóng biển:</span>
                  <span className="font-semibold text-slate-800">&le; {max_wave_m}m</span>
                </div>
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium block mb-1">Cam kết miễn trừ an toàn:</span>
                <p className="text-xs text-slate-700 leading-relaxed bg-slate-50 p-2.5 rounded-lg max-h-32 overflow-y-auto">
                  {waiver_content}
                </p>
              </div>
            </div>
            <div className="flex justify-end pt-2 border-t">
              <button
                onClick={() => setPreviewOpen(false)}
                className="px-4 py-2 bg-slate-900 text-white rounded-xl text-xs font-semibold hover:bg-slate-800 cursor-pointer"
              >
                Đóng xem trước
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}
