import { create } from 'zustand';

interface LoadingState {
  isLoading: boolean;
  message: string;
  subMessage: string;
  showLoader: (message?: string, subMessage?: string) => void;
  hideLoader: () => void;
  triggerDemoLoading: (durationMs?: number, message?: string, subMessage?: string) => void;
}

export const useLoadingStore = create<LoadingState>((set) => ({
  isLoading: false,
  message: 'Đang kết nối sóng biển DANASEA...',
  subMessage: 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm',
  showLoader: (
    message = 'Đang kết nối sóng biển DANASEA...',
    subMessage = 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm'
  ) => set({ isLoading: true, message, subMessage }),
  hideLoader: () => set({ isLoading: false }),
  triggerDemoLoading: (
    durationMs = 2000,
    message = 'Đang kết nối sóng biển DANASEA...',
    subMessage = 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm'
  ) => {
    set({ isLoading: true, message, subMessage });
    setTimeout(() => {
      set({ isLoading: false });
    }, durationMs);
  },
}));
