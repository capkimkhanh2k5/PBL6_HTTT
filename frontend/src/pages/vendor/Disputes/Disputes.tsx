import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MOCK_DISPUTES } from '../../../mockData';

export function Disputes() {
  const navigate = useNavigate();
  const [activeFilter, setActiveFilter] = useState('ALL');

  const filteredDisputes = MOCK_DISPUTES.filter(d => activeFilter === 'ALL' || d.status === activeFilter);

  return (
    <div className="p-space-lg w-full max-w-[1280px] mx-auto flex flex-col min-h-full">
      <div className="flex items-center justify-between mb-space-xl">
        <div className="flex flex-col gap-1">
          <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest">Hỗ trợ đối tác</span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface">Quản lý Khiếu nại</h1>
          <p className="font-body-md text-body-md text-on-surface-variant">Theo dõi và phản hồi các khiếu nại từ khách hàng.</p>
        </div>
      </div>

      <div className="flex gap-space-sm mb-space-lg overflow-x-auto pb-2">
        {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map(status => (
          <button
            key={status}
            onClick={() => setActiveFilter(status)}
            className={`px-space-md py-1.5 rounded-full font-label-md text-label-md whitespace-nowrap transition-colors ${activeFilter === status ? 'bg-primary text-on-primary' : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high'}`}
          >
            {status === 'ALL' ? 'Tất cả' : status}
          </button>
        ))}
      </div>

      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 overflow-hidden shadow-sm flex-1">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-outline-variant/30 bg-surface-container-low/50">
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant">Mã Khiếu Nại</th>
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant">Đơn hàng (SubOrder)</th>
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant">Phân loại</th>
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant">Trạng thái</th>
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant">Ngày tạo</th>
                <th className="p-space-md font-label-md text-label-md text-on-surface-variant text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredDisputes.length > 0 ? filteredDisputes.map(d => (
                <tr key={d.id} className="border-b border-outline-variant/30 hover:bg-surface-container-low/30 transition-colors">
                  <td className="p-space-md font-label-md text-label-md font-bold text-on-surface">{d.id}</td>
                  <td className="p-space-md font-body-sm text-body-sm text-on-surface-variant">{d.subOrderId}</td>
                  <td className="p-space-md font-body-sm text-body-sm text-on-surface-variant">{d.category}</td>
                  <td className="p-space-md">
                    <span className="px-2 py-1 rounded-full text-[10px] font-bold bg-primary-container text-on-primary-container">
                      {d.status}
                    </span>
                  </td>
                  <td className="p-space-md font-body-sm text-body-sm text-on-surface-variant">
                    {new Date(d.createdAt).toLocaleDateString('vi-VN')}
                  </td>
                  <td className="p-space-md text-right">
                    <button onClick={() => navigate(`/vendor/disputes/${d.id}`)} className="px-space-md py-1.5 bg-surface-container text-primary hover:bg-surface-container-high rounded-full font-label-sm text-label-sm font-semibold transition-colors">
                      Xem chi tiết
                    </button>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={6} className="p-space-2xl text-center font-body-md text-on-surface-variant">
                    Không có khiếu nại nào.
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
