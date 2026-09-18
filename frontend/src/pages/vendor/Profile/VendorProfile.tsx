export function VendorProfile() {
  return (
    <main className="w-full pt-16 bg-background flex-1"><div className="flex flex-col w-full">
{/* Status & Profile Ribbon */}
<div className="w-full bg-surface-container-low px-space-lg py-space-xl">
<div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-center lg:justify-between gap-space-lg">
<div className="flex items-start sm:items-center gap-space-lg">
<div className="relative w-20 h-20 rounded-2xl bg-surface-container-lowest shadow-md overflow-hidden flex-shrink-0 flex items-center justify-center">
<img className="w-full h-full object-cover" data-alt="Danang Ocean Club maritime sports club insignia with stylized waves and sailing catamaran in ocean teal and bright coral accents, framed against a crisp sunlit coastal backdrop with clean modern graphics." src="https://lh3.googleusercontent.com/aida-public/AB6AXuBElQeXIOk0RI7zUk-dU71wlKrRPyDE0whirwnxseg9DedCpb6U0mBOICJq6qPiSsPvvtKp9LBcs9LxkuesoQ0pyZIrjAPZhQZiocxSdhNPk1yqwDz156GwT1Mv7FT6WEuRjPgJzlDSa5SIJBGfJKT2geO7bUZ5rYKmzevOf-osQzVT7LQogxvGIG1wW4a5ildlusUq8_d65wZQQma-8gFvdf2qemA2koYM0EUBYsfDsA-xexrFr9BIEw"/>
<div className="absolute bottom-0 right-0 bg-primary-container text-on-primary-container p-1 rounded-tl-lg shadow-sm">
<span className="material-symbols-outlined text-[16px]">verified</span>
</div>
</div>
<div className="flex flex-col min-w-0">
<div className="flex flex-wrap items-center gap-space-sm mb-1">
<h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight truncate">Hồ sơ doanh nghiệp &amp; Hồ sơ pháp lý hàng hải</h1>
</div>
<div className="flex flex-wrap items-center gap-space-sm">
<span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-container text-on-primary-container font-label-md text-label-md shadow-sm">
<span className="material-symbols-outlined text-[16px]">verified</span>
              ĐÃ XÁC THỰC (VERIFIED)
            </span>
<span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-md text-label-md shadow-sm">
<span className="material-symbols-outlined text-[16px]">military_tech</span>
              Huy hiệu Vận hành Tiêu chuẩn (TOP_RATED)
            </span>
<div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container-lowest text-on-surface shadow-sm font-label-md text-label-md">
<span className="material-symbols-outlined text-secondary text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
<span className="font-bold">4.9</span>
<span className="text-on-surface-variant font-normal">/ 5.0</span>
<span className="text-outline-variant">•</span>
<span className="text-on-surface-variant">128 đánh giá</span>
</div>
</div>
</div>
</div>
<div className="flex items-center gap-space-sm self-start lg:self-center">
<a className="px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-primary font-label-md text-label-md hover:bg-surface-container transition-all flex items-center gap-2 shadow-sm" href="#compliance-policy">
<span className="material-symbols-outlined text-body-lg">policy</span>
          Quy chế kiểm duyệt
        </a>
<button className="px-space-lg py-2.5 rounded-xl bg-primary-container text-on-primary-container font-label-md text-label-md hover:bg-primary transition-all flex items-center gap-2 shadow-md" type="button">
<span className="material-symbols-outlined text-body-lg">save</span>
          Lưu thay đổi hồ sơ
        </button>
</div>
</div>
</div>
{/* Internal Navigation Tabs */}
<div className="w-full bg-surface-container-lowest shadow-sm sticky top-16 z-30">
<div className="max-w-7xl mx-auto px-space-lg flex items-center gap-space-xs overflow-x-auto py-2">
<button className="px-space-md py-2.5 rounded-xl bg-primary-container text-on-primary-container font-label-md text-label-md flex items-center gap-2 flex-shrink-0 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[18px]">apartment</span>
        Thông tin doanh nghiệp
      </button>
<a className="px-space-md py-2.5 rounded-xl text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low font-label-md text-label-md flex items-center gap-2 flex-shrink-0 transition-all" href="#bank-details-section">
<span className="material-symbols-outlined text-[18px]">account_balance</span>
        Tài khoản ngân hàng thụ hưởng
      </a>
<a className="px-space-md py-2.5 rounded-xl text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low font-label-md text-label-md flex items-center gap-2 flex-shrink-0 transition-all" href="#documents-section">
<span className="material-symbols-outlined text-[18px]">gavel</span>
        Giấy phép &amp; Chứng chỉ an toàn
        <span className="w-2 h-2 rounded-full bg-secondary-container"></span>
</a>
<a className="px-space-md py-2.5 rounded-xl text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low font-label-md text-label-md flex items-center gap-2 flex-shrink-0 transition-all" href="#compliance-policy">
<span className="material-symbols-outlined text-[18px]">menu_book</span>
        Hướng dẫn &amp; Quy định kiểm duyệt
      </a>
</div>
</div>
{/* Main Content Space */}
<div className="max-w-7xl mx-auto w-full px-space-lg py-space-2xl flex flex-col gap-space-3xl">
{/* Section 1: Business Profile Information */}
<div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
{/* Left Metadata & Context Banner */}
<div className="lg:col-span-4 flex flex-col gap-space-md">
<div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-md">
<div className="flex items-center gap-space-sm text-primary">
<span className="material-symbols-outlined text-headline-sm">storefront</span>
<span className="font-headline-sm text-headline-sm text-on-surface">Đơn vị Hàng hải</span>
</div>
<p className="font-body-md text-body-md text-on-surface-variant">
            Thông tin định danh pháp lý được hiển thị công khai trên hợp đồng trải nghiệm khách hàng và thông báo điều phối phao tiêu, cứu hộ của Cảng vụ Đà Nẵng.
          </p>
<div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-2">
<div className="flex items-center justify-between">
<span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Hạn mức vận hành</span>
<span className="font-label-sm text-label-sm text-primary font-bold">Cấp 1 (Quốc gia)</span>
</div>
<div className="flex items-center justify-between">
<span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Mã hồ sơ VITA</span>
<span className="font-label-sm text-label-sm text-on-surface font-semibold">DNS-DOC-9827</span>
</div>
<div className="flex items-center justify-between">
<span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Phân khu khai thác</span>
<span className="font-label-sm text-label-sm text-on-surface font-semibold">Bãi tắm Mỹ Khê 2</span>
</div>
</div>
</div>
{/* Maritime Operational Badge Card */}
<div className="relative overflow-hidden p-space-lg rounded-2xl bg-tertiary text-on-tertiary shadow-md">
<div className="relative z-10 flex flex-col gap-2">
<span className="font-label-sm text-label-sm uppercase tracking-widest text-primary-fixed-dim">Tiêu chuẩn kiểm duyệt</span>
<span className="font-headline-sm text-headline-sm">An toàn Mặt nước Đạt chuẩn</span>
<p className="font-body-sm text-body-sm text-tertiary-fixed opacity-90">
              Được bảo trợ kiểm định cứu hộ định kỳ bởi BQL Bán đảo Sơn Trà &amp; Các bãi biển du lịch Đà Nẵng.
            </p>
</div>
<span className="material-symbols-outlined absolute -right-6 -bottom-6 text-[110px] text-tertiary-container/30 pointer-events-none">shield_with_heart</span>
</div>
</div>
{/* Right Form Fields */}
<div className="lg:col-span-8 p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-lg">
<div className="flex items-center justify-between">
<h2 className="font-headline-md text-headline-md text-on-surface">1. Thông tin doanh nghiệp (vendors)</h2>
<span className="px-2.5 py-1 rounded-md bg-surface-container font-label-sm text-label-sm text-on-surface-variant">Bắt buộc theo NĐ 48/CP</span>
</div>
<div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
<div className="md:col-span-2 flex flex-col gap-1.5">
<label className="font-label-md text-label-md text-on-surface">Tên đơn vị / Câu lạc bộ</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">sailing</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="text" defaultValue="Câu lạc bộ Thể thao biển Danang Ocean Club"/>
</div>
</div>
<div className="flex flex-col gap-1.5">
<div className="flex items-center justify-between">
<label className="font-label-md text-label-md text-on-surface">Mã số thuế (MST)</label>
<span className="font-label-sm text-label-sm text-outline">Tùy chọn</span>
</div>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">pin</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="text" defaultValue="0401982736"/>
</div>
</div>
<div className="flex flex-col gap-1.5">
<label className="font-label-md text-label-md text-on-surface">Người đại diện pháp luật</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">badge</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="text" defaultValue="Lê Hoàng Hải"/>
</div>
</div>
<div className="flex flex-col gap-1.5">
<label className="font-label-md text-label-md text-on-surface">Số điện thoại điều hành</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">call</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="text" defaultValue="0905 123 456"/>
</div>
</div>
<div className="flex flex-col gap-1.5">
<label className="font-label-md text-label-md text-on-surface">Email liên hệ vận hành</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">alternate_email</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="email" defaultValue="hai.le@danangoceanclub.vn"/>
</div>
</div>
<div className="md:col-span-2 flex flex-col gap-1.5">
<label className="font-label-md text-label-md text-on-surface">Địa chỉ trụ sở / Bến bãi tiếp nhận</label>
<div className="relative flex items-center">
<span className="material-symbols-outlined absolute left-3.5 text-on-surface-variant text-body-lg pointer-events-none">location_on</span>
<input className="w-full pl-11 pr-4 py-3 bg-surface-container-low text-on-surface rounded-xl font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" type="text" defaultValue="Bãi tắm Mỹ Khê 2, Đường Võ Nguyên Giáp, Phường Phước Mỹ, Quận Sơn Trà, TP. Đà Nẵng"/>
</div>
</div>
</div>
<div className="flex items-center justify-between pt-2">
<div className="flex items-center gap-2 text-primary font-label-md text-label-md">
<span className="material-symbols-outlined text-[18px]">security_update_good</span>
            Dữ liệu đồng bộ trực tiếp với CSDL Quản lý Thể thao biển
          </div>
<button className="px-space-md py-2 rounded-xl bg-primary-container text-on-primary-container font-label-md text-label-md hover:bg-primary transition-all shadow-sm" type="button">
            Cập nhật thông tin
          </button>
</div>
</div>
</div>
{/* Section 2: Beneficiary Bank Account */}
<div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start" id="bank-details-section">
<div className="lg:col-span-4 flex flex-col gap-space-md">
<div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-sm">
<div className="flex items-center gap-space-sm text-primary">
<span className="material-symbols-outlined text-headline-sm">account_balance</span>
<h3 className="font-headline-sm text-headline-sm text-on-surface">Thanh toán &amp; Quyết toán</h3>
</div>
<p className="font-body-md text-body-md text-on-surface-variant">
            Tài khoản thụ hưởng doanh thu đặt vé SUP, dù lượn, cano và cano kéo phao từ du khách qua cổng thanh toán DANASEA Escrow.
          </p>
<div className="mt-2 p-3 rounded-xl bg-primary-fixed/30 flex items-start gap-2">
<span className="material-symbols-outlined text-primary text-[20px] mt-0.5">lock</span>
<span className="font-body-sm text-body-sm text-on-surface">Tài khoản doanh nghiệp được bảo mật cấp ngân hàng. Tự động đối soát định kỳ thứ 3 và thứ 6 hàng tuần.</span>
</div>
</div>
</div>
<div className="lg:col-span-8 p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-lg">
<div className="flex items-center justify-between">
<div>
<h2 className="font-headline-md text-headline-md text-on-surface">2. Tài khoản ngân hàng thụ hưởng</h2>
<p className="font-body-sm text-body-sm text-on-surface-variant">Tài khoản chính nhận chuyển khoản định kỳ</p>
</div>
<span className="px-3 py-1 rounded-full bg-primary-container/20 text-primary font-label-sm text-label-sm font-bold flex items-center gap-1">
<span className="material-symbols-outlined text-[14px]">check_circle</span>
            ĐÃ XÁC NHẬN NAPAS 247
          </span>
</div>
<div className="p-space-lg rounded-2xl bg-surface-container-low flex flex-col gap-space-md relative overflow-hidden">
<div className="flex flex-col md:flex-row md:items-center justify-between gap-space-md pb- space-md">
<div className="flex items-center gap-space-md">
<div className="w-14 h-14 rounded-xl bg-surface-container-lowest flex items-center justify-center shadow-sm">
<span className="material-symbols-outlined text-primary text-headline-lg">account_balance</span>
</div>
<div className="flex flex-col">
<span className="font-label-sm text-label-sm text-outline-variant uppercase tracking-wider">Ngân hàng thụ hưởng</span>
<span className="font-headline-sm text-headline-sm text-on-surface">Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)</span>
<span className="font-body-sm text-body-sm text-on-surface-variant">Chi nhánh: CN Đà Nẵng</span>
</div>
</div>
<div className="flex items-center gap-2">
<button className="px-3 py-1.5 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" id="toggleBankVisibility" type="button">
<span className="material-symbols-outlined text-[18px]" id="eyeIcon">visibility</span>
<span id="eyeText">Xem đầy đủ</span>
</button>
<button className="px-3 py-1.5 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" id="copyBankBtn" title="Sao chép số tài khoản" type="button">
<span className="material-symbols-outlined text-[18px]">content_copy</span>
<span>Sao chép</span>
</button>
</div>
</div>
<div className="grid grid-cols-1 md:grid-cols-2 gap-space-md pt-space-sm">
<div className="p-space-md rounded-xl bg-surface-container-lowest shadow-sm flex flex-col">
<span className="font-label-sm text-label-sm text-outline-variant uppercase tracking-wider">Số tài khoản</span>
<div className="flex items-center justify-between mt-1">
<span className="font-headline-sm text-headline-sm text-primary tracking-widest" id="accountNumber">•••• •••• •••• 8921</span>
<span className="material-symbols-outlined text-primary text-[20px]">credit_card</span>
</div>
</div>
<div className="p-space-md rounded-xl bg-surface-container-lowest shadow-sm flex flex-col">
<span className="font-label-sm text-label-sm text-outline-variant uppercase tracking-wider">Tên chủ tài khoản</span>
<div className="flex items-center justify-between mt-1">
<span className="font-headline-sm text-headline-sm text-on-surface truncate uppercase">CONG TY TNHH THE THAO BIEN DANANG OCEAN</span>
<span className="material-symbols-outlined text-primary text-[20px]">corporate_fare</span>
</div>
</div>
</div>
</div>
<div className="flex items-center justify-end gap-space-sm">
<button className="px-space-md py-2.5 rounded-xl bg-surface-container-low text-on-surface font-label-md text-label-md hover:bg-surface-container transition-all" type="button">
            Yêu cầu đổi tài khoản thụ hưởng
          </button>
</div>
</div>
</div>
{/* Section 3: Maritime Legal & Safety Documentation (vendor_documents) */}
<div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start" id="documents-section">
<div className="lg:col-span-4 flex flex-col gap-space-md">
<div className="p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-sm">
<div className="flex items-center gap-space-sm text-primary">
<span className="material-symbols-outlined text-headline-sm">verified_user</span>
<h3 className="font-headline-sm text-headline-sm text-on-surface">Tài liệu pháp lý &amp; An toàn</h3>
</div>
<p className="font-body-md text-body-md text-on-surface-variant">
            Toàn bộ giấy chứng nhận an toàn kỹ thuật, bảo hiểm trách nhiệm dân sự đường thủy và bằng lái cano cao tốc do Cảng vụ Hàng hải Đà Nẵng đối chiếu.
          </p>
<div className="flex flex-col gap-2 mt-2">
<div className="flex items-center justify-between p-2.5 rounded-xl bg-surface-container-low font-label-sm text-label-sm">
<span className="text-on-surface-variant">Tài liệu đã duyệt</span>
<span className="font-bold text-primary">2 / 3 danh mục</span>
</div>
<div className="flex items-center justify-between p-2.5 rounded-xl bg-surface-container-low font-label-sm text-label-sm">
<span className="text-on-surface-variant">Đang xử lý</span>
<span className="font-bold text-secondary">1 văn bản mới</span>
</div>
</div>
</div>
{/* Safety Inspection Milestone Visual */}
<div className="p-space-lg rounded-2xl bg-surface-container-low flex flex-col gap-space-sm">
<span className="font-label-sm text-label-sm uppercase tracking-wider text-outline-variant">Lịch kiểm tra bến bãi tiếp theo</span>
<div className="flex items-center gap-space-sm">
<span className="material-symbols-outlined text-primary text-headline-lg">alarm_on</span>
<div className="flex flex-col">
<span className="font-headline-sm text-headline-sm text-on-surface">25 Tháng 10, 2025</span>
<span className="font-body-sm text-body-sm text-on-surface-variant">Đoàn liên ngành Bãi biển Mỹ Khê</span>
</div>
</div>
</div>
</div>
<div className="lg:col-span-8 flex flex-col gap-space-lg">
{/* Document Records Table / List */}
<div className="p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-lg">
<div className="flex items-center justify-between">
<h2 className="font-headline-md text-headline-md text-on-surface">3. Tài liệu pháp lý &amp; Chứng chỉ an toàn (vendor_documents)</h2>
<span className="font-label-sm text-label-sm text-on-surface-variant">Cập nhật lần cuối: Hôm nay</span>
</div>
<div className="flex flex-col gap-space-md">
{/* Doc 1: Business License */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col md:flex-row md:items-center justify-between gap-space-md hover:shadow-sm transition-all">
<div className="flex items-center gap-space-md min-w-0">
<div className="w-12 h-12 rounded-xl bg-primary-container text-on-primary-container flex items-center justify-center flex-shrink-0">
<span className="material-symbols-outlined text-[24px]">description</span>
</div>
<div className="flex flex-col min-w-0">
<div className="flex items-center gap-2">
<span className="font-headline-sm text-headline-sm text-on-surface truncate">Giấy phép kinh doanh (BUSINESS_LICENSE)</span>
<span className="px-2 py-0.5 rounded-full bg-primary-container/20 text-primary font-label-sm text-label-sm font-bold flex-shrink-0">
                      ĐÃ PHÊ DUYỆT (APPROVED)
                    </span>
</div>
<div className="flex flex-wrap items-center gap-2 mt-0.5 font-body-sm text-body-sm text-on-surface-variant">
<span className="font-mono text-primary font-medium">giay_phep_kinh_doanh_2024.pdf</span>
<span>•</span>
<span>Phê duyệt ngày: 12/01/2025</span>
<span>•</span>
<span>Cỡ tệp: 2.8 MB</span>
</div>
</div>
</div>
<div className="flex items-center gap-2 flex-shrink-0">
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">visibility</span>
                  Xem tài liệu
                </button>
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-primary font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">sync</span>
                  Cập nhật bản mới
                </button>
</div>
</div>
{/* Doc 2: Safety & Rescue Certificate */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col md:flex-row md:items-center justify-between gap-space-md hover:shadow-sm transition-all">
<div className="flex items-center gap-space-md min-w-0">
<div className="w-12 h-12 rounded-xl bg-primary-container text-on-primary-container flex items-center justify-center flex-shrink-0">
<span className="material-symbols-outlined text-[24px]">health_and_safety</span>
</div>
<div className="flex flex-col min-w-0">
<div className="flex items-center gap-2">
<span className="font-headline-sm text-headline-sm text-on-surface truncate">Chứng chỉ an toàn hàng hải &amp; Cứu hộ bãi biển (SAFETY_CERT)</span>
<span className="px-2 py-0.5 rounded-full bg-primary-container/20 text-primary font-label-sm text-label-sm font-bold flex-shrink-0">
                      ĐÃ PHÊ DUYỆT (APPROVED)
                    </span>
</div>
<div className="flex flex-wrap items-center gap-2 mt-0.5 font-body-sm text-body-sm text-on-surface-variant">
<span className="font-mono text-primary font-medium">chung_chi_cuu_ho_padi_rescue.pdf</span>
<span>•</span>
<span>Tiêu chuẩn: PADI Rescue &amp; Cứu nạn ĐN</span>
<span>•</span>
<span>Cỡ tệp: 4.1 MB</span>
</div>
</div>
</div>
<div className="flex items-center gap-2 flex-shrink-0">
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">visibility</span>
                  Xem
                </button>
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-primary font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">published_with_changes</span>
                  Thay thế
                </button>
</div>
</div>
{/* Doc 3: Vehicle Periodic Inspection (Pending) */}
<div className="p-space-md rounded-2xl bg-secondary-fixed/30 flex flex-col md:flex-row md:items-center justify-between gap-space-md hover:shadow-sm transition-all">
<div className="flex items-center gap-space-md min-w-0">
<div className="w-12 h-12 rounded-xl bg-secondary-container text-on-secondary-container flex items-center justify-center flex-shrink-0">
<span className="material-symbols-outlined text-[24px]">speed</span>
</div>
<div className="flex flex-col min-w-0">
<div className="flex items-center gap-2">
<span className="font-headline-sm text-headline-sm text-on-surface truncate">Giấy kiểm định phương tiện Cano/SUP định kỳ</span>
<span className="px-2 py-0.5 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-label-sm font-bold flex-shrink-0 flex items-center gap-1">
<span className="material-symbols-outlined text-[12px] animate-spin">rotate_right</span>
                      ĐANG CHỜ DUYỆT (PENDING)
                    </span>
</div>
<div className="flex flex-wrap items-center gap-2 mt-0.5 font-body-sm text-body-sm text-on-surface-variant">
<span className="font-mono text-secondary font-medium">kiem_dinh_phuong_tien_thang_09_2025.pdf</span>
<span>•</span>
<span className="text-secondary font-medium">Ban quản lý cảng vụ đang thẩm định</span>
<span>•</span>
<span>Gửi lúc: 14:20 - 15/09/2025</span>
</div>
</div>
</div>
<div className="flex items-center gap-2 flex-shrink-0">
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">visibility</span>
                  Kiểm tra bản gửi
                </button>
<button className="px-3 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-container text-secondary font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-all" type="button">
<span className="material-symbols-outlined text-[16px]">upload_file</span>
                  Tải bổ sung
                </button>
</div>
</div>
</div>
{/* Document Upload Dropzone Component */}
<div className="mt-space-sm p-space-xl rounded-2xl bg-surface-container-low flex flex-col items-center justify-center text-center group cursor-pointer hover:bg-surface-container transition-all">
<div className="w-16 h-16 rounded-full bg-surface-container-lowest flex items-center justify-center text-primary shadow-sm mb-space-sm group-hover:scale-105 transition-transform">
<span className="material-symbols-outlined text-[32px]">cloud_upload</span>
</div>
<span className="font-headline-sm text-headline-sm text-on-surface">Kéo thả tệp tài liệu vào đây hoặc nhấn để chọn</span>
<p className="font-body-sm text-body-sm text-on-surface-variant max-w-md mt-1">
              Hỗ trợ định dạng PDF, JPG, PNG với dung lượng tối đa 15MB cho mỗi tệp. Hồ sơ tải lên cần có dấu mộc đỏ và chữ ký số hợp lệ của cơ quan thẩm quyền.
            </p>
<div className="mt-space-md flex items-center gap-space-sm">
<button className="px-space-lg py-2.5 rounded-xl bg-primary-container text-on-primary-container font-label-md text-label-md shadow-sm hover:bg-primary transition-all" type="button">
                Chọn tệp từ máy tính
              </button>
<span className="font-label-sm text-label-sm text-outline-variant">Tối đa 15MB/tệp</span>
</div>
</div>
</div>
{/* Section 4: Transparent Compliance Guidance & Policy Notice */}
<div className="p-space-xl rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col gap-space-md" id="compliance-policy">
<div className="flex items-center gap-space-sm text-primary">
<span className="material-symbols-outlined text-headline-sm">verified</span>
<h2 className="font-headline-md text-headline-md text-on-surface">4. Hướng dẫn &amp; Quy định kiểm duyệt minh bạch</h2>
</div>
<div className="p-space-lg rounded-2xl bg-surface-container-low flex flex-col sm:flex-row items-start gap-space-md">
<div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center flex-shrink-0 mt-0.5">
<span className="material-symbols-outlined text-[22px]">gavel</span>
</div>
<div className="flex flex-col gap-1">
<span className="font-label-lg text-label-lg text-on-surface font-semibold">Quy tắc chuẩn hóa xác thực Ban Quản lý</span>
<p className="font-body-md text-body-md text-on-surface-variant">
                Hệ thống duyệt hồ sơ dựa trên tiêu chuẩn Ban Quản lý Bán đảo Sơn Trà &amp; Các bãi biển du lịch Đà Nẵng. Không cho phép tự ý đổi huy hiệu hoặc sửa trạng thái xác thực. Toàn bộ hồ sơ được hội đồng thẩm định độc lập theo dõi định kỳ nhằm đảm bảo an toàn tuyệt đối cho du khách.
              </p>
</div>
</div>
<div className="grid grid-cols-1 md:grid-cols-3 gap-space-md pt-space-xs">
<div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-1.5">
<div className="flex items-center gap-2 text-primary font-label-md text-label-md">
<span className="material-symbols-outlined text-[18px]">schedule</span>
                Thời gian xử lý
              </div>
<p className="font-body-sm text-body-sm text-on-surface-variant">
                Từ 24 đến 48 giờ làm việc kể từ lúc tiếp nhận tệp số hóa đầy đủ quy chuẩn.
              </p>
</div>
<div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-1.5">
<div className="flex items-center gap-2 text-primary font-label-md text-label-md">
<span className="material-symbols-outlined text-[18px]">support_agent</span>
                Kênh hỗ trợ bến bãi
              </div>
<p className="font-body-sm text-body-sm text-on-surface-variant">
                Hotline Cảng vụ: (0236) 389 7789 hoặc trao đổi qua mục Tin nhắn đối tác nội bộ.
              </p>
</div>
<div className="p-space-md rounded-xl bg-surface-container-low flex flex-col gap-1.5">
<div className="flex items-center gap-2 text-primary font-label-md text-label-md">
<span className="material-symbols-outlined text-[18px]">rule_folder</span>
                Hồ sơ tái cấp
              </div>
<p className="font-body-sm text-body-sm text-on-surface-variant">
                Yêu cầu cập nhật mới trước ít nhất 15 ngày làm việc trước khi chứng chỉ hiện tại hết hiệu lực.
              </p>
</div>
</div>
</div>
</div>
</div>
</div>
{/* Interactive JavaScript */}

</div></main>
  );
}
