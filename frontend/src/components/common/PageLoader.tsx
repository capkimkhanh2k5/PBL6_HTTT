import { useLoadingStore } from '../../store/useLoadingStore';

export function PageLoader() {
  const { isLoading, hideLoader } = useLoadingStore();

  if (!isLoading) return null;

  return (
    <div
      onClick={hideLoader}
      className="fixed inset-0 z-[99999] flex items-center justify-center backdrop-blur-md bg-white/40 dark:bg-black/40 animate-in fade-in duration-200 select-none cursor-pointer"
      role="status"
      aria-label="Đang tải trang"
    >
      {/* Animated Loader Graphic Only - Feathered edges so the animation blends seamlessly with the blurred background */}
      <div className="relative w-64 h-64 sm:w-72 sm:h-72 flex items-center justify-center [mask-image:radial-gradient(ellipse_at_center,black_50%,transparent_70%)] [-webkit-mask-image:radial-gradient(ellipse_at_center,black_50%,transparent_70%)] pointer-events-none">
        <video
          src="/assets/summer-loader.mp4"
          poster="/assets/summer-loader-poster.png"
          autoPlay
          loop
          muted
          playsInline
          className="w-full h-full object-cover scale-115 mix-blend-multiply dark:mix-blend-normal"
        />
      </div>
    </div>
  );
}
