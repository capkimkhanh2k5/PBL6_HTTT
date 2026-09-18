import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';

type PeriodType = 'today' | '7days' | 'month' | 'custom';

export function AdminDashboard() {
  const navigate = useNavigate();
  const [period, setPeriod] = useState<PeriodType>('month');
  const [isAisModalOpen, setIsAisModalOpen] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [refreshToast, setRefreshToast] = useState(false);

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
      setRefreshToast(true);
      setTimeout(() => setRefreshToast(false), 3000);
    }, 600);
  };

  const periodMetrics = {
    today: {
      revenue: "38.900.000",
      revenueGrowth: "+8.4%",
      commission: "3.890.000",
      orders: "28",
      subOrders: "42",
      users: "2.418",
      peakDay: "Hôm nay: 38.9tr (198 khách)"
    },
    '7days': {
      revenue: "184.200.000",
      revenueGrowth: "+14.2%",
      commission: "18.420.000",
      orders: "135",
      subOrders: "208",
      users: "2.418",
      peakDay: "24/10: 42.1tr (210 khách)"
    },
    month: {
      revenue: "482.500.000",
      revenueGrowth: "+18.2%",
      commission: "48.250.000",
      orders: "368",
      subOrders: "542",
      users: "2.418",
      peakDay: "24/10: 42.1tr (210 khách)"
    },
    custom: {
      revenue: "720.000.000",
      revenueGrowth: "+22.5%",
      commission: "72.000.000",
      orders: "512",
      subOrders: "790",
      users: "2.418",
      peakDay: "Toàn mùa cao điểm"
    }
  };

  const currentStats = periodMetrics[period];

  return (
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full gap-space-xl animate-fade-in-up">
        {/* Toast Alert */}
        {refreshToast && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">Đã đồng bộ số liệu Cảng vụ Hải đăng thời gian thực!</span>
          </div>
        )}

        {/* BREADCRUMB & CONTEXT BANNER */}
        <div className="flex flex-col gap-space-xs">
          <div className="flex items-center gap-2 text-outline text-label-md font-label-md">
            <span className="hover:text-primary transition-colors cursor-pointer">Hệ thống Quản trị Cảng vụ</span>
            <span className="material-symbols-outlined text-[14px]">chevron_right</span>
            <span className="text-primary font-semibold">Trung tâm Điều hành &amp; Giám sát Tổng thể</span>
          </div>

          <div className="flex flex-col xl:flex-row xl:items-end justify-between gap-space-md pt-1">
            <div className="flex flex-col gap-1">
              <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black">
                Bảng Điều Hành Trung Tâm Du Lịch Biển Đà Nẵng
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl">
                Giám sát dòng tiền, an toàn luồng lạch, hồ sơ đối tác và chất lượng dịch vụ trên toàn bán đảo Sơn Trà &amp; bãi biển Mỹ Khê.
              </p>
            </div>

            {/* Top Filter & Live Marine Status Bar */}
            <div className="flex flex-wrap items-center gap-space-sm">
              {/* Live Weather / Sea condition badge */}
              <div 
                onClick={() => setIsAisModalOpen(true)}
                className="flex items-center gap-space-sm px-space-md py-2 rounded-xl bg-surface-container-low shadow-sm cursor-pointer hover:bg-surface-container transition-all"
                title="Bấm để mở radar AIS"
              >
                <div className="relative flex items-center justify-center">
                  <span className="w-2.5 h-2.5 rounded-full bg-primary animate-ping absolute"></span>
                  <span className="w-2.5 h-2.5 rounded-full bg-primary relative"></span>
                </div>
                <span className="material-symbols-outlined text-primary text-[20px]">waves</span>
                <div className="flex flex-col">
                  <span className="font-label-sm text-label-sm text-on-surface font-bold uppercase tracking-wider">Hải đồ an toàn</span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant">Sóng 0.6m • Gió 14km/h • 4/4 Bến xuất phát</span>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm ml-1 font-bold">CỜ XANH</span>
              </div>

              {/* Period Segmented Switcher */}
              <div className="flex items-center p-1 bg-surface-container rounded-xl shadow-sm">
                <button 
                  onClick={() => setPeriod('today')}
                  className={`px-3 py-1.5 rounded-lg font-label-md text-label-md transition-all ${period === 'today' ? 'bg-primary-container text-on-primary-container shadow-sm font-bold' : 'text-on-surface-variant hover:text-on-surface'}`}
                >
                  Hôm nay
                </button>
                <button 
                  onClick={() => setPeriod('7days')}
                  className={`px-3 py-1.5 rounded-lg font-label-md text-label-md transition-all ${period === '7days' ? 'bg-primary-container text-on-primary-container shadow-sm font-bold' : 'text-on-surface-variant hover:text-on-surface'}`}
                >
                  7 ngày qua
                </button>
                <button 
                  onClick={() => setPeriod('month')}
                  className={`px-3.5 py-1.5 rounded-lg font-label-md text-label-md transition-all ${period === 'month' ? 'bg-primary-container text-on-primary-container shadow-sm font-bold' : 'text-on-surface-variant hover:text-on-surface'}`}
                >
                  Tháng này (T10/2024)
                </button>
                <button 
                  onClick={() => setPeriod('custom')}
                  className={`px-3 py-1.5 rounded-lg font-label-md text-label-md flex items-center gap-1 transition-all ${period === 'custom' ? 'bg-primary-container text-on-primary-container shadow-sm font-bold' : 'text-on-surface-variant hover:text-on-surface'}`}
                >
                  <span className="material-symbols-outlined text-[16px]">calendar_today</span>
                  <span>Tùy chọn</span>
                </button>
              </div>

              {/* Refresh Action */}
              <button 
                onClick={handleRefresh}
                className={`w-10 h-10 rounded-xl bg-surface-container-low hover:bg-surface-container flex items-center justify-center text-on-surface-variant hover:text-primary transition-all shadow-sm ${isRefreshing ? 'animate-spin text-primary' : ''}`}
                title="Làm mới số liệu"
              >
                <span className="material-symbols-outlined text-[20px]">sync</span>
              </button>
            </div>
          </div>
        </div>

        {/* KPI SUMMARY ROW */}
        <div key={period} className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-space-lg animate-fade-in-up">
          {/* Card 1: Master Gross Volume */}
          <div className="flex flex-col justify-between p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all duration-300 relative overflow-hidden group">
            <div className="absolute -right-6 -bottom-6 w-28 h-28 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-colors"></div>
            <div className="flex items-center justify-between mb-space-md">
              <div className="w-11 h-11 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px]">payments</span>
              </div>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                <span className="material-symbols-outlined text-[14px]">trending_up</span>
                {currentStats.revenueGrowth}
              </span>
            </div>
            <div>
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider block mb-1">Tổng Doanh Thu Sàn</span>
              <div className="flex items-baseline gap-1">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">{currentStats.revenue}</span>
                <span className="font-label-md text-label-md text-on-surface-variant">đ</span>
              </div>
              <p className="font-body-sm text-body-sm text-outline mt-2 flex items-center gap-1">
                <span className="material-symbols-outlined text-[15px] text-primary">verified</span>
                Đã chốt COMPLETED/PAID (loại bỏ huỷ)
              </p>
            </div>
          </div>

          {/* Card 2: Port Commission */}
          <div className="flex flex-col justify-between p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all duration-300 relative overflow-hidden group">
            <div className="absolute -right-6 -bottom-6 w-28 h-28 bg-secondary-container/10 rounded-full blur-2xl group-hover:bg-secondary-container/20 transition-colors"></div>
            <div className="flex items-center justify-between mb-space-md">
              <div className="w-11 h-11 rounded-xl bg-secondary-container/20 text-secondary flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px]">account_balance_wallet</span>
              </div>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm font-semibold">
                Tỷ lệ: 10.0%
              </span>
            </div>
            <div>
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider block mb-1">Hoa Hồng Cảng Vụ (Thực thu)</span>
              <div className="flex items-baseline gap-1">
                <span className="font-headline-lg text-headline-lg font-bold text-secondary">{currentStats.commission}</span>
                <span className="font-label-md text-label-md text-on-surface-variant">đ</span>
              </div>
              <p className="font-body-sm text-body-sm text-outline mt-2 flex items-center gap-1">
                <span className="material-symbols-outlined text-[15px] text-secondary">pie_chart</span>
                Trích trực tiếp từ ví bảo chứng ký quỹ
              </p>
            </div>
          </div>

          {/* Card 3: Orders Count */}
          <div className="flex flex-col justify-between p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all duration-300 relative overflow-hidden group">
            <div className="absolute -right-6 -bottom-6 w-28 h-28 bg-primary-fixed/30 rounded-full blur-2xl transition-colors"></div>
            <div className="flex items-center justify-between mb-space-md">
              <div className="w-11 h-11 rounded-xl bg-tertiary-fixed text-tertiary flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px]">confirmation_number</span>
              </div>
              <span className="font-label-sm text-label-sm text-primary font-bold px-2 py-0.5 rounded-full bg-primary/10">
                {currentStats.subOrders} Dịch vụ con
              </span>
            </div>
            <div>
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider block mb-1">Tổng Đơn Đặt Tour (Master)</span>
              <div className="flex items-baseline gap-1">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">{currentStats.orders}</span>
                <span className="font-headline-sm text-headline-sm text-on-surface-variant">hợp đồng</span>
              </div>
              <p className="font-body-sm text-body-sm text-outline mt-2 flex items-center gap-1">
                <span className="material-symbols-outlined text-[15px] text-primary">pin_drop</span>
                Bán đảo Sơn Trà &amp; Biển Mỹ Khê
              </p>
            </div>
          </div>

          {/* Card 4: Ecosystem Users */}
          <div className="flex flex-col justify-between p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all duration-300 relative overflow-hidden group">
            <div className="absolute -right-6 -bottom-6 w-28 h-28 bg-primary/5 rounded-full blur-2xl transition-colors"></div>
            <div className="flex items-center justify-between mb-space-md">
              <div className="w-11 h-11 rounded-xl bg-primary-container/20 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px]">group</span>
              </div>
              <span className="flex items-center gap-1 font-label-sm text-label-sm text-on-surface-variant font-semibold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
                28 Đối tác mở bến
              </span>
            </div>
            <div>
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider block mb-1">Người Dùng Toàn Hệ Thống</span>
              <div className="flex items-baseline gap-1">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">{currentStats.users}</span>
                <span className="font-headline-sm text-headline-sm text-on-surface-variant">thành viên</span>
              </div>
              <p className="font-body-sm text-body-sm text-outline mt-2 flex items-center justify-between">
                <span>Khách: 2.385</span>
                <span>•</span>
                <span>Đối tác: 28</span>
                <span>•</span>
                <span>Cảng vụ: 5</span>
              </p>
            </div>
          </div>
        </div>

        {/* ACTION CENTER: Nhiệm Vụ Cần Thẩm Định & Xử Lý Gấp */}
        <div className="flex flex-col gap-space-md">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-secondary"></span>
              <h2 className="font-headline-sm text-headline-sm text-on-surface font-bold">Nhiệm Vụ Cần Thẩm Định &amp; Xử Lý Gấp</h2>
              <span className="px-2 py-0.5 rounded-full bg-secondary-container/20 text-secondary font-label-sm text-label-sm font-bold ml-1">17 việc đọng</span>
            </div>
            <button 
              onClick={() => navigate('/admin/vendor-approval')}
              className="font-label-md text-label-md text-primary hover:underline flex items-center gap-1"
            >
              <span>Xem tất cả luồng phê duyệt</span>
              <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-space-md">
            {/* Task 1: Partner Verification */}
            <div className="flex flex-col justify-between p-4 rounded-xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all group border border-outline-variant/30">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="p-2 rounded-lg bg-primary/10 text-primary">
                    <span className="material-symbols-outlined text-[20px]">corporate_fare</span>
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-secondary-container/15 text-secondary font-label-sm text-label-sm font-bold">Mới</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors font-bold">03 đối tác mới</h3>
                  <p className="font-body-sm text-body-sm text-outline-variant mt-0.5 line-clamp-2">Sơn Trà Marine Club, Da Nang Kayak Hub...</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/admin/vendor-approval')}
                className="mt-4 w-full py-2 px-3 rounded-lg bg-surface-container text-on-surface hover:bg-primary-container hover:text-on-primary font-label-md text-label-md transition-colors flex items-center justify-center gap-1.5 font-semibold"
              >
                <span>Thẩm định hồ sơ</span>
                <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
              </button>
            </div>

            {/* Task 2: Tour Safety Auditing */}
            <div className="flex flex-col justify-between p-4 rounded-xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all group border border-outline-variant/30">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="p-2 rounded-lg bg-surface-container text-primary">
                    <span className="material-symbols-outlined text-[20px]">scuba_diving</span>
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">An toàn biển</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors font-bold">05 dịch vụ tour</h3>
                  <p className="font-body-sm text-body-sm text-outline-variant mt-0.5 line-clamp-2">Lặn PADI sâu 12m Bãi Rạng, Lướt ván phản lực...</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/admin/service-approval')}
                className="mt-4 w-full py-2 px-3 rounded-lg bg-surface-container text-on-surface hover:bg-primary-container hover:text-on-primary font-label-md text-label-md transition-colors flex items-center justify-center gap-1.5 font-semibold"
              >
                <span>Kiểm duyệt ngay</span>
                <span className="material-symbols-outlined text-[16px]">verified_user</span>
              </button>
            </div>

            {/* Task 3: Customer Disputes */}
            <div className="flex flex-col justify-between p-4 rounded-xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all group border border-outline-variant/30">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="p-2 rounded-lg bg-secondary-container/20 text-secondary">
                    <span className="material-symbols-outlined text-[20px]">assignment_late</span>
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-secondary-container/20 text-secondary font-label-sm text-label-sm font-bold">Khẩn</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors font-bold">02 khiếu nại mở</h3>
                  <p className="font-body-sm text-body-sm text-outline-variant mt-0.5 line-clamp-2">Trễ giờ cano cao tốc tại Bến Bãi Bắc, lệch ca chiều</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/admin/disputes')}
                className="mt-4 w-full py-2 px-3 rounded-lg bg-surface-container text-on-surface hover:bg-secondary hover:text-on-secondary font-label-md text-label-md transition-colors flex items-center justify-center gap-1.5 font-semibold"
              >
                <span>Xem khiếu nại</span>
                <span className="material-symbols-outlined text-[16px]">visibility</span>
              </button>
            </div>

            {/* Task 4: Payout Disbursements */}
            <div className="flex flex-col justify-between p-4 rounded-xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all group border border-outline-variant/30">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="p-2 rounded-lg bg-surface-container text-tertiary">
                    <span className="material-symbols-outlined text-[20px]">account_balance</span>
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm font-bold">56.400.000 đ</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors font-bold">04 lệnh chi tiền</h3>
                  <p className="font-body-sm text-body-sm text-outline-variant mt-0.5 line-clamp-2">Yêu cầu thanh toán kỳ 15/10 cho 3 đối tác chèo thuyền</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/admin/payouts')}
                className="mt-4 w-full py-2 px-3 rounded-lg bg-surface-container text-on-surface hover:bg-primary hover:text-on-primary font-label-md text-label-md transition-colors flex items-center justify-center gap-1.5 font-semibold"
              >
                <span>Duyệt lệnh chi</span>
                <span className="material-symbols-outlined text-[16px]">check_circle</span>
              </button>
            </div>

            {/* Task 5: Refund Requests */}
            <div className="flex flex-col justify-between p-4 rounded-xl bg-surface-container-lowest shadow-sm hover:shadow-md transition-all group border border-outline-variant/30">
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="p-2 rounded-lg bg-surface-container text-on-surface-variant">
                    <span className="material-symbols-outlined text-[20px]">assignment_return</span>
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm font-bold">Chờ hoàn</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors font-bold">03 yêu cầu huỷ</h3>
                  <p className="font-body-sm text-body-sm text-outline-variant mt-0.5 line-clamp-2">Hủy trước 48h vì đổi lịch chuyến bay từ Hà Nội</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/admin/orders')}
                className="mt-4 w-full py-2 px-3 rounded-lg bg-surface-container text-on-surface hover:bg-primary-container hover:text-on-primary font-label-md text-label-md transition-colors flex items-center justify-center gap-1.5 font-semibold"
              >
                <span>Xử lý đơn hủy</span>
                <span className="material-symbols-outlined text-[16px]">price_check</span>
              </button>
            </div>
          </div>
        </div>

        {/* MAIN DUAL ANALYTICS LAYOUT */}
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-space-lg items-start">
          {/* KHỐI 1 (65% ~ 8 cols): Phân Bổ Doanh Thu & Lượt Khách */}
          <div className="xl:col-span-8 flex flex-col gap-space-lg">
            {/* Main Trend Chart Container */}
            <div className="p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-lg border border-outline-variant/20">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm">
                <div>
                  <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider font-bold">Thống kê vận hành</span>
                  <h3 className="font-headline-md text-headline-md text-on-surface font-bold mt-0.5">Biểu đồ Phân bổ Doanh thu &amp; Khách theo ngày</h3>
                </div>
                <div className="flex items-center gap-space-md text-body-sm font-body-sm">
                  <div className="flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-primary-container"></span>
                    <span className="text-on-surface-variant font-medium">Doanh thu (triệu đồng)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-secondary-container"></span>
                    <span className="text-on-surface-variant font-medium">Lượt du khách</span>
                  </div>
                </div>
              </div>

              {/* Inline Visual Chart */}
              <div className="w-full flex flex-col gap-2 pt-2">
                <div className="h-56 w-full flex items-end justify-between gap-2.5 px-2 relative">
                  {/* Grid Reference Lines */}
                  <div className="absolute inset-0 flex flex-col justify-between pointer-events-none opacity-25">
                    <div className="w-full h-px bg-outline-variant"></div>
                    <div className="w-full h-px bg-outline-variant"></div>
                    <div className="w-full h-px bg-outline-variant"></div>
                    <div className="w-full h-px bg-outline-variant"></div>
                  </div>
                  {/* SVG Trend Line Overlay */}
                  <svg className="absolute inset-0 w-full h-full pointer-events-none overflow-visible" preserveAspectRatio="none" viewBox="0 0 700 200">
                    <defs>
                      <linearGradient id="chartGradient" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0%" stopColor="#087F8C" stopOpacity="0.25" />
                        <stop offset="100%" stopColor="#087F8C" stopOpacity="0.0" />
                      </linearGradient>
                    </defs>
                    <path d="M 0 160 Q 60 140, 115 110 T 230 90 T 350 70 T 470 50 T 580 35 T 700 20 L 700 200 L 0 200 Z" fill="url(#chartGradient)" />
                    <path d="M 0 160 Q 60 140, 115 110 T 230 90 T 350 70 T 470 50 T 580 35 T 700 20" fill="none" stroke="#087F8C" strokeLinecap="round" strokeWidth="3" />
                  </svg>

                  {/* Interactive Bar Groups */}
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-20 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">14.2tr (78 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">01/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-24 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">18.5tr (94 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">04/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-32 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">24.0tr (125 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">08/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-28 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">21.8tr (110 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">12/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-40 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">31.2tr (160 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">16/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-36 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">28.4tr (145 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline">20/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-48 bg-primary-container rounded-t transition-all relative">
                      <div className="flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-primary text-on-primary rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md font-bold">Đỉnh: 42.1tr</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-primary font-bold">24/10</span>
                  </div>
                  <div className="flex-1 flex flex-col items-center gap-1.5 group z-10">
                    <div className="w-full max-w-[28px] h-44 bg-surface-container group-hover:bg-primary-container rounded-t transition-all relative">
                      <div className="hidden group-hover:flex absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-inverse-surface text-inverse-on-surface rounded text-label-sm font-label-sm whitespace-nowrap z-20 shadow-md">38.9tr (198 khách)</div>
                    </div>
                    <span className="text-label-sm font-label-sm text-outline font-semibold">Hôm nay</span>
                  </div>
                </div>
              </div>

              {/* Breakdown by Experience Category */}
              <div className="flex flex-col gap-space-sm pt-space-md bg-surface-container-low p-space-md rounded-xl">
                <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-bold">Cơ cấu doanh thu theo Danh mục trải nghiệm</span>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-space-md">
                  <div className="flex flex-col gap-1 p-space-sm rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-label-md text-label-md text-on-surface font-semibold">Chèo SUP &amp; Kayak</span>
                      <span className="font-label-sm text-label-sm text-primary font-bold">42%</span>
                    </div>
                    <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                      <div className="h-full bg-primary-container rounded-full" style={{ width: "42%" }}></div>
                    </div>
                    <span className="font-body-sm text-body-sm text-outline">202.650.000 đ (228 tour)</span>
                  </div>
                  <div className="flex flex-col gap-1 p-space-sm rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-label-md text-label-md text-on-surface font-semibold">Lặn ngắm San hô</span>
                      <span className="font-label-sm text-label-sm text-primary font-bold">35%</span>
                    </div>
                    <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                      <div className="h-full bg-primary rounded-full" style={{ width: "35%" }}></div>
                    </div>
                    <span className="font-body-sm text-body-sm text-outline">168.875.000 đ (142 tour)</span>
                  </div>
                  <div className="flex flex-col gap-1 p-space-sm rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-label-md text-label-md text-on-surface font-semibold">Cano &amp; Du thuyền</span>
                      <span className="font-label-sm text-label-sm text-tertiary font-bold">15%</span>
                    </div>
                    <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                      <div className="h-full bg-tertiary-container rounded-full" style={{ width: "15%" }}></div>
                    </div>
                    <span className="font-body-sm text-body-sm text-outline">72.375.000 đ (48 lượt)</span>
                  </div>
                  <div className="flex flex-col gap-1 p-space-sm rounded-lg bg-surface-container-lowest shadow-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-label-md text-label-md text-on-surface font-semibold">Dù lượn Biển Sơn Trà</span>
                      <span className="font-label-sm text-label-sm text-secondary font-bold">8%</span>
                    </div>
                    <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                      <div className="h-full bg-secondary-container rounded-full" style={{ width: "8%" }}></div>
                    </div>
                    <span className="font-body-sm text-body-sm text-outline">38.600.000 đ (31 lượt bay)</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Top Performing Operators Table */}
            <div className="p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-md border border-outline-variant/20">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">Top Đối Tác Doanh Thu &amp; Điểm An Toàn Biển</h3>
                  <p className="font-body-sm text-body-sm text-outline">Bảng xếp hạng năng lực tổ chức &amp; chấp hành quy chuẩn hàng hải TP. Đà Nẵng</p>
                </div>
                <button 
                  onClick={() => alert("Đang kết xuất báo cáo chuẩn Cảng vụ định dạng PDF...")}
                  className="px-3 py-1.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface-variant font-label-md text-label-md transition-colors flex items-center gap-1"
                >
                  <span className="material-symbols-outlined text-[16px]">picture_as_pdf</span>
                  <span>Xuất báo cáo PDF</span>
                </button>
              </div>

              <div className="w-full overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="text-outline text-label-sm font-label-sm uppercase tracking-wider bg-surface-container-low">
                      <th className="py-3 px-4 rounded-l-lg">Đối tác vận hành</th>
                      <th className="py-3 px-3">Khu vực khai thác</th>
                      <th className="py-3 px-3">Doanh số T10</th>
                      <th className="py-3 px-3">Hoa hồng trích nộp</th>
                      <th className="py-3 px-3">Chỉ số An toàn</th>
                      <th className="py-3 px-4 rounded-r-lg text-right">Tình trạng</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y-0 text-body-md font-body-md">
                    <tr className="hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3.5 px-4 flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold font-headline-sm text-headline-sm">
                          DO
                        </div>
                        <div>
                          <span className="font-label-lg text-label-lg font-bold text-on-surface block">Danang Ocean Club</span>
                          <span className="font-body-sm text-body-sm text-outline">Giấy phép #DN-VITA-8821</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-3 text-on-surface-variant font-body-sm text-body-sm">
                        Bến Sao Biển &amp; Bãi Rạng
                      </td>
                      <td className="py-3.5 px-3 font-bold text-on-surface">
                        146.200.000 đ
                      </td>
                      <td className="py-3.5 px-3 text-secondary font-semibold">
                        14.620.000 đ
                      </td>
                      <td className="py-3.5 px-3">
                        <div className="flex items-center gap-1 text-primary font-bold">
                          <span className="material-symbols-outlined text-[16px] text-secondary">star</span>
                          <span>4.9 / 5.0</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                          <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
                          Chuẩn Hàng Hải
                        </span>
                      </td>
                    </tr>

                    <tr className="hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3.5 px-4 flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-tertiary-fixed text-tertiary flex items-center justify-center font-bold font-headline-sm text-headline-sm">
                          ST
                        </div>
                        <div>
                          <span className="font-label-lg text-label-lg font-bold text-on-surface block">Son Tra Marine Diving</span>
                          <span className="font-body-sm text-body-sm text-outline">Giấy phép #DN-VITA-7712</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-3 text-on-surface-variant font-body-sm text-body-sm">
                        Vịnh Tiên Sa &amp; Bãi Nam
                      </td>
                      <td className="py-3.5 px-3 font-bold text-on-surface">
                        112.500.000 đ
                      </td>
                      <td className="py-3.5 px-3 text-secondary font-semibold">
                        11.250.000 đ
                      </td>
                      <td className="py-3.5 px-3">
                        <div className="flex items-center gap-1 text-primary font-bold">
                          <span className="material-symbols-outlined text-[16px] text-secondary">star</span>
                          <span>4.8 / 5.0</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                          <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
                          Chuẩn Hàng Hải
                        </span>
                      </td>
                    </tr>

                    <tr className="hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3.5 px-4 flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-secondary-container/20 text-secondary flex items-center justify-center font-bold font-headline-sm text-headline-sm">
                          SF
                        </div>
                        <div>
                          <span className="font-label-lg text-label-lg font-bold text-on-surface block">SkyFly Danang Paragliding</span>
                          <span className="font-body-sm text-body-sm text-outline">Giấy phép #DN-AERO-0941</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-3 text-on-surface-variant font-body-sm text-body-sm">
                        Đỉnh Sơn Trà &amp; Bãi Đa
                      </td>
                      <td className="py-3.5 px-3 font-bold text-on-surface">
                        68.000.000 đ
                      </td>
                      <td className="py-3.5 px-3 text-secondary font-semibold">
                        6.800.000 đ
                      </td>
                      <td className="py-3.5 px-3">
                        <div className="flex items-center gap-1 text-primary font-bold">
                          <span className="material-symbols-outlined text-[16px] text-secondary">star</span>
                          <span>4.9 / 5.0</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                          <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
                          Chuẩn Hàng Hải
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* KHỐI 2 (35% ~ 4 cols): Nhật Ký Điều Hành & Cảnh Báo Real-time */}
          <div className="xl:col-span-4 flex flex-col gap-space-lg">
            {/* Maritime Safety Live Stream Card */}
            <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-md border border-outline-variant/20">
              <div className="flex items-center justify-between pb-space-xs">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-3 w-3">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-secondary opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-3 w-3 bg-secondary"></span>
                  </span>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">Nhật Ký Điều Hành Real-time</h3>
                </div>
                <span className="font-label-sm text-label-sm text-outline">Cập nhật 1 phút trước</span>
              </div>

              {/* Port dispatch stream timeline */}
              <div className="flex flex-col gap-space-md relative pl-4">
                <div className="absolute left-1.5 top-2 bottom-4 w-0.5 bg-surface-container-high"></div>

                {/* Event 1: Weather Warning */}
                <div className="flex flex-col gap-1 relative pl-4">
                  <span className="absolute left-[-11px] top-1 w-3 h-3 rounded-full bg-secondary ring-4 ring-surface-container-lowest"></span>
                  <div className="flex items-center justify-between">
                    <span className="px-2 py-0.5 rounded bg-secondary-container/25 text-on-secondary-container font-label-sm text-label-sm font-bold">
                      CẢNH BÁO GIÓ GIẬT CỤC BỘ
                    </span>
                    <span className="font-label-sm text-label-sm text-outline">14:00</span>
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface mt-1">
                    Phát hiện luồng gió xoáy cục bộ cấp 4 tại bến <span className="font-semibold text-primary">Bãi Bụt</span>. Hệ thống tự động kích hoạt Cờ Vàng cảnh báo hạn chế cano cao tốc nhỏ.
                  </p>
                  <div className="mt-1 flex items-center gap-2">
                    <span className="font-label-sm text-label-sm text-secondary flex items-center gap-1 font-semibold">
                      <span className="material-symbols-outlined text-[15px]">flag</span>
                      Đã gửi SMS tới 6 thuyền trưởng
                    </span>
                  </div>
                </div>

                {/* Event 2: Ticket QR Check-in Success */}
                <div className="flex flex-col gap-1 relative pl-4">
                  <span className="absolute left-[-11px] top-1 w-3 h-3 rounded-full bg-primary ring-4 ring-surface-container-lowest"></span>
                  <div className="flex items-center justify-between">
                    <span className="px-2 py-0.5 rounded bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                      XÁC THỰC VÉ QR THÀNH CÔNG
                    </span>
                    <span className="font-label-sm text-label-sm text-outline">13:42</span>
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface mt-1">
                    Cổng soát vé Bến Sao Biển ghi nhận <span className="font-semibold text-on-surface">28 khách</span> check-in tour Chèo SUP đón hoàng hôn (Mã lô #DN-SUP-08).
                  </p>
                  <span className="font-body-sm text-body-sm text-outline">Thiết bị quét: Trạm kiểm soát Barcode 02</span>
                </div>

                {/* Event 3: License Document updated */}
                <div className="flex flex-col gap-1 relative pl-4">
                  <span className="absolute left-[-11px] top-1 w-3 h-3 rounded-full bg-tertiary-container ring-4 ring-surface-container-lowest"></span>
                  <div className="flex items-center justify-between">
                    <span className="px-2 py-0.5 rounded bg-surface-container text-tertiary font-label-sm text-label-sm font-bold">
                      HỒ SƠ ĐĂNG KIỂM TÀU
                    </span>
                    <span className="font-label-sm text-label-sm text-outline">12:15</span>
                  </div>
                  <p className="font-body-sm text-body-sm text-on-surface mt-1">
                    Đối tác <span className="font-semibold text-primary">Danang Ocean Club</span> vừa cập nhật giấy kiểm định cano kéo phao cứu hộ số ĐK-43-9821 định kỳ 6 tháng.
                  </p>
                  <button 
                    onClick={() => navigate('/admin/vendor-approval')}
                    className="text-label-sm font-label-sm text-primary hover:underline flex items-center gap-1 mt-0.5 w-fit"
                  >
                    <span>Kiểm tra chữ ký số Cục Hàng Hải</span>
                    <span className="material-symbols-outlined text-[14px]">open_in_new</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Quick Geographic Radar Card */}
            <div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-md border border-outline-variant/20">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-[22px]">explore</span>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">Vùng Trải Nghiệm Hoạt Động</h3>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold">4 VÙNG MỞ</span>
              </div>

              {/* Geographic map view */}
              <div 
                className="w-full h-44 rounded-xl bg-cover bg-center relative overflow-hidden flex flex-col justify-between p-3" 
                style={{ backgroundImage: "url('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop&q=80')" }}
              >
                <div className="flex justify-between items-start">
                  <span className="px-2.5 py-1 rounded-md bg-inverse-surface/80 backdrop-blur-md text-on-primary text-label-sm font-label-sm flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-primary"></span>
                    GPS Bán Đảo Sơn Trà &amp; Mỹ Khê
                  </span>
                  <span className="px-2 py-0.5 rounded bg-surface/90 text-on-surface text-label-sm font-label-sm font-bold shadow-sm">
                    Live AIS
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-inverse-surface/85 backdrop-blur-md text-on-primary flex items-center justify-between">
                  <div className="flex flex-col">
                    <span className="text-label-sm font-label-sm text-outline-variant">Tàu/Cano đang vận hành</span>
                    <span className="font-headline-sm text-headline-sm font-bold text-inverse-primary">42 phương tiện</span>
                  </div>
                  <button 
                    onClick={() => setIsAisModalOpen(true)}
                    className="px-2.5 py-1 rounded bg-primary text-on-primary text-label-sm font-label-sm font-semibold hover:bg-primary-container transition-colors"
                  >
                    Mở Radar AIS
                  </button>
                </div>
              </div>

              {/* Port status mini indicators */}
              <div className="flex flex-col gap-2 pt-1">
                <div className="flex items-center justify-between py-1.5 px-3 rounded-lg bg-surface-container-low">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-primary"></span>
                    <span className="font-body-sm text-body-sm font-semibold text-on-surface">Bến Thuyền Sao Biển</span>
                  </div>
                  <span className="font-label-sm text-label-sm text-primary font-bold">14 ca xuất bến</span>
                </div>
                <div className="flex items-center justify-between py-1.5 px-3 rounded-lg bg-surface-container-low">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-primary"></span>
                    <span className="font-body-sm text-body-sm font-semibold text-on-surface">Khu Du Lịch Bãi Rạng</span>
                  </div>
                  <span className="font-label-sm text-label-sm text-primary font-bold">19 ca xuất bến</span>
                </div>
                <div className="flex items-center justify-between py-1.5 px-3 rounded-lg bg-surface-container-low">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-secondary"></span>
                    <span className="font-body-sm text-body-sm font-semibold text-on-surface">Bến Bãi Bụt (Cờ Vàng)</span>
                  </div>
                  <span className="font-label-sm text-label-sm text-secondary font-bold">06 ca xuất bến</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* AIS RADAR MODAL (Portaled to body) */}
      {isAisModalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-scrim/60 backdrop-blur-sm animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl w-full max-w-4xl p-6 shadow-2xl border border-outline-variant flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">radar</span>
                <div>
                  <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">Hải Đồ Giám Sát AIS Tàu Du Lịch Biển Đà Nẵng</h3>
                  <p className="font-body-sm text-body-sm text-outline">Hệ thống định vị vệ tinh AIS &amp; phao giới hạn luồng lạch Sơn Trà</p>
                </div>
              </div>
              <button 
                onClick={() => setIsAisModalOpen(false)}
                className="w-9 h-9 rounded-full bg-surface-container hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="relative w-full h-80 rounded-xl overflow-hidden bg-inverse-surface flex items-center justify-center border border-outline-variant">
              {/* Radar Rings & Scan Line */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="w-72 h-72 rounded-full border border-primary/20 animate-pulse"></div>
                <div className="w-48 h-48 rounded-full border border-primary/30"></div>
                <div className="w-24 h-24 rounded-full border border-primary/40"></div>
                <div className="w-full h-px bg-primary/20 absolute"></div>
                <div className="h-full w-px bg-primary/20 absolute"></div>
              </div>

              {/* Vessels on Map */}
              <div className="absolute top-1/4 left-1/3 flex items-center gap-1 bg-surface-container-lowest/90 px-2 py-1 rounded-md text-xs font-bold text-on-surface shadow">
                <span className="w-2 h-2 rounded-full bg-primary animate-ping"></span>
                <span>Cano DN-4318 (Sơn Trà)</span>
              </div>
              <div className="absolute bottom-1/3 right-1/4 flex items-center gap-1 bg-surface-container-lowest/90 px-2 py-1 rounded-md text-xs font-bold text-on-surface shadow">
                <span className="w-2 h-2 rounded-full bg-primary animate-ping"></span>
                <span>Tàu HQ-04 (Tuần tra)</span>
              </div>
              <div className="absolute top-1/2 right-1/3 flex items-center gap-1 bg-secondary-container text-on-secondary-container px-2 py-1 rounded-md text-xs font-bold shadow">
                <span className="w-2 h-2 rounded-full bg-secondary"></span>
                <span>SUP Lô #08 (Bãi Rạng)</span>
              </div>

              <span className="absolute bottom-3 left-3 text-xs text-outline-variant bg-inverse-surface/80 px-2 py-1 rounded">
                Tọa độ trung tâm: 16.1132° N, 108.2815° E • Độ trễ: 1.2s
              </span>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button 
                onClick={() => setIsAisModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-primary text-on-primary font-label-md text-label-md font-semibold hover:bg-primary-container transition-colors"
              >
                Đóng màn hình Radar
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}
export default AdminDashboard;
