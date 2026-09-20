import { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_PAYOUTS } from '../../../data/adminMockData';
import type { PayoutRequestStatus } from '../../../types';

type PayoutExtended = typeof MOCK_ADMIN_PAYOUTS[0];

export function AdminPayouts() {
  const [payouts, setPayouts] = useState<PayoutExtended[]>(MOCK_ADMIN_PAYOUTS);
  const [selectedPayoutId, setSelectedPayoutId] = useState<string>(MOCK_ADMIN_PAYOUTS[0]?.id || '');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  
  // Disbursement Modal
  const [isDisburseModalOpen, setIsDisburseModalOpen] = useState(false);
  const [bankRefCode, setBankRefCode] = useState('VCB-NAPAS-998231');

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredPayouts = useMemo(() => {
    return payouts.filter(p => statusFilter === 'all' || p.status === statusFilter);
  }, [payouts, statusFilter]);

  const selectedPayout = payouts.find(p => p.id === selectedPayoutId) || payouts[0];

  const handleApproveDisbursement = () => {
    if (!selectedPayout) return;
    setPayouts(prev =>
      prev.map(p =>
        p.id === selectedPayout.id
          ? {
              ...p,
              status: 'PAID' as PayoutRequestStatus,
              processedBy: 'Vũ Hải Đăng',
              processedAt: new Date().toISOString()
            }
          : p
      )
    );
    setIsDisburseModalOpen(false);
    showToast(`Đã giải ngân ${selectedPayout.amount.toLocaleString('vi-VN')} đ cho ${selectedPayout.vendorName} thành công!`);
  };

  const handleRejectPayout = () => {
    if (!selectedPayout) return;
    setPayouts(prev =>
      prev.map(p =>
        p.id === selectedPayout.id
          ? {
              ...p,
              status: 'REJECTED' as PayoutRequestStatus,
              processedBy: 'Vũ Hải Đăng',
              processedAt: new Date().toISOString()
            }
          : p
      )
    );
    showToast(`Đã từ chối lệnh chi trả #${selectedPayout.id}.`);
  };

  const handleExportStatement = () => {
    const headers = "Mã Lệnh,Đối Tác,Kỳ Đối Soát,Số Tiền,Ngân Hàng,Số Tài Khoản,Trạng Thái\n";
    const rows = filteredPayouts.map(p =>
      `"${p.id}","${p.vendorName}","${p.periodText}","${p.amount}","${p.bankName}","'${p.bankAccountNumber}'","${p.status}"`
    ).join("\n");

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `DANASEA_Payout_Statement_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("Đã xuất báo cáo đối soát chi trả ra file CSV!");
  };

  // Metrics
  const pendingCount = payouts.filter(p => p.status === 'REQUESTED').length;
  const totalPaid = payouts.filter(p => p.status === 'PAID').reduce((sum, p) => sum + p.amount, 0);

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
        <div className="flex flex-col xl:flex-row xl:items-end justify-between gap-space-lg mb-space-xl">
          <div className="flex flex-col max-w-3xl">
            <div className="flex items-center gap-2 mb-2 font-bold">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm uppercase tracking-wider">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
                DANASEA - HỆ THỐNG QUẢN TRỊ NỀN TẢNG
              </span>
              <span className="text-outline-variant">•</span>
              <span className="font-label-sm text-on-surface-variant font-medium">Kỳ đối soát T10/2024</span>
            </div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface font-black tracking-tight">
              Đối soát Doanh thu Toàn sàn &amp; Phê duyệt Chi trả (Payouts)
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant mt-1">
              Chốt kỳ đối soát định kỳ với đối tác, khấu trừ hoa hồng Cảng vụ 10% và giải ngân về tài khoản ngân hàng chính thức.
            </p>
          </div>

          <div className="flex items-center gap-space-sm">
            <button 
              onClick={handleExportStatement}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-on-surface font-label-lg shadow-sm hover:bg-surface-container transition-colors border border-outline-variant/20 font-semibold"
            >
              <span className="material-symbols-outlined text-[20px] text-primary">download</span>
              <span>Xuất báo cáo tổng hợp</span>
            </button>
          </div>
        </div>

        {/* KPI Cash Flow Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-space-md mb-space-2xl">
          <div className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wider font-bold">Tổng doanh thu gộp (Gross)</span>
            <div className="mt-space-md">
              <span className="font-headline-lg font-black text-on-surface">482.500.000 đ</span>
            </div>
            <span className="text-xs text-primary font-semibold mt-2">100% qua cổng VietQR &amp; Thẻ</span>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wider font-bold">Hoa hồng Cảng vụ (10%)</span>
            <div className="mt-space-md">
              <span className="font-headline-lg font-black text-primary">48.250.000 đ</span>
            </div>
            <span className="text-xs text-outline mt-2">Tự động khấu trừ tại nguồn</span>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wider font-bold">Đã giải ngân (Paid Net)</span>
            <div className="mt-space-md">
              <span className="font-headline-lg font-black text-secondary">{totalPaid.toLocaleString('vi-VN')} đ</span>
            </div>
            <span className="text-xs text-secondary font-semibold mt-2">Lệnh chuyển Napas247 hoàn tất</span>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <span className="font-label-md text-outline uppercase tracking-wider font-bold">Lệnh chờ xử lý</span>
            <div className="mt-space-md">
              <span className="font-headline-lg font-black text-secondary">{pendingCount} lệnh</span>
            </div>
            <span className="text-xs text-outline font-semibold mt-2">Cần duyệt chuyển tiền</span>
          </div>
        </div>

        {/* Main 2-Column Split Console */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN: Payout List (7 cols) */}
          <div className="col-span-12 lg:col-span-7 flex flex-col gap-space-md">
            <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between border border-outline-variant/20">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">payments</span>
                <span className="font-label-lg font-bold text-on-surface">Lệnh Yêu Cầu Chi Trả</span>
              </div>
              <div className="flex gap-1.5">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'all' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}
                >
                  Tất cả
                </button>
                <button
                  onClick={() => setStatusFilter('REQUESTED')}
                  className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'REQUESTED' ? 'bg-secondary text-on-secondary' : 'bg-surface-container text-on-surface-variant'}`}
                >
                  Chờ duyệt ({pendingCount})
                </button>
                <button
                  onClick={() => setStatusFilter('PAID')}
                  className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'PAID' ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}
                >
                  Đã thanh toán
                </button>
              </div>
            </div>

            <div className="bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden border border-outline-variant/20">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-surface-container text-on-surface-variant font-label-sm text-xs uppercase tracking-wider">
                    <th className="py-3 px-4">Mã lệnh &amp; Đối tác</th>
                    <th className="py-3 px-3">Kỳ đối soát</th>
                    <th className="py-3 px-3 text-right">Số tiền giải ngân</th>
                    <th className="py-3 px-3 text-center">Trạng thái</th>
                    <th className="py-3 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10 font-body-md text-xs">
                  {filteredPayouts.map(payout => {
                    const isSelected = payout.id === selectedPayout?.id;
                    return (
                      <tr 
                        key={payout.id}
                        onClick={() => setSelectedPayoutId(payout.id)}
                        className={`cursor-pointer transition-colors ${
                          isSelected ? 'bg-primary/10' : 'hover:bg-surface-container-low/40'
                        }`}
                      >
                        <td className="py-3.5 px-4">
                          <span className="font-mono font-bold text-primary block">{payout.id}</span>
                          <span className="font-bold text-on-surface text-sm">{payout.vendorName}</span>
                        </td>

                        <td className="py-3.5 px-3 text-outline">
                          {payout.periodText}
                        </td>

                        <td className="py-3.5 px-3 text-right font-bold text-secondary text-sm">
                          {payout.amount.toLocaleString('vi-VN')} đ
                        </td>

                        <td className="py-3.5 px-3 text-center">
                          <span className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${
                            payout.status === 'PAID'
                              ? 'bg-primary/10 text-primary'
                              : payout.status === 'REQUESTED'
                              ? 'bg-secondary-container/25 text-on-secondary-container animate-pulse'
                              : 'bg-secondary-container/20 text-secondary'
                          }`}>
                            {payout.status === 'PAID' ? 'ĐÃ CHI TRẢ' : payout.status === 'REQUESTED' ? 'CHỜ DUYỆT' : 'TỪ CHỐI'}
                          </span>
                        </td>

                        <td className="py-3.5 px-4 text-right">
                          <button 
                            className="p-1.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-primary"
                            title="Xem chi tiết"
                          >
                            <span className="material-symbols-outlined text-[18px]">chevron_right</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* RIGHT COLUMN: Payout Detail & Action Drawer (5 cols) */}
          {selectedPayout && (
            <div className="col-span-12 lg:col-span-5 flex flex-col gap-space-lg sticky top-20">
              <div className="bg-surface-container-lowest rounded-2xl shadow-md p-space-lg flex flex-col gap-4 border border-outline-variant/20">
                <div className="flex items-start justify-between pb-3 border-b border-outline-variant/20">
                  <div>
                    <span className="text-xs font-bold text-primary uppercase tracking-wider">Lệnh rút tiền #{selectedPayout.id}</span>
                    <h2 className="font-headline-md font-black text-on-surface mt-0.5">{selectedPayout.vendorName}</h2>
                    <span className="text-xs text-outline">{selectedPayout.periodText}</span>
                  </div>
                  <span className="px-2.5 py-1 rounded-full bg-secondary/10 text-secondary font-bold text-xs">
                    {selectedPayout.status}
                  </span>
                </div>

                {/* Bank Details */}
                <div className="p-3.5 rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <span className="text-xs font-bold text-outline uppercase tracking-wider flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px] text-primary">account_balance</span>
                    Thông tin ngân hàng thụ hưởng
                  </span>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-outline">Ngân hàng:</span>
                    <span className="font-bold text-on-surface">{selectedPayout.bankName}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-outline">Số tài khoản:</span>
                    <span className="font-mono font-black text-primary text-sm">{selectedPayout.bankAccountNumber}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs border-t border-outline-variant/20 pt-1">
                    <span className="text-outline">Chủ tài khoản:</span>
                    <span className="font-bold text-on-surface text-right">{selectedPayout.bankAccountHolder}</span>
                  </div>
                </div>

                {/* Amount Summary */}
                <div className="p-3.5 rounded-xl bg-surface-container flex items-center justify-between">
                  <span className="text-xs text-outline font-bold">Số tiền giải ngân:</span>
                  <span className="font-headline-lg font-black text-secondary">
                    {selectedPayout.amount.toLocaleString('vi-VN')} đ
                  </span>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col gap-2 pt-2">
                  {selectedPayout.status === 'REQUESTED' ? (
                    <>
                      <button 
                        onClick={() => setIsDisburseModalOpen(true)}
                        className="w-full py-3 rounded-xl bg-primary text-on-primary font-bold text-xs hover:bg-primary-container transition-all flex items-center justify-center gap-1.5 shadow-sm"
                      >
                        <span className="material-symbols-outlined text-[18px]">verified</span>
                        <span>Phê duyệt lệnh chi &amp; Chuyển Napas247</span>
                      </button>
                      <button 
                        onClick={handleRejectPayout}
                        className="w-full py-2.5 rounded-xl bg-secondary-container/20 text-secondary font-bold text-xs hover:bg-secondary-container transition-colors"
                      >
                        Từ chối lệnh chi
                      </button>
                    </>
                  ) : (
                    <div className="p-3 rounded-xl bg-primary/10 text-primary text-xs font-bold flex items-center gap-2">
                      <span className="material-symbols-outlined text-[18px]">check_circle</span>
                      <span>Lệnh chi này đã được xử lý bởi {selectedPayout.processedBy || 'Cảng vụ'}.</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* DISBURSEMENT MODAL (Portaled to body) */}
      {isDisburseModalOpen && selectedPayout && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/50 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-3xl p-space-xl max-w-md w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <h3 className="font-headline-md font-bold text-on-surface">Xác Nhận Giải Ngân Napas247</h3>
            <p className="text-xs text-on-surface-variant">
              Bạn đang phê duyệt chi trả <strong className="text-secondary">{selectedPayout.amount.toLocaleString('vi-VN')} đ</strong> về tài khoản ngân hàng <strong>{selectedPayout.bankAccountNumber}</strong> ({selectedPayout.bankAccountHolder}).
            </p>

            <div>
              <label className="text-xs font-bold text-on-surface block mb-1">Mã tham chiếu ngân hàng (Bank Ref):</label>
              <input 
                value={bankRefCode}
                onChange={(e) => setBankRefCode(e.target.value)}
                className="w-full h-10 px-3 rounded-xl bg-surface text-xs font-mono font-bold text-on-surface border border-outline-variant/30 focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button 
                onClick={() => setIsDisburseModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container text-xs font-semibold"
              >
                Hủy
              </button>
              <button 
                onClick={handleApproveDisbursement}
                className="px-5 py-2.5 rounded-xl bg-primary text-on-primary text-xs font-bold shadow-sm"
              >
                Xác nhận chuyển tiền
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}

export default AdminPayouts;
