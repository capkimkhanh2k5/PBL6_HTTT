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

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 mb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">
              Quản lý Chi trả (Payouts)
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">
              Chờ xử lý: {pendingCount}
            </span>
            <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary border border-primary/20 text-xs font-semibold">
              Đã chi: {totalPaid.toLocaleString('vi-VN')} đ
            </span>
            <button 
              onClick={handleExportStatement}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 text-sm font-medium border border-slate-200 shadow-xs transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">file_download</span>
              <span>Xuất CSV</span>
            </button>
          </div>
        </div>

        {/* Main 2-Column Split Console */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN: Payout List (7 cols) */}
          <div className="col-span-12 lg:col-span-7 flex flex-col gap-space-md">
            <div className="p-space-md rounded-xl bg-white shadow-xs flex items-center justify-between border border-slate-200">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">payments</span>
                <span className="font-bold text-slate-800 text-sm">Lệnh Yêu Cầu Chi Trả</span>
              </div>
              <div className="flex gap-1.5 p-1 bg-slate-100 rounded-xl border border-slate-200">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'all' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  Tất cả
                </button>
                <button
                  onClick={() => setStatusFilter('REQUESTED')}
                  className={`px-3 py-1.5 rounded-lg text-xs flex items-center gap-1.5 transition-all ${statusFilter === 'REQUESTED' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  <span>Chờ duyệt</span>
                  {pendingCount > 0 && (
                    <span className={`px-1.5 py-0.2 rounded-full font-bold text-[10px] ${statusFilter === 'REQUESTED' ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'}`}>{pendingCount}</span>
                  )}
                </button>
                <button
                  onClick={() => setStatusFilter('PAID')}
                  className={`px-3 py-1.5 rounded-lg text-xs transition-all ${statusFilter === 'PAID' ? 'bg-primary text-white font-bold shadow-xs' : 'text-slate-600 hover:text-slate-900'}`}
                >
                  Đã thanh toán
                </button>
              </div>
            </div>

            <div className="bg-white rounded-xl shadow-xs overflow-hidden border border-slate-200">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50 text-slate-700 text-xs font-semibold border-b border-slate-200">
                    <th className="py-3 px-4 border-r border-slate-200 whitespace-nowrap">Mã lệnh &amp; Đối tác</th>
                    <th className="py-3 px-3 border-r border-slate-200 whitespace-nowrap">Kỳ đối soát</th>
                    <th className="py-3 px-3 text-right border-r border-slate-200 whitespace-nowrap">Số tiền giải ngân</th>
                    <th className="py-3 px-3 text-center border-r border-slate-200 whitespace-nowrap">Trạng thái</th>
                    <th className="py-3 px-4 text-center whitespace-nowrap">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-xs text-slate-700">
                  {filteredPayouts.map(payout => {
                    const isSelected = payout.id === selectedPayout?.id;
                    return (
                      <tr 
                        key={payout.id}
                        onClick={() => setSelectedPayoutId(payout.id)}
                        className={`cursor-pointer transition-colors ${
                          isSelected ? 'bg-primary/5 hover:bg-primary/10' : 'hover:bg-slate-50'
                        }`}
                      >
                        <td className="py-3.5 px-4 border-r border-slate-200">
                          <span className="font-mono font-bold text-primary block">{payout.id}</span>
                          <span className="font-bold text-slate-800 text-sm">{payout.vendorName}</span>
                        </td>

                        <td className="py-3.5 px-3 text-slate-500 border-r border-slate-200 whitespace-nowrap">
                          {payout.periodText}
                        </td>

                        <td className="py-3.5 px-3 text-right font-bold text-slate-800 text-sm border-r border-slate-200 whitespace-nowrap">
                          {payout.amount.toLocaleString('vi-VN')} đ
                        </td>

                        <td className="py-3.5 px-3 text-center border-r border-slate-200 whitespace-nowrap">
                          <span className={`px-2.5 py-1 rounded-full font-bold text-[10px] ${
                            payout.status === 'PAID'
                              ? 'bg-primary/10 text-primary border border-primary/20'
                              : payout.status === 'REQUESTED'
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-red-50 text-red-700 border border-red-200'
                          }`}>
                            {payout.status === 'PAID' ? 'ĐÃ CHI TRẢ' : payout.status === 'REQUESTED' ? 'CHỜ DUYỆT' : 'TỪ CHỐI'}
                          </span>
                        </td>

                        <td className="py-3.5 px-4 text-center whitespace-nowrap">
                          <div className="flex items-center justify-center">
                            <button 
                              onClick={(e) => { e.stopPropagation(); setSelectedPayoutId(payout.id); }}
                              className={`p-1.5 rounded-lg border transition-colors inline-flex items-center justify-center ${
                                isSelected ? 'bg-primary text-white border-primary shadow-xs' : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                              }`}
                              title="Xem chi tiết"
                            >
                              <span className="material-symbols-outlined text-[18px]">chevron_right</span>
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

          {/* RIGHT COLUMN: Payout Detail & Action Drawer (5 cols) */}
          {selectedPayout && (
            <div className="col-span-12 lg:col-span-5 flex flex-col gap-space-lg sticky top-20">
              <div className="bg-white rounded-xl shadow-xs p-space-lg flex flex-col gap-4 border border-slate-200">
                <div className="flex items-start justify-between pb-3 border-b border-slate-200">
                  <div>
                    <span className="text-xs font-medium text-primary">Lệnh rút tiền #{selectedPayout.id}</span>
                    <h2 className="text-lg font-bold text-slate-800 mt-0.5">{selectedPayout.vendorName}</h2>
                    <span className="text-xs text-slate-500">{selectedPayout.periodText}</span>
                  </div>
                  <span className={`px-2.5 py-1 rounded-full font-bold text-xs ${
                    selectedPayout.status === 'PAID'
                      ? 'bg-primary/10 text-primary border border-primary/20'
                      : selectedPayout.status === 'REQUESTED'
                      ? 'bg-amber-50 text-amber-700 border border-amber-200'
                      : 'bg-red-50 text-red-700 border border-red-200'
                  }`}>
                    {selectedPayout.status}
                  </span>
                </div>

                {/* Bank Details */}
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex flex-col gap-2">
                  <span className="text-xs font-semibold text-slate-800 flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px] text-primary">account_balance</span>
                    Thông tin ngân hàng thụ hưởng
                  </span>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Ngân hàng:</span>
                    <span className="font-bold text-slate-800">{selectedPayout.bankName}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Số tài khoản:</span>
                    <span className="font-mono font-bold text-primary text-sm">{selectedPayout.bankAccountNumber}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs border-t border-slate-200 pt-1">
                    <span className="text-slate-500">Chủ tài khoản:</span>
                    <span className="font-bold text-slate-800 text-right">{selectedPayout.bankAccountHolder}</span>
                  </div>
                </div>

                {/* Amount Summary */}
                <div className="p-3.5 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-between">
                  <span className="text-xs text-slate-600 font-bold">Số tiền giải ngân:</span>
                  <span className="text-xl font-bold text-slate-900">
                    {selectedPayout.amount.toLocaleString('vi-VN')} đ
                  </span>
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col gap-2 pt-2">
                  {selectedPayout.status === 'REQUESTED' ? (
                    <>
                      <button 
                        onClick={() => setIsDisburseModalOpen(true)}
                        className="w-full py-2.5 rounded-xl bg-primary text-white font-semibold text-xs hover:bg-primary-container transition-all flex items-center justify-center gap-1.5 shadow-xs"
                      >
                        <span className="material-symbols-outlined text-[18px]">verified</span>
                        <span>Phê duyệt lệnh chi &amp; Chuyển Napas247</span>
                      </button>
                      <button 
                        onClick={handleRejectPayout}
                        className="w-full py-2.5 rounded-xl bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 font-semibold text-xs transition-colors"
                      >
                        Từ chối lệnh chi
                      </button>
                    </>
                  ) : (
                    <div className="p-3 rounded-xl bg-primary/10 text-primary border border-primary/20 text-xs font-bold flex items-center gap-2">
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
