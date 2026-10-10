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

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 mb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">
              Quản lý Khuyến mãi Toàn sàn
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-600 text-xs font-medium">
              Tổng: <strong>{promotions.length}</strong> mã
            </span>
            <button 
              onClick={handleResetForNew}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-primary text-white text-sm font-semibold hover:bg-primary-container transition-colors shadow-xs"
            >
              <span className="material-symbols-outlined text-[18px]">add</span>
              <span>Tạo Mã Toàn Sàn</span>
            </button>
          </div>
        </div>

        {/* Main 2-Column Split */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* Left Column: Promotions Ledger Table (7 cols) */}
          <div className="col-span-12 lg:col-span-7 bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
            <div className="px-4 py-3 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">Sổ Cái Voucher Toàn Sàn</span>
              <span className="text-xs text-slate-500 font-medium">{promotions.length} mã khả dụng</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-slate-700 font-semibold text-[11px] uppercase">
                    <th className="py-2.5 px-3 border-r border-slate-200">Mã voucher</th>
                    <th className="py-2.5 px-3 border-r border-slate-200">Mức giảm</th>
                    <th className="py-2.5 px-3 border-r border-slate-200">Tiến độ sử dụng</th>
                    <th className="py-2.5 px-3 border-r border-slate-200">Thời hạn</th>
                    <th className="py-2.5 px-3 text-center">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {promotions.map(promo => {
                    const isSelected = promo.id === selectedPromo?.id;
                    const progressPct = Math.min(100, Math.round((promo.usedCount / (promo.maxUses || 1)) * 100));

                    return (
                      <tr
                        key={promo.id}
                        onClick={() => handleSelectPromo(promo)}
                        className={`cursor-pointer transition-colors ${
                          isSelected
                            ? 'bg-primary/5 border-l-4 border-l-primary font-medium text-slate-900'
                            : 'hover:bg-slate-50 text-slate-600'
                        }`}
                      >
                        <td className="py-3 px-3 border-r border-slate-200">
                          <span className="font-mono text-xs font-bold text-primary px-1.5 py-0.5 rounded bg-primary/10 border border-primary/20">
                            {promo.code}
                          </span>
                        </td>
                        <td className="py-3 px-3 font-semibold text-slate-800 border-r border-slate-200">
                          {promo.discountType === 'FIXED'
                            ? `${promo.discountValue.toLocaleString('vi-VN')} đ`
                            : `${promo.discountValue}%`}
                        </td>
                        <td className="py-3 px-3 min-w-[120px] border-r border-slate-200">
                          <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1">
                            <span>{promo.usedCount}/{promo.maxUses}</span>
                            <span className="font-semibold text-slate-700">{progressPct}%</span>
                          </div>
                          <div className="w-full h-1.5 rounded-full bg-slate-100 overflow-hidden border border-slate-200">
                            <div className="h-full bg-primary rounded-full" style={{ width: `${progressPct}%` }}></div>
                          </div>
                        </td>
                        <td className="py-3 px-3 text-slate-500 text-[11px] whitespace-nowrap border-r border-slate-200">
                          {promo.validFrom?.slice(0, 10)} → {promo.validTo?.slice(0, 10)}
                        </td>
                        <td className="py-3 px-3 text-center whitespace-nowrap">
                          <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                            promo.isActive ? 'bg-primary/10 text-primary border border-primary/20' : 'bg-slate-100 text-slate-500 border border-slate-200'
                          }`}>
                            {promo.isActive ? 'ĐANG CHẠY' : 'TẠM TẮT'}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Right Column: Editor Form (5 cols) */}
          <div className="col-span-12 lg:col-span-5 flex flex-col gap-space-lg sticky top-20">
            <div className="bg-white rounded-xl shadow-xs p-space-xl flex flex-col gap-4 border border-slate-200">
              <div className="flex items-start justify-between pb-3 border-b border-slate-200">
                <div>
                  <span className="font-label-sm text-xs font-medium text-primary">Cấu hình chiến dịch</span>
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
