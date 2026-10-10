import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { VendorMockService, type VendorProfileData } from '../../../services/vendorMockService';

export function Profile() {
  const [profile, setProfile] = useState<VendorProfileData | null>(null);
  const [activeTab, setActiveTab] = useState<'info' | 'bank' | 'docs'>('info');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Bank form state
  const [bankName, setBankName] = useState('');
  const [bankAccountNumber, setBankAccountNumber] = useState('');
  const [bankAccountHolder, setBankAccountHolder] = useState('');

  // Document preview state
  const [previewDoc, setPreviewDoc] = useState<{ name: string; type: string } | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  useEffect(() => {
    VendorMockService.getProfile().then(data => {
      setProfile(data);
      setBankName(data.bank_name);
      setBankAccountNumber(data.bank_account_number);
      setBankAccountHolder(data.bank_account_holder);
    });
  }, []);

  const handleSaveBank = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bankAccountNumber.trim()) {
      showToast('Vui lòng nhập số tài khoản ngân hàng.');
      return;
    }
    const updated = await VendorMockService.updateBankInfo(bankName, bankAccountNumber, bankAccountHolder);
    setProfile(updated);
    showToast('Đã lưu thông tin tài khoản ngân hàng đối soát (Dữ liệu demo)!');
  };

  const handleUploadDoc = async (type: 'BUSINESS_LICENSE' | 'SAFETY_CERT', file: File) => {
    const updated = await VendorMockService.submitDocument(type, file.name);
    setProfile(updated);
    showToast(`Đã tải lên "${file.name}"! Hồ sơ chuyển sang trạng thái PENDING chờ Cảng vụ phê duyệt.`);
  };

  if (!profile) {
    return (
      <div className="w-full px-6 py-4 text-xs text-black">
        Đang tải hồ sơ đối tác...
      </div>
    );
  }

  return (
    <div className="w-full px-6 py-4 space-y-3.5 bg-background min-h-screen">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <h1 className="text-xl md:text-2xl font-bold text-black tracking-tight">
          Hồ sơ &amp; Pháp lý
        </h1>

        {/* System Read-only Status Badges */}
        <div className="flex items-center gap-2">
          {profile.verification_status === 'APPROVED' && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-emerald-100 text-emerald-800 text-xs font-bold border border-emerald-300">
              <span className="material-symbols-outlined text-[15px]">verified</span>
              <span>Đã duyệt (APPROVED)</span>
            </span>
          )}
          {profile.verification_status === 'PENDING' && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-amber-100 text-amber-800 text-xs font-bold border border-amber-300">
              <span className="material-symbols-outlined text-[15px]">pending</span>
              <span>Chờ duyệt (PENDING)</span>
            </span>
          )}
          {profile.verification_status === 'REJECTED' && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-red-100 text-red-800 text-xs font-bold border border-red-300">
              <span className="material-symbols-outlined text-[15px]">cancel</span>
              <span>Từ chối (REJECTED)</span>
            </span>
          )}

          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 text-slate-800 text-xs font-semibold border border-slate-200">
            <span className="material-symbols-outlined text-[15px] text-primary">military_tech</span>
            <span>Hạng: {profile.badge_tier}</span>
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200">
        <button 
          onClick={() => setActiveTab('info')} 
          className={`px-3 py-2 text-xs font-semibold transition-colors border-b-2 cursor-pointer ${
            activeTab === 'info' ? 'border-primary text-primary font-bold' : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
          type="button"
        >
          Thông tin chung
        </button>
        <button 
          onClick={() => setActiveTab('bank')} 
          className={`px-3 py-2 text-xs font-semibold transition-colors border-b-2 cursor-pointer ${
            activeTab === 'bank' ? 'border-primary text-primary font-bold' : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
          type="button"
        >
          Tài khoản Ngân hàng (Payout)
        </button>
        <button 
          onClick={() => setActiveTab('docs')} 
          className={`px-3 py-2 text-xs font-semibold transition-colors border-b-2 cursor-pointer ${
            activeTab === 'docs' ? 'border-primary text-primary font-bold' : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
          type="button"
        >
          Hồ sơ pháp lý ({profile.documents.length})
        </button>
      </div>

      {/* Tab Body Container */}
      <div>
        {/* TAB 1: General Info */}
        {activeTab === 'info' && (
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3.5">
            <div className="pb-2 border-b border-slate-100">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Thông tin doanh nghiệp</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Tên doanh nghiệp / Hộ kinh doanh</label>
                <input 
                  type="text" 
                  defaultValue={profile.name} 
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 text-xs font-medium focus:outline-none focus:border-primary" 
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Số điện thoại liên hệ</label>
                <input 
                  type="text" 
                  defaultValue={profile.phone} 
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 text-xs font-medium focus:outline-none focus:border-primary" 
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Email tiếp nhận thông báo</label>
                <input 
                  type="email" 
                  defaultValue={profile.email} 
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 text-xs font-medium focus:outline-none focus:border-primary" 
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Địa chỉ cơ sở / Trạm bến trực</label>
                <input 
                  type="text" 
                  defaultValue={profile.address} 
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 text-xs font-medium focus:outline-none focus:border-primary" 
                />
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-slate-100">
              <button 
                onClick={() => showToast('Đã lưu thông tin liên hệ thành công (Dữ liệu demo)!')}
                className="px-3.5 py-2 bg-primary text-white rounded-lg text-xs font-semibold hover:bg-primary-container transition-colors cursor-pointer shadow-xs"
                type="button"
              >
                Lưu thay đổi
              </button>
            </div>
          </div>
        )}

        {/* TAB 2: Bank Account Info */}
        {activeTab === 'bank' && (
          <form onSubmit={handleSaveBank} className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3.5">
            <div className="pb-2 border-b border-slate-100">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Tài khoản nhận tiền đối soát (Payout)</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Tài khoản này dùng để nhận tiền đối soát doanh thu định kỳ 15 ngày và giải ngân theo yêu cầu của Đối tác.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-3 max-w-lg">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Ngân hàng (bank_name)</label>
                <select 
                  value={bankName}
                  onChange={(e) => setBankName(e.target.value)}
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-800 text-xs font-medium focus:outline-none focus:border-primary cursor-pointer"
                >
                  <option value="Vietcombank">Vietcombank - Ngân hàng Ngoại thương Việt Nam</option>
                  <option value="Techcombank">Techcombank - Ngân hàng Kỹ Thương</option>
                  <option value="MB Bank">MB Bank - Ngân hàng Quân Đội</option>
                  <option value="VietinBank">VietinBank - Ngân hàng Công Thương Việt Nam</option>
                  <option value="BIDV">BIDV - Ngân hàng Đầu tư và Phát triển Việt Nam</option>
                </select>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span>Số tài khoản (bank_account_number)</span>
                  <span className="text-[11px] text-slate-400 font-normal">(Bảo lưu số 0 đầu chuỗi)</span>
                </label>
                <input 
                  type="text" 
                  value={bankAccountNumber}
                  onChange={(e) => setBankAccountNumber(e.target.value)}
                  placeholder="Ví dụ: 0041000332891"
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 font-mono font-bold text-xs tracking-wide focus:outline-none focus:border-primary" 
                />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold text-slate-700">Chủ tài khoản (bank_account_holder)</label>
                <input 
                  type="text" 
                  value={bankAccountHolder}
                  onChange={(e) => setBankAccountHolder(e.target.value)}
                  placeholder="Tên in hoa trên thẻ / ĐKKD"
                  className="px-3 py-2 rounded-lg border border-slate-200 bg-white text-slate-900 text-xs uppercase font-semibold focus:outline-none focus:border-primary" 
                />
              </div>
            </div>

            <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 text-slate-600 text-[11px] flex items-start gap-2">
              <span className="material-symbols-outlined text-primary text-[16px] shrink-0 mt-0.5">info</span>
              <span>Dữ liệu số tài khoản được lưu giữ dưới dạng chuỗi chuẩn, bảo toàn đầy đủ các số 0 ở đầu tài khoản ngân hàng.</span>
            </div>

            <div className="pt-2 border-t border-slate-100">
              <button 
                type="submit"
                className="px-3.5 py-2 bg-primary text-white rounded-lg text-xs font-semibold hover:bg-primary-container transition-colors cursor-pointer shadow-xs"
              >
                Cập nhật tài khoản ngân hàng
              </button>
            </div>
          </form>
        )}

        {/* TAB 3: Legal Documents */}
        {activeTab === 'docs' && (
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3.5">
            <div className="pb-2 border-b border-slate-100">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Hồ sơ pháp lý cấp Vendor</h3>
              <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">
                Theo quy chuẩn Cảng vụ DANASEA, hồ sơ cấp Vendor bao gồm duy nhất 02 loại giấy tờ: <strong>BUSINESS_LICENSE</strong> (Giấy phép ĐKKD) và <strong>SAFETY_CERT</strong> (Chứng nhận ATGT đường thủy).
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              {/* Document 1: BUSINESS_LICENSE */}
              {(() => {
                const doc = profile.documents.find(d => d.type === 'BUSINESS_LICENSE');
                return (
                  <div className="p-3.5 border border-slate-200 rounded-lg flex flex-col justify-between gap-3 bg-slate-50">
                    <div className="flex justify-between items-start gap-2">
                      <div>
                        <span className="px-2 py-0.5 rounded bg-slate-200 text-slate-800 text-[10px] font-bold border border-slate-300">
                          BUSINESS_LICENSE
                        </span>
                        <h4 className="text-xs font-bold text-slate-900 mt-1.5">
                          Giấy phép Đăng ký Kinh doanh
                        </h4>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Tệp: <strong className="text-slate-800">{doc ? doc.name : 'Chưa tải lên'}</strong>
                        </p>
                      </div>

                      {doc?.status === 'APPROVED' && (
                        <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 text-[10px] font-semibold flex items-center gap-1 border border-emerald-200 shrink-0">
                          <span className="material-symbols-outlined text-[13px]">verified</span>
                          <span>Đã duyệt</span>
                        </span>
                      )}
                      {doc?.status === 'PENDING' && (
                        <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-700 text-[10px] font-semibold flex items-center gap-1 border border-amber-200 shrink-0">
                          <span className="material-symbols-outlined text-[13px]">pending</span>
                          <span>Chờ duyệt</span>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2 pt-2 border-t border-slate-200">
                      <button 
                        onClick={() => setPreviewDoc({ name: doc?.name || 'Giấy phép ĐKKD', type: 'BUSINESS_LICENSE' })}
                        className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 text-xs font-medium transition-colors cursor-pointer shadow-xs"
                        type="button"
                      >
                        Xem trước
                      </button>
                      <label className="px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-container transition-colors cursor-pointer shadow-xs">
                        Thay thế tệp
                        <input 
                          type="file" 
                          accept=".pdf,.png,.jpg" 
                          className="hidden" 
                          onChange={(e) => {
                            if (e.target.files?.[0]) handleUploadDoc('BUSINESS_LICENSE', e.target.files[0]);
                          }} 
                        />
                      </label>
                    </div>
                  </div>
                );
              })()}

              {/* Document 2: SAFETY_CERT */}
              {(() => {
                const doc = profile.documents.find(d => d.type === 'SAFETY_CERT');
                return (
                  <div className="p-3.5 border border-slate-200 rounded-lg flex flex-col justify-between gap-3 bg-slate-50">
                    <div className="flex justify-between items-start gap-2">
                      <div>
                        <span className="px-2 py-0.5 rounded bg-slate-200 text-slate-800 text-[10px] font-bold border border-slate-300">
                          SAFETY_CERT
                        </span>
                        <h4 className="text-xs font-bold text-slate-900 mt-1.5">
                          Chứng nhận An toàn Giao thông Biển
                        </h4>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Tệp: <strong className="text-slate-800">{doc ? doc.name : 'Chưa tải lên'}</strong>
                        </p>
                      </div>

                      {doc?.status === 'APPROVED' && (
                        <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 text-[10px] font-semibold flex items-center gap-1 border border-emerald-200 shrink-0">
                          <span className="material-symbols-outlined text-[13px]">verified</span>
                          <span>Đã duyệt</span>
                        </span>
                      )}
                      {doc?.status === 'PENDING' && (
                        <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-700 text-[10px] font-semibold flex items-center gap-1 border border-amber-200 shrink-0">
                          <span className="material-symbols-outlined text-[13px]">pending</span>
                          <span>Chờ duyệt</span>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2 pt-2 border-t border-slate-200">
                      <button 
                        onClick={() => setPreviewDoc({ name: doc?.name || 'Chứng nhận ATGT Biển', type: 'SAFETY_CERT' })}
                        className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 text-xs font-medium transition-colors cursor-pointer shadow-xs"
                        type="button"
                      >
                        Xem trước
                      </button>
                      <label className="px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-container transition-colors cursor-pointer shadow-xs">
                        Thay thế tệp
                        <input 
                          type="file" 
                          accept=".pdf,.png,.jpg" 
                          className="hidden" 
                          onChange={(e) => {
                            if (e.target.files?.[0]) handleUploadDoc('SAFETY_CERT', e.target.files[0]);
                          }} 
                        />
                      </label>
                    </div>
                  </div>
                );
              })()}
            </div>
          </div>
        )}
      </div>

      {/* Document Preview Modal (Portaled to body) */}
      {previewDoc && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-4 shadow-xl flex flex-col gap-3 border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[18px]">description</span>
                <h3 className="font-bold text-xs text-slate-900">Xem trước tài liệu</h3>
              </div>
              <button 
                onClick={() => setPreviewDoc(null)} 
                className="text-slate-400 hover:text-slate-600 cursor-pointer"
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>
            <div className="p-4 rounded-lg bg-slate-50 text-center flex flex-col items-center gap-2 border border-slate-200">
              <span className="material-symbols-outlined text-3xl text-primary">article</span>
              <p className="text-xs font-bold text-slate-800">{previewDoc.name}</p>
              <span className="text-[11px] text-slate-500">Định dạng PDF đã nộp lên hệ thống kiểm duyệt Cảng vụ</span>
            </div>
            <div className="flex justify-end pt-2 border-t border-slate-100">
              <button 
                onClick={() => setPreviewDoc(null)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 text-xs font-medium hover:bg-slate-100 transition-colors cursor-pointer"
                type="button"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 px-4 py-2.5 rounded-lg bg-black text-white shadow-xl flex items-center gap-2 border border-slate-600 animate-bounce">
          <span className="material-symbols-outlined text-emerald-400 text-[18px]">task_alt</span>
          <span className="text-xs font-medium">{toastMessage}</span>
        </div>
      )}
    </div>
  );
}
