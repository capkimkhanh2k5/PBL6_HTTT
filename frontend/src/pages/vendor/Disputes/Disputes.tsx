import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MOCK_DISPUTES } from '../../../mockData';

const STATUS_FILTERS = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'OPEN', label: 'Mới mở' },
  { key: 'IN_PROGRESS', label: 'Đang xử lý' },
  { key: 'RESOLVED', label: 'Đã giải quyết' },
  { key: 'CLOSED', label: 'Đã đóng' },
];

export function Disputes() {
  const navigate = useNavigate();
  const [activeFilter, setActiveFilter] = useState('ALL');

  const filteredDisputes = MOCK_DISPUTES.filter(d => activeFilter === 'ALL' || d.status === activeFilter);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
            <span>Mới mở (OPEN)</span>
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
            <span>Đang xử lý</span>
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>Đã giải quyết</span>
          </span>
        );
      case 'CLOSED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant text-xs font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-outline"></span>
            <span>Đã đóng</span>
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface text-xs">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="w-full px-6 py-4 space-y-3.5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-slate-900 tracking-tight">
            Khiếu nại
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200 font-semibold text-xs">
            {MOCK_DISPUTES.length} vụ việc
          </span>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-nowrap scrollbar-none">
        {STATUS_FILTERS.map(tab => {
          const count = tab.key === 'ALL' 
            ? MOCK_DISPUTES.length 
            : MOCK_DISPUTES.filter(d => d.status === tab.key).length;

          return (
            <button
              key={tab.key}
              onClick={() => setActiveFilter(tab.key)}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5 cursor-pointer ${
                activeFilter === tab.key 
                  ? 'bg-primary text-white shadow-xs' 
                  : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200'
              }`}
              type="button"
            >
              <span>{tab.label}</span>
              <span className={`px-1.5 py-0.2 rounded-full text-[11px] font-bold ${
                activeFilter === tab.key ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-700'
              }`}>
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Ledger Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto scrollbar-thin">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr className="bg-slate-100 text-slate-900 text-xs font-bold border-b border-slate-200">
                <th className="py-3 px-4 font-bold border-r border-slate-200">Mã khiếu nại</th>
                <th className="py-3 px-3 font-bold border-r border-slate-200">Đơn hàng (SubOrder)</th>
                <th className="py-3 px-3 font-bold border-r border-slate-200">Phân loại</th>
                <th className="py-3 px-3 font-bold border-r border-slate-200">Trạng thái</th>
                <th className="py-3 px-3 font-bold border-r border-slate-200">Ngày tạo</th>
                <th className="py-3 px-4 text-center font-bold">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-sm text-slate-800">
              {filteredDisputes.length > 0 ? filteredDisputes.map(d => (
                <tr key={d.id} className="hover:bg-slate-50 transition-colors">
                  <td className="py-3 px-4 font-mono font-bold text-xs text-slate-900 border-r border-slate-200">
                    {d.id}
                  </td>
                  <td className="py-3 px-3 font-mono text-xs text-slate-800 border-r border-slate-200">
                    {d.subOrderId}
                  </td>
                  <td className="py-3 px-3 text-xs text-slate-700 border-r border-slate-200">
                    {d.category}
                  </td>
                  <td className="py-3 px-3 border-r border-slate-200">
                    {getStatusBadge(d.status)}
                  </td>
                  <td className="py-3 px-3 font-mono text-xs text-slate-600 border-r border-slate-200">
                    {new Date(d.createdAt).toLocaleDateString('vi-VN')}
                  </td>
                  <td className="py-3 px-4 text-center">
                    <button 
                      onClick={() => navigate(`/vendor/disputes/${d.id}`)} 
                      className="px-3 py-1 bg-white hover:bg-slate-100 border border-slate-200 text-slate-700 rounded-lg text-xs font-medium transition-colors cursor-pointer shadow-xs"
                      type="button"
                    >
                      Chi tiết
                    </button>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-xs text-slate-500">
                    Không có khiếu nại nào trong mục này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Minimal Clean Pagination Bar */}
        <div className="px-4 py-2 border-t border-slate-200 bg-slate-50 flex items-center justify-between text-xs select-none">
          <div className="flex items-center gap-2">
            <span className="text-slate-600 font-medium">Số dòng:</span>
            <select
              defaultValue={10}
              className="px-2 py-1 rounded bg-white border border-slate-200 text-xs font-semibold text-slate-800 cursor-pointer focus:outline-none"
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </div>
          <div className="flex items-center gap-1">
            <button className="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 font-medium cursor-pointer" disabled>
              Trước
            </button>
            <span className="w-7 h-7 rounded bg-primary text-white flex items-center justify-center font-bold text-xs">
              1
            </span>
            <button className="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 font-medium cursor-pointer" disabled>
              Sau
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
