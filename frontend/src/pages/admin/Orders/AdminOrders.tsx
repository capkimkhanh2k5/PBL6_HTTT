import { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { MOCK_MASTER_ORDERS, MOCK_SUB_ORDERS } from '../../../mockData';
import type { MasterOrder, SubOrder } from '../../../types';

export function AdminOrders() {
  const navigate = useNavigate();
  const [masterOrders, setMasterOrders] = useState<MasterOrder[]>(MOCK_MASTER_ORDERS);
  const [subOrders, setSubOrders] = useState<SubOrder[]>(MOCK_SUB_ORDERS);
  const [selectedOrderId, setSelectedOrderId] = useState<string>(MOCK_MASTER_ORDERS[0]?.id || 'master-1');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [gatewayFilter, setGatewayFilter] = useState<string>('all');

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

        {/* Top Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-xl">
          <div>
            <div className="flex items-center gap-space-xs text-outline font-label-md text-label-md mb-1">
              <span className="hover:text-primary cursor-pointer transition-colors">Trung tâm Cảng vụ</span>
              <span className="material-symbols-outlined text-[14px]">chevron_right</span>
              <span className="text-primary font-semibold">Giám sát Đơn đặt tour &amp; Thanh toán</span>
            </div>
            <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black flex items-center gap-3">
              Giám sát Đơn đặt tour &amp; Giao dịch Thanh toán
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-label-sm font-label-sm bg-primary/10 text-primary font-bold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary mr-1.5 animate-ping"></span> Realtime VITA Sync
              </span>
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant mt-1">
              Theo dõi toàn bộ luồng giao dịch Master Orders, Sub Orders nhà cung cấp, đối soát cổng thanh toán và hóa đơn điện tử.
            </p>
          </div>

          <div className="flex items-center gap-space-sm flex-shrink-0">
            <button 
              onClick={handleExportCSV}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-primary hover:bg-surface-container shadow-sm transition-all font-label-lg text-label-lg border border-outline-variant/20 font-semibold"
            >
              <span className="material-symbols-outlined text-[18px]">file_download</span>
              <span>Xuất CSV</span>
            </button>
            <button 
              onClick={() => showToast("Đã kích hoạt đối soát tự động webhook với các cổng thanh toán!")}
              className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-primary text-on-primary hover:bg-primary-container shadow-sm transition-all font-label-lg text-label-lg font-bold"
            >
              <span className="material-symbols-outlined text-[18px]">sync_alt</span>
              <span>Đối soát Webhooks</span>
            </button>
          </div>
        </div>

        {/* Bento Metrics */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-space-md mb-space-xl">
          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-md text-outline uppercase tracking-wider font-bold">Master Orders</span>
              <div className="w-9 h-9 rounded-xl bg-surface-container flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[20px]">shopping_bag</span>
              </div>
            </div>
            <div className="mt-space-md flex items-baseline gap-2">
              <span className="font-headline-xl text-headline-xl text-on-surface font-black">{masterOrders.length}</span>
              <span className="font-label-md text-primary font-bold flex items-center">
                <span className="material-symbols-outlined text-[16px]">arrow_upward</span> +14.2%
              </span>
            </div>
            <span className="text-xs text-outline mt-2">Hợp đồng đặt tour</span>
          </div>

          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-md text-outline uppercase tracking-wider font-bold">Dòng Tiền Đã Khớp</span>
              <div className="w-9 h-9 rounded-xl bg-surface-container flex items-center justify-center text-secondary">
                <span className="material-symbols-outlined text-[20px]">payments</span>
              </div>
            </div>
            <div className="mt-space-md">
              <span className="font-headline-lg font-black text-secondary">482.500.000 đ</span>
            </div>
            <span className="text-xs text-primary font-semibold mt-2">Đã đối soát 100% CSDL Hàng hải</span>
          </div>

          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-md text-outline uppercase tracking-wider font-bold">Cổng Thanh Toán</span>
              <div className="w-9 h-9 rounded-xl bg-surface-container flex items-center justify-center text-tertiary">
                <span className="material-symbols-outlined text-[20px]">account_balance_wallet</span>
              </div>
            </div>
            <div className="mt-space-md">
              <div className="flex items-center justify-between text-xs font-bold mb-1.5">
                <span className="text-primary">SEPAY 62%</span>
                <span className="text-tertiary">VNPAY 28%</span>
                <span className="text-secondary">MoMo 10%</span>
              </div>
              <div className="w-full h-2 rounded-full bg-surface-container flex overflow-hidden">
                <div className="h-full bg-primary" style={{ width: "62%" }}></div>
                <div className="h-full bg-tertiary" style={{ width: "28%" }}></div>
                <div className="h-full bg-secondary" style={{ width: "10%" }}></div>
              </div>
            </div>
            <span className="text-xs text-outline mt-2">Tỉ lệ lỗi &lt; 0.01%</span>
          </div>

          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-md text-outline uppercase tracking-wider font-bold">Sub Orders Dịch Vụ</span>
              <div className="w-9 h-9 rounded-xl bg-surface-container flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[20px]">kayaking</span>
              </div>
            </div>
            <div className="mt-space-md flex items-baseline gap-2">
              <span className="font-headline-xl text-headline-xl text-on-surface font-black">{subOrders.length}</span>
              <span className="font-body-md text-outline font-semibold">dịch vụ biển</span>
            </div>
            <span className="text-xs text-primary font-bold mt-2">Tự động gắn mã vé QR</span>
          </div>
        </div>

        {/* Main 2-Column Split Console */}
        <div className="grid grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN (Span 7): Master & Sub Orders Table */}
          <div className="col-span-12 xl:col-span-7 flex flex-col gap-space-lg">
            {/* Filter Card */}
            <div className="bg-surface-container-lowest p-space-md rounded-2xl shadow-sm flex flex-col gap-space-md border border-outline-variant/20">
              <div className="grid grid-cols-1 md:grid-cols-12 gap-space-sm items-center">
                <div className="md:col-span-6 relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
                  <input 
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full h-10 pl-9 pr-3 rounded-xl bg-surface-container-low text-on-surface font-body-md text-body-md placeholder:text-outline focus:outline-none focus:ring-1 focus:ring-primary border border-outline-variant/30" 
                    placeholder="Mã Master (#master-...), Mã khách..." 
                    type="text"
                  />
                </div>
                <div className="md:col-span-6 relative">
                  <select 
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="w-full h-10 px-3 rounded-xl bg-surface-container-low text-on-surface font-body-md text-body-md focus:outline-none focus:ring-1 focus:ring-primary border border-outline-variant/30"
                  >
                    <option value="all">Trạng thái: Tất cả</option>
                    <option value="CONFIRMED">CONFIRMED (Đã thanh toán)</option>
                    <option value="PENDING">PENDING (Chờ thanh toán)</option>
                    <option value="CANCELLED">CANCELLED (Đã hủy)</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Orders List */}
            <div className="flex flex-col gap-space-md">
              {filteredOrders.map(order => {
                const isSelected = order.id === selectedMaster?.id;
                const subs = subOrders.filter(s => s.masterOrderId === order.id);

                return (
                  <div 
                    key={order.id}
                    onClick={() => setSelectedOrderId(order.id)}
                    className={`bg-surface-container-lowest rounded-2xl shadow-sm p-space-lg transition-all cursor-pointer border ${
                      isSelected ? 'border-primary ring-2 ring-primary/20' : 'border-outline-variant/20 hover:border-outline-variant'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm pb-space-md border-b border-outline-variant/10">
                      <div className="flex items-start gap-space-sm">
                        <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary flex-shrink-0 mt-0.5">
                          <span className="material-symbols-outlined text-[22px]">anchor</span>
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-headline-sm font-bold text-primary">#{order.id}</span>
                            <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold ${
                              order.status === 'CONFIRMED'
                                ? 'bg-primary/10 text-primary'
                                : order.status === 'CANCELLED'
                                ? 'bg-secondary-container/20 text-secondary'
                                : 'bg-surface-container text-on-surface-variant'
                            }`}>
                              {order.status}
                            </span>
                          </div>
                          <div className="flex items-center gap-3 mt-1 font-body-sm text-xs text-on-surface-variant">
                            <span className="font-semibold text-on-surface">Khách: {order.customerId}</span>
                            <span>•</span>
                            <span className="text-outline">{order.createdAt ? new Date(order.createdAt).toLocaleTimeString('vi-VN') : '14:18'}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-col sm:items-end">
                        <span className="font-headline-sm font-black text-secondary">
                          {order.totalAmount.toLocaleString('vi-VN')} đ
                        </span>
                        {order.discountAmount > 0 && (
                          <span className="text-xs text-outline">Giảm: -{order.discountAmount.toLocaleString('vi-VN')} đ</span>
                        )}
                      </div>
                    </div>

                    {/* Nested Sub-Orders */}
                    <div className="mt-space-md bg-surface-container-low/60 rounded-xl p-space-md flex flex-col gap-2">
                      <div className="flex items-center justify-between text-xs font-bold text-outline uppercase tracking-wider mb-1">
                        <span>Danh Sách {subs.length} Dịch Vụ Con (Sub-Orders)</span>
                        <span className="text-primary">QR Check-in</span>
                      </div>

                      {subs.map(sub => (
                        <div key={sub.id} className="bg-surface-container-lowest rounded-xl p-3 shadow-xs flex items-center justify-between gap-3">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                              {sub.quantity}x
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="text-xs font-bold text-on-surface font-mono">#{sub.id}</span>
                                <span className="text-[10px] px-1.5 py-0.2 rounded bg-surface-container font-bold text-primary">
                                  {sub.status}
                                </span>
                              </div>
                              <span className="text-xs text-outline block">Mã dịch vụ: {sub.serviceId}</span>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-on-surface">
                              {sub.subtotalAmount.toLocaleString('vi-VN')} đ
                            </span>
                            {sub.status !== 'REFUNDED' && (
                              <button 
                                onClick={(e) => { e.stopPropagation(); handleOpenRefund(sub.id); }}
                                className="px-2 py-1 rounded bg-secondary-container/20 text-secondary hover:bg-secondary-container/40 text-[11px] font-bold"
                              >
                                Hoàn tiền
                              </button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* RIGHT COLUMN (Span 5): Order Detail Drawer */}
          {selectedMaster && (
            <div className="col-span-12 xl:col-span-5 flex flex-col gap-space-lg sticky top-20">
              <div className="bg-surface-container-lowest rounded-2xl shadow-md p-space-lg flex flex-col gap-4 border border-outline-variant/20">
                <div className="flex items-start justify-between pb-2 border-b border-outline-variant/10">
                  <div>
                    <span className="text-xs uppercase tracking-wider text-primary font-bold">Chi tiết Master Order</span>
                    <h2 className="font-headline-lg font-black text-on-surface">#{selectedMaster.id}</h2>
                  </div>
                  <span className="px-2.5 py-1 rounded-full bg-primary/10 text-primary font-bold text-xs">
                    {selectedMaster.status}
                  </span>
                </div>

                {/* Customer Details */}
                <div className="bg-surface-container-low p-3 rounded-xl flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary text-on-primary flex items-center justify-center font-bold">
                    NA
                  </div>
                  <div>
                    <span className="font-bold text-sm text-on-surface block">Mã khách: {selectedMaster.customerId}</span>
                    <span className="text-xs text-outline">Hội viên VIP Sàn DANASEA</span>
                  </div>
                </div>

                {/* Gateway Details */}
                <div className="bg-surface-container-low p-4 rounded-xl flex flex-col gap-2">
                  <span className="text-xs font-bold text-outline uppercase tracking-wider">Đối soát thanh toán (Payments)</span>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-on-surface-variant">Cổng giao dịch:</span>
                    <span className="font-bold text-on-surface">SEPAY VietQR Gateway</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-on-surface-variant">Tổng thanh toán:</span>
                    <span className="font-bold text-secondary text-sm">{selectedMaster.totalAmount.toLocaleString('vi-VN')} đ</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-on-surface-variant">Khuyến mãi áp dụng:</span>
                    <span className="font-bold text-primary">-{selectedMaster.discountAmount.toLocaleString('vi-VN')} đ</span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2 pt-2">
                  <button 
                    onClick={() => navigate('/admin/disputes')}
                    className="w-full py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-[16px] text-primary">troubleshoot</span>
                    <span>Mở quy trình Tra soát / Khiếu nại</span>
                  </button>
                  <button 
                    onClick={() => showToast("Đã tải hóa đơn điện tử VAT định dạng PDF...")}
                    className="w-full py-2.5 rounded-xl bg-primary text-on-primary hover:bg-primary-container font-bold text-xs transition-colors flex items-center justify-center gap-1.5 shadow-sm"
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
