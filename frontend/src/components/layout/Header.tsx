import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";
import { useCartStore } from "../../store/useCartStore";
import { useWishlistStore } from "../../store/useWishlistStore";

export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  
  const { user, isAuthenticated, logout } = useAuthStore();
  const cartItemCount = useCartStore((state) => state.getTotalItems());
  const wishlistCount = useWishlistStore((state) => state.wishlistIds.length);

  const isActive = (path: string) => location.pathname === path;

  const scrollToSection = (id: string) => {
    if (location.pathname !== "/") {
      navigate("/" + id);
    } else {
      const element = document.querySelector(id);
      if (element) {
        const headerHeight = 80;
        const targetPosition = element.getBoundingClientRect().top + window.scrollY - headerHeight;
        const startPosition = window.scrollY;
        const distance = targetPosition - startPosition;
        const duration = 600; // 600ms
        let start: number | null = null;

        const easeInOutQuad = (t: number, b: number, c: number, d: number) => {
          t /= d / 2;
          if (t < 1) return (c / 2) * t * t + b;
          t--;
          return (-c / 2) * (t * (t - 2) - 1) + b;
        };

        const animation = (currentTime: number) => {
          if (start === null) start = currentTime;
          const timeElapsed = currentTime - start;
          const run = easeInOutQuad(timeElapsed, startPosition, distance, duration);
          window.scrollTo(0, run);
          if (timeElapsed < duration) requestAnimationFrame(animation);
        };

        requestAnimationFrame(animation);
      }
    }
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-surface-container-lowest/85 backdrop-blur-xl shadow-[0_4px_20px_-2px_rgba(16,47,58,0.06)]">
      <div className="h-20 max-w-[1280px] mx-auto px-margin-desktop flex items-center justify-between gap-space-lg">
        <div className="flex items-center gap-space-sm flex-shrink-0">
          <img alt="DANASEA Brand Logo" className="h-8 w-auto object-contain" src="https://lh3.googleusercontent.com/aida/AEtjO1Wc3PDumzL5I2MlIC7zdJZmfwBfm4RHsplJ5nk-9ZaRKWyozKczleP4gMlRMYECYGlmoy61P0H5GG6zbTiOnv9OfrbuVqBwoPco9lX5TdnIOndbHvSBQrTFFImlLmSHKB20EH_fp68zcXl68-GiUe2CUKvkZGprNc2OedIHPdFefB5izHfijulRwWFEJMtI3mN_pen7Nzc9tS1VIfkLiHft_-cJWqR00zO95yZuFm90DNcjzOJt5JEfyFNO" />
          <Link to="/" className="font-headline-md text-headline-md tracking-tight text-on-surface hover:text-primary transition-colors flex items-center gap-1.5">DANASEA</Link>
        </div>
        
        <nav className="hidden xl:flex items-center gap-space-xl">
          <Link 
            to="/" 
            className={`font-label-lg text-label-lg transition-all hover:-translate-y-0.5 hover:text-primary ${isActive("/") ? "text-primary font-bold border-b-2 border-primary pb-1" : "text-on-surface-variant"}`}
          >
            Khám phá
          </Link>
          <Link 
            to="/search" 
            className={`font-label-lg text-label-lg transition-all hover:-translate-y-0.5 hover:text-primary ${isActive("/search") ? "text-primary font-bold border-b-2 border-primary pb-1" : "text-on-surface-variant"}`}
          >
            Gói trải nghiệm
          </Link>
          <button 
            onClick={() => scrollToSection('#featured')} 
            className="font-label-lg text-label-lg text-on-surface-variant transition-all hover:-translate-y-0.5 hover:text-primary cursor-pointer"
          >
            Điểm đến
          </button>
        </nav>
        
        <div className="flex items-center gap-space-md flex-shrink-0">
          <div className="flex items-center bg-surface-container-low rounded-full p-space-2xs text-on-surface-variant font-label-sm text-label-sm">
            <span className="px-space-xs py-0.5 rounded-full bg-surface-container-lowest text-on-surface font-bold shadow-[0_1px_4px_rgba(0,0,0,0.04)] cursor-pointer">VI</span>
            <span className="px-space-xs py-0.5 cursor-pointer hover:text-on-surface transition-colors">EN</span>
          </div>
          
          <div className="flex items-center gap-space-xs">
            <Link to="/profile/wishlist" aria-label="Yêu thích" className={`relative w-10 h-10 rounded-full flex items-center justify-center bg-surface-container-low transition-colors ${wishlistCount > 0 ? 'text-red-500 hover:bg-red-50' : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'}`}>
              <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: wishlistCount > 0 ? "'FILL' 1" : "'FILL' 0" }}>favorite</span>
              {wishlistCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-red-500 text-white font-label-sm text-[10px] font-bold flex items-center justify-center leading-none shadow-sm">{wishlistCount}</span>
              )}
            </Link>
            
            <Link to="/cart" aria-label="Giỏ hàng" className="relative w-10 h-10 rounded-full flex items-center justify-center bg-surface-container-low text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors">
              <span className="material-symbols-outlined text-[20px]">shopping_bag</span>
              {cartItemCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-secondary text-on-secondary font-label-sm text-[10px] flex items-center justify-center leading-none">{cartItemCount}</span>
              )}
            </Link>
          </div>
          
          <div className="hidden md:flex items-center gap-space-xs">
            {!isAuthenticated ? (
              <Link to="/auth" className="px-space-md py-space-xs rounded-full bg-surface-container-low text-on-surface font-label-lg text-label-lg hover:bg-surface-container-high transition-all">Đăng nhập</Link>
            ) : null}
            <button onClick={() => scrollToSection('#ai-concierge')} className="px-space-md py-space-xs rounded-full bg-primary-container text-on-primary font-label-lg text-label-lg shadow-[0_8px_20px_rgba(255,115,92,0.3)] hover:scale-[1.02] transition-transform cursor-pointer">Lên lịch cùng AI</button>
          </div>
          
          {isAuthenticated && user ? (
            <Link to="/profile" className="w-8 h-8 rounded-full overflow-hidden flex items-center justify-center flex-shrink-0 shadow-[0_2px_8px_rgba(170,53,36,0.25)] hover:ring-2 hover:ring-primary transition-all">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.fullName} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full bg-primary flex items-center justify-center">
                  <span className="material-symbols-outlined text-on-primary text-[18px]">person</span>
                </div>
              )}
            </Link>
          ) : null}
        </div>
      </div>
    </header>
  );
}
