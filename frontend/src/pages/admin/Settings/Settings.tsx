import React, { useState } from 'react';
import { MOCK_ADMIN_CONFIGS } from '../../../data/adminMockData';
import type { SystemConfig } from '../../../types';

export function Settings() {
  const [configs, setConfigs] = useState<SystemConfig[]>(MOCK_ADMIN_CONFIGS);

  // Form states mapping to system_configs table
  const [commissionRate, setCommissionRate] = useState<string>(
    configs.find(c => c.key === 'PLATFORM_COMMISSION_RATE')?.value || '10'
  );
  const [payoutCycle, setPayoutCycle] = useState<string>(
    configs.find(c => c.key === 'AUTO_PAYOUT_CYCLE_DAYS')?.value || '15'
  );
  const [maxSafeWave, setMaxSafeWave] = useState<string>(
    configs.find(c => c.key === 'WEATHER_MAX_SAFE_WAVE_M')?.value || '1.5'
  );
  const [maxSafeWind, setMaxSafeWind] = useState<string>(
    configs.find(c => c.key === 'WEATHER_MAX_SAFE_WIND_KMH')?.value || '25'
  );
  const [emergencyHotline, setEmergencyHotline] = useState<string>(
    configs.find(c => c.key === 'EMERGENCY_PORT_HOTLINE')?.value || '1900 8989'
  );

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleSaveAll = () => {
    setConfigs(prev =>
      prev.map(c => {
        if (c.key === 'PLATFORM_COMMISSION_RATE') return { ...c, value: commissionRate, updatedBy: 'Vũ Hải Đăng' };
        if (c.key === 'AUTO_PAYOUT_CYCLE_DAYS') return { ...c, value: payoutCycle, updatedBy: 'Vũ Hải Đăng' };
        if (c.key === 'WEATHER_MAX_SAFE_WAVE_M') return { ...c, value: maxSafeWave, updatedBy: 'Vũ Hải Đăng' };
        if (c.key === 'WEATHER_MAX_SAFE_WIND_KMH') return { ...c, value: maxSafeWind, updatedBy: 'Vũ Hải Đăng' };
        if (c.key === 'EMERGENCY_PORT_HOTLINE') return { ...c, value: emergencyHotline, updatedBy: 'Vũ Hải Đăng' };
        return c;
      })
    );
    showToast("Đã lưu các tham số hệ thống và đồng bộ vào AuditLog!");
  };

  const handleResetDefaults = () => {
    setCommissionRate('10');
    setPayoutCycle('15');
    setMaxSafeWave('1.5');
    setMaxSafeWind('25');
    setEmergencyHotline('1900 8989');
    showToast("Đã khôi phục các tham số mặc định của Cảng vụ.");
  };

  return (
    <main className="w-full pt-16 bg-surface px-6 md:px-8 pb-12 min-h-screen">
      <div className="flex flex-col w-full animate-fade-in-up">
        {/* Toast */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-white shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="text-sm font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 mb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-slate-900 tracking-tight">
              Cài đặt Hệ thống
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button 
              onClick={handleResetDefaults}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 text-sm font-medium border border-slate-200 transition-colors shadow-xs"
            >
              <span className="material-symbols-outlined text-[18px]">restart_alt</span>
              <span>Khôi phục mặc định</span>
            </button>
            <button 
              onClick={handleSaveAll}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-primary text-white text-sm font-semibold hover:bg-primary-container transition-colors shadow-xs"
            >
              <span className="material-symbols-outlined text-[18px]">save</span>
              <span>Lưu Cài đặt</span>
            </button>
          </div>
        </div>

        {/* System & Telemetry Banner */}
        <div className="bg-white rounded-xl border border-slate-200 p-4 mb-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center text-primary shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[20px]">verified_user</span>
            </div>
            <div>
              <div className="flex items-center gap-2 font-bold text-sm text-slate-900">
                <span>Kiểm soát Bất biến &amp; Nhật ký Kiểm toán</span>
                <span className="px-1.5 py-0.5 rounded bg-primary/10 text-primary font-mono text-[10px]">audit_logs</span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Mọi thay đổi tham số được ghi vết tự động. Không làm thay đổi tỷ lệ hoa hồng lịch sử của các đơn đã chốt.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4 px-3 py-2 rounded-lg bg-slate-50 border border-slate-200 text-xs shrink-0">
            <div className="flex items-center gap-1.5 text-slate-700 font-medium">
              <span className="material-symbols-outlined text-primary text-[16px]">routine</span>
              <span>Bộ đệm thời tiết:</span>
            </div>
            <div className="flex items-center gap-3 font-semibold text-slate-900">
              <span>Sóng: <strong className="text-primary">0.5m</strong></span>
              <span>Gió: <strong className="text-primary">12km/h</strong></span>
              <span>Mưa: <strong className="text-primary">0mm</strong></span>
            </div>
          </div>
        </div>

        {/* Configuration Form Sections (Vertical Document Layout) */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs divide-y divide-slate-200">
          {/* Section 1: Vận hành & Giao dịch */}
          <div className="p-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[18px]">payments</span>
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">Vận hành &amp; Giao dịch Sàn</h2>
                <p className="text-xs text-slate-500">Tham số tài chính, trích xuất hoa hồng và chu kỳ kế toán</p>
              </div>
            </div>

            <div className="space-y-6 divide-y divide-slate-200">
              {/* Row 1: Commission Rate */}
              <div className="pt-4 first:pt-0 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">Tỷ lệ hoa hồng mặc định sàn</span>
                    <code className="font-mono text-[11px] text-primary bg-primary/10 px-1.5 py-0.5 rounded">PLATFORM_COMMISSION_RATE</code>
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    Khấu trừ tự động vào ví bảo chứng của đối tác trên mỗi dịch vụ con (SubOrder) hoàn tất.
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="number"
                    step="0.5"
                    value={commissionRate}
                    onChange={(e) => setCommissionRate(e.target.value)}
                    className="w-24 h-9 px-3 rounded-lg bg-slate-50 text-right font-bold text-sm text-primary border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                  <span className="text-xs font-bold text-slate-400">%</span>
                </div>
              </div>

              {/* Row 2: Auto Payout Cycle */}
              <div className="pt-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">Chu kỳ rút tiền đối tác</span>
                    <code className="font-mono text-[11px] text-primary bg-primary/10 px-1.5 py-0.5 rounded">AUTO_PAYOUT_CYCLE_DAYS</code>
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    Thời hạn chốt sổ quyết toán settlement và cho phép nhà cung cấp tạo lệnh giải ngân định kỳ.
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="number"
                    value={payoutCycle}
                    onChange={(e) => setPayoutCycle(e.target.value)}
                    className="w-24 h-9 px-3 rounded-lg bg-slate-50 text-right font-bold text-sm text-slate-900 border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                  <span className="text-xs font-bold text-slate-400">ngày</span>
                </div>
              </div>
            </div>
          </div>

          {/* Section 2: An toàn Hàng hải */}
          <div className="p-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-8 h-8 rounded-lg bg-amber-500/10 flex items-center justify-center text-amber-600">
                <span className="material-symbols-outlined text-[18px]">waves</span>
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">Định Mức An Toàn Hàng Hải</h2>
                <p className="text-xs text-slate-500">Ngưỡng thời tiết tự động kích hoạt cảnh báo Cờ Vàng hoặc Cấm Biển xuất bến</p>
              </div>
            </div>

            <div className="space-y-6 divide-y divide-slate-200">
              {/* Row 3: Max Safe Wave */}
              <div className="pt-4 first:pt-0 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">Ngưỡng sóng tối đa cho phép</span>
                    <code className="font-mono text-[11px] text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200">WEATHER_MAX_SAFE_WAVE_M</code>
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    Nếu sóng biển thực đo vượt ngưỡng này, hệ thống tự động cảnh báo cấm xuất bến tour SUP và cano.
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="number"
                    step="0.1"
                    value={maxSafeWave}
                    onChange={(e) => setMaxSafeWave(e.target.value)}
                    className="w-24 h-9 px-3 rounded-lg bg-slate-50 text-right font-bold text-sm text-amber-700 border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                  <span className="text-xs font-bold text-slate-400">mét (m)</span>
                </div>
              </div>

              {/* Row 4: Max Safe Wind */}
              <div className="pt-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">Ngưỡng gió giật tối đa</span>
                    <code className="font-mono text-[11px] text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200">WEATHER_MAX_SAFE_WIND_KMH</code>
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    Kích hoạt thông báo cảnh báo Cờ Vàng tới tất cả thuyền trưởng khi gió giật chạm ngưỡng.
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="number"
                    value={maxSafeWind}
                    onChange={(e) => setMaxSafeWind(e.target.value)}
                    className="w-24 h-9 px-3 rounded-lg bg-slate-50 text-right font-bold text-sm text-amber-700 border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                  <span className="text-xs font-bold text-slate-400">km/h</span>
                </div>
              </div>

              {/* Row 5: Emergency Hotline */}
              <div className="pt-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="max-w-xl">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">Đường dây nóng cứu hộ Cảng vụ</span>
                    <code className="font-mono text-[11px] text-primary bg-primary/10 px-1.5 py-0.5 rounded">EMERGENCY_PORT_HOTLINE</code>
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    Số điện thoại trực ban cứu hộ hiển thị khẩn cấp trên ứng dụng của tài xế cano và hướng dẫn viên.
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="text"
                    value={emergencyHotline}
                    onChange={(e) => setEmergencyHotline(e.target.value)}
                    className="w-36 h-9 px-3 rounded-lg bg-slate-50 text-center font-bold text-sm text-primary border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}

export default Settings;
