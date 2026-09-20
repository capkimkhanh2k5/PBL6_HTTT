import { Outlet, useLocation } from "react-router-dom";
import { Header } from "../components/layout/Header";
import { Footer } from "../components/layout/Footer";
import { useEffect, useState } from "react";
import { useWishlistStore } from "../store/useWishlistStore";

// Định nghĩa thứ tự các trang chính để xác định hướng trượt
const PAGE_ORDER: Record<string, number> = {
  "/": 0,
  "/search": 1,
  "/cart": 2,
  "/profile": 3,
};

export function CustomerLayout() {
  const location = useLocation();
  const [prevPath, setPrevPath] = useState(location.pathname);
  const [animationClass, setAnimationClass] = useState("animate-fade-in-up");
  const { toastMessage, clearToast } = useWishlistStore();

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => {
        clearToast();
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage, clearToast]);

  useEffect(() => {
    let isPathChanged = false;
    if (location.pathname !== prevPath) {
      isPathChanged = true;
      const prevOrder = PAGE_ORDER[prevPath] ?? 0;
      const currentOrder = PAGE_ORDER[location.pathname] ?? 0;

      if (currentOrder > prevOrder) {
        setAnimationClass("animate-slide-in-right");
      } else if (currentOrder < prevOrder) {
        setAnimationClass("animate-slide-in-left");
      } else {
        setAnimationClass("animate-fade-in-up");
      }
      setPrevPath(location.pathname);
    }

    // Đợi DOM render xong (100ms) rồi cuộn
    setTimeout(() => {
      if (location.hash) {
        const element = document.querySelector(location.hash);
        if (element) {
          const headerHeight = 80;
          const targetPosition = element.getBoundingClientRect().top + window.scrollY - headerHeight;
          window.scrollTo({ top: targetPosition, behavior: "smooth" });
        }
      } else if (isPathChanged) {
        window.scrollTo(0, 0);
      }
    }, 100);

  }, [location.pathname, location.hash, prevPath]);

  return (
    <div className="bg-surface font-body-md text-on-surface min-h-full flex flex-col overflow-x-hidden">
      <Header />
      {/* Bao bọc Outlet bằng một the div chứa key={location.pathname} để ép React render lại khi đổi route */}
      <div
        key={location.pathname}
        onAnimationEnd={() => setAnimationClass("")}
        className={`flex-1 ${animationClass}`}
      >
        <Outlet />
      </div>
      <Footer />

      {/* Global Floating Wishlist Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-[99999] bg-inverse-surface/95 text-inverse-on-surface px-5 py-3.5 rounded-2xl shadow-2xl flex items-center gap-3 backdrop-blur-md border border-outline-variant/20 animate-fade-in-up">
          <span
            className="material-symbols-outlined text-red-500 text-[24px]"
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            favorite
          </span>
          <span className="font-label-md text-label-md font-medium max-w-xs sm:max-w-md">
            {toastMessage}
          </span>
          <button
            onClick={clearToast}
            className="ml-2 p-1 rounded-full hover:bg-white/10 text-inverse-on-surface/70 hover:text-inverse-on-surface cursor-pointer transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}
