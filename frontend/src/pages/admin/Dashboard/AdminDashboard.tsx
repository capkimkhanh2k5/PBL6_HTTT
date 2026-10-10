import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MOCK_ADMIN_VENDORS, MOCK_ADMIN_SERVICES } from '../../../data/adminMockData';
import { MOCK_MASTER_ORDERS } from '../../../mockData';

export function AdminDashboard() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'all' | 'vendors' | 'services' | 'payouts' | 'disputes'>('all');

  // Count pending operational items based on mock data
  const pendingVendors = MOCK_ADMIN_VENDORS.filter(v => v.verificationStatus === 'PENDING');
  const pendingServices = MOCK_ADMIN_SERVICES.filter(s => s.status === 'PENDING_APPROVAL');
  
  // Pending payouts mock items
  const pendingPayouts = [
    { id: 'PO-2024-001', vendor: 'Danang Ocean Club', amount: '24.500.000 đ', bank: 'Vietcombank - 004100038821', date: '14/10/2024' },
    { id: 'PO-2024-002', vendor: 'Sơn Trà Marine Diving', amount: '18.200.000 đ', bank: 'Techcombank - 1903348129', date: '14/10/2024' },
    { id: 'PO-2024-003', vendor: 'SkyFly Paragliding', amount: '13.700.000 đ', bank: 'MB Bank - 0882199410', date: '15/10/2024' },
  ];

  // Open disputes mock items
  const openDisputes = [
    { id: 'DISP-101', customer: 'Nguyễn Văn An', orderId: 'ORD-8821', reason: 'Thời tiết xấu, yêu cầu dời lịch', date: '15/10/2024', status: 'OPEN' },
    { id: 'DISP-102', customer: 'Trần Thị Mai', orderId: 'ORD-8804', reason: 'Dịch vụ trễ giờ quá 30 phút', date: '14/10/2024', status: 'IN_REVIEW' },
  ];

  const recentOrders = MOCK_MASTER_ORDERS.slice(0, 5);

  return (
    <main className="w-full pt-16 bg-surface px-6 md:px-8 pb-12 min-h-screen">
      <div className="flex flex-col w-full gap-6">
        
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-900 tracking-tight">
              Tổng quan Quản trị
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button 
              onClick={() => navigate('/admin/orders')}
              className="px-3.5 py-1.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 text-sm font-medium transition-colors border border-slate-200 shadow-xs"
            >
              Xem tất cả đơn hàng
            </button>
            <button 
              onClick={() => navigate('/admin/logs')}
              className="px-3.5 py-1.5 rounded-lg bg-primary text-white text-sm font-semibold hover:bg-primary-container transition-colors shadow-xs"
            >
              Nhật ký hệ thống
            </button>
          </div>
        </div>

        {/* Asymmetric Workspace (8 cols Left / 4 cols Right) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* LEFT COLUMN: Actionable Backlog & Priority Queue (8 cols) */}
          <div className="lg:col-span-8 flex flex-col gap-6">
            {/* Urgent Backlog Hero Card */}
            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shrink-0 shadow-xs mt-0.5">
                  <span className="material-symbols-outlined text-[22px]">assignment_late</span>
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-primary font-bold">Nhiệm vụ ưu tiên cao</span>
                    <span className="px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200 font-bold text-xs">
                      {pendingServices.length + openDisputes.length}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-slate-900 mt-0.5">
                    {pendingServices.length} tour biển mới &amp; {openDisputes.length} khiếu nại khách hàng đang chờ giải quyết
                  </h3>
                  <p className="text-xs text-slate-500 mt-1">
                    Cần thẩm định quy chuẩn an toàn trước giờ triều cường để đối tác kịp lịch khởi hành.
                  </p>
                </div>
              </div>
              <div className="flex sm:flex-col items-center sm:items-end gap-2 shrink-0">
                <button
                  onClick={() => navigate('/admin/service-approval')}
                  className="px-3.5 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-container transition-colors shadow-xs"
                >
                  Xử lý tour ngay
                </button>
              </div>
            </div>

            {/* Priority Queue Console */}
            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col gap-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-primary"></span>
                  <h2 className="text-base font-bold text-slate-900">Hàng đợi Cần Xử lý</h2>
                </div>
                
                {/* Tab Filter */}
                <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg text-xs overflow-x-auto border border-slate-200">
                  <button
                    onClick={() => setActiveTab('all')}
                    className={`px-2.5 py-1 rounded-md transition-colors whitespace-nowrap ${
                      activeTab === 'all' 
                        ? 'bg-primary text-white font-bold shadow-xs' 
                        : 'text-slate-600 hover:text-slate-900 font-medium'
                    }`}
                  >
                    Tất cả ({pendingVendors.length + pendingServices.length + pendingPayouts.length + openDisputes.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('services')}
                    className={`px-2.5 py-1 rounded-md transition-colors whitespace-nowrap ${
                      activeTab === 'services' 
                        ? 'bg-primary text-white font-bold shadow-xs' 
                        : 'text-slate-600 hover:text-slate-900 font-medium'
                    }`}
                  >
                    Dịch vụ ({pendingServices.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('disputes')}
                    className={`px-2.5 py-1 rounded-md transition-colors whitespace-nowrap ${
                      activeTab === 'disputes' 
                        ? 'bg-primary text-white font-bold shadow-xs' 
                        : 'text-slate-600 hover:text-slate-900 font-medium'
                    }`}
                  >
                    Khiếu nại ({openDisputes.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('vendors')}
                    className={`px-2.5 py-1 rounded-md transition-colors whitespace-nowrap ${
                      activeTab === 'vendors' 
                        ? 'bg-primary text-white font-bold shadow-xs' 
                        : 'text-slate-600 hover:text-slate-900 font-medium'
                    }`}
                  >
                    Đối tác ({pendingVendors.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('payouts')}
                    className={`px-2.5 py-1 rounded-md transition-colors whitespace-nowrap ${
                      activeTab === 'payouts' 
                        ? 'bg-primary text-white font-bold shadow-xs' 
                        : 'text-slate-600 hover:text-slate-900 font-medium'
                    }`}
                  >
                    Lệnh chi ({pendingPayouts.length})
                  </button>
                </div>
              </div>

              {/* Action List */}
              <div className="divide-y divide-slate-200 text-sm">
                {/* Services (Top priority) */}
                {(activeTab === 'all' || activeTab === 'services') && pendingServices.map(s => (
                  <div key={s.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-[18px]">verified_user</span>
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-slate-900 flex items-center gap-2 truncate">
                          <span className="truncate">{s.name}</span>
                          <span className="px-1.5 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold rounded shrink-0">
                            Chờ duyệt
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5 truncate">
                          Đơn vị: {s.vendorName} • Giá: {((s.price ?? s.basePrice) || 0).toLocaleString('vi-VN')} đ • {s.locationName || 'Đà Nẵng'}
                        </p>
                      </div>
                    </div>
                    <button 
                      onClick={() => navigate('/admin/service-approval')}
                      className="px-3 py-1 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary hover:text-white rounded-lg transition-colors shrink-0"
                    >
                      Thẩm định
                    </button>
                  </div>
                ))}

                {/* Disputes */}
                {(activeTab === 'all' || activeTab === 'disputes') && openDisputes.map(d => (
                  <div key={d.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-700 border border-rose-200 flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-[18px]">report_problem</span>
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-slate-900 flex items-center gap-2 truncate">
                          <span>{d.customer} (#{d.orderId})</span>
                          <span className="px-1.5 py-0.5 bg-rose-50 text-rose-700 border border-rose-200 text-[10px] font-bold rounded shrink-0">
                            {d.status}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5 truncate">{d.reason} • Ngày: {d.date}</p>
                      </div>
                    </div>
                    <button 
                      onClick={() => navigate('/admin/disputes')}
                      className="px-3 py-1 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-600 hover:text-white rounded-lg transition-colors border border-rose-200 shrink-0"
                    >
                      Xử lý
                    </button>
                  </div>
                ))}

                {/* Vendors */}
                {(activeTab === 'all' || activeTab === 'vendors') && pendingVendors.map(v => (
                  <div key={v.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center font-bold text-xs shrink-0 border border-slate-200">
                        {v.businessName.slice(0, 2).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-slate-900 flex items-center gap-2 truncate">
                          <span className="truncate">{v.businessName}</span>
                          <span className="px-1.5 py-0.5 bg-slate-100 text-slate-700 border border-slate-200 text-[10px] font-medium rounded shrink-0">
                            Chờ duyệt
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5 truncate">MST: {v.taxCode} • {v.address}</p>
                      </div>
                    </div>
                    <button 
                      onClick={() => navigate('/admin/vendor-approval')}
                      className="px-3 py-1 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 rounded-lg transition-colors shrink-0"
                    >
                      Hồ sơ
                    </button>
                  </div>
                ))}

                {/* Payouts */}
                {(activeTab === 'all' || activeTab === 'payouts') && pendingPayouts.map(p => (
                  <div key={p.id} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center shrink-0 border border-slate-200">
                        <span className="material-symbols-outlined text-[18px]">payments</span>
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-slate-900 flex items-center gap-2 truncate">
                          <span className="truncate">{p.vendor}</span>
                          <span className="px-1.5 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold rounded shrink-0">
                            {p.amount}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5 truncate">{p.bank} • Yêu cầu: {p.date}</p>
                      </div>
                    </div>
                    <button 
                      onClick={() => navigate('/admin/payouts')}
                      className="px-3 py-1 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary hover:text-white rounded-lg transition-colors shrink-0"
                    >
                      Duyệt chi
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: Operational Summary & Live Feed (4 cols) */}
          <div className="lg:col-span-4 flex flex-col gap-6">
            {/* Prioritized Operational Metrics Card */}
            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col gap-4">
              <div className="flex items-center justify-between pb-2 border-b border-slate-200">
                <span className="text-sm font-bold text-slate-900">Chỉ số Tồn đọng</span>
                <span className="text-xs text-slate-500 font-medium">Toàn sàn</span>
              </div>

              {/* 2 Primary Urgent Metrics */}
              <div className="grid grid-cols-2 gap-3">
                <div 
                  onClick={() => navigate('/admin/service-approval')}
                  className="p-3.5 rounded-xl bg-emerald-50/50 border border-emerald-200 hover:border-emerald-400 transition-all cursor-pointer flex flex-col"
                >
                  <span className="text-xs text-emerald-800 font-semibold">Dịch vụ chờ duyệt</span>
                  <span className="text-2xl font-bold text-primary mt-1">{pendingServices.length}</span>
                  <span className="text-[10px] text-slate-500 mt-1">Tour mới gửi</span>
                </div>

                <div 
                  onClick={() => navigate('/admin/disputes')}
                  className="p-3.5 rounded-xl bg-rose-50/50 border border-rose-200 hover:border-rose-400 transition-all cursor-pointer flex flex-col"
                >
                  <span className="text-xs text-rose-800 font-semibold">Khiếu nại mở</span>
                  <span className="text-2xl font-bold text-rose-600 mt-1">{openDisputes.length}</span>
                  <span className="text-[10px] text-slate-500 mt-1">Cần đối soát hoàn</span>
                </div>
              </div>

              {/* 2 Compact Secondary Metrics */}
              <div className="flex flex-col divide-y divide-slate-200 text-xs">
                <div 
                  onClick={() => navigate('/admin/vendor-approval')}
                  className="py-2.5 flex items-center justify-between cursor-pointer hover:text-primary transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-[16px] text-slate-400">corporate_fare</span>
                    <span className="text-slate-700 font-medium">Hồ sơ đối tác</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-slate-900">{pendingVendors.length}</span>
                    <span className="text-slate-400 text-[11px]">hồ sơ</span>
                  </div>
                </div>

                <div 
                  onClick={() => navigate('/admin/payouts')}
                  className="py-2.5 flex items-center justify-between cursor-pointer hover:text-primary transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-[16px] text-slate-400">payments</span>
                    <span className="text-slate-700 font-medium">Lệnh rút tiền chờ duyệt</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-slate-900">{pendingPayouts.length}</span>
                    <span className="text-slate-400 text-[11px]">yêu cầu</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Recent Orders Live Feed */}
            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-xs flex flex-col gap-3">
              <div className="flex items-center justify-between pb-2 border-b border-slate-200">
                <span className="text-sm font-bold text-slate-900">Đơn Đặt Chỗ Gần Đây</span>
                <button 
                  onClick={() => navigate('/admin/orders')}
                  className="text-xs text-primary font-medium hover:underline"
                >
                  Xem tất cả
                </button>
              </div>

              <div className="divide-y divide-slate-200 text-xs">
                {recentOrders.map(order => (
                  <div key={order.id} className="py-2.5 flex items-center justify-between gap-2">
                    <div className="flex flex-col min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-bold text-slate-900 truncate">#{order.id}</span>
                        <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold border ${
                          order.status === 'CONFIRMED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                          order.status === 'COMPLETED' ? 'bg-slate-100 text-slate-700 border-slate-200' :
                          'bg-slate-50 text-slate-500 border-slate-200'
                        }`}>
                          {order.status}
                        </span>
                      </div>
                      <span className="text-[11px] text-slate-400 truncate mt-0.5">Khách: {order.customerId}</span>
                    </div>
                    <div className="text-right shrink-0">
                      <span className="font-bold text-slate-900 block">
                        {order.totalAmount.toLocaleString('vi-VN')} đ
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

        </div>
      </div>
    </main>
  );
}

export default AdminDashboard;
