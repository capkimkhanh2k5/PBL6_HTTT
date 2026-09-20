import React, { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_VENDORS } from '../../../data/adminMockData';
import type { Vendor, BadgeTier, VerificationStatus } from '../../../types';

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

        {/* Page Header & Maritime Security Summary */}
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-space-lg">
          <div className="flex flex-col gap-space-xs max-w-2xl">
            <div className="flex items-center gap-space-xs text-primary font-label-md">
              <span className="material-symbols-outlined text-[18px]">verified_user</span>
              <span>HỆ THỐNG AN TOÀN DU LỊCH BIỂN ĐÀ NẴNG</span>
              <span className="text-outline-variant">•</span>
              <span className="text-on-surface-variant font-normal">CẢNG VỤ HÀNG HẢI ĐỒNG BỘ 24/7</span>
            </div>
            <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black">
              Thẩm định &amp; Quản lý Đối tác Vận hành Hàng hải
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Duyệt hồ sơ doanh nghiệp, chứng chỉ an toàn cứu hộ mặt nước và cấp huy hiệu vận hành cho các câu lạc bộ thể thao biển tại Đà Nẵng.
            </p>
          </div>

          <div className="flex items-center gap-space-sm self-start lg:self-auto">
            <button 
              onClick={() => showToast("Đang đồng bộ dữ liệu với Cổng thông tin Cục Hàng hải Việt Nam (VITA)...")}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-lg text-label-lg transition-colors shadow-sm font-semibold"
            >
              <span className="material-symbols-outlined text-[20px]">cloud_sync</span>
              <span>Đồng bộ CSDL VITA</span>
            </button>
            <button 
              onClick={() => alert("Chức năng ghi nhận biên bản kiểm tra đột xuất tại bến đã sẵn sàng.")}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-primary text-on-primary font-label-lg text-label-lg hover:bg-primary-container transition-colors shadow-sm font-bold"
            >
              <span className="material-symbols-outlined text-[20px]">add_circle</span>
              <span>Thêm hồ sơ kiểm tra bến</span>
            </button>
          </div>
        </div>

        {/* KPI Metric Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-space-md">
          {/* Metric 1 */}
          <div className="relative overflow-hidden p-space-lg rounded-xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm uppercase tracking-wider text-outline font-bold">Tổng đối tác biển</span>
              <span className="w-8 h-8 rounded-lg bg-primary-container/15 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[20px]">sailing</span>
              </span>
            </div>
            <div className="mt-space-md">
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-display-lg text-on-surface tracking-tight font-black">{totalCount}</span>
                <span className="font-label-md text-label-md text-on-surface-variant font-semibold">đơn vị</span>
              </div>
              <div className="mt-2 flex items-center gap-2 font-label-sm text-label-sm text-on-surface-variant">
                <span className="inline-flex items-center gap-1 text-primary font-bold"><span className="w-1.5 h-1.5 rounded-full bg-primary"></span> {verifiedCount} hoạt động</span>
                <span>•</span>
                <span className="inline-flex items-center gap-1 text-secondary font-bold"><span className="w-1.5 h-1.5 rounded-full bg-secondary-container"></span> {pendingCount} chờ duyệt</span>
                <span>•</span>
                <span className="text-outline">{rejectedCount} từ chối</span>
              </div>
            </div>
          </div>

          {/* Metric 2 */}
          <div className="relative overflow-hidden p-space-lg rounded-xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm uppercase tracking-wider text-outline font-bold">Hồ sơ an toàn cứu hộ</span>
              <span className="w-8 h-8 rounded-lg bg-primary-container/15 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[20px]">verified</span>
              </span>
            </div>
            <div className="mt-space-md">
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-display-lg text-primary tracking-tight font-black">100%</span>
                <span className="font-label-md text-label-md text-primary font-bold">đạt chuẩn</span>
              </div>
              <p className="mt-2 font-body-sm text-body-sm text-on-surface-variant truncate">
                100% kiểm định PADI / Đăng kiểm đường thủy
              </p>
            </div>
            <div className="mt-2 w-full bg-surface-container h-1.5 rounded-full overflow-hidden">
              <div className="bg-primary h-full rounded-full w-full"></div>
            </div>
          </div>

          {/* Metric 3 */}
          <div className="relative overflow-hidden p-space-lg rounded-xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm uppercase tracking-wider text-outline font-bold">Đánh giá vận hành</span>
              <span className="w-8 h-8 rounded-lg bg-secondary-container/15 text-secondary flex items-center justify-center">
                <span className="material-symbols-outlined text-[20px]">grade</span>
              </span>
            </div>
            <div className="mt-space-md">
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-display-lg text-on-surface tracking-tight font-black">4.86</span>
                <span className="font-label-md text-label-md text-secondary font-bold">/ 5.0</span>
              </div>
              <p className="mt-2 font-body-sm text-body-sm text-on-surface-variant">
                Tổng hợp từ 1.250 lượt phản hồi của du khách
              </p>
            </div>
            <div className="mt-1 flex gap-1 text-secondary text-[14px]">
              {[1, 2, 3, 4, 5].map(i => (
                <span key={i} className="material-symbols-outlined text-[16px]">star</span>
              ))}
            </div>
          </div>

          {/* Metric 4 */}
          <div className="relative overflow-hidden p-space-lg rounded-xl bg-surface-container-lowest shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm uppercase tracking-wider text-outline font-bold">Bến bãi đang mở</span>
              <span className="w-8 h-8 rounded-lg bg-tertiary/15 text-tertiary flex items-center justify-center">
                <span className="material-symbols-outlined text-[20px]">anchor</span>
              </span>
            </div>
            <div className="mt-space-md">
              <div className="flex items-baseline gap-2">
                <span className="font-display-lg text-display-lg text-on-surface tracking-tight font-black">04</span>
                <span className="font-label-md text-label-md text-on-surface-variant font-bold">bến đỗ chính</span>
              </div>
              <div className="mt-2 flex flex-wrap gap-1.5">
                <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-[10px] font-semibold">Sao Biển</span>
                <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-[10px] font-semibold">Bãi Rạng</span>
                <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-[10px] font-semibold">Sông Hàn</span>
                <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-[10px] font-semibold">Bãi Bụt</span>
              </div>
            </div>
          </div>
        </div>

        {/* Main Workspace: Asymmetric 65% List / 35% Drawer */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* LEFT PANEL: Table (8 cols) */}
          <div className="lg:col-span-8 flex flex-col gap-space-md min-w-0">
            {/* Filter Bar */}
            <div className="p-space-md rounded-xl bg-surface-container-lowest shadow-sm flex flex-col md:flex-row items-center justify-between gap-space-sm border border-outline-variant/20">
              <div className="relative w-full md:w-80">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
                <input 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-10 pl-9 pr-space-md rounded-lg bg-surface-container-low text-on-surface font-body-md text-body-md placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest transition-colors border border-outline-variant/30" 
                  placeholder="Tìm theo tên đơn vị, MST..." 
                  type="text"
                />
              </div>

              {/* Status & Badge Tabs */}
              <div className="flex items-center gap-space-xs w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
                <div className="flex p-1 bg-surface-container-low rounded-lg">
                  <button 
                    onClick={() => setStatusFilter('all')}
                    className={`px-3 py-1.5 rounded font-label-sm text-label-sm transition-all ${statusFilter === 'all' ? 'bg-surface-container-lowest text-on-surface font-bold shadow-xs' : 'text-on-surface-variant hover:text-on-surface'}`}
                  >
                    Tất cả
                  </button>
                  <button 
                    onClick={() => setStatusFilter('PENDING')}
                    className={`px-3 py-1.5 rounded font-label-sm text-label-sm flex items-center gap-1.5 transition-all ${statusFilter === 'PENDING' ? 'bg-surface-container-lowest text-on-surface font-bold shadow-xs' : 'text-on-surface-variant hover:text-on-surface'}`}
                  >
                    <span>Chờ duyệt</span>
                    {pendingCount > 0 && (
                      <span className="px-1.5 py-0.2 rounded-full bg-secondary text-on-secondary font-bold text-[10px]">{pendingCount}</span>
                    )}
                  </button>
                  <button 
                    onClick={() => setStatusFilter('VERIFIED')}
                    className={`px-3 py-1.5 rounded font-label-sm text-label-sm transition-all ${statusFilter === 'VERIFIED' ? 'bg-surface-container-lowest text-on-surface font-bold shadow-xs' : 'text-on-surface-variant hover:text-on-surface'}`}
                  >
                    Đã duyệt
                  </button>
                  <button 
                    onClick={() => setStatusFilter('REJECTED')}
                    className={`px-3 py-1.5 rounded font-label-sm text-label-sm transition-all ${statusFilter === 'REJECTED' ? 'bg-surface-container-lowest text-on-surface font-bold shadow-xs' : 'text-on-surface-variant hover:text-on-surface'}`}
                  >
                    Từ chối
                  </button>
                </div>

                {/* Badge Dropdown */}
                <select 
                  value={badgeFilter}
                  onChange={(e) => setBadgeFilter(e.target.value)}
                  className="h-9 px-3 rounded-lg bg-surface-container-low text-on-surface font-label-sm text-label-sm focus:outline-none cursor-pointer border border-outline-variant/30"
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
            <div className="rounded-xl bg-surface-container-lowest shadow-sm overflow-hidden flex flex-col border border-outline-variant/20">
              <div className="overflow-x-auto custom-scrollbar">
                <table className="w-full text-left border-collapse min-w-[620px]">
                  <thead>
                    <tr className="bg-surface-container-low/70 text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider">
                      <th className="py-3.5 px-space-md whitespace-nowrap">Đơn vị &amp; Đại diện</th>
                      <th className="py-3.5 px-space-md whitespace-nowrap">Mã số thuế</th>
                      <th className="py-3.5 px-space-md whitespace-nowrap">Bến bãi &amp; Đánh giá</th>
                      <th className="py-3.5 px-space-md whitespace-nowrap">Trạng thái</th>
                      <th className="py-3.5 px-space-md whitespace-nowrap">Huy hiệu</th>
                      <th className="py-3.5 px-space-md text-right whitespace-nowrap">Chi tiết</th>
                    </tr>
                  </thead>
                  <tbody className="font-body-md text-body-md divide-y divide-outline-variant/10">
                    {filteredVendors.map(vendor => {
                      const isSelected = vendor.id === selectedVendor?.id;
                      return (
                        <tr 
                          key={vendor.id}
                          onClick={() => setSelectedVendorId(vendor.id)}
                          className={`cursor-pointer transition-colors ${
                            isSelected ? 'bg-primary/5 hover:bg-primary/10' : 'hover:bg-surface-container-low/40'
                          }`}
                        >
                          <td className="py-4 px-space-md">
                            <div className="flex items-center gap-space-sm">
                              <img 
                                src={vendor.avatarUrl} 
                                alt={vendor.businessName}
                                className="w-10 h-10 rounded-xl object-cover shadow-sm border border-outline-variant shrink-0"
                              />
                              <div className="flex flex-col min-w-0 max-w-[190px]">
                                <span className="font-label-lg font-bold text-on-surface truncate" title={vendor.businessName}>
                                  {vendor.businessName}
                                </span>
                                <span className="font-body-sm text-xs text-outline truncate" title={vendor.bankAccountHolder || vendor.email}>
                                  Đại diện: {vendor.bankAccountHolder || vendor.email || 'Chưa cập nhật'}
                                </span>
                              </div>
                            </div>
                          </td>

                          <td className="py-4 px-space-md whitespace-nowrap">
                            <span className="font-mono text-body-sm text-on-surface font-semibold block">{vendor.taxCode}</span>
                            <div className="text-[11px] text-outline font-normal truncate max-w-[130px]" title={vendor.bankName}>{vendor.bankName}</div>
                          </td>

                          <td className="py-4 px-space-md">
                            <div className="flex flex-col min-w-0 max-w-[170px]">
                              <span className="text-xs text-on-surface font-semibold truncate" title={vendor.address}>
                                {vendor.address}
                              </span>
                              <div className="flex items-center gap-2 mt-0.5">
                                <span className="flex items-center gap-0.5 text-[11px] text-amber-600 font-bold">
                                  <span className="material-symbols-outlined text-[13px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                                  {vendor.ratingAvg}
                                </span>
                                <span className="text-[11px] text-outline">({vendor.ratingCount} đ/g)</span>
                              </div>
                            </div>
                          </td>

                          <td className="py-4 px-space-md whitespace-nowrap">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full font-label-sm text-xs font-bold ${
                              vendor.verificationStatus === 'VERIFIED'
                                ? 'bg-primary/10 text-primary'
                                : vendor.verificationStatus === 'PENDING'
                                ? 'bg-secondary-container/25 text-on-secondary-container animate-pulse'
                                : 'bg-secondary-container/20 text-secondary'
                            }`}>
                              <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                              {vendor.verificationStatus === 'VERIFIED' ? 'Đã duyệt' : vendor.verificationStatus === 'PENDING' ? 'Chờ duyệt' : 'Từ chối'}
                            </span>
                          </td>

                          <td className="py-4 px-space-md whitespace-nowrap">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-surface-container text-on-surface font-label-sm text-[11px] font-bold">
                              <span className="material-symbols-outlined text-[13px] text-secondary">military_tech</span>
                              {vendor.badgeTier}
                            </span>
                          </td>

                          <td className="py-4 px-space-md text-right whitespace-nowrap">
                            <button 
                              onClick={(e) => { e.stopPropagation(); setSelectedVendorId(vendor.id); }}
                              className={`w-8 h-8 rounded-lg inline-flex items-center justify-center transition-all ${
                                isSelected ? 'bg-primary text-on-primary shadow-xs' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                              }`}
                            >
                              <span className="material-symbols-outlined text-[18px]">visibility</span>
                            </button>
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
              <div className="p-space-lg rounded-xl bg-surface-container-lowest shadow-md flex flex-col gap-space-lg border border-outline-variant/20">
                {/* Drawer Header */}
                <div className="flex flex-col gap-space-xs">
                  <div className="flex items-center justify-between">
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-surface-container text-on-surface-variant font-mono text-[12px] font-bold">
                      MÃ ĐỐI TÁC: #{selectedVendor.id}
                    </span>
                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full font-label-sm text-xs font-bold ${
                      selectedVendor.verificationStatus === 'VERIFIED'
                        ? 'bg-primary/10 text-primary'
                        : selectedVendor.verificationStatus === 'PENDING'
                        ? 'bg-secondary-container/20 text-on-secondary-container'
                        : 'bg-secondary-container/20 text-secondary'
                    }`}>
                      {selectedVendor.verificationStatus === 'VERIFIED' ? 'ĐÃ PHÊ DUYỆT' : selectedVendor.verificationStatus === 'PENDING' ? 'CHỜ THẨM ĐỊNH' : 'TỪ CHỐI'}
                    </span>
                  </div>

                  <div className="mt-2">
                    <h2 className="font-headline-md text-headline-md text-on-surface font-bold">{selectedVendor.businessName}</h2>
                    <div className="flex items-center gap-1.5 text-on-surface-variant font-body-sm text-body-sm mt-0.5">
                      <span className="material-symbols-outlined text-[16px] text-primary">location_on</span>
                      <span>{selectedVendor.address}</span>
                    </div>
                  </div>

                  <div className="p-space-sm rounded-lg bg-surface-container-low font-body-sm text-[12px] text-on-surface-variant mt-1">
                    Người duyệt gần nhất: <strong className="text-on-surface">{selectedVendor.verifiedBy || 'Đang chờ thẩm định'}</strong>
                  </div>
                </div>

                {/* Section: Legal & Financial Verification */}
                <div className="flex flex-col gap-space-sm">
                  <span className="font-label-sm text-label-sm uppercase tracking-wider text-outline font-bold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">account_balance</span>
                    Thông tin pháp lý &amp; Tài khoản thụ hưởng
                  </span>
                  <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                    <div className="flex justify-between items-center text-body-sm">
                      <span className="text-on-surface-variant">Mã số thuế doanh nghiệp:</span>
                      <span className="font-mono font-bold text-on-surface">{selectedVendor.taxCode}</span>
                    </div>
                    <div className="flex justify-between items-center text-body-sm">
                      <span className="text-on-surface-variant">Ngân hàng thanh toán:</span>
                      <span className="font-semibold text-on-surface">{selectedVendor.bankName}</span>
                    </div>
                    <div className="flex justify-between items-center text-body-sm">
                      <span className="text-on-surface-variant">Số tài khoản thụ hưởng:</span>
                      <span className="font-mono font-black text-primary text-sm">{selectedVendor.bankAccountNumber}</span>
                    </div>
                    <div className="flex justify-between items-center text-body-sm pt-1 border-t border-outline-variant/20">
                      <span className="text-on-surface-variant">Chủ tài khoản:</span>
                      <span className="font-bold text-on-surface text-right text-xs uppercase">{selectedVendor.bankAccountHolder}</span>
                    </div>
                  </div>
                </div>

                {/* Section: Attached Documents */}
                <div className="flex flex-col gap-space-sm">
                  <span className="font-label-sm text-label-sm uppercase tracking-wider text-outline font-bold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">folder_open</span>
                    Tài liệu hồ sơ đính kèm (3 tệp)
                  </span>
                  <div className="flex flex-col gap-2">
                    <div 
                      onClick={() => setDocPreviewName('giay_phep_kinh_doanh_2024.pdf')}
                      className="p-space-sm rounded-xl bg-surface-container-low hover:bg-surface-container transition-colors flex items-center justify-between gap-2 cursor-pointer"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-surface-container-lowest text-primary flex items-center justify-center shrink-0 shadow-sm">
                          <span className="material-symbols-outlined text-[20px]">picture_as_pdf</span>
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="font-label-md text-label-md text-on-surface truncate font-semibold">giay_phep_kinh_doanh_2024.pdf</span>
                          <span className="font-body-sm text-[11px] text-on-surface-variant">2.8 MB • Mã: #GP-DANANG-2024</span>
                        </div>
                      </div>
                      <span className="material-symbols-outlined text-outline text-[18px]">open_in_new</span>
                    </div>

                    <div 
                      onClick={() => setDocPreviewName('chung_chi_cuu_ho_padi_rescue.pdf')}
                      className="p-space-sm rounded-xl bg-surface-container-low hover:bg-surface-container transition-colors flex items-center justify-between gap-2 cursor-pointer"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-surface-container-lowest text-primary flex items-center justify-center shrink-0 shadow-sm">
                          <span className="material-symbols-outlined text-[20px]">verified</span>
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="font-label-md text-label-md text-on-surface truncate font-semibold">chung_chi_cuu_ho_padi_rescue.pdf</span>
                          <span className="font-body-sm text-[11px] text-on-surface-variant">4.1 MB • PADI Rescue Standard</span>
                        </div>
                      </div>
                      <span className="material-symbols-outlined text-outline text-[18px]">open_in_new</span>
                    </div>
                  </div>
                </div>

                {/* Badge Adjustment */}
                <div className="flex flex-col gap-space-xs">
                  <span className="font-label-sm text-label-sm uppercase tracking-wider text-outline font-bold flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">military_tech</span>
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
                            ? 'bg-primary text-on-primary shadow-sm'
                            : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
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
                      className="flex-1 py-3 px-space-md rounded-xl bg-primary text-on-primary font-label-lg text-label-lg hover:bg-primary-container transition-colors shadow-sm flex items-center justify-center gap-2 font-bold"
                    >
                      <span className="material-symbols-outlined text-[18px]">verified</span>
                      <span>Cập nhật phê duyệt</span>
                    </button>
                    <button 
                      onClick={() => setIsRejectModalOpen(true)}
                      className="py-3 px-space-md rounded-xl bg-secondary-container/20 hover:bg-secondary-container text-secondary font-label-lg text-label-lg transition-colors flex items-center justify-center gap-1.5 font-semibold"
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
                Nhập lý do chưa đạt chuẩn để ghi nhận vào CSDL Cảng vụ và gửi thông báo phản hồi cho đối tác:
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
                Tệp tin PDF đã được đối soát chữ ký số PKI từ Trung tâm Quản lý Vận tải Thủy Đà Nẵng, mã xác thực hợp chuẩn VITA-2024.
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
