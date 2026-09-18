import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../store/useAuthStore";
import { MOCK_TEST_ACCOUNTS } from "../../../mockData";

export function Auth() {
  const [activeTab, setActiveTab] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [banner, setBanner] = useState<string | null>(null);
  
  const navigate = useNavigate();
  const login = useAuthStore(state => state.login);

  const handleQuickLogin = (roleKey: 'customer' | 'vendor' | 'admin') => {
    const target = MOCK_TEST_ACCOUNTS[roleKey];
    login(target);
    if (roleKey === 'admin') {
      navigate('/admin');
    } else if (roleKey === 'vendor') {
      navigate('/vendor');
    } else {
      navigate('/');
    }
  };

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setBanner('wrong_creds');
      return;
    }
    
    const lower = email.trim().toLowerCase();
    if (lower.includes('admin') || lower === 'admin') {
      login(MOCK_TEST_ACCOUNTS.admin);
      navigate('/admin');
    } else if (lower.includes('vendor') || lower === 'vendor') {
      login(MOCK_TEST_ACCOUNTS.vendor);
      navigate('/vendor');
    } else {
      login(MOCK_TEST_ACCOUNTS.customer);
      navigate('/');
    }
  };

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Simulate register
    setBanner('register_error');
  };

  return (
    <main className="w-full min-h-screen pt-20 bg-surface flex flex-col justify-center items-center">
      <div className="flex flex-col w-full">
        <div className="relative w-full overflow-hidden py-space-2xl px-space-lg lg:px-margin flex justify-center items-center">
          <div className="absolute -top-40 -left-20 w-96 h-96 rounded-full bg-primary-fixed-dim/20 blur-3xl pointer-events-none"></div>
          <div className="absolute top-1/2 -right-32 w-[30rem] h-[30rem] rounded-full bg-secondary-container/10 blur-3xl pointer-events-none"></div>
          
          <div className="relative w-full max-w-[1280px] grid grid-cols-1 lg:grid-cols-12 gap-gutter-lg items-center z-10">
            <div className="lg:col-span-5 flex flex-col space-y-space-xl">
              <div className="inline-flex items-center gap-space-xs px-space-md py-1.5 rounded-full bg-surface-container-low text-primary w-fit shadow-sm">
                <span className="material-symbols-outlined text-headline-sm" style={{ fontVariationSettings: "'FILL' 1" }}>sailing</span>
                <span className="font-label-sm text-label-sm uppercase tracking-wider">Nền tảng trải nghiệm &amp; thể thao biển Đà Nẵng</span>
              </div>
              <div className="space-y-space-sm">
                <h1 className="font-headline-xl text-headline-xl text-on-surface leading-tight">
                  Khám phá đại dương cùng <span className="text-primary-container">DANASEA</span>
                </h1>
                <p className="font-body-lg text-body-lg text-on-surface-variant max-w-md">Cổng xác thực tập trung dành cho du khách trải nghiệm cano, lặn biển ngắm san hô Bán đảo Sơn Trà và nền tảng trải nghiệm &amp; thể thao biển Đà Nẵng.</p>
              </div>
              
              <div className="space-y-space-md">
                <div className="flex items-start gap-space-md p-space-md rounded-2xl bg-surface-container-low transition-all duration-200 hover:bg-surface-container hover:shadow-sm">
                  <div className="flex-shrink-0 w-11 h-11 rounded-xl bg-primary-container/10 flex items-center justify-center text-primary-container">
                    <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>scuba_diving</span>
                  </div>
                  <div className="space-y-0.5">
                    <h2 className="font-headline-sm text-headline-sm text-on-surface">Đặt trải nghiệm biển chính thống</h2>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">Lặn ngắm san hô Bãi Bụt, bay dù lượn Mỹ Khê, tour cano cao cấp đạt chuẩn cứu hộ quốc tế.</p>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="lg:col-span-7 flex justify-center">
              <div className="w-full max-w-xl bg-surface-container-lowest rounded-[24px] shadow-xl p-space-xl sm:p-space-2xl space-y-space-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-xl bg-primary-container flex items-center justify-center text-on-primary shadow-sm">
                      <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>waves</span>
                    </div>
                    <span className="font-headline-md text-headline-md text-on-surface tracking-tight">DANA<span className="text-primary-container">SEA</span></span>
                  </div>
                  <span className="font-label-sm text-label-sm text-on-surface-variant bg-surface-container-high px-space-sm py-1 rounded-full uppercase">
                    Cổng Tài Khoản
                  </span>
                </div>
                
                <div className="relative p-1 bg-surface-container rounded-2xl flex items-center">
                  <button className={`relative z-10 w-1/2 py-2.5 rounded-xl font-label-lg text-label-lg transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "login" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} onClick={() => setActiveTab("login")}>
                    <span className="material-symbols-outlined text-body-lg">login</span>
                    Đăng nhập
                  </button>
                  <button className={`relative z-10 w-1/2 py-2.5 rounded-xl font-label-lg text-label-lg transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "register" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} onClick={() => setActiveTab("register")}>
                    <span className="material-symbols-outlined text-body-lg">person_add</span>
                    Tạo tài khoản mới
                  </button>
                </div>
                
                {activeTab === "login" && (
                  <div className="space-y-space-lg block">
                    <div className="space-y-space-sm">
                      {banner === 'locked' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container">
                          <p className="font-label-lg text-label-lg font-bold">Tài khoản tạm thời bị khóa</p>
                        </div>
                      )}
                      {banner === 'wrong_creds' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container">
                          <p className="font-body-sm text-body-sm font-medium">Email hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại thông tin hoặc đặt lại mật khẩu.</p>
                        </div>
                      )}
                    </div>

                    {/* Quick Demo Role Selector */}
                    <div className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 space-y-2.5">
                      <div className="flex items-center justify-between">
                        <span className="font-label-sm text-label-sm font-bold text-on-surface uppercase tracking-wider flex items-center gap-1.5">
                          <span className="material-symbols-outlined text-[16px] text-secondary">flash_on</span>
                          Tài khoản thử nghiệm (1-Click Login)
                        </span>
                        <span className="font-label-sm text-[11px] text-outline">Pass chung: 123456</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                        <button
                          type="button"
                          onClick={() => handleQuickLogin('customer')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-primary text-left transition-all hover:shadow-md cursor-pointer group"
                        >
                          <div className="flex items-center gap-1.5 text-primary font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">person</span>
                            Khách hàng
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">customer@danasea.vn</p>
                        </button>

                        <button
                          type="button"
                          onClick={() => handleQuickLogin('vendor')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-secondary text-left transition-all hover:shadow-md cursor-pointer group"
                        >
                          <div className="flex items-center gap-1.5 text-secondary font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">sailing</span>
                            Đối tác Vendor
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">vendor@danasea.vn</p>
                        </button>

                        <button
                          type="button"
                          onClick={() => handleQuickLogin('admin')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-primary-container text-left transition-all hover:shadow-md cursor-pointer group"
                        >
                          <div className="flex items-center gap-1.5 text-primary-container font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">admin_panel_settings</span>
                            Quản trị viên
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">admin@danasea.vn</p>
                        </button>
                      </div>
                    </div>
                    
                    <form className="space-y-space-md" onSubmit={handleLoginSubmit}>
                      <div className="space-y-1.5">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Địa chỉ Email hoặc Tên tài khoản</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">mail</span>
                          <input value={email} onChange={e => setEmail(e.target.value)} className="w-full h-[52px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:bg-surface-container-lowest focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Nhập email hoặc gõ: customer, vendor, admin" required type="text" />
                        </div>
                      </div>
                      
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                          <label className="font-label-md text-label-md text-on-surface">Mật khẩu</label>
                          <a className="font-label-sm text-label-sm text-primary-container hover:underline cursor-pointer" onClick={() => navigate('/auth/forgot-password')}>
                            Quên mật khẩu?
                          </a>
                        </div>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">lock</span>
                          <input value={password} onChange={e => setPassword(e.target.value)} className="w-full h-[52px] pl-11 pr-12 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Nhập mật khẩu của bạn" required type="password" />
                        </div>
                      </div>
                      
                      <button className="w-full h-[52px] rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg font-semibold flex items-center justify-center gap-2 shadow-md hover:bg-primary transition-all duration-200 cursor-pointer" type="submit">
                        <span>Đăng nhập hệ sinh thái DANASEA</span>
                        <span className="material-symbols-outlined text-body-lg">arrow_forward</span>
                      </button>
                    </form>
                    
                    <div className="pt-space-xs text-center">
                      <p className="font-body-sm text-body-sm text-on-surface-variant">
                        Chưa có tài khoản tham gia trải nghiệm? 
                        <button type="button" className="text-primary-container font-semibold hover:underline bg-transparent border-0 cursor-pointer ml-1" onClick={() => setActiveTab("register")}>Đăng ký thành viên ngay</button>
                      </p>
                    </div>
                  </div>
                )}

                {activeTab === "register" && (
                  <div className="space-y-space-md">
                    <div className="space-y-1">
                      <h3 className="font-headline-sm text-headline-sm text-on-surface">Đăng ký tài khoản du khách &amp; đối tác</h3>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">Điền đầy đủ thông tin chuẩn định danh để nhận vé QR chính chủ tại cảng biển Đà Nẵng.</p>
                    </div>
                    {banner === 'register_error' && (
                      <div className="p-space-md rounded-2xl bg-error-container text-on-error-container">
                        <p className="font-body-sm text-body-sm font-semibold">Địa chỉ email này đã được đăng ký trong hệ thống DANASEA.</p>
                      </div>
                    )}
                    <form className="space-y-space-md" onSubmit={handleRegisterSubmit}>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Họ và tên</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">badge</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Ví dụ: Nguyễn Hải Đăng" required type="text" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Địa chỉ Email</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">mail</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="ban@email.com" required type="email" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Mật khẩu</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">lock</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Nhập mật khẩu" required type="password" />
                        </div>
                      </div>
                      <button className="w-full h-[52px] rounded-xl bg-surface-container-high text-on-surface font-label-lg text-label-lg font-semibold flex items-center justify-center gap-2 hover:bg-surface-dim transition-all duration-200 cursor-pointer" type="submit">
                        Tạo tài khoản miễn phí
                      </button>
                    </form>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
