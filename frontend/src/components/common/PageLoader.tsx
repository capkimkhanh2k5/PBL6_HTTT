import { useLoadingStore } from '../../store/useLoadingStore';

export function PageLoader() {
  const { isLoading, message, subMessage, hideLoader } = useLoadingStore();

  if (!isLoading) return null;

  return (
    <div
      className="fixed inset-0 z-[99999] flex flex-col items-center justify-center p-4 bg-slate-950/40 backdrop-blur-md animate-in fade-in duration-250 select-none"
      role="dialog"
      aria-modal="true"
      aria-label="Đang tải trang"
    >
      {/* Centered Glassmorphic Loading Card */}
      <div className="relative w-full max-w-sm sm:max-w-md bg-surface-container-lowest/95 rounded-[32px] p-6 sm:p-8 shadow-2xl border border-white/40 flex flex-col items-center text-center space-y-4 animate-in zoom-in-95 duration-200">
        
        {/* Soft Ambient Glow Behind Animation */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-48 h-48 bg-secondary/15 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-1/4 left-1/2 -translate-x-1/2 translate-y-1/2 w-48 h-48 bg-primary/10 rounded-full blur-3xl pointer-events-none" />

        {/* Video Animation Container */}
        <div className="relative w-44 h-44 sm:w-52 sm:h-52 rounded-3xl overflow-hidden shadow-inner bg-gradient-to-b from-sky-50/50 to-amber-50/40 flex items-center justify-center border border-outline-variant/20">
          <video
            src="/assets/summer-loader.mp4"
            poster="/assets/summer-loader-poster.png"
            autoPlay
            loop
            muted
            playsInline
            className="w-full h-full object-cover scale-105"
          />
        </div>

        {/* Brand & Loading Info */}
        <div className="space-y-1.5 z-10">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold uppercase tracking-wider">
            <span className="material-symbols-outlined text-[14px] animate-spin">
              progress_activity
            </span>
            <span>DANASEA OCEAN TRAVEL</span>
          </div>
          
          <h3 className="font-headline-sm text-lg sm:text-xl font-bold text-on-surface tracking-tight">
            {message}
          </h3>
          
          <p className="font-body-sm text-xs sm:text-sm text-on-surface-variant max-w-xs mx-auto">
            {subMessage}
          </p>
        </div>

        {/* Ocean Waves Shimmer Progress Bar */}
        <div className="w-full max-w-[200px] h-1.5 bg-surface-container-high rounded-full overflow-hidden relative">
          <div className="absolute inset-0 bg-gradient-to-r from-primary via-secondary to-primary-container rounded-full animate-[shimmer_1.5s_infinite_linear] [background-size:200%_100%]" />
        </div>

        {/* Quick Dismiss in Dev Mode */}
        <button
          type="button"
          onClick={hideLoader}
          className="text-[11px] text-outline hover:text-on-surface transition-colors pt-1 cursor-pointer"
          title="Bấm để tắt nhanh màn hình chờ"
        >
          Bỏ qua màn hình chờ ✕
        </button>
      </div>
    </div>
  );
}
