import React, { useState } from 'react';

interface SettlementPeriod {
  code: string;
  dateRange: string;
  grossAmount: number;
  commissionRate: number; // 0.10
  commissionAmount: number;
  netAmount: number;
  payoutDate: string;
  status: 'PAID' | 'PENDING';
  itemsCount: number;
  items: SettlementItem[];
}

interface SettlementItem {
  id: string;
  subOrderCode: string;
  serviceName: string;
  slotTime: string;
  customerName: string;
  quantity: number;
  unitPrice: number;
  gross: number;
  rate: string;
  commission: number;
  net: number;
}

const SETTLEMENT_PERIODS: SettlementPeriod[] = [
  {
    code: '#SET-202410-01',
    dateRange: '01/10/2024 – 15/10/2024',
    grossAmount: 10000000,
    commissionRate: 0.10,
    commissionAmount: 1000000,
    netAmount: 9000000,
    payoutDate: '18/10/2024 14:30',
    status: 'PAID',
    itemsCount: 18,
    items: [
      {
        id: '1',
        subOrderCode: '#SUB-89412-01',
        serviceName: 'Chèo SUP ngắm bình minh Mỹ Khê',
        slotTime: '05:30 14/10/2024 • Bãi biển Phạm Văn Đồng',
        customerName: 'Nguyễn Văn An',
        quantity: 2,
        unitPrice: 280000,
        gross: 560000,
        rate: '10%',
        commission: 56000,
        net: 504000
      },
      {
        id: '2',
        subOrderCode: '#SUB-89380-02',
        serviceName: 'Tour SUP Bãi Bụt ngắm san hô',
        slotTime: '08:30 12/10/2024 • Bán đảo Sơn Trà',
        customerName: 'Lê Quốc Bảo',
        quantity: 4,
        unitPrice: 350000,
        gross: 1400000,
        rate: '10%',
        commission: 140000,
        net: 1260000
      },
      {
        id: '3',
        subOrderCode: '#SUB-89345-01',
        serviceName: 'Chèo SUP ngắm bình minh Mỹ Khê',
        slotTime: '05:30 11/10/2024 • Bãi biển Phạm Văn Đồng',
        customerName: 'Trần Minh Tuấn',
        quantity: 6,
        unitPrice: 280000,
        gross: 1680000,
        rate: '10%',
        commission: 168000,
        net: 1512000
      },
      {
        id: '4',
        subOrderCode: '#SUB-89311-03',
        serviceName: 'Lướt ván chèo SUP ban đêm có đèn LED',
        slotTime: '19:00 09/10/2024 • Biển Non Nước',
        customerName: 'Phạm Thu Hằng',
        quantity: 2,
        unitPrice: 320000,
        gross: 640000,
        rate: '10%',
        commission: 64000,
        net: 576000
      }
    ]
  },
  {
    code: '#SET-202410-02',
    dateRange: '16/10/2024 – 31/10/2024',
    grossAmount: 14800000,
    commissionRate: 0.10,
    commissionAmount: 1480000,
    netAmount: 13320000,
    payoutDate: 'Dự kiến 03/11/2024',
    status: 'PENDING',
    itemsCount: 24,
    items: [
      {
        id: '5',
        subOrderCode: '#SUB-89502-01',
        serviceName: 'Khóa lặn biển PADI Open Water',
        slotTime: '08:00 18/10/2024 • Hòn Chảo',
        customerName: 'Võ Minh Quân',
        quantity: 2,
        unitPrice: 4500000,
        gross: 9000000,
        rate: '12%',
        commission: 1080000,
        net: 7920000
      },
      {
        id: '6',
        subOrderCode: '#SUB-89555-02',
        serviceName: 'Chèo SUP hoàng hôn Tiên Sa',
        slotTime: '16:30 19/10/2024 • Vịnh Tiên Sa',
        customerName: 'Đặng Thảo Linh',
        quantity: 4,
        unitPrice: 350000,
        gross: 1400000,
        rate: '10%',
        commission: 140000,
        net: 1260000
      }
    ]
  },
  {
    code: '#SET-202409-02',
    dateRange: '16/09/2024 – 30/09/2024',
    grossAmount: 22500000,
    commissionRate: 0.10,
    commissionAmount: 2250000,
    netAmount: 20250000,
    payoutDate: '01/10/2024 10:15',
    status: 'PAID',
    itemsCount: 38,
    items: [
      {
        id: '7',
        subOrderCode: '#SUB-88120-01',
        serviceName: 'Cano cao tốc tham quan Cù Lao Chàm',
        slotTime: '07:30 25/09/2024 • Cảng Sông Hàn',
        customerName: 'Bùi Gia Huy',
        quantity: 10,
        unitPrice: 650000,
        gross: 6500000,
        rate: '10%',
        commission: 650000,
        net: 5850000
      }
    ]
  }
];

export function Revenue() {
  const [selectedCycleCode, setSelectedCycleCode] = useState<string>('#SET-202410-01');
  const [searchQuery, setSearchQuery] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const currentCycle = SETTLEMENT_PERIODS.find(p => p.code === selectedCycleCode) || SETTLEMENT_PERIODS[0];

  const filteredItems = currentCycle.items.filter(item => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return item.subOrderCode.toLowerCase().includes(q) ||
           item.customerName.toLowerCase().includes(q) ||
           item.serviceName.toLowerCase().includes(q);
  });

  const handleExportCSV = () => {
    const csvRows = [
      ['Mã SubOrder', 'Dịch vụ', 'Thời gian', 'Khách hàng', 'Số lượng', 'Đơn giá (VND)', 'Doanh thu gộp (VND)', 'Tỷ lệ sàn', 'Khấu trừ sàn (VND)', 'Thực nhận (VND)'],
      ...currentCycle.items.map(item => [
        item.subOrderCode,
        `"${item.serviceName}"`,
        `"${item.slotTime}"`,
        `"${item.customerName}"`,
        item.quantity.toString(),
        item.unitPrice.toString(),
        item.gross.toString(),
        item.rate,
        item.commission.toString(),
        item.net.toString()
      ])
    ];

    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.map(e => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `DANASEA_REVENUE_${currentCycle.code.replace('#', '')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast(`Đã xuất bảng kê ${currentCycle.code} thành công!`);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="w-full bg-surface min-h-screen">
      <div className="flex flex-col w-full">
        <div className="px-6 py-4 space-y-3.5 w-full">
          {/* Top Utility Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200">
            <div>
              <h1 className="text-xl md:text-2xl font-bold text-slate-900 tracking-tight">
                Doanh thu
              </h1>
            </div>

            <div className="flex items-center gap-2 self-start sm:self-auto shrink-0">
              <button 
                onClick={handlePrint}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-xs font-medium hover:bg-slate-50 transition-all shadow-xs cursor-pointer" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">print</span>
                <span>In biên bản</span>
              </button>
              <button 
                onClick={handleExportCSV}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-container transition-all shadow-xs cursor-pointer" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">download</span>
                <span>Xuất CSV</span>
              </button>
            </div>
          </div>

          {/* Active Settlement Period Summary Card Banner */}
          <div className="rounded-xl bg-white shadow-xs p-4 relative overflow-hidden border border-slate-200">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 pb-3 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-primary shrink-0">
                  <span className="material-symbols-outlined text-[24px]">account_balance_wallet</span>
                </div>
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-bold text-slate-900">
                      Kỳ đối soát: {currentCycle.code}
                    </span>
                    {currentCycle.status === 'PAID' ? (
                      <span className="px-2 py-0.5 rounded text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                        Đã thanh toán
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200 flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-600"></span>
                        Đang đối soát
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Thời gian: <span className="font-medium text-slate-700">{currentCycle.dateRange}</span> • {currentCycle.payoutDate}
                  </p>
                </div>
              </div>

              {/* Inline Visual Settlement Ratio Bar */}
              <div className="flex flex-col gap-1.5 w-full lg:w-72 bg-slate-50 p-3 rounded-xl border border-slate-200">
                <div className="flex justify-between text-xs">
                  <span className="text-slate-600 font-medium">Tỷ lệ thực nhận</span>
                  <span className="font-bold text-primary">90% Net / 10% Sàn</span>
                </div>
                <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden flex">
                  <div className="h-full bg-primary" style={{ width: '90%' }} title="Thực nhận đối tác: 90%"></div>
                  <div className="h-full bg-amber-500" style={{ width: '10%' }} title="Phí dịch vụ sàn: 10%"></div>
                </div>
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>Đối tác: {currentCycle.netAmount.toLocaleString('vi-VN')} đ</span>
                  <span>Hoa hồng: {currentCycle.commissionAmount.toLocaleString('vi-VN')} đ</span>
                </div>
              </div>
            </div>

            {/* Inline Financial Summary Strip */}
            <div className="flex flex-wrap items-center justify-between gap-4 px-4 py-3 rounded-xl bg-slate-50 border border-slate-200 shadow-xs mt-3">
              <div className="flex flex-wrap items-center gap-6 text-xs">
                <div>
                  <span className="text-slate-500 text-[11px] block font-medium">Doanh thu gộp (Gross)</span>
                  <span className="font-mono font-bold text-base text-slate-900">
                    {currentCycle.grossAmount.toLocaleString('vi-VN')} <span className="text-xs font-normal">đ</span>
                  </span>
                  <span className="text-[10px] text-slate-500 block">{currentCycle.itemsCount} đơn hoàn tất</span>
                </div>
                <span className="text-slate-300 self-center">•</span>
                <div>
                  <span className="text-slate-500 text-[11px] block font-medium">Phí sàn &amp; Cảng vụ (10%)</span>
                  <span className="font-mono font-bold text-base text-amber-600">
                    -{currentCycle.commissionAmount.toLocaleString('vi-VN')} <span className="text-xs font-normal">đ</span>
                  </span>
                </div>
                <span className="text-slate-300 self-center">•</span>
                <div>
                  <span className="text-slate-500 text-[11px] block font-medium">Thực nhận đối tác (Net)</span>
                  <span className="font-mono font-bold text-base text-primary">
                    {currentCycle.netAmount.toLocaleString('vi-VN')} <span className="text-xs font-normal">đ</span>
                  </span>
                  <span className="text-[10px] text-primary block font-medium">
                    {currentCycle.status === 'PAID' ? 'Đã chi trả' : 'Đang xử lý thanh toán'}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white border border-slate-200 text-xs text-slate-600 font-medium">
                <span className="material-symbols-outlined text-[16px] text-slate-400">account_balance</span>
                <span>VCB: <strong className="font-mono text-slate-800">0041000332891</strong></span>
              </div>
            </div>
          </div>

          {/* Section: Period Selection Table */}
          <div className="flex flex-col gap-2">
            <h2 className="text-sm font-bold text-slate-900">
              Lịch sử các kỳ đối soát
            </h2>
            <div className="rounded-xl bg-white shadow-xs overflow-hidden border border-slate-200">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-100 text-slate-900 font-bold border-b border-slate-200">
                    <th className="py-3 px-4 font-bold border-r border-slate-200">Mã kỳ đối soát</th>
                    <th className="py-3 px-3 font-bold border-r border-slate-200">Khoảng thời gian</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Doanh thu gộp</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Khấu trừ (10%)</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Thực nhận</th>
                    <th className="py-3 px-3 font-bold border-r border-slate-200">Trạng thái</th>
                    <th className="py-3 px-4 text-center font-bold">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-sm text-slate-800">
                  {SETTLEMENT_PERIODS.map(period => (
                    <tr 
                      key={period.code} 
                      className={`hover:bg-slate-50 transition-colors ${selectedCycleCode === period.code ? 'bg-slate-50 font-medium' : ''}`}
                    >
                      <td className="py-3 px-4 font-bold font-mono text-slate-900 border-r border-slate-200">{period.code}</td>
                      <td className="py-3 px-3 text-slate-600 text-xs border-r border-slate-200">{period.dateRange}</td>
                      <td className="py-3 px-3 text-right font-mono font-bold text-slate-900 border-r border-slate-200">{period.grossAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-3 text-right font-mono text-amber-600 border-r border-slate-200">-{period.commissionAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-3 text-right font-mono font-bold text-primary border-r border-slate-200">{period.netAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        {period.status === 'PAID' ? (
                          <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            ĐÃ THANH TOÁN
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                            CHỜ ĐỐI SOÁT
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <button 
                          onClick={() => {
                            setSelectedCycleCode(period.code);
                            showToast(`Đã chuyển xem chi tiết kỳ ${period.code}`);
                          }}
                          className={`px-3 py-1 rounded-lg text-xs font-medium transition-all cursor-pointer shadow-xs ${
                            selectedCycleCode === period.code 
                              ? 'bg-primary text-white' 
                              : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                          }`}
                        >
                          {selectedCycleCode === period.code ? 'Đang xem' : 'Xem'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Section: Sub-orders Breakdown of Current Settlement */}
          <div className="flex flex-col gap-2 mt-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-bold text-slate-900">
                    Bảng kê chi tiết đơn trong kỳ
                  </h2>
                  <span className="text-xs font-bold text-slate-700 px-2 py-0.5 rounded bg-slate-100 border border-slate-200">
                    {currentCycle.code}
                  </span>
                </div>
              </div>

              <div className="relative w-full sm:w-64">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">search</span>
                <input 
                  type="text"
                  placeholder="Tìm mã đơn hoặc khách..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-800 text-xs focus:outline-none focus:ring-1 focus:ring-primary shadow-xs"
                />
              </div>
            </div>

            <div
              key={selectedCycleCode}
              className="rounded-xl bg-white shadow-xs overflow-hidden border border-slate-200"
            >
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-100 text-slate-900 font-bold border-b border-slate-200">
                    <th className="py-3 px-4 font-bold border-r border-slate-200">Mã Sub-order</th>
                    <th className="py-3 px-3 font-bold border-r border-slate-200">Dịch vụ</th>
                    <th className="py-3 px-3 font-bold border-r border-slate-200">Khách hàng</th>
                    <th className="py-3 px-2 text-center font-bold border-r border-slate-200">Số lượng</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Đơn giá</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Tổng tiền</th>
                    <th className="py-3 px-2 text-center font-bold border-r border-slate-200">Tỷ lệ</th>
                    <th className="py-3 px-3 text-right font-bold border-r border-slate-200">Khấu trừ sàn</th>
                    <th className="py-3 px-4 text-right font-bold">Thực nhận</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-sm text-slate-800">
                  {filteredItems.map(item => (
                    <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 px-4 font-bold font-mono text-slate-900 border-r border-slate-200">{item.subOrderCode}</td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        <div className="font-bold text-slate-900 text-xs">{item.serviceName}</div>
                        <div className="text-slate-500 text-[11px] flex items-center gap-1 mt-0.5">
                          <span className="material-symbols-outlined text-[13px]">schedule</span>
                          {item.slotTime}
                        </div>
                      </td>
                      <td className="py-3 px-3 font-medium text-slate-800 text-xs border-r border-slate-200">{item.customerName}</td>
                      <td className="py-3 px-2 text-center border-r border-slate-200">
                        <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 text-xs font-medium text-slate-700">
                          {item.quantity} người
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right font-mono text-xs text-slate-700 border-r border-slate-200">{item.unitPrice.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-3 text-right font-mono font-bold text-xs text-slate-900 border-r border-slate-200">{item.gross.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-2 text-center text-xs text-slate-600 border-r border-slate-200">{item.rate}</td>
                      <td className="py-3 px-3 text-right font-mono text-xs text-amber-600 border-r border-slate-200">-{item.commission.toLocaleString('vi-VN')} đ</td>
                      <td className="py-3 px-4 text-right font-mono font-bold text-xs text-primary">{item.net.toLocaleString('vi-VN')} đ</td>
                    </tr>
                  ))}
                </tbody>
              </table>

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
        </div>

        {/* Toast Notification */}
        {toastMessage && (
          <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
            <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
            <span className="font-label-md text-label-md">{toastMessage}</span>
          </div>
        )}
      </div>
    </div>
  );
}
