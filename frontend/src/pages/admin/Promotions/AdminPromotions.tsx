import React, { useState } from 'react';
import { MOCK_ADMIN_PROMOTIONS } from '../../../data/adminMockData';
import type { DiscountCode, DiscountType } from '../../../types';

export function AdminPromotions() {
  const [promotions, setPromotions] = useState<DiscountCode[]>(MOCK_ADMIN_PROMOTIONS);
  const [selectedPromo, setSelectedPromo] = useState<DiscountCode | null>(MOCK_ADMIN_PROMOTIONS[0] || null);

  // Form State
  const [formCode, setFormCode] = useState(MOCK_ADMIN_PROMOTIONS[0]?.code || '');
  const [formType, setFormType] = useState<DiscountType>(MOCK_ADMIN_PROMOTIONS[0]?.discountType || 'FIXED');
  const [formValue, setFormValue] = useState<number>(MOCK_ADMIN_PROMOTIONS[0]?.discountValue || 50000);
  const [formMaxUses, setFormMaxUses] = useState<number>(MOCK_ADMIN_PROMOTIONS[0]?.maxUses || 1000);
  const [formValidFrom, setFormValidFrom] = useState<string>(MOCK_ADMIN_PROMOTIONS[0]?.validFrom?.slice(0, 10) || '2024-10-01');
  const [formValidTo, setFormValidTo] = useState<string>(MOCK_ADMIN_PROMOTIONS[0]?.validTo?.slice(0, 10) || '2024-10-31');
  const [formIsActive, setFormIsActive] = useState<boolean>(MOCK_ADMIN_PROMOTIONS[0]?.isActive ?? true);

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleSelectPromo = (promo: DiscountCode) => {
    setSelectedPromo(promo);
    setFormCode(promo.code);
    setFormType(promo.discountType);
    setFormValue(promo.discountValue);
    setFormMaxUses(promo.maxUses);
    setFormValidFrom(promo.validFrom?.slice(0, 10) || '');
    setFormValidTo(promo.validTo?.slice(0, 10) || '');
    setFormIsActive(promo.isActive);
  };

  const handleResetForNew = () => {
    setSelectedPromo(null);
    setFormCode('');
    setFormType('FIXED');
    setFormValue(50000);
    setFormMaxUses(500);
    setFormValidFrom(new Date().toISOString().slice(0, 10));
    setFormValidTo('2024-12-31');
    setFormIsActive(true);
  };

  const handleTypeChange = (newType: DiscountType) => {
    if (newType === formType) return;
    setFormType(newType);
    if (newType === 'PERCENTAGE') {
      // If switching from cash (e.g. 50000, 5000) to percentage, adapt to a sensible % (15%)
      if (formValue > 100) {
        setFormValue(15);
      }
    } else {
      // If switching from percentage (<= 100) to cash, adapt to a sensible cash amount (50.000 VNĐ)
      if (formValue <= 100) {
        setFormValue(50000);
      }
    }
  };

  const handleSavePromo = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formCode) {
      alert("Vui lòng nhập mã ưu đãi!");
      return;
    }

    if (formType === 'PERCENTAGE' && (Number(formValue) < 1 || Number(formValue) > 100)) {
      alert("Mức giảm theo phần trăm phải nằm trong khoảng từ 1% đến 100%!");
      return;
    }

    if (formType === 'FIXED' && Number(formValue) < 1000) {
      alert("Mức giảm cố định tối thiểu là 1.000 VNĐ!");
      return;
    }

    if (selectedPromo) {
      setPromotions(prev =>
        prev.map(p =>
          p.id === selectedPromo.id
            ? {
                ...p,
                code: formCode.toUpperCase(),
                discountType: formType,
                discountValue: Number(formValue),
                maxUses: Number(formMaxUses),
                validFrom: new Date(formValidFrom).toISOString(),
                validTo: new Date(formValidTo).toISOString(),
                isActive: formIsActive,
                updatedAt: new Date().toISOString()
              }
            : p
        )
      );
      showToast(`Đã lưu cấu hình mã ưu đãi ${formCode.toUpperCase()}!`);
    } else {
      const newPromo: DiscountCode = {
        id: `promo-${Date.now()}`,
        code: formCode.toUpperCase(),
        scope: 'GLOBAL',
        vendorId: '',
        discountType: formType,
        discountValue: Number(formValue),
        maxUses: Number(formMaxUses),
        usedCount: 0,
        validFrom: new Date(formValidFrom).toISOString(),
        validTo: new Date(formValidTo).toISOString(),
        isActive: formIsActive,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      setPromotions(prev => [newPromo, ...prev]);
      setSelectedPromo(newPromo);
      showToast(`Đã tạo mới mã ưu đãi toàn sàn ${newPromo.code}!`);
    }
  };

  const handleToggleActiveQuick = () => {
    if (!selectedPromo) return;
    const nextState = !selectedPromo.isActive;
    setPromotions(prev =>
      prev.map(p => (p.id === selectedPromo.id ? { ...p, isActive: nextState } : p))
    );
    setFormIsActive(nextState);
    showToast(`Đã ${nextState ? 'kích hoạt' : 'tạm tắt'} mã ${selectedPromo.code}.`);
  };

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

        {/* Page Header */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md pb-space-lg">
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-space-sm font-bold">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm uppercase tracking-wider">
                <span className="material-symbols-outlined text-[14px]">verified_user</span>
                Scope: PLATFORM
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-secondary-container/20 text-secondary font-label-sm">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                Ngân sách Cảng vụ VITA
              </span>
            </div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight font-black">
              Quản lý Mã Khuyến mãi Nền tảng &amp; Chiến dịch Biển
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl">
              Thiết lập và kiểm soát các mã ưu đãi kích cầu du lịch biển toàn sàn (Scope: PLATFORM) do Ban Quản lý DANASEA bảo trợ ngân sách.
            </p>
          </div>

          <div className="flex items-center gap-space-sm">
            <button 
              onClick={handleResetForNew}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-primary text-on-primary font-label-lg font-bold shadow-sm hover:bg-primary-container transition-all"
            >
              <span className="material-symbols-outlined text-[20px]">add_circle</span>
              <span>Tạo mã toàn sàn mới</span>
            </button>
          </div>
        </div>

        {/* KPI Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-space-md mb-space-xl">
          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wide font-bold">Chiến dịch toàn sàn</span>
            <div className="mt-space-md flex items-baseline gap-2">
              <span className="font-display-lg font-black text-on-surface">{promotions.length}</span>
              <span className="text-xs text-outline font-semibold">mã khuyến mãi</span>
            </div>
            <span className="text-xs text-primary font-bold mt-2">100% tài trợ nền tảng</span>
          </div>

          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wide font-bold">Lượt khách quy đổi</span>
            <div className="mt-space-md flex items-baseline gap-2">
              <span className="font-display-lg font-black text-secondary">
                {promotions.reduce((sum, p) => sum + p.usedCount, 0)}
              </span>
              <span className="text-xs text-outline font-semibold">lượt dùng</span>
            </div>
            <span className="text-xs text-secondary font-bold mt-2">Áp dụng trực tiếp tại Checkout</span>
          </div>

          <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wide font-bold">Ngân sách kích cầu BQL</span>
            <div className="mt-space-md flex items-baseline gap-1">
              <span className="font-display-lg font-black text-primary">50.000.000</span>
              <span className="font-bold text-on-surface">đ</span>
            </div>
            <span className="text-xs text-outline mt-2">Hạn mức Q4/2024</span>
          </div>
        </div>

        {/* Main 2-Column Split */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* Left Column: Promotions List (7 cols) */}
          <div className="col-span-12 lg:col-span-7 flex flex-col gap-space-md">
            <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between border border-outline-variant/20">
              <span className="font-label-lg font-bold text-on-surface">Danh sách Voucher Toàn Sàn</span>
              <span className="text-xs text-outline">{promotions.length} mã</span>
            </div>

            <div className="flex flex-col gap-3">
              {promotions.map(promo => {
                const isSelected = promo.id === selectedPromo?.id;
                const progressPct = Math.min(100, Math.round((promo.usedCount / (promo.maxUses || 1)) * 100));

                return (
                  <div
                    key={promo.id}
                    onClick={() => handleSelectPromo(promo)}
                    className={`p-space-lg rounded-2xl shadow-sm transition-all cursor-pointer border ${
                      isSelected ? 'border-primary ring-2 ring-primary/20 bg-primary/5' : 'border-outline-variant/20 bg-surface-container-lowest hover:bg-surface-container-low/40'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="font-mono text-base font-black text-primary tracking-wider">{promo.code}</span>
                      <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        promo.isActive ? 'bg-primary/10 text-primary' : 'bg-surface-container text-outline'
                      }`}>
                        {promo.isActive ? 'ĐANG CHẠY' : 'TẠM TẮT'}
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-xs text-on-surface-variant mb-2">
                      <span>Giảm: <strong className="text-secondary font-bold">
                        {promo.discountType === 'FIXED' ? `${promo.discountValue.toLocaleString('vi-VN')} đ` : `${promo.discountValue}%`}
                      </strong></span>
                      <span>Hiệu lực: {promo.validFrom?.slice(0, 10)} → {promo.validTo?.slice(0, 10)}</span>
                    </div>

                    {/* Usage Progress */}
                    <div className="flex flex-col gap-1">
                      <div className="flex justify-between text-[11px] text-outline font-semibold">
                        <span>Đã dùng {promo.usedCount} / {promo.maxUses} lượt</span>
                        <span>{progressPct}%</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-surface-container overflow-hidden">
                        <div className="h-full bg-primary rounded-full" style={{ width: `${progressPct}%` }}></div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right Column: Editor Form (5 cols) */}
          <div className="col-span-12 lg:col-span-5 flex flex-col gap-space-lg sticky top-20">
            <div className="bg-surface-container-lowest rounded-2xl shadow-md p-space-xl flex flex-col gap-4 border border-outline-variant/20">
              <div className="flex items-start justify-between pb-3 border-b border-outline-variant/20">
                <div>
                  <span className="text-xs font-bold text-primary uppercase tracking-wider">Cấu hình Chiến Dịch</span>
                  <h3 className="font-headline-sm font-black text-on-surface mt-0.5">
                    {selectedPromo ? `Sửa mã: ${selectedPromo.code}` : 'Tạo mới Voucher'}
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={handleResetForNew}
                  className="px-2.5 py-1 rounded-lg bg-surface-container text-xs font-semibold text-on-surface"
                >
                  Tạo mới
                </button>
              </div>

              <form onSubmit={handleSavePromo} className="flex flex-col gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">MÃ ƯU ĐÃI (CODE) *</label>
                  <input 
                    value={formCode}
                    onChange={(e) => setFormCode(e.target.value.toUpperCase())}
                    required
                    placeholder="VD: DANASEA50"
                    className="w-full h-11 px-3.5 rounded-xl bg-surface-container-low font-mono font-black text-base text-on-surface uppercase focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">LOẠI CHIẾT KHẤU *</label>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => handleTypeChange('FIXED')}
                      className={`h-10 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
                        formType === 'FIXED' ? 'bg-primary text-on-primary shadow-xs' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[16px]">payments</span>
                      <span>Cố định (VNĐ)</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleTypeChange('PERCENTAGE')}
                      className={`h-10 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
                        formType === 'PERCENTAGE' ? 'bg-primary text-on-primary shadow-xs' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[16px]">percent</span>
                      <span>Phần trăm (%)</span>
                    </button>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-xs font-bold text-on-surface">
                      GIÁ TRỊ GIẢM ({formType === 'FIXED' ? 'VNĐ' : '%'}) *
                    </label>
                    <span className="text-[11px] text-outline font-semibold">
                      {formType === 'PERCENTAGE' ? 'Tối đa 100%' : 'Tối thiểu 1.000 đ'}
                    </span>
                  </div>

                  <div className="relative">
                    <input 
                      type="number"
                      value={formValue}
                      onChange={(e) => {
                        let val = Number(e.target.value);
                        if (formType === 'PERCENTAGE' && val > 100) {
                          val = 100;
                        }
                        setFormValue(val);
                      }}
                      required
                      min={1}
                      max={formType === 'PERCENTAGE' ? 100 : undefined}
                      step={formType === 'PERCENTAGE' ? 1 : 1000}
                      className="w-full h-11 pl-3.5 pr-14 rounded-xl bg-surface-container-low font-bold text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                    />
                    <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-black text-primary pointer-events-none">
                      {formType === 'FIXED' ? 'VNĐ' : '%'}
                    </span>
                  </div>

                  {/* Quick Preset Chips */}
                  <div className="flex items-center gap-1.5 mt-2 flex-wrap">
                    <span className="text-[10px] text-outline uppercase font-bold mr-0.5">Chọn nhanh:</span>
                    {formType === 'PERCENTAGE' ? (
                      [5, 10, 15, 20, 30, 50].map(pct => (
                        <button
                          key={pct}
                          type="button"
                          onClick={() => setFormValue(pct)}
                          className={`px-2 py-0.5 rounded-lg text-[11px] font-bold transition-colors ${
                            formValue === pct
                              ? 'bg-primary text-on-primary shadow-xs'
                              : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                          }`}
                        >
                          {pct}%
                        </button>
                      ))
                    ) : (
                      [20000, 50000, 100000, 200000, 500000].map(amt => (
                        <button
                          key={amt}
                          type="button"
                          onClick={() => setFormValue(amt)}
                          className={`px-2 py-0.5 rounded-lg text-[11px] font-bold transition-colors ${
                            formValue === amt
                              ? 'bg-primary text-on-primary shadow-xs'
                              : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                          }`}
                        >
                          {(amt / 1000).toLocaleString('vi-VN')}k
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">GIỚI HẠN SỐ LƯỢT DÙNG *</label>
                  <input 
                    type="number"
                    value={formMaxUses}
                    onChange={(e) => setFormMaxUses(Number(e.target.value))}
                    required
                    min={1}
                    className="w-full h-11 px-3.5 rounded-xl bg-surface-container-low font-bold text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs font-bold text-on-surface mb-1">TỪ NGÀY</label>
                    <input 
                      type="date"
                      value={formValidFrom}
                      onChange={(e) => setFormValidFrom(e.target.value)}
                      className="w-full h-10 px-2.5 rounded-xl bg-surface-container-low text-xs text-on-surface border border-outline-variant/30"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-on-surface mb-1">ĐẾN NGÀY</label>
                    <input 
                      type="date"
                      value={formValidTo}
                      onChange={(e) => setFormValidTo(e.target.value)}
                      className="w-full h-10 px-2.5 rounded-xl bg-surface-container-low text-xs text-on-surface border border-outline-variant/30"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between p-3 rounded-xl bg-surface-container-low">
                  <span className="text-xs font-bold text-on-surface">Kích hoạt chiến dịch (is_active)</span>
                  <input 
                    type="checkbox"
                    checked={formIsActive}
                    onChange={(e) => setFormIsActive(e.target.checked)}
                    className="w-4 h-4 accent-primary cursor-pointer"
                  />
                </div>

                <div className="flex flex-col gap-2 pt-2">
                  <button 
                    type="submit"
                    className="w-full h-11 rounded-xl bg-primary text-on-primary font-bold text-sm hover:bg-primary-container transition-all flex items-center justify-center gap-1.5 shadow-sm"
                  >
                    <span className="material-symbols-outlined text-[18px]">save</span>
                    <span>{selectedPromo ? 'Lưu Cấu Hình' : 'Tạo Chiến Dịch'}</span>
                  </button>

                  {selectedPromo && (
                    <button 
                      type="button"
                      onClick={handleToggleActiveQuick}
                      className="w-full h-10 rounded-xl bg-surface-container hover:bg-surface-container-high text-xs font-bold text-on-surface transition-colors"
                    >
                      {selectedPromo.isActive ? 'Tạm tắt mã' : 'Bật lại mã'}
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}

export default AdminPromotions;
