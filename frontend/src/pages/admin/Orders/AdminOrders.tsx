import { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { MOCK_MASTER_ORDERS, MOCK_SUB_ORDERS } from '../../../mockData';
import type { MasterOrder, SubOrder } from '../../../types';

export function AdminOrders() {
  const navigate = useNavigate();
  const [masterOrders] = useState<MasterOrder[]>(MOCK_MASTER_ORDERS);
  const [subOrders, setSubOrders] = useState<SubOrder[]>(MOCK_SUB_ORDERS);
  const [selectedOrderId, setSelectedOrderId] = useState<string>(MOCK_MASTER_ORDERS[0]?.id || 'master-1');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  // Refund Modal
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundSubOrderId, setRefundSubOrderId] = useState<string>('');
  const [refundReason, setRefundReason] = useState('');

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredOrders = useMemo(() => {
    return masterOrders.filter(order => {
      const matchSearch =
        order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.customerId.toLowerCase().includes(searchQuery.toLowerCase());
      const matchStatus = statusFilter === 'all' || order.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [masterOrders, searchQuery, statusFilter]);

  const selectedMaster = masterOrders.find(o => o.id === selectedOrderId) || masterOrders[0];
  const relatedSubOrders = subOrders.filter(s => s.masterOrderId === selectedMaster?.id);

  const handleOpenRefund = (subId: string) => {
    setRefundSubOrderId(subId);
    setRefundReason('');
    setIsRefundModalOpen(true);
  };

  const handleConfirmRefund = () => {
    setSubOrders(prev =>
      prev.map(s => (s.id === refundSubOrderId ? { ...s, status: 'REFUNDED' } : s))
    );
    setIsRefundModalOpen(false);
    showToast(`Đã khởi tạo quy trình hoàn tiền cho đơn con #${refundSubOrderId}!`);
  };

  const handleExportCSV = () => {
    const headers = "Mã Đơn,Khách Hàng,Trạng Thái,Tổng Tiền,Giảm Giá,Thời Gian\n";
    const rows = filteredOrders.map(o =>
      `"${o.id}","${o.customerId}","${o.status}","${o.totalAmount}","${o.discountAmount}","${o.createdAt || ''}"`
    ).join("\n");

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `DANASEA_Orders_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("Đã xuất danh sách đơn hàng sang file CSV!");
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
            <div className="flex items-center gap-3">
              <h1 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">
                Quản lý Đơn hàng
              </h1>
              <div className="hidden sm:flex items-center gap-2 text-xs font-medium">
                <span className="px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-600">
                  Tổng: <strong>{masterOrders.length}</strong>
                </span>
                <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary border border-primary/20">
                  Đã thanh toán: <strong>{masterOrders.filter(o => o.status === 'CONFIRMED').length}</strong>
                </span>
                <span className="px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
                  Chờ: <strong>{masterOrders.filter(o => o.status === 'PENDING').length}</strong>
                </span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button 
              onClick={handleExportCSV}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 text-sm font-medium border border-slate-200 shadow-xs transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">file_download</span>
              <span>Xuất CSV</span>
            </button>
          </div>
        </div>

        {/* Main 2-Column Split Console */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN (Span 7): Master Orders Ledger Table */}
          <div className="col-span-12 xl:col-span-7 flex flex-col gap-space-md">
            {/* Filter Bar */}
            <div className="bg-white p-space-sm rounded-xl shadow-xs flex flex-col sm:flex-row gap-space-sm items-center justify-between border border-slate-200">
              <div className="relative flex-1 w-full">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">search</span>
                <input 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-9 pl-9 pr-3 rounded-lg bg-slate-50 text-slate-800 text-xs placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-1 focus:ring-primary border border-slate-200" 
                  placeholder="Mã Master (#master-...), Mã khách..." 
                  type="text"
                />
              </div>
              <div className="w-full sm:w-56">
                <select 
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full h-9 px-2.5 rounded-lg bg-slate-50 text-slate-700 text-xs focus:outline-none focus:ring-1 focus:ring-primary border border-slate-200 cursor-pointer"
                >
                  <option value="all">Tất cả trạng thái</option>
                  <option value="CONFIRMED">Đã thanh toán (CONFIRMED)</option>
                  <option value="PENDING">Chờ thanh toán (PENDING)</option>
                  <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                </select>
              </div>
            </div>

            {/* Orders Table */}
            <div className="bg-white rounded-xl shadow-xs overflow-hidden border border-slate-200">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
                      <th className="py-3 px-3.5 border-r border-slate-200 whitespace-nowrap">Mã đơn &amp; Thời gian</th>
                      <th className="py-3 px-3 border-r border-slate-200 whitespace-nowrap">Khách hàng</th>
                      <th className="py-3 px-2.5 text-center border-r border-slate-200 whitespace-nowrap">Dịch vụ con</th>
                      <th className="py-3 px-3 text-right border-r border-slate-200 whitespace-nowrap">Tổng tiền</th>
                      <th className="py-3 px-3 text-center whitespace-nowrap">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 text-slate-700">
                    {filteredOrders.map(order => {
                      const isSelected = order.id === selectedMaster?.id;
                      const subs = subOrders.filter(s => s.masterOrderId === order.id);

                      return (
                        <tr 
                          key={order.id}
                          onClick={() => setSelectedOrderId(order.id)}
                          className={`cursor-pointer transition-colors ${
                            isSelected 
                              ? 'bg-primary/5 font-semibold border-l-4 border-primary' 
                              : 'hover:bg-slate-50'
                          }`}
                        >
                          <td className="py-3 px-3.5 whitespace-nowrap border-r border-slate-200">
                            <span className="font-mono font-bold text-primary block">#{order.id}</span>
                            <span className="text-[11px] text-slate-500">
                              {order.createdAt ? new Date(order.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '14:18'} • {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : 'Hôm nay'}
                            </span>
                          </td>

                          <td className="py-3 px-3 whitespace-nowrap border-r border-slate-200">
                            <span className="text-slate-800 font-medium block truncate max-w-[120px]" title={order.customerId}>
                              {order.customerId}
                            </span>
                            <span className="text-[10px] text-slate-400">Khách đặt tour</span>
                          </td>

                          <td className="py-3 px-2.5 text-center whitespace-nowrap border-r border-slate-200">
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-slate-100 border border-slate-200 text-slate-700 font-bold text-[11px]">
                              {subs.length} vé
                            </span>
                          </td>

                          <td className="py-3 px-3 text-right whitespace-nowrap border-r border-slate-200">
                            <span className="font-bold text-slate-800 text-sm block">
                              {order.totalAmount.toLocaleString('vi-VN')} đ
                            </span>
                            {order.discountAmount > 0 && (
                              <span className="text-[10px] text-primary font-medium block">
                                -{order.discountAmount.toLocaleString('vi-VN')} đ
                              </span>
                            )}
                          </td>

                          <td className="py-3 px-3 text-center whitespace-nowrap">
                            <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              order.status === 'CONFIRMED'
                                ? 'bg-primary/10 text-primary border border-primary/20'
                                : order.status === 'CANCELLED'
                                ? 'bg-red-50 text-red-700 border border-red-200'
                                : 'bg-amber-50 text-amber-700 border border-amber-200'
                            }`}>
                              {order.status}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN (Span 5): Order Detail Drawer */}
          {selectedMaster && (
            <div className="col-span-12 xl:col-span-5 flex flex-col gap-space-lg sticky top-20">
              <div className="bg-white rounded-xl shadow-xs p-space-lg flex flex-col gap-4 border border-slate-200">
                <div className="flex items-start justify-between pb-2 border-b border-slate-200">
                  <div>
                    <span className="font-label-sm text-xs text-primary font-medium">Chi tiết Master Order</span>
                    <h2 className="text-xl font-bold text-slate-800">#{selectedMaster.id}</h2>
                  </div>
                  <span className="px-2.5 py-1 rounded-full bg-primary/10 text-primary border border-primary/20 font-bold text-xs">
                    {selectedMaster.status}
                  </span>
                </div>

                {/* Customer Details */}
                <div className="bg-slate-50 border border-slate-200 p-3 rounded-xl flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold">
                    NA
                  </div>
                  <div>
                    <span className="font-bold text-sm text-slate-800 block">Mã khách: {selectedMaster.customerId}</span>
                    <span className="text-xs text-slate-500">Hội viên VIP Sàn DANASEA</span>
                  </div>
                </div>

                {/* Gateway Details */}
                <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl flex flex-col gap-2">
                  <span className="text-xs font-semibold text-slate-800">Đối soát thanh toán (Payments)</span>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Cổng giao dịch:</span>
                    <span className="font-bold text-slate-800">SEPAY VietQR Gateway</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Số lượng dịch vụ con:</span>
                    <span className="font-bold text-slate-800">{relatedSubOrders.length} vé / dịch vụ</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Tổng thanh toán:</span>
                    <span className="font-bold text-slate-800 text-sm">{selectedMaster.totalAmount.toLocaleString('vi-VN')} đ</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Khuyến mãi áp dụng:</span>
                    <span className="font-bold text-primary">-{selectedMaster.discountAmount.toLocaleString('vi-VN')} đ</span>
                  </div>
                </div>

                {/* Related Sub Orders List */}
                <div className="flex flex-col gap-2">
                  <span className="text-xs font-bold text-slate-800">Danh sách dịch vụ con ({relatedSubOrders.length})</span>
                  <div className="flex flex-col gap-2 max-h-48 overflow-y-auto">
                    {relatedSubOrders.map(sub => (
                      <div key={sub.id} className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-between text-xs">
                        <div>
                          <span className="font-mono font-bold text-slate-800">#{sub.id}</span>
                          <span className="text-[11px] text-slate-500 block">Tour #{sub.serviceId} (SL: {sub.quantity})</span>
                          <span className="font-semibold text-slate-800">{sub.subtotalAmount.toLocaleString('vi-VN')} đ</span>
                        </div>
                        <div className="flex flex-col items-end gap-1">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            sub.status === 'REFUNDED' ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-primary/10 text-primary border border-primary/20'
                          }`}>
                            {sub.status}
                          </span>
                          {sub.status !== 'REFUNDED' && (
                            <button
                              type="button"
                              onClick={() => handleOpenRefund(sub.id)}
                              className="text-[11px] text-primary hover:underline font-semibold cursor-pointer"
                            >
                              Hoàn tiền
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2 pt-2">
                  <button 
                    onClick={() => navigate('/admin/disputes')}
                    className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 font-semibold text-xs transition-colors flex items-center justify-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-[16px] text-primary">troubleshoot</span>
                    <span>Mở quy trình Tra soát / Khiếu nại</span>
                  </button>
                  <button 
                    onClick={() => showToast("Đã tải hóa đơn điện tử VAT định dạng PDF...")}
                    className="w-full py-2.5 rounded-xl bg-primary text-white hover:bg-primary-container font-semibold text-xs transition-colors flex items-center justify-center gap-1.5 shadow-xs"
                  >
                    <span className="material-symbols-outlined text-[16px]">download</span>
                    <span>Tải PDF Hóa Đơn Điện Tử</span>
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* REFUND MODAL (Portaled to body) */}
      {isRefundModalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/50 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl p-space-xl max-w-md w-full shadow-2xl border border-outline-variant flex flex-col gap-4">
            <h3 className="font-headline-md font-bold text-on-surface">Khởi tạo Hoàn Tiền (Refund)</h3>
            <p className="text-xs text-on-surface-variant">
              Xác nhận hoàn tiền cho đơn con <strong>#{refundSubOrderId}</strong>. Số tiền sẽ được trích từ quỹ bảo chứng của đối tác và chuyển hoàn về khách hàng:
            </p>
            <textarea 
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              placeholder="VD: Tour bị hoãn do thời tiết cấm biển, hoặc khách hủy trước 48h theo quy chế..."
              className="w-full h-24 p-3 rounded-xl bg-surface text-xs text-on-surface border border-outline-variant/30 focus:outline-none focus:ring-2 focus:ring-secondary resize-none"
            />
            <div className="flex justify-end gap-2">
              <button 
                onClick={() => setIsRefundModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-surface-container text-xs font-semibold"
              >
                Hủy
              </button>
              <button 
                onClick={handleConfirmRefund}
                className="px-5 py-2 rounded-xl bg-secondary text-on-secondary text-xs font-bold"
              >
                Xác nhận hoàn tiền
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}

export default AdminOrders;
