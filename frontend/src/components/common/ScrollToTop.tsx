import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * ScrollToTop Component
 * Ensures that on any route navigation (e.g. clicking 'View All' / 'Xem tất cả' or navigating between pages),
 * the window instantly resets scroll position to the top of the new page, unless a hash anchor is provided.
 */
export function ScrollToTop() {
  const { pathname, search, hash } = useLocation();

  useEffect(() => {
    if (hash) {
      const timer = setTimeout(() => {
        const element = document.querySelector(hash);
        if (element) {
          const headerHeight = 80;
          const targetPosition = element.getBoundingClientRect().top + window.scrollY - headerHeight;
          window.scrollTo({ top: targetPosition, behavior: "smooth" });
        }
      }, 100);
      return () => clearTimeout(timer);
    } else {
      // Instant reset to the very top of the newly loaded page
      window.scrollTo({ top: 0, left: 0, behavior: "instant" });
    }
  }, [pathname, search, hash]);

  return null;
}

export default ScrollToTop;
