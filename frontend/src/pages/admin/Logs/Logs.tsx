import React, { useState } from 'react';
import { MOCK_ADMIN_AUDIT_LOGS } from '../../../data/adminMockData';

export function Logs() {
  const [logs] = useState(MOCK_ADMIN_AUDIT_LOGS);
  const [search, setSearch] = useState('');
  const [selectedAction, setSelectedAction] = useState<string>('ALL');

  const filteredLogs = logs.filter(log => {
    const matchesSearch = 
      log.id.toLowerCase().includes(search.toLowerCase()) ||
      (log.actorName && log.actorName.toLowerCase().includes(search.toLowerCase())) ||
      (log.entityType && log.entityType.toLowerCase().includes(search.toLowerCase())) ||
      log.action.toLowerCase().includes(search.toLowerCase());
    const matchesAction = selectedAction === 'ALL' || log.action === selectedAction;
    return matchesSearch && matchesAction;
  });

  const getActionBadge = (action: string) => {
    if (action.includes('APPROVE') || action.includes('VERIFY')) {
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    }
    if (action.includes('LOCK') || action.includes('REJECT') || action.includes('DISPUTE')) {
      return 'bg-rose-100 text-rose-800 border-rose-200';
    }
    return 'bg-blue-100 text-blue-800 border-blue-200';
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Nhật Ký Kiểm Toán (Audit Logs)</h1>
          <p className="text-sm text-gray-500 mt-1">Theo dõi toàn bộ lịch sử thao tác quản trị, bảo mật và vận hành hệ thống.</p>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <input
            type="text"
            placeholder="Tìm theo ID, người thực hiện, đối tượng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="px-4 py-2 border rounded-xl text-sm w-full sm:w-72 outline-none focus:ring-2 focus:ring-blue-500 border-gray-200"
          />
          <select
            value={selectedAction}
            onChange={(e) => setSelectedAction(e.target.value)}
            className="px-3 py-2 border rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 border-gray-200 bg-white"
          >
            <option value="ALL">Tất cả hành động</option>
            <option value="APPROVE_PAYOUT">APPROVE_PAYOUT</option>
            <option value="VERIFY_VENDOR">VERIFY_VENDOR</option>
            <option value="RESOLVE_DISPUTE">RESOLVE_DISPUTE</option>
          </select>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-100 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                <th className="py-3.5 px-4">Mã Log</th>
                <th className="py-3.5 px-4">Thời gian</th>
                <th className="py-3.5 px-4">Người thực hiện</th>
                <th className="py-3.5 px-4">Hành động</th>
                <th className="py-3.5 px-4">Đối tượng</th>
                <th className="py-3.5 px-4">Chi tiết (Metadata)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredLogs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50/50 transition-colors">
                  <td className="py-3.5 px-4 font-mono text-xs text-gray-600">{log.id}</td>
                  <td className="py-3.5 px-4 text-xs text-gray-500 whitespace-nowrap">
                    {log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="font-medium text-gray-900">{log.actorName || log.actorUserId}</div>
                    {log.actorEmail && <div className="text-xs text-gray-400">{log.actorEmail}</div>}
                  </td>
                  <td className="py-3.5 px-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getActionBadge(log.action)}`}>
                      {log.action}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="font-mono text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                      {log.entityType} #{log.entityId}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-xs text-gray-600 max-w-md truncate">
                    {log.metadata || '—'}
                  </td>
                </tr>
              ))}
              {filteredLogs.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-400">
                    Không tìm thấy nhật ký kiểm toán phù hợp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
