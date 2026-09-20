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
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full animate-fade-in-up">
        {/* Toast */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Page Header */}
        <div className="flex flex-col xl:flex-row xl:items-end justify-between gap-space-lg mb-space-xl">
          <div className="flex flex-col gap-space-xs max-w-3xl">
            <div className="flex items-center gap-space-sm font-bold">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm uppercase tracking-wider">
                <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
                Cơ chế runtime production • node-danang-alpha-02
              </span>
              <span className="font-label-sm text-outline">v4.18.2-maritime</span>
            </div>
            <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black">
              Cấu hình Tham số Hệ thống &amp; An toàn Hàng hải
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Kiểm soát các tham số vận hành toàn sàn (system_configs), quy tắc chu kỳ chi trả, tỷ lệ hoa hồng mặc định và định mức an toàn mặt nước theo chuẩn Cảng vụ DANASEA.
            </p>
          </div>

          <div className="flex items-center gap-space-sm self-start xl:self-auto shrink-0">
            <button 
              onClick={() => showToast("Đã đồng bộ tức thì các tham số sang cụm bộ nhớ đệm Redis!")}
              className="flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-primary font-label-lg shadow-sm hover:bg-surface-container transition-all border border-outline-variant/20 font-semibold"
            >
              <span className="material-symbols-outlined text-[18px]">sync</span>
              <span>Đồng bộ Redis Cache</span>
            </button>
            <button 
              onClick={handleResetDefaults}
              className="flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-high text-on-surface font-label-lg hover:bg-surface-container transition-all font-semibold"
            >
              <span className="material-symbols-outlined text-[18px]">restart_alt</span>
              <span>Khôi phục mặc định</span>
            </button>
            <button 
              onClick={handleSaveAll}
              className="flex items-center gap-2 px-space-lg py-2.5 rounded-xl bg-primary text-on-primary font-label-lg shadow-md hover:bg-primary-container transition-all font-bold"
            >
              <span className="material-symbols-outlined text-[18px]">save</span>
              <span>Lưu thay đổi tham số</span>
            </button>
          </div>
        </div>

        {/* Operational Notices */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-md mb-space-xl">
          <div className="lg:col-span-7 bg-surface-container-lowest p-space-lg rounded-xl shadow-sm flex items-start gap-space-md relative overflow-hidden border border-outline-variant/20">
            <div className="w-1.5 absolute left-0 top-0 bottom-0 bg-primary"></div>
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary shrink-0">
              <span className="material-symbols-outlined text-[24px]">verified_user</span>
            </div>
            <div className="flex flex-col gap-1 min-w-0">
              <div className="flex items-center gap-2 font-bold">
                <span className="font-headline-sm text-on-surface">Kiểm soát Bất biến &amp; Nhật ký Kiểm toán</span>
                <span className="px-2 py-0.5 rounded bg-primary text-on-primary font-mono text-xs">audit_logs strict</span>
              </div>
              <p className="font-body-md text-xs text-on-surface-variant leading-relaxed">
                Mọi thay đổi tham số hệ thống sẽ được ghi vết tự động vào Nhật ký kiểm toán (audit_logs). 
                <strong> Tuyệt đối không làm thay đổi tỷ lệ hoa hồng lịch sử</strong> của các đơn hàng đã chốt (commission snapshot).
              </p>
            </div>
          </div>

          <div className="lg:col-span-5 bg-inverse-surface text-on-primary p-space-lg rounded-xl shadow-sm flex flex-col justify-between border border-outline-variant/20">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2 font-bold">
                <span className="material-symbols-outlined text-inverse-primary text-[20px]">routine</span>
                <span className="text-xs uppercase tracking-wider">Bộ nhớ đệm thời tiết biển (weather_cache)</span>
              </div>
              <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-primary-container text-on-primary-container text-xs font-bold">
                TTL: 15 phút
              </span>
            </div>
            <div className="grid grid-cols-3 gap-2 my-space-sm bg-surface-variant/10 p-2.5 rounded-lg text-center">
              <div>
                <span className="text-xs text-outline-variant block">Sóng biển</span>
                <span className="font-headline-sm font-black text-inverse-primary">0.5 m</span>
              </div>
              <div>
                <span className="text-xs text-outline-variant block">Tốc độ gió</span>
                <span className="font-headline-sm font-black text-inverse-primary">12 km/h</span>
              </div>
              <div>
                <span className="text-xs text-outline-variant block">Lượng mưa</span>
                <span className="font-headline-sm font-black text-inverse-primary">0 mm</span>
              </div>
            </div>
          </div>
        </div>

        {/* Configuration Blocks */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-space-xl mb-space-2xl">
          {/* BLOCK 1: Vận hành & Giao dịch */}
          <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl flex flex-col justify-between border border-outline-variant/20">
            <div>
              <div className="flex items-center gap-3 mb-space-md border-b border-outline-variant/10 pb-3">
                <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                  <span className="material-symbols-outlined text-[22px]">payments</span>
                </div>
                <div>
                  <h2 className="font-headline-md font-bold text-on-surface">Khối 1: Vận hành &amp; Giao dịch Sàn</h2>
                  <span className="text-xs text-outline">Tham số tài chính và chu kỳ kế toán</span>
                </div>
              </div>

              <div className="flex flex-col gap-4 mt-2">
                <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-on-surface block">Tỷ lệ hoa hồng mặc định sàn (%)</span>
                      <code className="font-mono text-xs text-primary font-bold">PLATFORM_COMMISSION_RATE</code>
                    </div>
                    <div className="flex items-center gap-1">
                      <input 
                        type="number"
                        step="0.5"
                        value={commissionRate}
                        onChange={(e) => setCommissionRate(e.target.value)}
                        className="w-20 h-10 px-2 rounded-lg bg-surface-container-lowest text-right font-black text-base text-primary border border-outline-variant/30"
                      />
                      <span className="font-bold text-sm text-outline">%</span>
                    </div>
                  </div>
                  <p className="text-xs text-on-surface-variant">
                    Khấu trừ tự động vào ví bảo chứng của đối tác trên mỗi dịch vụ con (SubOrder) hoàn tất.
                  </p>
                </div>

                <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-on-surface block">Chu kỳ rút tiền đối tác (ngày)</span>
                      <code className="font-mono text-xs text-primary font-bold">AUTO_PAYOUT_CYCLE_DAYS</code>
                    </div>
                    <div className="flex items-center gap-1">
                      <input 
                        type="number"
                        value={payoutCycle}
                        onChange={(e) => setPayoutCycle(e.target.value)}
                        className="w-20 h-10 px-2 rounded-lg bg-surface-container-lowest text-right font-black text-base text-on-surface border border-outline-variant/30"
                      />
                      <span className="font-bold text-xs text-outline">ngày</span>
                    </div>
                  </div>
                  <p className="text-xs text-on-surface-variant">
                    Thời hạn chốt sổ settlement và cho phép NCC tạo lệnh giải ngân định kỳ.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* BLOCK 2: An toàn Hàng hải */}
          <div className="bg-surface-container-lowest rounded-xl shadow-sm p-space-xl flex flex-col justify-between border border-outline-variant/20">
            <div>
              <div className="flex items-center gap-3 mb-space-md border-b border-outline-variant/10 pb-3">
                <div className="w-10 h-10 rounded-xl bg-secondary-container/20 flex items-center justify-center text-secondary">
                  <span className="material-symbols-outlined text-[22px]">waves</span>
                </div>
                <div>
                  <h2 className="font-headline-md font-bold text-on-surface">Khối 2: Định Mức An Toàn Hàng Hải</h2>
                  <span className="text-xs text-outline">Ngưỡng tự động kích hoạt Cờ Vàng / Cấm Biển</span>
                </div>
              </div>

              <div className="flex flex-col gap-4 mt-2">
                <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-on-surface block">Ngưỡng sóng tối đa cho phép (mét)</span>
                      <code className="font-mono text-xs text-primary font-bold">WEATHER_MAX_SAFE_WAVE_M</code>
                    </div>
                    <div className="flex items-center gap-1">
                      <input 
                        type="number"
                        step="0.1"
                        value={maxSafeWave}
                        onChange={(e) => setMaxSafeWave(e.target.value)}
                        className="w-20 h-10 px-2 rounded-lg bg-surface-container-lowest text-right font-black text-base text-secondary border border-outline-variant/30"
                      />
                      <span className="font-bold text-xs text-outline">m</span>
                    </div>
                  </div>
                  <p className="text-xs text-on-surface-variant">
                    Nếu sóng biển thực đo &gt; ngưỡng này, hệ thống tự động cấm xuất bến tour SUP &amp; cano cao tốc.
                  </p>
                </div>

                <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-on-surface block">Ngưỡng gió giật tối đa (km/h)</span>
                      <code className="font-mono text-xs text-primary font-bold">WEATHER_MAX_SAFE_WIND_KMH</code>
                    </div>
                    <div className="flex items-center gap-1">
                      <input 
                        type="number"
                        value={maxSafeWind}
                        onChange={(e) => setMaxSafeWind(e.target.value)}
                        className="w-20 h-10 px-2 rounded-lg bg-surface-container-lowest text-right font-black text-base text-secondary border border-outline-variant/30"
                      />
                      <span className="font-bold text-xs text-outline">km/h</span>
                    </div>
                  </div>
                  <p className="text-xs text-on-surface-variant">
                    Kích hoạt thông báo cảnh báo Cờ Vàng tới tất cả thuyền trưởng khi gió giật chạm ngưỡng.
                  </p>
                </div>

                <div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-on-surface block">Đường dây nóng cứu hộ Cảng vụ</span>
                      <code className="font-mono text-xs text-primary font-bold">EMERGENCY_PORT_HOTLINE</code>
                    </div>
                    <input 
                      type="text"
                      value={emergencyHotline}
                      onChange={(e) => setEmergencyHotline(e.target.value)}
                      className="w-32 h-10 px-2 rounded-lg bg-surface-container-lowest text-center font-black text-sm text-primary border border-outline-variant/30"
                    />
                  </div>
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
