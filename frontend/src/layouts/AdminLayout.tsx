import { Outlet, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";

const ADMIN_PAGE_ORDER: Record<string, number> = {
  "/admin": 0,
  "/admin/users": 1,
  "/admin/vendor-approval": 2,
  "/admin/service-approval": 3,
  "/admin/categories": 4,
  "/admin/orders": 5,
  "/admin/disputes": 6,
  "/admin/promotions": 7,
  "/admin/payouts": 8,
  "/admin/settings": 9,
  "/admin/logs": 10,
};

export function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [prevPath, setPrevPath] = useState(location.pathname);
  const [animationClass, setAnimationClass] = useState("animate-fade-in-up");
  const [searchGlobal, setSearchGlobal] = useState("");
  const [isNotiOpen, setIsNotiOpen] = useState(false);

  if (location.pathname !== prevPath) {
    const prevOrder = ADMIN_PAGE_ORDER[prevPath] ?? 0;
    const currentOrder = ADMIN_PAGE_ORDER[location.pathname] ?? 0;
    setPrevPath(location.pathname);
    if (currentOrder > prevOrder) {
      setAnimationClass("animate-slide-in-right");
    } else if (currentOrder < prevOrder) {
      setAnimationClass("animate-slide-in-left");
    } else {
      setAnimationClass("animate-fade-in-up");
    }
  }

  const navItems = [
    { to: "/admin", end: true, icon: "dashboard", label: "Tổng quan" },
    { to: "/admin/users", icon: "manage_accounts", label: "Người dùng & Vai trò" },
    { to: "/admin/vendor-approval", icon: "corporate_fare", label: "Đối tác vận hành", badge: "3" },
    { to: "/admin/service-approval", icon: "fact_check", label: "Dịch vụ & Duyệt tour", badge: "5" },
    { to: "/admin/categories", icon: "kayaking", label: "Danh mục trải nghiệm" },
    { to: "/admin/orders", icon: "receipt_long", label: "Đơn & Thanh toán" },
    { to: "/admin/disputes", icon: "assignment_return", label: "Hoàn tiền & Khiếu nại", badge: "2" },
    { to: "/admin/promotions", icon: "sell", label: "Khuyến mãi toàn sàn" },
    { to: "/admin/payouts", icon: "account_balance", label: "Đối soát & Quyết toán", badge: "1" },
    { to: "/admin/settings", icon: "shield", label: "Cấu hình & An toàn biển" },
    { to: "/admin/logs", icon: "history_toggle_off", label: "Nhật ký hệ thống" },
  ];

  return (
    <div className="bg-surface font-body-md text-on-surface min-h-screen flex antialiased selection:bg-primary-container selection:text-on-primary-container">
      {/* Fixed Admin Sidebar (260px) */}
      <aside className="fixed left-0 top-0 h-screen w-[260px] bg-white border-r border-slate-200 z-50 flex flex-col justify-between shadow-xs select-none">
        <div className="flex flex-col flex-1 overflow-y-auto scrollbar-none">
          {/* Brand Header */}
          <div className="p-space-lg flex flex-col gap-space-xs border-b border-slate-200">
            <div
              className="flex items-center gap-space-sm cursor-pointer"
              onClick={() => navigate("/admin")}
            >
              <img
                src="/images/danasea-logo.png"
                alt="DANASEA"
                className="w-8 h-8 rounded-lg object-contain shrink-0"
              />
              <span className="font-headline-sm text-headline-sm font-bold tracking-tight text-primary">
                DANASEA
              </span>
            </div>
            <div className="mt-space-xs">
              <span className="inline-flex items-center px-2 py-0.5 rounded bg-primary/10 text-primary font-label-sm text-[11px] font-semibold">
                Quản trị Sàn DANASEA
              </span>
            </div>
          </div>

          {/* Nav Items List */}
          <nav className="flex-1 px-space-md py-space-xs flex flex-col gap-1 mt-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center justify-between px-space-md py-2.5 rounded-xl font-label-md text-[13px] transition-all ${
                    isActive
                      ? "bg-primary text-white font-bold shadow-xs"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  }`
                }
              >
                <div className="flex items-center gap-space-sm">
                  <span className="material-symbols-outlined text-[20px]">
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                      location.pathname === item.to || (item.to !== "/admin" && location.pathname.startsWith(item.to))
                        ? "bg-white/20 text-white"
                        : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Footer Admin Profile Card */}
        <div className="p-space-md bg-white border-t border-slate-200">
          <div className="flex items-center gap-space-sm p-space-sm rounded-xl bg-slate-50 border border-slate-200 text-slate-800">
            <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-white font-bold text-label-md shrink-0 shadow-xs">
              <span className="material-symbols-outlined text-[20px]">person</span>
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <span className="font-label-md text-label-md text-slate-900 font-bold truncate">
                Trần Hải Đăng
              </span>
              <span className="font-label-sm text-[10px] text-slate-500 truncate">
                Ban Quản trị Sàn • Admin
              </span>
            </div>
            <button
              onClick={() => navigate("/admin/settings")}
              className="text-slate-400 hover:text-primary transition-colors p-1 cursor-pointer"
              title="Cài đặt hệ thống"
            >
              <span className="material-symbols-outlined text-[18px]">settings</span>
            </button>
          </div>
        </div>
      </aside>

      {/* Main Wrapper with Left Margin 260px */}
      <div className="pl-[260px] flex-1 flex flex-col min-w-0 min-h-screen">
        {/* Top Sticky Header */}
        <header className="fixed top-0 left-[260px] right-0 h-16 bg-white/90 backdrop-blur-xl shadow-xs z-40 flex items-center justify-between px-space-xl border-b border-slate-200">
          <div className="flex items-center gap-space-lg flex-1 max-w-xl">
            <div className="relative w-full max-w-md">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">
                search
              </span>
              <input
                value={searchGlobal}
                onChange={(e) => setSearchGlobal(e.target.value)}
                className="w-full h-10 pl-10 pr-space-md rounded-lg bg-slate-50 text-slate-800 font-body-sm text-body-sm placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-primary shadow-xs border border-slate-200"
                placeholder="Tìm kiếm hồ sơ, đơn vị vận hành, mã tour..."
                type="text"
              />
            </div>
          </div>

          <div className="flex items-center gap-space-md">
            {/* Quick Urgent Count Pill */}
            <div
              onClick={() => navigate("/admin/vendor-approval")}
              className="hidden md:flex items-center gap-2 px-space-md py-1.5 rounded-lg bg-amber-50 text-amber-800 border border-amber-200 font-label-md text-label-md cursor-pointer hover:bg-amber-100 transition-colors shadow-xs"
            >
              <span className="material-symbols-outlined text-amber-600 text-[18px]">
                notification_important
              </span>
              <span className="font-bold text-xs">8 cần duyệt</span>
            </div>

            {/* Notification Bell Dropdown Toggle */}
            <div className="relative">
              <button
                onClick={() => setIsNotiOpen(!isNotiOpen)}
                className="relative w-10 h-10 rounded-full flex items-center justify-center text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer"
              >
                <span className="material-symbols-outlined text-[22px]">
                  notifications
                </span>
                <span className="absolute top-2 right-2 w-2.5 h-2.5 bg-amber-500 rounded-full animate-ping"></span>
                <span className="absolute top-2 right-2 w-2.5 h-2.5 bg-amber-500 rounded-full"></span>
              </button>

              {isNotiOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 p-space-sm z-50 animate-in fade-in zoom-in-95">
                  <div className="flex items-center justify-between p-2 border-b border-slate-100">
                    <span className="font-label-md font-bold text-slate-900 text-xs">Thông báo vận hành hệ thống</span>
                    <span className="px-1.5 py-0.5 rounded bg-primary/10 text-primary text-[10px] font-bold">3</span>
                  </div>
                  <div className="flex flex-col gap-1 py-1 max-h-72 overflow-y-auto">
                    <div
                      onClick={() => { setIsNotiOpen(false); navigate("/admin/vendor-approval"); }}
                      className="p-2.5 rounded-xl hover:bg-surface-container-low cursor-pointer transition-colors flex flex-col gap-0.5"
                    >
                      <span className="font-label-sm font-bold text-primary">Sơn Trà Diving nộp lại chứng chỉ</span>
                      <span className="text-[11px] text-on-surface-variant">Giấy phép cứu hộ PADI vừa tải lên bổ sung</span>
                    </div>
                    <div
                      onClick={() => { setIsNotiOpen(false); navigate("/admin/disputes"); }}
                      className="p-2.5 rounded-xl hover:bg-surface-container-low cursor-pointer transition-colors flex flex-col gap-0.5"
                    >
                      <span className="font-label-sm font-bold text-secondary">Khiếu nại mới đơn #SUB-89412-01</span>
                      <span className="text-[11px] text-on-surface-variant">Khách yêu cầu hoàn tiền do sóng biển cao</span>
                    </div>
                    <div
                      onClick={() => { setIsNotiOpen(false); navigate("/admin/payouts"); }}
                      className="p-2.5 rounded-xl hover:bg-surface-container-low cursor-pointer transition-colors flex flex-col gap-0.5"
                    >
                      <span className="font-label-sm font-bold text-on-surface">Yêu cầu rút tiền 12.780.000 đ</span>
                      <span className="text-[11px] text-on-surface-variant">Danang Ocean Club yêu cầu thanh toán VCB</span>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Admin Avatar */}
            <div
              onClick={() => navigate("/admin/users")}
              className="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-on-primary cursor-pointer shadow-sm hover:scale-105 transition-transform"
              title="Quản trị viên hệ thống"
            >
              <span className="material-symbols-outlined text-[18px]">person</span>
            </div>
          </div>
        </header>

        {/* Dynamic Outlet with Directional Slide Transitions */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden">
          <div
            key={location.pathname}
            onAnimationEnd={() => setAnimationClass("")}
            className={`min-h-full flex flex-col ${animationClass}`}
          >
            <Outlet />
          </div>
        </div>
      </div>
    </div>
  );
}
