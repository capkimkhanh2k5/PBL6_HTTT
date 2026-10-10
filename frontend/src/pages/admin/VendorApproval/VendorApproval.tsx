import React, { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_VENDORS } from '../../../data/adminMockData';
import type { BadgeTier, VerificationStatus } from '../../../types';

type VendorExtended = typeof MOCK_ADMIN_VENDORS[0];

export function VendorApproval() {
  const [vendors, setVendors] = useState<VendorExtended[]>(MOCK_ADMIN_VENDORS);
  const [selectedVendorId, setSelectedVendorId] = useState<string>(MOCK_ADMIN_VENDORS[0]?.id || '');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [badgeFilter, setBadgeFilter] = useState<string>('all');

  // Modals
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [selectedTier, setSelectedTier] = useState<BadgeTier>('PLATINUM');
  const [docPreviewName, setDocPreviewName] = useState<string | null>(null);

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Filtered vendors
  const filteredVendors = useMemo(() => {
    return vendors.filter(v => {
      const matchSearch =
        v.businessName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.taxCode.includes(searchQuery);

      const matchStatus =
        statusFilter === 'all' || v.verificationStatus === statusFilter;

      const matchBadge =
        badgeFilter === 'all' || v.badgeTier === badgeFilter;

      return matchSearch && matchStatus && matchBadge;
    });
  }, [vendors, searchQuery, statusFilter, badgeFilter]);

  const selectedVendor = vendors.find(v => v.id === selectedVendorId) || vendors[0];

  const handleApprove = () => {
    if (!selectedVendor) return;
    setVendors(prev =>
      prev.map(v =>
        v.id === selectedVendor.id
          ? {
              ...v,
              verificationStatus: 'VERIFIED' as VerificationStatus,
              badgeTier: selectedTier,
              verifiedBy: 'Vũ Hải Đăng',
              verifiedAt: new Date().toISOString()
            }
          : v
      )
    );
    setIsApproveModalOpen(false);
    showToast(`Đã phê duyệt đối tác ${selectedVendor.businessName} với hạng ${selectedTier}!`);
  };

  const handleReject = () => {
    if (!selectedVendor) return;
    setVendors(prev =>
      prev.map(v =>
        v.id === selectedVendor.id
          ? {
              ...v,
              verificationStatus: 'REJECTED' as VerificationStatus,
              verifiedBy: 'Vũ Hải Đăng',
              verifiedAt: new Date().toISOString()
            }
          : v
      )
    );
    setIsRejectModalOpen(false);
    showToast(`Đã từ chối duyệt hồ sơ ${selectedVendor.businessName}.`);
  };

  // Metrics
  const totalCount = vendors.length;
  const verifiedCount = vendors.filter(v => v.verificationStatus === 'VERIFIED').length;
  const pendingCount = vendors.filter(v => v.verificationStatus === 'PENDING').length;
  const rejectedCount = vendors.filter(v => v.verificationStatus === 'REJECTED').length;

  return (
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full gap-space-xl animate-fade-in-up">
        {/* Toast Notification */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">
              Phê duyệt Đối tác
            </h1>
          </div>
          <div className="flex items-center gap-2 text-xs font-medium">
            <span className="px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-600">
              Tổng: <strong>{totalCount}</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary border border-primary/20">
              Hoạt động: <strong>{verifiedCount}</strong>
            </span>
            <span className="px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
              Chờ duyệt: <strong>{pendingCount}</strong>
            </span>
            {rejectedCount > 0 && (
              <span className="px-2.5 py-1 rounded-md bg-red-50 text-red-700 border border-red-200">
                Từ chối: <strong>{rejectedCount}</strong>
              </span>
            )}
          </div>
        </div>

        {/* Main Workspace: Asymmetric 65% List / 35% Drawer */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* LEFT PANEL: Table (8 cols) */}
          <div className="lg:col-span-8 flex flex-col gap-space-md min-w-0">
            {/* Filter Bar */}
            <div className="p-space-md rounded-xl bg-white shadow-xs flex flex-col md:flex-row items-center justify-between gap-space-sm border border-slate-200">
              <div className="relative w-full md:w-80">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">search</span>
                <input 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-10 pl-9 pr-space-md rounded-lg bg-slate-50 text-slate-800 font-body-md text-body-md placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-1 focus:ring-primary transition-colors border border-slate-200" 
                  placeholder="Tìm theo tên đơn vị, MST..." 
                  type="text"
                />
              </div>

              {/* Status & Badge Tabs */}
              <div className="flex items-center gap-space-xs w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
                <div className="flex p-1 bg-slate-100 rounded-xl border border-slate-200">
                  <button 
                    onClick={() => setStatusFilter('all')}
                    className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'all' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                  >
                    Tất cả
                  </button>
                  <button 
                    onClick={() => setStatusFilter('PENDING')}
                    className={`px-3 py-1.5 rounded-lg text-xs flex items-center gap-1.5 transition-all ${statusFilter === 'PENDING' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                  >
                    <span>Chờ duyệt</span>
                    {pendingCount > 0 && (
                      <span className={`px-1.5 py-0.2 rounded-full font-bold text-[10px] ${statusFilter === 'PENDING' ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'}`}>{pendingCount}</span>
                    )}
                  </button>
                  <button 
                    onClick={() => setStatusFilter('VERIFIED')}
                    className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'VERIFIED' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                  >
                    Đã duyệt
                  </button>
                  <button 
                    onClick={() => setStatusFilter('REJECTED')}
                    className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'REJECTED' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                  >
                    Từ chối
                  </button>
                </div>

                {/* Badge Dropdown */}
                <select 
                  value={badgeFilter}
                  onChange={(e) => setBadgeFilter(e.target.value)}
                  className="h-9 px-3 rounded-lg bg-slate-50 text-slate-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary cursor-pointer border border-slate-200"
                >
                  <option value="all">Huy hiệu: Tất cả</option>
                  <option value="BRONZE">Đồng (BRONZE)</option>
                  <option value="SILVER">Bạc (SILVER)</option>
                  <option value="GOLD">Vàng (GOLD)</option>
                  <option value="PLATINUM">Bạch kim (PLATINUM)</option>
                </select>
              </div>
            </div>

            {/* Partner Table Card with custom visible horizontal scrollbar */}
            <div className="rounded-xl bg-white shadow-xs overflow-hidden flex flex-col border border-slate-200">
              <div className="overflow-x-auto custom-scrollbar">
                <table className="w-full text-left border-collapse min-w-[620px]">
                  <thead>
                    <tr className="bg-slate-50 text-slate-700 text-xs font-semibold border-b border-slate-200">
                      <th className="py-3 px-4 whitespace-nowrap border-r border-slate-200">Đơn vị &amp; Đại diện</th>
                      <th className="py-3 px-4 whitespace-nowrap border-r border-slate-200">Mã số thuế</th>
                      <th className="py-3 px-4 whitespace-nowrap border-r border-slate-200">Bến bãi &amp; Đánh giá</th>
                      <th className="py-3 px-4 whitespace-nowrap border-r border-slate-200">Trạng thái</th>
                      <th className="py-3 px-4 whitespace-nowrap border-r border-slate-200">Huy hiệu</th>
                      <th className="py-3 px-4 text-center whitespace-nowrap">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 text-xs text-slate-700">
                    {filteredVendors.map(vendor => {
                      const isSelected = vendor.id === selectedVendor?.id;
                      return (
                        <tr 
                          key={vendor.id}
                          onClick={() => setSelectedVendorId(vendor.id)}
                          className={`cursor-pointer transition-colors ${
                            isSelected ? 'bg-primary/5 hover:bg-primary/10' : 'hover:bg-slate-50'
                          }`}
                        >
                          <td className="py-3.5 px-4 border-r border-slate-200">
                            <div className="flex items-center gap-space-sm">
                              <img 
                                src={vendor.avatarUrl} 
                                alt={vendor.businessName}
                                className="w-10 h-10 rounded-xl object-cover shadow-xs border border-slate-200 shrink-0"
                              />
                              <div className="flex flex-col min-w-0 max-w-[190px]">
                                <span className="font-bold text-slate-800 truncate" title={vendor.businessName}>
                                  {vendor.businessName}
                                </span>
                                <span className="text-[11px] text-slate-500 truncate" title={vendor.bankAccountHolder || vendor.email}>
                                  Đại diện: {vendor.bankAccountHolder || vendor.email || 'Chưa cập nhật'}
                                </span>
                              </div>
                            </div>
                          </td>

                          <td className="py-3.5 px-4 whitespace-nowrap border-r border-slate-200">
                            <span className="font-mono text-slate-800 font-semibold block">{vendor.taxCode}</span>
                            <div className="text-[11px] text-slate-500 font-normal truncate max-w-[130px]" title={vendor.bankName}>{vendor.bankName}</div>
                          </td>

                          <td className="py-3.5 px-4 border-r border-slate-200">
                            <div className="flex flex-col min-w-0 max-w-[170px]">
                              <span className="text-xs text-slate-800 font-semibold truncate" title={vendor.address}>
                                {vendor.address}
                              </span>
                              <div className="flex items-center gap-2 mt-0.5">
                                <span className="flex items-center gap-0.5 text-[11px] text-amber-600 font-bold">
                                  <span className="material-symbols-outlined text-[13px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                                  {vendor.ratingAvg}
                                </span>
                                <span className="text-[11px] text-slate-500">({vendor.ratingCount} đ/g)</span>
                              </div>
                            </div>
                          </td>

                          <td className="py-3.5 px-4 whitespace-nowrap border-r border-slate-200">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold ${
                              vendor.verificationStatus === 'VERIFIED'
                                ? 'bg-primary/10 text-primary border border-primary/20'
                                : vendor.verificationStatus === 'PENDING'
                                ? 'bg-amber-50 text-amber-700 border border-amber-200'
                                : 'bg-red-50 text-red-700 border border-red-200'
                            }`}>
                              <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                              {vendor.verificationStatus === 'VERIFIED' ? 'Đã duyệt' : vendor.verificationStatus === 'PENDING' ? 'Chờ duyệt' : 'Từ chối'}
                            </span>
                          </td>

                          <td className="py-3.5 px-4 whitespace-nowrap border-r border-slate-200">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-slate-100 text-slate-700 text-[11px] font-bold border border-slate-200">
                              <span className="material-symbols-outlined text-[13px] text-amber-600">military_tech</span>
                              {vendor.badgeTier}
                            </span>
                          </td>

                          <td className="py-3.5 px-4 text-center whitespace-nowrap">
                            <div className="flex items-center justify-center">
                              <button 
                                onClick={(e) => { e.stopPropagation(); setSelectedVendorId(vendor.id); }}
                                className={`w-8 h-8 rounded-lg inline-flex items-center justify-center transition-all ${
                                  isSelected ? 'bg-primary text-white shadow-xs' : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200'
                                }`}
                                title="Xem chi tiết"
                              >
                                <span className="material-symbols-outlined text-[18px]">visibility</span>
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* RIGHT PANEL: Partner Verification Drawer (4 cols) */}
          {selectedVendor && (
            <div className="lg:col-span-4 flex flex-col gap-space-md sticky top-20">
              <div className="p-space-lg rounded-xl bg-white shadow-xs flex flex-col gap-space-lg border border-slate-200">
                {/* Drawer Header */}
                <div className="flex flex-col gap-space-xs">
                  <div className="flex items-center justify-between">
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-slate-100 border border-slate-200 text-slate-700 font-mono text-[12px] font-bold">
                      MÃ ĐỐI TÁC: #{selectedVendor.id}
                    </span>
                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full font-label-sm text-xs font-bold ${
                      selectedVendor.verificationStatus === 'VERIFIED'
                        ? 'bg-primary/10 text-primary border border-primary/20'
                        : selectedVendor.verificationStatus === 'PENDING'
                        ? 'bg-amber-50 text-amber-700 border border-amber-200'
                        : 'bg-red-50 text-red-700 border border-red-200'
                    }`}>
                      {selectedVendor.verificationStatus === 'VERIFIED' ? 'ĐÃ PHÊ DUYỆT' : selectedVendor.verificationStatus === 'PENDING' ? 'CHỜ THẨM ĐỊNH' : 'TỪ CHỐI'}
                    </span>
                  </div>

                  <div className="mt-2">
                    <h2 className="text-lg text-slate-800 font-bold">{selectedVendor.businessName}</h2>
                    <div className="flex items-center gap-1.5 text-slate-500 font-body-sm text-xs mt-0.5">
                      <span className="material-symbols-outlined text-[16px] text-primary">location_on</span>
                      <span>{selectedVendor.address}</span>
                    </div>
                  </div>

                  <div className="p-space-sm rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-600 mt-1">
                    Người duyệt gần nhất: <strong className="text-slate-800">{selectedVendor.verifiedBy || 'Đang chờ thẩm định'}</strong>
                  </div>
                </div>

                {/* Section: Legal & Financial Verification */}
                <div className="flex flex-col gap-space-sm">
                  <span className="text-xs text-slate-800 font-semibold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px] text-primary">account_balance</span>
                    Thông tin pháp lý &amp; Tài khoản thụ hưởng
                  </span>
                  <div className="p-space-md rounded-xl bg-slate-50 border border-slate-200 flex flex-col gap-2">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-slate-500">Mã số thuế doanh nghiệp:</span>
                      <span className="font-mono font-bold text-slate-800">{selectedVendor.taxCode}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-slate-500">Ngân hàng thanh toán:</span>
                      <span className="font-semibold text-slate-800">{selectedVendor.bankName}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-slate-500">Số tài khoản thụ hưởng:</span>
                      <span className="font-mono font-bold text-primary text-sm">{selectedVendor.bankAccountNumber}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs pt-1 border-t border-slate-200">
                      <span className="text-slate-500">Chủ tài khoản:</span>
                      <span className="font-bold text-slate-800 text-right uppercase">{selectedVendor.bankAccountHolder}</span>
                    </div>
                  </div>
                </div>

                {/* Section: Attached Documents */}
                <div className="flex flex-col gap-space-sm">
                  <span className="text-xs text-slate-800 font-semibold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px] text-primary">folder_open</span>
                    Tài liệu hồ sơ đính kèm (3 tệp)
                  </span>
                  <div className="flex flex-col gap-2">
                    <div 
                      onClick={() => setDocPreviewName('giay_phep_kinh_doanh_2024.pdf')}
                      className="p-space-sm rounded-xl bg-slate-50 border border-slate-200 hover:bg-slate-100 transition-colors flex items-center justify-between gap-2 cursor-pointer"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-primary flex items-center justify-center shrink-0 shadow-xs">
                          <span className="material-symbols-outlined text-[20px]">picture_as_pdf</span>
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="text-xs text-slate-800 truncate font-semibold">giay_phep_kinh_doanh_2024.pdf</span>
                          <span className="text-[11px] text-slate-500">2.8 MB • Mã: #GP-DANANG-2024</span>
                        </div>
                      </div>
                      <span className="material-symbols-outlined text-slate-400 text-[18px]">open_in_new</span>
                    </div>

                    <div 
                      onClick={() => setDocPreviewName('chung_chi_cuu_ho_padi_rescue.pdf')}
                      className="p-space-sm rounded-xl bg-slate-50 border border-slate-200 hover:bg-slate-100 transition-colors flex items-center justify-between gap-2 cursor-pointer"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-white border border-slate-200 text-primary flex items-center justify-center shrink-0 shadow-xs">
                          <span className="material-symbols-outlined text-[20px]">verified</span>
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="text-xs text-slate-800 truncate font-semibold">chung_chi_cuu_ho_padi_rescue.pdf</span>
                          <span className="text-[11px] text-slate-500">4.1 MB • PADI Rescue Standard</span>
                        </div>
                      </div>
                      <span className="material-symbols-outlined text-slate-400 text-[18px]">open_in_new</span>
                    </div>
                  </div>
                </div>

                {/* Badge Adjustment */}
                <div className="flex flex-col gap-space-xs">
                  <span className="text-xs text-slate-800 font-semibold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px] text-primary">military_tech</span>
                    Cấp huy hiệu sàn vận hành
                  </span>
                  <div className="grid grid-cols-4 gap-1.5 mt-1">
                    {(['BRONZE', 'SILVER', 'GOLD', 'PLATINUM'] as BadgeTier[]).map(tier => (
                      <button
                        key={tier}
                        type="button"
                        onClick={() => setSelectedTier(tier)}
                        className={`p-2 rounded-xl text-center text-xs font-bold transition-all ${
                          selectedTier === tier
                            ? 'bg-primary text-white shadow-xs'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200 border border-slate-200'
                        }`}
                      >
                        {tier}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2 pt-2">
                  <div className="flex items-center gap-2">
                    <button 
                      onClick={() => setIsApproveModalOpen(true)}
                      className="flex-1 py-2.5 px-space-md rounded-xl bg-primary text-white font-semibold hover:bg-primary-container transition-colors shadow-xs flex items-center justify-center gap-2 text-xs"
                    >
                      <span className="material-symbols-outlined text-[18px]">verified</span>
                      <span>Cập nhật phê duyệt</span>
                    </button>
                    <button 
                      onClick={() => setIsRejectModalOpen(true)}
                      className="py-2.5 px-space-md rounded-xl bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 transition-colors flex items-center justify-center gap-1.5 font-semibold text-xs"
                    >
                      <span className="material-symbols-outlined text-[18px]">block</span>
                      <span>Từ chối</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* APPROVE MODAL (PORTAL TO BODY TO ENSURE PERFECT CENTER ALIGNMENT ACROSS SCROLL STATES) */}
      {isApproveModalOpen && selectedVendor && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl p-space-xl max-w-md w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="w-12 h-12 rounded-full bg-primary/10 text-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[28px]">fact_check</span>
            </div>
            <div>
              <h3 className="font-headline-md text-headline-md text-on-surface font-bold">Xác nhận phê duyệt đối tác</h3>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1">
                Bạn đang phê duyệt và cấp huy hiệu <strong className="text-primary">{selectedTier}</strong> cho đối tác <strong>{selectedVendor.businessName}</strong>.
              </p>
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button 
                onClick={() => setIsApproveModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md font-semibold transition-colors"
              >
                Hủy bỏ
              </button>
              <button 
                onClick={handleApprove}
                className="px-5 py-2.5 rounded-xl bg-primary text-on-primary font-label-md font-bold hover:bg-primary-container transition-colors shadow-sm"
              >
                Xác nhận phê duyệt
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* REJECT MODAL */}
      {isRejectModalOpen && selectedVendor && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl p-space-xl max-w-md w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="w-12 h-12 rounded-full bg-secondary-container/20 text-secondary flex items-center justify-center">
              <span className="material-symbols-outlined text-[28px]">warning</span>
            </div>
            <div>
              <h3 className="font-headline-md text-headline-md text-on-surface font-bold">Từ chối thẩm định hồ sơ</h3>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1">
                Nhập lý do chưa đạt chuẩn để gửi thông báo phản hồi cho đối tác:
              </p>
            </div>
            <textarea 
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full h-24 p-3 rounded-xl bg-surface text-on-surface font-body-sm text-body-sm focus:outline-none focus:ring-2 focus:ring-secondary border border-outline-variant/30 resize-none"
              placeholder="VD: Chứng chỉ cứu hộ mặt nước PADI Rescue đã hết hạn, hoặc ca nô chưa hoàn thành kiểm định đường thủy định kỳ..."
            />
            <div className="flex items-center justify-end gap-2 pt-1">
              <button 
                onClick={() => setIsRejectModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md font-semibold transition-colors"
              >
                Hủy
              </button>
              <button 
                onClick={handleReject}
                className="px-5 py-2.5 rounded-xl bg-secondary text-on-secondary font-label-md font-bold hover:bg-secondary-container transition-colors shadow-sm"
              >
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* PDF PREVIEW MODAL */}
      {docPreviewName && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">description</span>
                <span className="font-bold text-on-surface text-base">{docPreviewName}</span>
              </div>
              <button 
                onClick={() => setDocPreviewName(null)}
                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-outline hover:text-on-surface transition-colors"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>
            <div className="h-72 rounded-xl bg-surface-container-low flex flex-col items-center justify-center p-6 text-center border border-dashed border-outline-variant">
              <span className="material-symbols-outlined text-primary text-[48px] mb-2">verified</span>
              <span className="font-bold text-on-surface text-sm">Văn bản chứng thực số điện tử Cục Hàng Hải</span>
              <p className="text-xs text-outline max-w-md mt-1">
                Tệp tin PDF đã được đối soát chữ ký số PKI hợp lệ.
              </p>
            </div>
            <div className="flex justify-end">
              <button 
                onClick={() => setDocPreviewName(null)}
                className="px-4 py-2 rounded-xl bg-primary text-on-primary font-bold text-sm hover:bg-primary-container transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}

export default VendorApproval;
