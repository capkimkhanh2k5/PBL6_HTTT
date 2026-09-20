import { useState } from "react";
import { useNavigate } from "react-router-dom";

export function ChangePassword() {
  const navigate = useNavigate();
  const [success, setSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSuccess(true);
    setTimeout(() => {
      navigate('/profile');
    }, 2000);
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1 flex flex-col">
      <div className="max-w-[800px] mx-auto px-margin-mobile md:px-margin-desktop w-full py-space-xl">
        <button onClick={() => navigate('/profile')} className="mb-space-lg flex items-center gap-1 text-on-surface-variant hover:text-on-surface transition-colors font-label-md">
          <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          Quay lại Hồ sơ
        </button>

        <div className="bg-surface-container-lowest rounded-2xl shadow-sm p-space-xl border border-surface-container-high">
          <div className="flex items-center gap-space-sm mb-space-lg">
            <div className="w-12 h-12 rounded-full bg-primary-fixed-dim/20 text-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[24px]">lock_reset</span>
            </div>
            <div>
              <h1 className="font-headline-md text-headline-md text-on-surface">Đổi mật khẩu</h1>
              <p className="font-body-sm text-body-sm text-on-surface-variant">Bảo vệ tài khoản DANASEA của bạn bằng mật khẩu an toàn.</p>
            </div>
          </div>

          {success ? (
            <div className="p-space-lg rounded-xl bg-secondary-fixed/30 text-on-secondary-fixed text-center animate-in fade-in zoom-in">
              <span className="material-symbols-outlined text-[48px] text-secondary mb-2">check_circle</span>
              <h2 className="font-headline-sm text-headline-sm">Đổi mật khẩu thành công!</h2>
              <p className="font-body-sm text-body-sm mt-1">Hệ thống sẽ chuyển hướng bạn về trang Hồ sơ trong giây lát.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="flex flex-col gap-space-md">
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-on-surface">Mật khẩu hiện tại</label>
                <div className="relative flex items-center">
                  <span className="material-symbols-outlined absolute left-space-md text-outline pointer-events-none">lock</span>
                  <input required type="password" placeholder="Nhập mật khẩu cũ" className="w-full pl-12 pr-4 py-3 rounded-xl bg-surface-container-low focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary-container outline-none transition-all" />
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-on-surface">Mật khẩu mới</label>
                <div className="relative flex items-center">
                  <span className="material-symbols-outlined absolute left-space-md text-outline pointer-events-none">key</span>
                  <input required type="password" placeholder="Tối thiểu 8 ký tự" className="w-full pl-12 pr-4 py-3 rounded-xl bg-surface-container-low focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary-container outline-none transition-all" />
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-on-surface">Xác nhận mật khẩu mới</label>
                <div className="relative flex items-center">
                  <span className="material-symbols-outlined absolute left-space-md text-outline pointer-events-none">key</span>
                  <input required type="password" placeholder="Nhập lại mật khẩu mới" className="w-full pl-12 pr-4 py-3 rounded-xl bg-surface-container-low focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary-container outline-none transition-all" />
                </div>
              </div>
              
              <div className="pt-space-md border-t border-surface-container mt-2">
                <button type="submit" className="w-full py-3 rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg shadow-md hover:bg-primary transition-all">
                  Cập nhật mật khẩu
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </main>
  );
}
