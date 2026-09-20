import { Outlet, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";

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

  useEffect(() => {
    if (location.pathname !== prevPath) {
      const prevOrder = ADMIN_PAGE_ORDER[prevPath] ?? 0;
      const currentOrder = ADMIN_PAGE_ORDER[location.pathname] ?? 0;

      if (currentOrder > prevOrder) {
        setAnimationClass("animate-slide-in-right");
      } else if (currentOrder < prevOrder) {
        setAnimationClass("animate-slide-in-left");
      } else {
        setAnimationClass("animate-fade-in-up");
      }
      setPrevPath(location.pathname);
    }
  }, [location.pathname, prevPath]);

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
      <aside className="fixed left-0 top-0 h-screen w-[260px] bg-inverse-surface z-50 flex flex-col justify-between shadow-[0_1px_8px_rgba(0,0,0,0.04)] select-none">
        <div className="flex flex-col flex-1 overflow-y-auto scrollbar-none">
          {/* Brand Header */}
          <div className="p-space-lg flex flex-col gap-space-xs border-b border-surface-variant/10">
            <div
              className="flex items-center gap-space-sm cursor-pointer"
              onClick={() => navigate("/admin")}
            >
              <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-on-primary font-black text-base shadow-sm">
                D
              </div>
              <span className="font-headline-sm text-headline-sm font-bold tracking-tight text-on-primary">
                DANASEA
              </span>
            </div>
            <div className="mt-space-xs">
              <span className="inline-flex items-center px-space-xs py-0.5 rounded bg-primary-container/20 text-inverse-primary font-label-sm text-[10px] uppercase font-bold tracking-wider">
                HỆ THỐNG QUẢN TRỊ CẢNG VỤ
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
                      ? "bg-primary-container text-on-primary-container font-bold shadow-sm"
                      : "text-outline-variant hover:bg-surface-variant/20 hover:text-on-primary"
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
                  <span className="px-1.5 py-0.2 rounded-full bg-secondary-container text-on-secondary-container text-[10px] font-bold">
                    {item.badge}
                  </span>
                )}
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Footer Admin Profile Card */}
        <div className="p-space-md bg-inverse-surface border-t border-surface-variant/10">
          <div className="flex items-center gap-space-sm p-space-sm rounded-xl bg-surface-variant/10 text-on-primary">
            <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-on-primary font-bold text-label-md shrink-0">
              <span className="material-symbols-outlined text-[20px]">person</span>
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <span className="font-label-md text-label-md text-on-primary font-bold truncate">
                Trần Hải Đăng
              </span>
              <span className="font-label-sm text-[10px] text-outline-variant truncate">
                Ban QL Cảng VITA • ADMIN
              </span>
            </div>
            <button
              onClick={() => navigate("/admin/settings")}
              className="text-outline-variant hover:text-on-primary transition-colors p-1"
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
        <header className="fixed top-0 left-[260px] right-0 h-16 bg-surface/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-40 flex items-center justify-between px-space-xl border-b border-outline-variant/30">
          <div className="flex items-center gap-space-lg flex-1 max-w-xl">
            <div className="relative w-full max-w-md">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[20px]">
                search
              </span>
              <input
                value={searchGlobal}
                onChange={(e) => setSearchGlobal(e.target.value)}
                className="w-full h-10 pl-10 pr-space-md rounded-xl bg-surface-container-lowest text-on-surface font-body-sm text-body-sm placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary shadow-sm border border-outline-variant/30"
                placeholder="Tìm kiếm hồ sơ, đơn vị vận hành, mã tour..."
                type="text"
              />
            </div>
            <div className="hidden xl:flex items-center gap-2 px-space-sm py-1.5 rounded-full bg-surface-container border border-outline-variant/20">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="font-label-sm text-[11px] text-on-surface-variant font-medium">
                Kết nối CSDL Hàng hải 100%
              </span>
            </div>
          </div>

          <div className="flex items-center gap-space-md">
            {/* Quick Urgent Count Pill */}
            <div
              onClick={() => navigate("/admin/vendor-approval")}
              className="hidden md:flex items-center gap-2 px-space-md py-1.5 rounded-lg bg-secondary-container/20 text-on-secondary-container font-label-md text-label-md cursor-pointer hover:bg-secondary-container/30 transition-colors"
            >
              <span className="material-symbols-outlined text-secondary text-[18px]">
                notification_important
              </span>
              <span className="font-bold">8 Cần duyệt</span>
            </div>

            {/* Notification Bell Dropdown Toggle */}
            <div className="relative">
              <button
                onClick={() => setIsNotiOpen(!isNotiOpen)}
                className="relative w-10 h-10 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-colors"
              >
                <span className="material-symbols-outlined text-[22px]">
                  notifications
                </span>
                <span className="absolute top-2 right-2 w-2.5 h-2.5 bg-secondary rounded-full animate-ping"></span>
                <span className="absolute top-2 right-2 w-2.5 h-2.5 bg-secondary rounded-full"></span>
              </button>

              {isNotiOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-surface-container-lowest rounded-2xl shadow-xl border border-outline-variant/30 p-space-sm z-50 animate-in fade-in zoom-in-95">
                  <div className="flex items-center justify-between p-2 border-b border-outline-variant/30">
                    <span className="font-label-md font-bold text-on-surface">Thông báo khẩn Cảng vụ</span>
                    <span className="px-1.5 py-0.5 rounded bg-primary/10 text-primary text-[10px] font-bold">3 mới</span>
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
