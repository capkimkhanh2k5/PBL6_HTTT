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
      <div className="p-8 text-center font-body-md text-on-surface-variant">
        Đang tải hồ sơ đối tác...
      </div>
    );
  }

  return (
    <div className="p-space-lg w-full max-w-[1000px] mx-auto flex flex-col min-h-full">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-space-md mb-space-xl">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">
              Thiết lập tài khoản
            </span>
            <span className="px-2 py-0.5 rounded-full bg-surface-container-high text-[11px] font-medium text-on-surface-variant">
              Dữ liệu demo
            </span>
          </div>
          <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold">Hồ sơ &amp; Xác minh Đối tác</h1>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Quản lý thông tin doanh nghiệp, tài khoản nhận tiền đối soát và hồ sơ pháp lý Cảng vụ.
          </p>
        </div>

        {/* System Read-only Status Badges */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          {/* Verification Status (Read-only, Admin controlled) */}
          <div className="flex flex-col items-end">
            <span className="text-[10px] text-on-surface-variant uppercase font-semibold">Trạng thái hồ sơ</span>
            {profile.verification_status === 'APPROVED' && (
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-[11px] font-bold flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">verified</span>
                ĐÃ DUYỆT (APPROVED)
              </span>
            )}
            {profile.verification_status === 'PENDING' && (
              <span className="px-2.5 py-1 rounded-full bg-amber-50 text-amber-700 font-label-sm text-[11px] font-bold flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">pending</span>
                CHỜ DUYỆT (PENDING)
              </span>
            )}
            {profile.verification_status === 'REJECTED' && (
              <span className="px-2.5 py-1 rounded-full bg-red-50 text-red-700 font-label-sm text-[11px] font-bold flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">cancel</span>
                TỪ CHỐI (REJECTED)
              </span>
            )}
          </div>

          {/* Badge Tier (Read-only, System tier) */}
          <div className="flex flex-col items-end pl-2 border-l border-outline-variant/30">
            <span className="text-[10px] text-on-surface-variant uppercase font-semibold">Hạng đối tác</span>
            <span className="px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-[11px] font-bold flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px]">military_tech</span>
              {profile.badge_tier}
            </span>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-space-sm mb-space-lg border-b border-outline-variant/30">
        <button 
          onClick={() => setActiveTab('info')} 
          className={`px-space-md py-space-sm font-label-lg text-label-lg transition-colors border-b-2 ${
            activeTab === 'info' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-on-surface'
          }`}
        >
          Thông tin chung
        </button>
        <button 
          onClick={() => setActiveTab('bank')} 
          className={`px-space-md py-space-sm font-label-lg text-label-lg transition-colors border-b-2 ${
            activeTab === 'bank' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-on-surface'
          }`}
        >
          Tài khoản Ngân hàng (Payout)
        </button>
        <button 
          onClick={() => setActiveTab('docs')} 
          className={`px-space-md py-space-sm font-label-lg text-label-lg transition-colors border-b-2 ${
            activeTab === 'docs' ? 'border-primary text-primary font-bold' : 'border-transparent text-on-surface-variant hover:text-on-surface'
          }`}
        >
          Hồ sơ pháp lý ({profile.documents.length})
        </button>
      </div>

      {/* Tab Body Container */}
      <div key={activeTab} className="animate-fade-in-up">
        {/* TAB 1: General Info */}
        {activeTab === 'info' && (
        <div className="bg-surface-container-lowest p-space-xl rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-lg">
          <div className="flex items-center justify-between">
            <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">Thông tin doanh nghiệp</h3>
            <span className="text-[12px] text-on-surface-variant italic">* Trạng thái duyệt hồ sơ do Cảng vụ quyết định, đối tác không tự điều chỉnh.</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface-variant font-semibold">Tên doanh nghiệp / Hộ kinh doanh</label>
              <input 
                type="text" 
                defaultValue={profile.name} 
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md focus:outline-primary" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface-variant font-semibold">Số điện thoại liên hệ</label>
              <input 
                type="text" 
                defaultValue={profile.phone} 
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md focus:outline-primary" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface-variant font-semibold">Email tiếp nhận thông báo</label>
              <input 
                type="email" 
                defaultValue={profile.email} 
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md focus:outline-primary" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface-variant font-semibold">Địa chỉ cơ sở / Trạm bến trực</label>
              <input 
                type="text" 
                defaultValue={profile.address} 
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md focus:outline-primary" 
              />
            </div>
          </div>

          <button 
            onClick={() => showToast('Đã lưu thông tin liên hệ thành công (Dữ liệu demo)!')}
            className="self-end px-space-lg py-2.5 bg-primary text-on-primary rounded-xl font-label-md font-bold hover:bg-primary-container transition-colors shadow-sm"
          >
            Lưu thay đổi
          </button>
        </div>
      )}

      {/* TAB 2: Bank Account Info */}
      {activeTab === 'bank' && (
        <form onSubmit={handleSaveBank} className="bg-surface-container-lowest p-space-xl rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-lg">
          <div>
            <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">Tài khoản nhận tiền (Payout)</h3>
            <p className="font-body-sm text-on-surface-variant mt-1">
              Tài khoản này được đồng bộ để nhận tiền đối soát doanh thu định kỳ 15 ngày và xử lý các yêu cầu rút tiền của Vendor.
            </p>
          </div>

          <div className="grid grid-cols-1 gap-space-md max-w-lg">
            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface font-semibold">Ngân hàng (bank_name)</label>
              <select 
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md focus:outline-primary cursor-pointer"
              >
                <option value="Vietcombank">Vietcombank - Ngân hàng Ngoại thương Việt Nam</option>
                <option value="Techcombank">Techcombank - Ngân hàng Kỹ Thương</option>
                <option value="MB Bank">MB Bank - Ngân hàng Quân Đội</option>
                <option value="VietinBank">VietinBank - Ngân hàng Công Thương Việt Nam</option>
                <option value="BIDV">BIDV - Ngân hàng Đầu tư và Phát triển Việt Nam</option>
              </select>
            </div>

            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface font-semibold">
                Số tài khoản (bank_account_number)
                <span className="text-[11px] text-on-surface-variant font-normal ml-2">(Bảo lưu số 0 đầu chuỗi)</span>
              </label>
              <input 
                type="text" 
                value={bankAccountNumber}
                onChange={(e) => setBankAccountNumber(e.target.value)}
                placeholder="Ví dụ: 0041000332891"
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-headline-sm font-bold tracking-wide focus:outline-primary" 
              />
            </div>

            <div className="flex flex-col gap-1">
              <label className="font-label-md text-on-surface font-semibold">Chủ tài khoản (bank_account_holder)</label>
              <input 
                type="text" 
                value={bankAccountHolder}
                onChange={(e) => setBankAccountHolder(e.target.value)}
                placeholder="Tên in hoa trên thẻ / ĐKKD"
                className="px-space-md py-2.5 rounded-xl border border-outline-variant/50 bg-surface text-on-surface font-body-md uppercase font-semibold focus:outline-primary" 
              />
            </div>
          </div>

          <div className="p-space-sm rounded-xl bg-surface-container-low text-on-surface-variant text-[12px] flex items-start gap-2">
            <span className="material-symbols-outlined text-primary text-[18px]">info</span>
            <span>Dữ liệu số tài khoản được lưu giữ dưới dạng chuỗi chuẩn, bảo toàn đầy đủ các số 0 ở đầu tài khoản ngân hàng.</span>
          </div>

          <button 
            type="submit"
            className="self-start px-space-lg py-2.5 bg-primary text-on-primary rounded-xl font-label-md font-bold hover:bg-primary-container transition-colors shadow-sm"
          >
            Cập nhật tài khoản ngân hàng
          </button>
        </form>
      )}

      {/* TAB 3: Legal Documents */}
      {activeTab === 'docs' && (
        <div className="bg-surface-container-lowest p-space-xl rounded-2xl border border-outline-variant/30 shadow-sm flex flex-col gap-space-lg">
          <div>
            <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">Hồ sơ pháp lý cấp Vendor</h3>
            <p className="font-body-sm text-on-surface-variant mt-1">
              Theo quy chuẩn Cảng vụ DANASEA, hồ sơ cấp Vendor bao gồm duy nhất 02 loại giấy tờ: <strong>BUSINESS_LICENSE</strong> (Giấy phép ĐKKD) và <strong>SAFETY_CERT</strong> (Chứng nhận ATGT đường thủy). <em>(Kiểm định an toàn cho từng phương tiện/tour được quản lý riêng tại mục Chứng chỉ an toàn dịch vụ).</em>
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
            {/* Document 1: BUSINESS_LICENSE */}
            {(() => {
              const doc = profile.documents.find(d => d.type === 'BUSINESS_LICENSE');
              return (
                <div className="p-space-md border border-outline-variant/50 rounded-2xl flex flex-col justify-between gap-space-md relative overflow-hidden bg-surface-container-low/20">
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="px-2 py-0.5 rounded bg-primary/10 text-primary font-label-sm text-[10px] font-bold">
                        BUSINESS_LICENSE
                      </span>
                      <h4 className="font-headline-sm text-headline-sm font-bold text-on-surface mt-1">
                        Giấy phép Đăng ký Kinh doanh
                      </h4>
                      <p className="font-body-sm text-on-surface-variant mt-0.5 text-[12px]">
                        Tệp: {doc ? doc.name : 'Chưa tải lên'}
                      </p>
                    </div>

                    {doc?.status === 'APPROVED' && (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">verified</span> ĐÃ DUYỆT
                      </span>
                    )}
                    {doc?.status === 'PENDING' && (
                      <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 text-[10px] font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">pending</span> CHỜ DUYỆT
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 pt-2 border-t border-outline-variant/20">
                    <button 
                      onClick={() => setPreviewDoc({ name: doc?.name || 'Giấy phép ĐKKD', type: 'BUSINESS_LICENSE' })}
                      className="px-3 py-1.5 rounded-lg bg-surface-container-high text-on-surface font-label-sm text-label-sm hover:bg-surface-container-highest"
                    >
                      Xem trước
                    </button>
                    <label className="px-3 py-1.5 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-bold hover:bg-primary-container cursor-pointer">
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
                <div className="p-space-md border border-outline-variant/50 rounded-2xl flex flex-col justify-between gap-space-md relative overflow-hidden bg-surface-container-low/20">
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="px-2 py-0.5 rounded-full bg-secondary/10 text-secondary font-label-sm text-[10px] font-bold">
                        SAFETY_CERT
                      </span>
                      <h4 className="font-headline-sm text-headline-sm font-bold text-on-surface mt-1">
                        Chứng nhận An toàn Giao thông Biển
                      </h4>
                      <p className="font-body-sm text-on-surface-variant mt-0.5 text-[12px]">
                        Tệp: {doc ? doc.name : 'Chưa tải lên'}
                      </p>
                    </div>

                    {doc?.status === 'APPROVED' && (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">verified</span> ĐÃ DUYỆT
                      </span>
                    )}
                    {doc?.status === 'PENDING' && (
                      <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 text-[10px] font-bold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">pending</span> CHỜ DUYỆT
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 pt-2 border-t border-outline-variant/20">
                    <button 
                      onClick={() => setPreviewDoc({ name: doc?.name || 'Chứng nhận ATGT Biển', type: 'SAFETY_CERT' })}
                      className="px-3 py-1.5 rounded-lg bg-surface-container-high text-on-surface font-label-sm text-label-sm hover:bg-surface-container-highest"
                    >
                      Xem trước
                    </button>
                    <label className="px-3 py-1.5 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-bold hover:bg-primary-container cursor-pointer">
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
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface rounded-2xl max-w-md w-full p-space-lg shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">description</span>
                <h3 className="font-headline-sm font-bold text-on-surface">Xem trước tài liệu</h3>
              </div>
              <button onClick={() => setPreviewDoc(null)} className="text-on-surface-variant hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-space-lg rounded-xl bg-surface-container-low text-center flex flex-col items-center gap-2">
              <span className="material-symbols-outlined text-5xl text-primary">article</span>
              <p className="font-label-md font-bold text-on-surface">{previewDoc.name}</p>
              <span className="text-[12px] text-on-surface-variant">Định dạng PDF đã nộp lên hệ thống kiểm duyệt Cảng vụ</span>
            </div>
            <div className="flex justify-end pt-2">
              <button 
                onClick={() => setPreviewDoc(null)}
                className="px-4 py-2 rounded-xl bg-surface-container-high text-on-surface font-label-md"
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
        <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
          <span className="font-label-md text-label-md">{toastMessage}</span>
        </div>
      )}
    </div>
  );
}
