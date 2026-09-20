import React, { useState } from 'react';
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

  // Departure point & GPS coordinates
  const [stationName, setStationName] = useState('Trạm cứu hộ bãi tắm Mỹ Khê 2');
  const [stationAddress, setStationAddress] = useState('Đường Võ Nguyên Giáp, Phường Phước Mỹ, Sơn Trà, Đà Nẵng');
  const [latitude, setLatitude] = useState('16.0612');
  const [longitude, setLongitude] = useState('108.2435');

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
    <main className="w-full pt-16 bg-background flex-1">
      <div className="flex flex-col w-full">
        {/* Sticky Header Bar for Service Editor */}
        <div className="sticky top-16 z-30 bg-surface/90 backdrop-blur-md shadow-sm px-space-lg py-space-md flex flex-wrap items-center justify-between gap-space-md">
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
              className="px-space-md py-2.5 rounded-xl bg-surface-container-highest hover:bg-surface-container text-on-surface font-label-lg text-label-lg transition-all flex items-center gap-1.5 shadow-sm" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">save</span>
              Lưu bản nháp (DRAFT)
            </button>
            <button 
              onClick={() => setPreviewOpen(true)}
              className="px-space-md py-2.5 rounded-xl bg-surface-container-lowest hover:bg-surface-container-low text-primary font-label-lg text-label-lg transition-all flex items-center gap-1.5 shadow-sm" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">visibility</span>
              Xem trước
            </button>
            <button 
              onClick={handleSubmitApproval}
              className="px-space-lg py-2.5 rounded-xl bg-primary hover:bg-primary-container text-on-primary font-label-lg text-label-lg transition-all flex items-center gap-1.5 shadow-md font-bold" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">send</span>
              Gửi Cảng vụ xét duyệt
            </button>
          </div>
        </div>

        {/* Main Content Layout */}
        <div className="w-full px-space-lg py-space-xl max-w-[1440px] mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
            {/* LEFT COLUMN: 65% Primary Form */}
            <div className="lg:col-span-8 flex flex-col gap-space-xl">
              {/* 1. Thông tin cơ bản */}
              <section className="bg-surface-container-lowest rounded-2xl p-space-xl shadow-sm flex flex-col gap-space-lg border border-outline-variant/30">
                <div className="flex items-center justify-between pb-space-sm border-b border-outline-variant/20">
                  <div className="flex items-center gap-space-sm">
                    <span className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[20px]">tune</span>
                    </span>
                    <h2 className="font-headline-sm text-headline-sm text-on-surface font-bold">1. Thông tin cơ bản</h2>
                  </div>
                  <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider font-bold">Bắt buộc</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">
                      Tên dịch vụ (Tiếng Việt) <span className="text-error font-bold">*</span>
                    </label>
                    <input 
                      type="text" 
                      value={nameVi}
                      onChange={(e) => setNameVi(e.target.value)}
                      className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary shadow-sm" 
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">
                      Service Name (English)
                    </label>
                    <input 
                      type="text" 
                      value={nameEn}
                      onChange={(e) => setNameEn(e.target.value)}
                      className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary shadow-sm" 
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">Danh mục hoạt động</label>
                    <select 
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary shadow-sm cursor-pointer"
                    >
                      <option value="Chèo SUP & Kayak">Chèo SUP &amp; Kayak</option>
                      <option value="Mô tô nước & Jetski">Mô tô nước &amp; Jetski</option>
                      <option value="Lặn biển & San hô">Lặn biển &amp; San hô</option>
                      <option value="Cano cao tốc">Cano cao tốc</option>
                      <option value="Dù bay Parasailing">Dù bay Parasailing</option>
                    </select>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">
                      Đơn giá niêm yết trọn gói (base_price) <span className="text-error font-bold">*</span>
                    </label>
                    <div className="relative">
                      <input 
                        type="number" 
                        value={basePrice}
                        onChange={(e) => setBasePrice(Number(e.target.value))}
                        className="w-full pl-space-md pr-24 py-3 rounded-xl bg-surface-container-low text-on-surface font-headline-sm font-bold text-primary focus:ring-2 focus:ring-primary shadow-sm" 
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant font-label-md font-semibold">đ / người</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">Thời lượng (duration_minutes)</label>
                    <div className="relative flex items-center">
                      <input 
                        type="number" 
                        value={durationMinutes}
                        onChange={(e) => setDurationMinutes(Number(e.target.value))}
                        className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary shadow-sm" 
                      />
                      <span className="absolute right-4 text-on-surface-variant font-label-md">phút</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md text-label-md text-on-surface font-semibold">Sức chứa tối đa / ca (capacity)</label>
                    <div className="relative flex items-center">
                      <input 
                        type="number" 
                        value={capacity}
                        onChange={(e) => setCapacity(Number(e.target.value))}
                        className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary shadow-sm" 
                      />
                      <span className="absolute right-4 text-on-surface-variant font-label-md">khách / slot</span>
                    </div>
                  </div>
                </div>
              </section>

              {/* 2. Quy chuẩn An toàn Hàng hải & Điều kiện thời tiết (Schema Strict) */}
              <section className="bg-surface-container-lowest rounded-2xl p-space-xl shadow-sm flex flex-col gap-space-lg border border-outline-variant/30">
                <div className="flex items-center justify-between pb-space-sm border-b border-outline-variant/20">
                  <div className="flex items-center gap-space-sm">
                    <span className="w-8 h-8 rounded-xl bg-secondary/10 text-secondary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[20px]">air</span>
                    </span>
                    <h2 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                      2. Tiêu chuẩn An toàn Biển &amp; Thời tiết
                    </h2>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full bg-secondary-container/20 text-secondary font-label-sm font-semibold">
                    Quy chuẩn Cảng vụ
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-space-md">
                  {/* min_wind_kmh - Strictly named as requested */}
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md font-semibold text-on-surface flex items-center gap-1">
                      <span>Tốc độ gió tối thiểu (min_wind_kmh)</span>
                      <span className="material-symbols-outlined text-outline text-[16px]" title="Độ nhạy gió tối thiểu cần để vận hành">help</span>
                    </label>
                    <div className="relative flex items-center">
                      <input 
                        type="number"
                        value={min_wind_kmh}
                        onChange={(e) => setMinWindKmh(Number(e.target.value))}
                        className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary"
                      />
                      <span className="absolute right-4 text-on-surface-variant font-label-sm">km/h</span>
                    </div>
                  </div>

                  {/* max_wave_m */}
                  <div className="flex flex-col gap-1.5">
                    <label className="font-label-md font-semibold text-on-surface flex items-center gap-1">
                      <span>Chiều cao sóng tối đa (max_wave_m)</span>
                      <span className="material-symbols-outlined text-outline text-[16px]" title="Ngưỡng sóng biển tối đa cho phép xuất bến an toàn">waves</span>
                    </label>
                    <div className="relative flex items-center">
                      <input 
                        type="number"
                        step="0.1"
                        value={max_wave_m}
                        onChange={(e) => setMaxWaveM(Number(e.target.value))}
                        className="w-full px-space-md py-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:ring-2 focus:ring-primary"
                      />
                      <span className="absolute right-4 text-on-surface-variant font-label-sm">mét</span>
                    </div>
                  </div>

                  {/* weather_sensitive toggle */}
                  <div className="flex flex-col gap-1.5 justify-center">
                    <span className="font-label-md font-semibold text-on-surface">Nhạy cảm thời tiết (weather_sensitive)</span>
                    <label className="flex items-center gap-2 cursor-pointer mt-2">
                      <input 
                        type="checkbox"
                        checked={weather_sensitive}
                        onChange={(e) => setWeatherSensitive(e.target.checked)}
                        className="w-5 h-5 rounded text-primary focus:ring-primary"
                      />
                      <span className="font-body-sm text-on-surface">Có (Tự động cảnh báo khi thời tiết xấu)</span>
                    </label>
                  </div>
                </div>

                {/* waiver_content */}
                <div className="flex flex-col gap-1.5 pt-2">
                  <label className="font-label-md font-semibold text-on-surface flex items-center justify-between">
                    <span>Điều khoản miễn trừ trách nhiệm hàng hải (waiver_content)</span>
                    <span className="text-error font-bold">* Bắt buộc</span>
                  </label>
                  <textarea 
                    rows={3}
                    value={waiver_content}
                    onChange={(e) => setWaiverContent(e.target.value)}
                    placeholder="Nhập nội dung cam kết an toàn, chấp thuận mang phao và tuân thủ quy chế an toàn biển..."
                    className="w-full p-space-md rounded-xl bg-surface-container-low text-on-surface font-body-sm focus:ring-2 focus:ring-primary shadow-sm"
                  />
                  <p className="text-[11px] text-on-surface-variant">
                    Nội dung này bắt buộc du khách phải đọc và tích chọn chấp thuận trước khi tiến hành thanh toán đặt cọc.
                  </p>
                </div>
              </section>

              {/* 3. Thư viện hình ảnh có sort_order */}
              <section className="bg-surface-container-lowest rounded-2xl p-space-xl shadow-sm flex flex-col gap-space-lg border border-outline-variant/30">
                <div className="flex items-center justify-between pb-space-sm border-b border-outline-variant/20">
                  <div className="flex items-center gap-space-sm">
                    <span className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
                      <span className="material-symbols-outlined text-[20px]">photo_library</span>
                    </span>
                    <h2 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                      3. Hình ảnh trải nghiệm (quản lý theo sort_order)
                    </h2>
                  </div>
                  <button 
                    onClick={handleAddImage}
                    className="px-3 py-1.5 rounded-xl bg-primary-fixed text-on-primary-fixed font-label-sm font-semibold hover:bg-primary hover:text-on-primary transition-all flex items-center gap-1"
                  >
                    <span className="material-symbols-outlined text-[16px]">add_photo_alternate</span>
                    Thêm ảnh
                  </button>
                </div>

                <div className="flex flex-col gap-space-sm">
                  {images.map((img, idx) => (
                    <div key={img.id} className="flex items-center justify-between p-space-sm rounded-xl bg-surface-container-low/40 border border-outline-variant/30 gap-space-md">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-surface-container-high flex items-center justify-center font-bold text-label-sm text-primary">
                          #{img.sort_order}
                        </div>
                        <div className="w-16 h-12 rounded-lg overflow-hidden shrink-0 shadow-sm bg-surface-container-high">
                          <img src={img.url} alt={img.caption} className="w-full h-full object-cover" />
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="font-label-md font-bold text-on-surface truncate">
                            {idx === 0 ? 'Ảnh đại diện chính (Cover)' : `Ảnh chi tiết #${img.sort_order}`}
                          </span>
                          <span className="text-[12px] text-on-surface-variant truncate">{img.caption}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <button 
                          onClick={() => handleMoveImage(idx, 'UP')}
                          disabled={idx === 0}
                          title="Di chuyển lên trước"
                          className="p-1.5 rounded-lg hover:bg-surface-container disabled:opacity-30 transition-colors"
                        >
                          <span className="material-symbols-outlined text-[18px]">arrow_upward</span>
                        </button>
                        <button 
                          onClick={() => handleMoveImage(idx, 'DOWN')}
                          disabled={idx === images.length - 1}
                          title="Di chuyển xuống sau"
                          className="p-1.5 rounded-lg hover:bg-surface-container disabled:opacity-30 transition-colors"
                        >
                          <span className="material-symbols-outlined text-[18px]">arrow_downward</span>
                        </button>
                        <button 
                          onClick={() => handleRemoveImage(img.id)}
                          title="Xóa ảnh"
                          className="p-1.5 rounded-lg text-error hover:bg-error/10 transition-colors"
                        >
                          <span className="material-symbols-outlined text-[18px]">delete</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </div>

            {/* RIGHT COLUMN: Compliance Checklist */}
            <div className="lg:col-span-4 flex flex-col gap-space-lg sticky top-36">
              <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm flex flex-col gap-space-md border border-outline-variant/30">
                <div className="flex items-center gap-2 text-primary font-label-lg font-bold">
                  <span className="material-symbols-outlined text-[22px]">gavel</span>
                  <span>Quy trình Xét duyệt Cảng vụ</span>
                </div>
                <p className="font-body-sm text-on-surface-variant leading-relaxed">
                  Dịch vụ mới gửi sẽ có trạng thái <strong>PENDING_APPROVAL</strong>. Ban Quản lý Cảng vụ sẽ kiểm định tọa độ xuất bến, giới hạn thời tiết an toàn và điều khoản miễn trừ trước khi duyệt sang <strong>PUBLISHED</strong>.
                </p>

                <div className="pt-2 border-t border-outline-variant/20 flex flex-col gap-2">
                  <button 
                    onClick={handleSubmitApproval}
                    className="w-full py-3 rounded-xl bg-primary text-on-primary font-label-md font-bold shadow-md hover:bg-primary-container transition-all flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[18px]">send</span>
                    Gửi Cảng vụ xét duyệt
                  </button>
                  <button 
                    onClick={handleSaveDraft}
                    className="w-full py-2.5 rounded-xl bg-surface-container-high text-on-surface font-label-md font-semibold hover:bg-surface-container-highest transition-all"
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
    </main>
  );
}
