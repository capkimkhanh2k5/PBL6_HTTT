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
    <main className="w-full pt-16 bg-surface min-h-screen">
      <div className="flex flex-col w-full">
        <div className="px-space-xl py-space-lg flex flex-col gap-space-xl max-w-[1360px] mx-auto w-full">
          {/* Breadcrumb & Top Utility Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-space-md">
            <div className="flex flex-col gap-space-xs">
              <nav className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
                <span>Quản trị Tài chính</span>
                <span className="material-symbols-outlined text-[16px] text-outline">chevron_right</span>
                <span className="text-primary font-semibold">Đối soát kỳ thanh toán &amp; Doanh thu biển</span>
              </nav>
              <div className="flex items-baseline gap-space-sm mt-1">
                <h1 className="font-headline-lg text-headline-lg font-bold text-on-surface tracking-tight">
                  Đối soát Doanh thu &amp; Bảng kê Chiết khấu
                </h1>
                <span className="hidden sm:inline-block px-2.5 py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed font-label-sm text-label-sm font-semibold">
                  Kỳ thanh toán 15 ngày
                </span>
              </div>
              <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl">
                Quản lý các kỳ quyết toán định kỳ giữa Ban Quản lý Cảng vụ DANASEA và Đối tác vận hành biển Danang Ocean Club theo công thức chuẩn: <span className="font-semibold text-on-surface">Thực nhận (Net) = Doanh thu gộp (Gross) - Phí hoa hồng sàn (Commission 10%)</span>.
              </p>
            </div>

            <div className="flex items-center gap-space-sm self-start md:self-auto shrink-0">
              <button 
                onClick={handlePrint}
                className="inline-flex items-center gap-space-xs px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-on-surface font-label-lg text-label-lg shadow-sm hover:bg-surface-container-high transition-all" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px] text-tertiary">print</span>
                <span>In biên bản</span>
              </button>
              <button 
                onClick={handleExportCSV}
                className="inline-flex items-center gap-space-xs px-space-md py-2.5 rounded-xl bg-primary text-on-primary font-label-lg text-label-lg shadow-sm hover:bg-primary-container transition-all" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">download</span>
                <span>Xuất bảng kê (CSV)</span>
              </button>
            </div>
          </div>

          {/* Active Settlement Period Summary Card Banner */}
          <div className="rounded-2xl bg-surface-container-lowest shadow-sm p-space-lg relative overflow-hidden border border-outline-variant/30">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md pb-space-md border-b border-outline-variant/20">
              <div className="flex items-center gap-space-md">
                <div className="w-12 h-12 rounded-xl bg-primary-fixed flex items-center justify-center text-primary shrink-0">
                  <span className="material-symbols-outlined text-[28px]">account_balance_wallet</span>
                </div>
                <div>
                  <div className="flex items-center gap-space-sm flex-wrap">
                    <span className="font-headline-sm text-headline-sm font-bold text-on-surface">
                      Kỳ đối soát đang xem: {currentCycle.code}
                    </span>
                    {currentCycle.status === 'PAID' ? (
                      <span className="px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-label-sm font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">check_circle</span>
                        ĐÃ THANH TOÁN (PAID)
                      </span>
                    ) : (
                      <span className="px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 font-label-sm text-label-sm font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">pending</span>
                        ĐANG ĐỐI SOÁT (PENDING)
                      </span>
                    )}
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                    Khoảng thời gian: <span className="font-semibold text-on-surface">{currentCycle.dateRange}</span> • {currentCycle.payoutDate}
                  </p>
                </div>
              </div>

              {/* Inline Visual Settlement Ratio Bar */}
              <div className="flex flex-col gap-1.5 w-full lg:w-72 bg-surface-container-low p-space-sm rounded-xl">
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant font-medium">Tỷ lệ thực nhận</span>
                  <span className="font-bold text-primary">90% Net / 10% Sàn</span>
                </div>
                <div className="w-full h-2.5 bg-surface-container-highest rounded-full overflow-hidden flex">
                  <div className="h-full bg-primary" style={{ width: '90%' }} title="Thực nhận đối tác: 90%"></div>
                  <div className="h-full bg-secondary" style={{ width: '10%' }} title="Phí dịch vụ sàn: 10%"></div>
                </div>
                <div className="flex justify-between font-label-sm text-[10px] text-outline">
                  <span>Đối tác: {currentCycle.netAmount.toLocaleString('vi-VN')} đ</span>
                  <span>Hoa hồng: {currentCycle.commissionAmount.toLocaleString('vi-VN')} đ</span>
                </div>
              </div>
            </div>

            {/* 4 Core Metrics */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-space-md pt-space-md">
              {/* Metric 1: Gross */}
              <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-1">
                <div className="flex items-center justify-between text-on-surface-variant">
                  <span className="font-label-md text-label-md">Doanh thu gộp (Gross)</span>
                  <span className="material-symbols-outlined text-[18px] text-tertiary">receipt_long</span>
                </div>
                <div className="font-headline-md text-headline-md font-bold text-on-surface mt-1">
                  {currentCycle.grossAmount.toLocaleString('vi-VN')} <span className="text-label-md font-medium text-tertiary">đ</span>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant line-clamp-1">
                  Tổng {currentCycle.itemsCount} dịch vụ con hoàn tất
                </p>
              </div>

              {/* Metric 2: Commission */}
              <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-1">
                <div className="flex items-center justify-between text-on-surface-variant">
                  <span className="font-label-md text-label-md">Phí sàn &amp; Cảng vụ (10%)</span>
                  <span className="material-symbols-outlined text-[18px] text-secondary">pie_chart</span>
                </div>
                <div className="font-headline-md text-headline-md font-bold text-secondary mt-1">
                  {currentCycle.commissionAmount.toLocaleString('vi-VN')} <span className="text-label-md font-medium text-secondary">đ</span>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant line-clamp-1">
                  Chiết khấu theo hợp đồng VITA
                </p>
              </div>

              {/* Metric 3: Net Payable */}
              <div className="p-space-md rounded-xl bg-primary-fixed/30 flex flex-col gap-1">
                <div className="flex items-center justify-between text-primary">
                  <span className="font-label-md text-label-md font-semibold">Thực nhận đối tác (Net)</span>
                  <span className="material-symbols-outlined text-[20px]">payments</span>
                </div>
                <div className="font-headline-md text-headline-md font-bold text-primary mt-1">
                  {currentCycle.netAmount.toLocaleString('vi-VN')} <span className="text-label-md font-medium text-primary">đ</span>
                </div>
                <p className="font-body-sm text-body-sm text-tertiary font-medium line-clamp-1">
                  {currentCycle.status === 'PAID' ? 'Đã chuyển khoản Vietcombank' : 'Đang xử lý thanh toán'}
                </p>
              </div>

              {/* Metric 4: Account */}
              <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col justify-between">
                <div className="flex items-center justify-between text-on-surface-variant">
                  <span className="font-label-md text-label-md">Tài khoản thụ hưởng</span>
                  <span className="material-symbols-outlined text-[18px] text-outline">account_balance</span>
                </div>
                <div className="mt-1">
                  <div className="font-label-md font-bold text-on-surface">0041000332891 (VCB)</div>
                  <div className="text-[12px] text-on-surface-variant font-medium">CTY TNHH DU THUYỀN VÀ THỂ THAO BIỂN ĐÀ NẴNG</div>
                </div>
              </div>
            </div>
          </div>

          {/* Section: Period Selection Table */}
          <div className="flex flex-col gap-space-sm">
            <h2 className="font-headline-sm text-headline-sm font-bold text-on-surface">
              Lịch sử các kỳ đối soát doanh thu
            </h2>
            <div className="rounded-2xl bg-surface-container-lowest shadow-sm overflow-hidden border border-outline-variant/30">
              <table className="w-full text-left font-body-sm">
                <thead className="bg-surface-container-low font-label-sm text-on-surface-variant uppercase tracking-wider">
                  <tr>
                    <th className="py-3 px-space-lg">Mã kỳ đối soát</th>
                    <th className="py-3 px-space-md">Khoảng thời gian</th>
                    <th className="py-3 px-space-md text-right">Doanh thu gộp</th>
                    <th className="py-3 px-space-md text-right">Khấu trừ (10%)</th>
                    <th className="py-3 px-space-md text-right">Thực nhận</th>
                    <th className="py-3 px-space-md">Trạng thái</th>
                    <th className="py-3 px-space-lg text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/20">
                  {SETTLEMENT_PERIODS.map(period => (
                    <tr 
                      key={period.code} 
                      className={`hover:bg-surface-container-low/40 transition-colors ${selectedCycleCode === period.code ? 'bg-primary/5' : ''}`}
                    >
                      <td className="py-space-md px-space-lg font-bold text-primary">{period.code}</td>
                      <td className="py-space-md px-space-md text-on-surface-variant">{period.dateRange}</td>
                      <td className="py-space-md px-space-md text-right font-semibold">{period.grossAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-md text-right text-secondary font-semibold">-{period.commissionAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-md text-right font-bold text-on-surface">{period.netAmount.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-md">
                        {period.status === 'PAID' ? (
                          <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-[11px] font-bold">
                            ĐÃ THANH TOÁN
                          </span>
                        ) : (
                          <span className="px-2.5 py-1 rounded-full bg-amber-50 text-amber-700 font-label-sm text-[11px] font-bold">
                            CHỜ ĐỐI SOÁT
                          </span>
                        )}
                      </td>
                      <td className="py-space-md px-space-lg text-right">
                        <button 
                          onClick={() => {
                            setSelectedCycleCode(period.code);
                            showToast(`Đã chuyển xem chi tiết kỳ ${period.code}`);
                          }}
                          className={`px-3 py-1.5 rounded-lg font-label-sm font-bold transition-all ${
                            selectedCycleCode === period.code 
                              ? 'bg-primary text-on-primary' 
                              : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                          }`}
                        >
                          {selectedCycleCode === period.code ? 'Đang xem' : 'Xem bảng kê'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Section: Sub-orders Breakdown of Current Settlement */}
          <div className="flex flex-col gap-space-sm mt-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-md">
              <div>
                <div className="flex items-center gap-space-xs">
                  <h2 className="font-headline-sm text-headline-sm font-bold text-on-surface">
                    Bảng kê dịch vụ hoàn tất trong kỳ
                  </h2>
                  <span className="font-label-md text-label-md font-bold text-primary px-2 py-0.5 rounded bg-primary-fixed">
                    {currentCycle.code}
                  </span>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
                  Dữ liệu đồng bộ từ cổng check-in QR bến bãi VITA và kiểm định an toàn Cảng vụ
                </p>
              </div>

              <div className="relative w-full sm:w-72">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
                <input 
                  type="text"
                  placeholder="Tìm mã đơn hoặc khách..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-on-surface font-body-sm focus:outline-none focus:ring-2 focus:ring-primary shadow-sm"
                />
              </div>
            </div>

            <div
              key={selectedCycleCode}
              className="rounded-2xl bg-surface-container-lowest shadow-sm overflow-hidden border border-outline-variant/30 animate-fade-in-up"
            >
              <table className="w-full text-left font-body-sm">
                <thead className="bg-surface-container-low font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">
                  <tr>
                    <th className="py-3.5 px-space-lg">Mã Sub-order</th>
                    <th className="py-3.5 px-space-md">Dịch vụ trải nghiệm</th>
                    <th className="py-3.5 px-space-md">Khách hàng</th>
                    <th className="py-3.5 px-space-sm text-center">Số lượng</th>
                    <th className="py-3.5 px-space-md text-right">Đơn giá</th>
                    <th className="py-3.5 px-space-md text-right font-semibold text-on-surface">Tổng tiền (Gross)</th>
                    <th className="py-3.5 px-space-sm text-center">Tỷ lệ</th>
                    <th className="py-3.5 px-space-md text-right text-secondary font-semibold">Khấu trừ sàn</th>
                    <th className="py-3.5 px-space-lg text-right font-bold text-primary">Thực nhận</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/20">
                  {filteredItems.map(item => (
                    <tr key={item.id} className="hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-space-md px-space-lg font-semibold text-primary">{item.subOrderCode}</td>
                      <td className="py-space-md px-space-md">
                        <div className="font-semibold text-on-surface">{item.serviceName}</div>
                        <div className="text-on-surface-variant font-label-sm text-[12px] flex items-center gap-1 mt-0.5">
                          <span className="material-symbols-outlined text-[14px]">schedule</span>
                          {item.slotTime}
                        </div>
                      </td>
                      <td className="py-space-md px-space-md font-medium text-on-surface">{item.customerName}</td>
                      <td className="py-space-md px-space-sm text-center">
                        <span className="px-2 py-0.5 rounded-full bg-surface-container-high font-label-sm text-label-sm font-semibold">
                          {item.quantity} khách
                        </span>
                      </td>
                      <td className="py-space-md px-space-md text-right text-on-surface-variant">{item.unitPrice.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-md text-right font-semibold text-on-surface">{item.gross.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-sm text-center font-label-sm text-on-surface-variant">{item.rate}</td>
                      <td className="py-space-md px-space-md text-right font-medium text-secondary">-{item.commission.toLocaleString('vi-VN')} đ</td>
                      <td className="py-space-md px-space-lg text-right font-bold text-primary">{item.net.toLocaleString('vi-VN')} đ</td>
                    </tr>
                  ))}
                </tbody>
              </table>
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
    </main>
  );
}
