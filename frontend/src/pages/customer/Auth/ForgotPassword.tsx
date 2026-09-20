import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

export function ForgotPassword() {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("an.nguyen@example.com");
  const [timer, setTimer] = useState(54);
  const [isResending, setIsResending] = useState(false);
  
  // Passwords
  const [newPass, setNewPass] = useState("");
  const [confirmPass, setConfirmPass] = useState("");
  const [showNewPass, setShowNewPass] = useState(false);
  const [showConfirmPass, setShowConfirmPass] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  
  const navigate = useNavigate();

  // Timer countdown for Step 2
  useEffect(() => {
    let interval: any = null;
    if (step === 2 && timer > 0) {
      interval = setInterval(() => {
        setTimer((prev) => prev - 1);
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [step, timer]);

  const handleResend = () => {
    if (timer > 0 || isResending) return;
    setIsResending(true);
    setTimeout(() => {
      setIsResending(false);
      setTimer(60);
    }, 800);
  };

  // Password checks
  const hasMinLength = newPass.length >= 8;
  const hasUppercase = /[A-Z]/.test(newPass);
  const hasNumber = /[0-9]/.test(newPass);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPass);

  const passedCriteria = [hasMinLength, hasUppercase, hasNumber, hasSpecial].filter(Boolean).length;
  
  const getStrengthLabel = () => {
    if (newPass.length === 0) return "Chưa nhập";
    if (passedCriteria <= 1) return "Rất yếu";
    if (passedCriteria === 2) return "Trung bình";
    if (passedCriteria === 3) return "Khá an toàn";
    return "Rất mạnh";
  };

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    if (!hasMinLength) {
      setErrorMsg("Mật khẩu phải có ít nhất 8 ký tự.");
      return;
    }
    if (newPass !== confirmPass) {
      setErrorMsg("Mật khẩu xác nhận không khớp!");
      return;
    }
    setStep(5);
  };

  return (
    <main className="w-full min-h-screen pt-20 bg-surface flex flex-col justify-center items-center">
      <div className="flex flex-col w-full">
        {/* Interactive Presentation Mode Toggle & Header Scrim */}
        <div className="relative w-full overflow-hidden py-12 px-6 lg:px-20">
          {/* Oceanic Ambient Background Orbs */}
          <div className="absolute -top-32 -left-20 w-96 h-96 rounded-full bg-primary-fixed-dim/20 blur-3xl pointer-events-none"></div>
          <div className="absolute top-1/2 -right-32 w-80 h-80 rounded-full bg-secondary-fixed/20 blur-3xl pointer-events-none"></div>
          
          <div className="max-w-7xl mx-auto flex flex-col gap-10">
            {/* Top Flow Switcher Bar */}
            <div className="flex flex-wrap items-center justify-between gap-4 bg-surface-container-lowest/80 backdrop-blur-md px-6 py-4 rounded-xl shadow-sm">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center text-on-primary">
                  <span className="material-symbols-outlined text-[24px]">anchor</span>
                </div>
                <div>
                  <div className="font-headline-sm text-headline-sm text-on-surface">DANASEA Security Portal</div>
                  <div className="font-body-sm text-body-sm text-tertiary">Bảo vệ tài khoản hải trình &amp; đặt chỗ hàng hải cao cấp</div>
                </div>
              </div>

              {/* Step Tabs */}
              <div className="flex items-center gap-1 bg-surface-container-high p-1.5 rounded-xl">
                <button
                  type="button"
                  className={`px-3 sm:px-4 py-2 rounded-lg font-label-md text-label-md transition-all duration-200 ${step === 1 ? "bg-primary-container text-on-primary-container shadow-sm" : "text-tertiary hover:text-on-surface"}`}
                  onClick={() => setStep(1)}
                >
                  1. Gửi yêu cầu
                </button>
                <button
                  type="button"
                  className={`px-3 sm:px-4 py-2 rounded-lg font-label-md text-label-md transition-all duration-200 ${step === 2 ? "bg-primary-container text-on-primary-container shadow-sm" : "text-tertiary hover:text-on-surface"}`}
                  onClick={() => setStep(2)}
                >
                  2. Kiểm tra Email
                </button>
                <button
                  type="button"
                  className={`px-3 sm:px-4 py-2 rounded-lg font-label-md text-label-md transition-all duration-200 ${step === 3 ? "bg-primary-container text-on-primary-container shadow-sm" : "text-tertiary hover:text-on-surface"}`}
                  onClick={() => setStep(3)}
                >
                  3. Đặt mật khẩu mới
                </button>
                <button
                  type="button"
                  className={`px-3 sm:px-4 py-2 rounded-lg font-label-md text-label-md transition-all duration-200 ${step === 4 ? "bg-primary-container text-on-primary-container shadow-sm" : "text-tertiary hover:text-on-surface"}`}
                  onClick={() => setStep(4)}
                >
                  4. Trạng thái lỗi/hết hạn
                </button>
                <button
                  type="button"
                  className={`px-3 sm:px-4 py-2 rounded-lg font-label-md text-label-md transition-all duration-200 ${step === 5 ? "bg-primary-container text-on-primary-container shadow-sm" : "text-tertiary hover:text-on-surface"}`}
                  onClick={() => setStep(5)}
                >
                  5. Thành công
                </button>
              </div>
            </div>

            {/* Main Layout Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
              {/* Left Column: Maritime Brand Storytelling */}
              <div className="lg:col-span-5 flex flex-col justify-between rounded-xl bg-surface-container-lowest shadow-sm p-8 relative overflow-hidden">
                <div
                  className="absolute inset-0 bg-cover bg-center opacity-10"
                  style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuCBYMvCvw4APVy6HXmYf2xiApAtsKrcGjFMqT91Sw0jT4_de3RyC9nh5SBVAbYle-f6qrhLB7_nMajpKxbuH-6NEnRmZYaWffgLuQz-tM-U89orF6g1yU9zfLsYUSALSlE3UFTryqYA0R_8LAkOwl8FkhLBmHvFQc95FlfSQbRgyEVNRl3QW4ivSsTFVpN1flzBnb6u9pwN-v2JdQ8ZygcGn4kn7Vw6kZwMbxeQxN-5hkB1dYwae3grAA')" }}
                ></div>
                <div className="relative z-10 flex flex-col gap-6">
                  <div className="inline-flex items-center gap-2 self-start px-3 py-1.5 rounded-full bg-surface-container-high text-primary font-label-sm text-label-sm uppercase tracking-wider">
                    <span className="material-symbols-outlined text-[16px]">verified_user</span>
                    Hệ thống xác thực chuẩn hàng hải
                  </div>
                  <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight">
                    An tâm khám phá đại dương cùng DANASEA
                  </h1>
                  <p className="font-body-md text-body-md text-tertiary leading-relaxed">
                    Quy trình khôi phục và bảo mật tài khoản được mã hóa tiêu chuẩn quốc tế, bảo vệ quyền lợi cá nhân, lịch trình du thuyền và trải nghiệm lặn biển của bạn mọi lúc, mọi nơi.
                  </p>

                  {/* Trust list */}
                  <div className="flex flex-col gap-3.5 pt-4">
                    <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low">
                      <div className="w-8 h-8 rounded-full bg-primary-fixed-dim/30 flex items-center justify-center text-primary">
                        <span className="material-symbols-outlined text-[18px]">lock_clock</span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-label-md text-label-md text-on-surface">Thời hạn liên kết an toàn</span>
                        <span className="font-body-sm text-body-sm text-outline">Tự động vô hiệu hóa sau 15 phút nhằm bảo đảm an ninh</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low">
                      <div className="w-8 h-8 rounded-full bg-primary-fixed-dim/30 flex items-center justify-center text-primary">
                        <span className="material-symbols-outlined text-[18px]">shield</span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-label-md text-label-md text-on-surface">Mã hóa đa tầng SSL 256-bit</span>
                        <span className="font-body-sm text-body-sm text-outline">Bảo vệ toàn vẹn dữ liệu cá nhân & thông tin giao dịch</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-container-low">
                      <div className="w-8 h-8 rounded-full bg-primary-fixed-dim/30 flex items-center justify-center text-primary">
                        <span className="material-symbols-outlined text-[18px]">support_agent</span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-label-md text-label-md text-on-surface">Hỗ trợ xác thực tài khoản 24/7</span>
                        <span className="font-body-sm text-body-sm text-outline">Hỗ trợ trực tuyến kịp thời khi gặp sự cố đăng nhập</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Right Column: Interactive State Panels */}
              <div className="lg:col-span-7 flex flex-col justify-center">
                {/* STATE 1: Request Password Recovery */}
                {step === 1 && (
                  <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-8 md:p-12 animate-fade-in-up">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 rounded-full bg-primary-fixed-dim/30 flex items-center justify-center text-primary-container">
                        <span className="material-symbols-outlined text-[22px]">key</span>
                      </div>
                      <span className="font-label-sm text-label-sm uppercase tracking-widest text-primary font-bold">Bước 01 / Khôi phục quyền truy cập</span>
                    </div>
                    <h2 className="font-headline-lg text-headline-lg text-on-surface mb-3">Quên mật khẩu?</h2>
                    <p className="font-body-md text-body-md text-tertiary mb-8">
                      Nhập địa chỉ email liên kết với tài khoản DANASEA của bạn để nhận liên kết thiết lập mật khẩu mới.
                    </p>
                    <form className="flex flex-col gap-6" onSubmit={(e) => { e.preventDefault(); setStep(2); }}>
                      <div className="flex flex-col gap-2">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between" htmlFor="recovery-email">
                          <span>Địa chỉ email tài khoản</span>
                          <span className="text-tertiary font-body-sm">Đã đăng ký khi đặt tour</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-[20px]">mail</span>
                          <input
                            className="w-full h-[52px] pl-12 pr-4 bg-surface-container-lowest text-on-surface rounded-xl shadow-sm outline-none focus:ring-2 focus:ring-primary-container transition font-body-md text-body-md"
                            id="recovery-email"
                            placeholder="nhap.email@example.com"
                            required
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                          />
                        </div>
                      </div>
                      <button
                        className="w-full h-[52px] rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg flex items-center justify-center gap-2 hover:bg-primary transition shadow-md"
                        type="submit"
                      >
                        <span>Gửi liên kết khôi phục</span>
                        <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                      </button>
                      <div className="flex items-center justify-center gap-2 pt-2">
                        <span className="font-body-md text-body-md text-tertiary">Bạn đã nhớ lại mật khẩu?</span>
                        <button
                          type="button"
                          className="font-label-md text-label-md text-primary-container hover:underline inline-flex items-center gap-1"
                          onClick={() => navigate("/auth")}
                        >
                          <span>Đăng nhập</span>
                          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
                        </button>
                      </div>
                    </form>
                  </div>
                )}

                {/* STATE 2: Email Sent Confirmation & Countdown */}
                {step === 2 && (
                  <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-8 md:p-12 animate-fade-in-up">
                    <div className="flex flex-col items-center text-center">
                      <div className="relative mb-6">
                        <div className="w-20 h-20 rounded-full bg-primary-container/10 flex items-center justify-center text-primary-container">
                          <span className="material-symbols-outlined text-[40px]">mark_email_read</span>
                        </div>
                        <div className="absolute inset-0 rounded-full bg-primary-container/20 animate-ping pointer-events-none"></div>
                      </div>
                      <div className="inline-block px-3 py-1 rounded-full bg-tertiary-fixed text-on-tertiary-fixed font-label-sm text-label-sm uppercase mb-3 font-semibold">
                        Đã gửi thành công
                      </div>
                      <h2 className="font-headline-lg text-headline-lg text-on-surface mb-3">Kiểm tra hộp thư của bạn</h2>
                      <p className="font-body-md text-body-md text-tertiary max-w-lg mb-6 leading-relaxed">
                        Đã gửi hướng dẫn khôi phục mật khẩu tới địa chỉ <span className="font-semibold underline decoration-primary-container text-on-surface">{email}</span>. Vui lòng kiểm tra hộp thư đến (và thư rác).
                      </p>
                      
                      <div className="w-full bg-surface-container-low p-4 rounded-xl text-left mb-6 flex items-start gap-3">
                        <span className="material-symbols-outlined text-primary-container text-[20px] shrink-0 mt-0.5">info</span>
                        <p className="font-body-sm text-body-sm text-tertiary">
                          Để bảo vệ an ninh hàng hải cá nhân, liên kết chỉ có hiệu lực trong vòng <strong className="text-on-surface">15 phút</strong>. Sau thời gian này, vui lòng gửi lại yêu cầu mới.
                        </p>
                      </div>

                      <div className="w-full flex flex-col sm:flex-row items-center justify-center gap-4">
                        <button
                          type="button"
                          className={`w-full sm:w-auto h-[48px] px-6 rounded-xl font-label-md text-label-md flex items-center justify-center gap-2 transition ${
                            timer > 0 ? "bg-surface-container-high text-outline cursor-not-allowed" : "bg-primary text-white hover:bg-primary/90"
                          }`}
                          disabled={timer > 0}
                          onClick={handleResend}
                        >
                          <span className="material-symbols-outlined text-[18px]">cached</span>
                          <span>{timer > 0 ? `Gửi lại email (${timer}s)` : "Gửi lại email ngay"}</span>
                        </button>
                        <button
                          type="button"
                          className="w-full sm:w-auto h-[48px] px-6 rounded-xl bg-primary-container text-on-primary font-label-md text-label-md flex items-center justify-center gap-2 hover:bg-primary transition shadow-sm"
                          onClick={() => setStep(3)}
                        >
                          <span>Mở liên kết mẫu (Thử nghiệm)</span>
                          <span className="material-symbols-outlined text-[18px]">open_in_new</span>
                        </button>
                      </div>

                      <div className="mt-8 pt-6 w-full flex items-center justify-center">
                        <button
                          type="button"
                          className="font-label-md text-label-md text-tertiary hover:text-on-surface flex items-center gap-1.5"
                          onClick={() => setStep(1)}
                        >
                          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
                          <span>Dùng địa chỉ email khác</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* STATE 3: Set New Password Form */}
                {step === 3 && (
                  <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-8 md:p-12 animate-fade-in-up">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 rounded-full bg-primary-fixed-dim/30 flex items-center justify-center text-primary-container">
                        <span className="material-symbols-outlined text-[22px]">lock_reset</span>
                      </div>
                      <span className="font-label-sm text-label-sm uppercase tracking-widest text-primary font-bold">Bước 02 / Hoàn tất xác minh</span>
                    </div>
                    <h2 className="font-headline-lg text-headline-lg text-on-surface mb-2">Tạo mật khẩu mới</h2>
                    <p className="font-body-md text-body-md text-tertiary mb-6">
                      Thiết lập mật khẩu mạnh và an toàn để tiếp tục trải nghiệm các chuyến du ngoạn trên biển cùng DANASEA.
                    </p>

                    {errorMsg && (
                      <div className="p-3 mb-4 rounded-lg bg-error-container text-on-error-container text-sm flex items-center gap-2">
                        <span className="material-symbols-outlined text-sm">error</span>
                        {errorMsg}
                      </div>
                    )}

                    <form className="flex flex-col gap-5" onSubmit={handlePasswordSubmit}>
                      {/* New Password */}
                      <div className="flex flex-col gap-2">
                        <label className="font-label-md text-label-md text-on-surface" htmlFor="new-pass">Mật khẩu mới</label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-[20px]">lock</span>
                          <input
                            className="w-full h-[52px] pl-12 pr-12 bg-surface-container-lowest text-on-surface rounded-xl shadow-sm outline-none focus:ring-2 focus:ring-primary-container transition font-body-md text-body-md"
                            id="new-pass"
                            placeholder="Tối thiểu 8 ký tự..."
                            required
                            type={showNewPass ? "text" : "password"}
                            value={newPass}
                            onChange={(e) => setNewPass(e.target.value)}
                          />
                          <button
                            type="button"
                            className="absolute right-4 text-outline hover:text-on-surface"
                            onClick={() => setShowNewPass(!showNewPass)}
                          >
                            <span className="material-symbols-outlined text-[20px]">
                              {showNewPass ? "visibility_off" : "visibility"}
                            </span>
                          </button>
                        </div>
                      </div>

                      {/* Password Strength Meter */}
                      <div className="bg-surface-container-low p-4 rounded-xl flex flex-col gap-3">
                        <div className="flex items-center justify-between">
                          <span className="font-label-sm text-label-sm text-tertiary">Độ mạnh mật khẩu</span>
                          <span className="font-label-sm text-label-sm text-primary font-bold">
                            {getStrengthLabel()}
                          </span>
                        </div>
                        <div className="grid grid-cols-4 gap-1.5 h-1.5 w-full">
                          <div className={`h-full rounded-full transition-all duration-300 ${passedCriteria >= 1 ? "bg-secondary" : "bg-surface-container-highest"}`}></div>
                          <div className={`h-full rounded-full transition-all duration-300 ${passedCriteria >= 2 ? "bg-secondary" : "bg-surface-container-highest"}`}></div>
                          <div className={`h-full rounded-full transition-all duration-300 ${passedCriteria >= 3 ? "bg-primary" : "bg-surface-container-highest"}`}></div>
                          <div className={`h-full rounded-full transition-all duration-300 ${passedCriteria >= 4 ? "bg-primary" : "bg-surface-container-highest"}`}></div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 font-body-sm text-body-sm">
                          <div className={`flex items-center gap-1.5 ${hasMinLength ? "text-primary font-medium" : "text-outline"}`}>
                            <span className="material-symbols-outlined text-[16px]">{hasMinLength ? "check_circle" : "cancel"}</span>
                            <span>Tối thiểu 8 ký tự</span>
                          </div>
                          <div className={`flex items-center gap-1.5 ${hasUppercase ? "text-primary font-medium" : "text-outline"}`}>
                            <span className="material-symbols-outlined text-[16px]">{hasUppercase ? "check_circle" : "cancel"}</span>
                            <span>Ít nhất 1 chữ hoa (A-Z)</span>
                          </div>
                          <div className={`flex items-center gap-1.5 ${hasNumber ? "text-primary font-medium" : "text-outline"}`}>
                            <span className="material-symbols-outlined text-[16px]">{hasNumber ? "check_circle" : "cancel"}</span>
                            <span>Ít nhất 1 chữ số (0-9)</span>
                          </div>
                          <div className={`flex items-center gap-1.5 ${hasSpecial ? "text-primary font-medium" : "text-outline"}`}>
                            <span className="material-symbols-outlined text-[16px]">{hasSpecial ? "check_circle" : "cancel"}</span>
                            <span>Ký tự đặc biệt (!@#$)</span>
                          </div>
                        </div>
                      </div>

                      {/* Confirm Password */}
                      <div className="flex flex-col gap-2">
                        <label className="font-label-md text-label-md text-on-surface" htmlFor="confirm-pass">Xác nhận mật khẩu mới</label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-[20px]">lock_clock</span>
                          <input
                            className="w-full h-[52px] pl-12 pr-12 bg-surface-container-lowest text-on-surface rounded-xl shadow-sm outline-none focus:ring-2 focus:ring-primary-container transition font-body-md text-body-md"
                            id="confirm-pass"
                            placeholder="Nhập lại mật khẩu mới..."
                            required
                            type={showConfirmPass ? "text" : "password"}
                            value={confirmPass}
                            onChange={(e) => setConfirmPass(e.target.value)}
                          />
                          <button
                            type="button"
                            className="absolute right-4 text-outline hover:text-on-surface"
                            onClick={() => setShowConfirmPass(!showConfirmPass)}
                          >
                            <span className="material-symbols-outlined text-[20px]">
                              {showConfirmPass ? "visibility_off" : "visibility"}
                            </span>
                          </button>
                        </div>
                      </div>

                      <button
                        className="w-full h-[52px] mt-2 rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg flex items-center justify-center gap-2 hover:bg-primary transition shadow-md"
                        type="submit"
                      >
                        <span>Cập nhật mật khẩu</span>
                        <span className="material-symbols-outlined text-[18px]">check_circle</span>
                      </button>
                    </form>
                  </div>
                )}

                {/* STATE 4: Exception State - Expired Link */}
                {step === 4 && (
                  <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-8 md:p-12 animate-fade-in-up">
                    <div className="flex flex-col items-center text-center">
                      <div className="w-20 h-20 rounded-full bg-error-container/40 flex items-center justify-center text-secondary mb-6">
                        <span className="material-symbols-outlined text-[44px]">link_off</span>
                      </div>
                      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-error-container text-on-error-container font-label-sm text-label-sm uppercase mb-3 font-semibold">
                        <span className="material-symbols-outlined text-[14px]">warning</span>
                        Yêu cầu không khả dụng
                      </div>
                      <h2 className="font-headline-lg text-headline-lg text-on-surface mb-3">Liên kết đã hết hạn</h2>
                      <div className="w-full p-5 rounded-xl bg-surface-container-high/80 text-center mb-6">
                        <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                          Liên kết khôi phục mật khẩu đã hết hạn hoặc đã được sử dụng. Vui lòng gửi lại yêu cầu mới.
                        </p>
                      </div>
                      <div className="flex flex-col sm:flex-row items-center gap-4 w-full justify-center">
                        <button
                          type="button"
                          className="w-full sm:w-auto h-[50px] px-8 rounded-xl bg-secondary-container text-on-secondary font-label-lg text-label-lg flex items-center justify-center gap-2 hover:bg-secondary transition shadow-md"
                          onClick={() => setStep(1)}
                        >
                          <span>Yêu cầu liên kết mới</span>
                          <span className="material-symbols-outlined text-[18px]">refresh</span>
                        </button>
                        <button
                          type="button"
                          className="w-full sm:w-auto h-[50px] px-6 rounded-xl bg-surface-container-low text-tertiary font-label-lg text-label-lg flex items-center justify-center gap-2 hover:text-on-surface transition"
                          onClick={() => navigate("/")}
                        >
                          <span>Về trang chủ</span>
                        </button>
                      </div>
                      <div className="mt-8 text-center font-body-sm text-body-sm text-outline">
                        Cần thêm sự trợ giúp hàng hải? Liên hệ <a className="text-primary-container font-semibold hover:underline" href="mailto:hotline@danasea.vn">hotline@danasea.vn</a>
                      </div>
                    </div>
                  </div>
                )}

                {/* STATE 5: Success State */}
                {step === 5 && (
                  <div className="w-full bg-surface-container-lowest rounded-xl shadow-md p-8 md:p-12 animate-fade-in-up">
                    <div className="flex flex-col items-center text-center">
                      <div className="w-20 h-20 rounded-full bg-primary-fixed flex items-center justify-center text-primary mb-6 shadow-sm">
                        <span className="material-symbols-outlined text-[44px]">task_alt</span>
                      </div>
                      <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-primary-fixed-dim/40 text-primary-container font-label-sm text-label-sm uppercase mb-3 font-semibold">
                        <span className="material-symbols-outlined text-[15px]">security</span>
                        Bảo mật hoàn tất
                      </div>
                      <h2 className="font-headline-lg text-headline-lg text-on-surface mb-3">Thành công tuyệt đối</h2>
                      <div className="w-full p-5 rounded-xl bg-primary-fixed/20 text-center mb-8">
                        <p className="font-body-md text-body-md text-on-surface leading-relaxed">
                          Mật khẩu đã được cập nhật thành công. Bạn có thể đăng nhập ngay bây giờ bằng mật khẩu mới.
                        </p>
                      </div>
                      <div className="w-full max-w-sm flex flex-col gap-3">
                        <button
                          type="button"
                          className="w-full h-[52px] rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg flex items-center justify-center gap-2 hover:bg-primary transition shadow-md"
                          onClick={() => navigate("/auth")}
                        >
                          <span>Đăng nhập ngay</span>
                          <span className="material-symbols-outlined text-[18px]">login</span>
                        </button>
                        <button
                          type="button"
                          className="w-full h-[44px] rounded-xl bg-transparent text-tertiary font-label-md text-label-md hover:text-on-surface transition"
                          onClick={() => setStep(1)}
                        >
                          Quay về màn hình khôi phục ban đầu
                        </button>
                      </div>
                      <div className="mt-8 pt-6 flex items-center gap-2 font-body-sm text-body-sm text-outline">
                        <span className="material-symbols-outlined text-[18px] text-primary">sailing</span>
                        <span>Thông tin xác nhận đã được gửi một bản tóm tắt đến hòm thư an ninh của bạn.</span>
                      </div>
                    </div>
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
