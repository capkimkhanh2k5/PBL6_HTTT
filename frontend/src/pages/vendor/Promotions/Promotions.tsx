import { useState } from "react";
import { createPortal } from "react-dom";

interface PromoCode {
  id: string;
  code: string;
  description: string;
  type: "PERCENTAGE" | "FIXED";
  scope: "VENDOR";
  value: number;
  maxUses: number;
  usedCount: number;
  validFrom: string;
  validTo: string;
  isActive: boolean;
  tag: string;
}

export function Promotions() {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [selectedVoucherHistory, setSelectedVoucherHistory] = useState<PromoCode | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // Filter tab
  const [filterStatus, setFilterStatus] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");

  // Vouchers List
  const [promotions, setPromotions] = useState<PromoCode[]>([
    {
      id: "promo-1",
      code: "SUPHE2024",
      description: "Chèo SUP ngắm bình minh mùa hè",
      type: "PERCENTAGE",
      scope: "VENDOR",
      value: 15,
      maxUses: 100,
      usedCount: 45,
      validFrom: "2024-06-01",
      validTo: "2024-10-31",
      isActive: true,
      tag: "Hiệu lực mùa hè",
    },
    {
      id: "promo-2",
      code: "OCEAN50K",
      description: "Giảm thẳng toàn bộ gói lặn biển",
      type: "FIXED",
      scope: "VENDOR",
      value: 50000,
      maxUses: 200,
      usedCount: 82,
      validFrom: "2024-05-01",
      validTo: "2024-12-31",
      isActive: true,
      tag: "Ưu đãi lặn ngắm san hô",
    },
    {
      id: "promo-3",
      code: "BINHMINH10",
      description: "Giảm 10% ca sớm 05:00",
      type: "PERCENTAGE",
      scope: "VENDOR",
      value: 10,
      maxUses: 50,
      usedCount: 15,
      validFrom: "2024-08-01",
      validTo: "2024-11-30",
      isActive: true,
      tag: "Dành riêng ca sáng sớm",
    },
    {
      id: "promo-4",
      code: "TETBIEN2024",
      description: "Tri ân mùa cao điểm đầu năm",
      type: "FIXED",
      scope: "VENDOR",
      value: 100000,
      maxUses: 50,
      usedCount: 50,
      validFrom: "2024-01-01",
      validTo: "2024-03-31",
      isActive: false,
      tag: "Đã hết hạn",
    },
  ]);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  // Form Fields
  const [code, setCode] = useState("SUNRISE20");
  const [description, setDescription] = useState("Giảm giá đặc biệt tour chèo SUP");
  const [type, setType] = useState<"PERCENTAGE" | "FIXED">("PERCENTAGE");
  const [value, setValue] = useState(15);
  const [maxUses, setMaxUses] = useState(100);
  const [validFrom, setValidFrom] = useState("2024-10-01");
  const [validTo, setValidTo] = useState("2024-12-31");
  const [isActive, setIsActive] = useState(true);

  // Toggle Active
  const handleToggleActive = (id: string) => {
    setPromotions((prev) =>
      prev.map((p) => {
        if (p.id === id) {
          const next = !p.isActive;
          showToast(`Đã ${next ? "kích hoạt" : "tạm dừng"} mã ${p.code}!`);
          return { ...p, isActive: next };
        }
        return p;
      })
    );
  };

  // Open Create
  const handleOpenCreate = () => {
    setEditingId(null);
    setCode("");
    setDescription("");
    setType("PERCENTAGE");
    setValue(10);
    setMaxUses(100);
    setValidFrom("2024-10-24");
    setValidTo("2024-12-31");
    setIsActive(true);
    setIsModalOpen(true);
  };

  // Open Edit
  const handleOpenEdit = (p: PromoCode) => {
    setEditingId(p.id);
    setCode(p.code);
    setDescription(p.description);
    setType(p.type);
    setValue(p.value);
    setMaxUses(p.maxUses);
    setValidFrom(p.validFrom);
    setValidTo(p.validTo);
    setIsActive(p.isActive);
    setIsModalOpen(true);
  };

  // Save Voucher
  const handleSaveVoucher = (e: React.FormEvent) => {
    e.preventDefault();

    if (!code.trim()) {
      showToast("Vui lòng nhập mã khuyến mãi!");
      return;
    }

    if (type === "PERCENTAGE" && (value <= 0 || value > 100)) {
      showToast("Lỗi: Mã giảm giá phần trăm phải từ 1% đến 100%!");
      return;
    }

    if (type === "FIXED" && value <= 0) {
      showToast("Lỗi: Số tiền giảm phải lớn hơn 0 VNĐ!");
      return;
    }

    if (validTo <= validFrom) {
      showToast("Lỗi: Ngày kết thúc phải sau ngày bắt đầu!");
      return;
    }

    if (editingId) {
      // Edit
      setPromotions((prev) =>
        prev.map((p) =>
          p.id === editingId
            ? {
                ...p,
                code: code.trim().toUpperCase(),
                description,
                type,
                value,
                maxUses,
                validFrom,
                validTo,
                isActive,
              }
            : p
        )
      );
      showToast(`Đã cập nhật thành công mã ${code.toUpperCase()}!`);
    } else {
      // Create new
      const exists = promotions.some(
        (p) => p.code.toLowerCase() === code.trim().toLowerCase()
      );
      if (exists) {
        showToast(`Mã "${code}" đã tồn tại trên hệ thống của bạn!`);
        return;
      }

      const newPromo: PromoCode = {
        id: `promo-${Date.now()}`,
        code: code.trim().toUpperCase(),
        description: description || "Khuyến mãi đối tác",
        type,
        value,
        maxUses,
        usedCount: 0,
        validFrom,
        validTo,
        isActive,
        scope: "VENDOR",
        tag: "Mới tạo",
      };
      setPromotions((prev) => [newPromo, ...prev]);
      showToast(`Đã tạo thành công mã khuyến mãi ${newPromo.code}!`);
    }

    setIsModalOpen(false);
  };

  // Filtered
  const filteredPromotions = promotions.filter((p) => {
    if (filterStatus === "ACTIVE") return p.isActive;
    if (filterStatus === "INACTIVE") return !p.isActive;
    return true;
  });

  const activeCount = promotions.filter((p) => p.isActive).length;
  const totalUsed = promotions.reduce((sum, p) => sum + p.usedCount, 0);

  return (
    <div className="w-full pt-4 pb-20 bg-background min-h-screen">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-on-surface text-surface px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-outline-variant/30 animate-bounce">
          <span className="material-symbols-outlined text-primary text-[22px]">check_circle</span>
          <span className="font-label-md text-label-md font-medium">{toastMessage}</span>
        </div>
      )}

      <div className="px-space-xl py-space-lg flex flex-col gap-space-xl max-w-[1400px] mx-auto w-full">
        {/* Header Block */}
        <div className="flex flex-col gap-space-sm">
          <div className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
            <span>Quản trị Bán hàng</span>
            <span className="material-symbols-outlined text-[16px]">chevron_right</span>
            <span className="text-primary font-bold">Mã ưu đãi & Khuyến mãi (Vendor)</span>
          </div>

          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
            <div>
              <h1 className="font-headline-xl text-headline-xl font-bold text-on-surface tracking-tight">
                Quản lý Mã Khuyến mãi Đối tác
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1 max-w-3xl">
                Thiết lập mã giảm giá riêng của cơ sở kinh doanh nhằm kích cầu tour chèo SUP bán đảo Sơn Trà và lặn ngắm san hô. Áp dụng độc quyền cho các dịch vụ thuộc <span className="font-bold text-primary">Danang Ocean Club</span>.
              </p>
            </div>
            <div className="flex items-center gap-space-sm">
              <button
                onClick={handleOpenCreate}
                className="inline-flex items-center gap-space-sm px-space-lg py-3 rounded-xl bg-primary text-on-primary font-label-lg text-label-lg font-bold shadow-md hover:bg-primary-container transition-all cursor-pointer"
                type="button"
              >
                <span className="material-symbols-outlined text-[20px]">add_circle</span>
                <span>+ Tạo mã khuyến mãi mới</span>
              </button>
            </div>
          </div>
        </div>

        {/* Scope Warning Banner */}
        <div className="bg-surface-container-lowest rounded-2xl p-space-md shadow-sm border border-outline-variant/20 flex items-start gap-space-md">
          <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-0.5 text-primary">
            <span className="material-symbols-outlined text-[20px]">verified_user</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className="font-headline-sm text-headline-sm text-primary font-bold">
              Phạm vi áp dụng cố định: Cấp Đối tác (Scope: VENDOR)
            </span>
            <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
              Mã giảm giá do Đối tác thiết lập sẽ khấu trừ trực tiếp vào phần doanh thu của đối tác cho các dịch vụ do đơn vị vận hành (SUP, Kayak, Lặn biển), không áp dụng sang các đối tác cano hay tour trọn gói của Ban Quản lý VITA.
            </p>
          </div>
        </div>

        {/* 4 Top KPI Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-space-md">
          <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <span className="font-label-sm uppercase tracking-wider text-tertiary">Tổng số mã</span>
            <span className="font-display-lg text-display-lg text-primary font-bold mt-1">
              {promotions.length} <span className="font-headline-sm text-headline-sm text-tertiary">mã</span>
            </span>
            <span className="text-body-sm text-on-surface-variant mt-2">Toàn bộ chiến dịch</span>
          </div>

          <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <span className="font-label-sm uppercase tracking-wider text-primary font-bold">Đang chạy</span>
            <span className="font-display-lg text-display-lg text-primary font-bold mt-1">
              {activeCount} <span className="font-headline-sm text-headline-sm text-primary">mã</span>
            </span>
            <span className="text-body-sm text-primary mt-2">Đang mở áp dụng cho khách</span>
          </div>

          <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <span className="font-label-sm uppercase tracking-wider text-secondary font-bold">Lượt đã kích hoạt</span>
            <span className="font-display-lg text-display-lg text-secondary font-bold mt-1">
              {totalUsed} <span className="font-headline-sm text-headline-sm text-secondary">lượt</span>
            </span>
            <span className="text-body-sm text-on-surface-variant mt-2">Tổng số đơn có mã giảm</span>
          </div>

          <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <span className="font-label-sm uppercase tracking-wider text-outline">Ưu đãi trung bình</span>
            <span className="font-display-lg text-display-lg text-on-surface font-bold mt-1">
              12.5%
            </span>
            <span className="text-body-sm text-on-surface-variant mt-2">Tỷ lệ giảm trung bình</span>
          </div>
        </div>

        {/* Filter and Table Section */}
        <div className="space-y-space-md">
          <div className="flex items-center justify-between">
            <div className="flex gap-2">
              <button
                onClick={() => setFilterStatus("ALL")}
                className={`px-4 py-1.5 rounded-full font-label-md cursor-pointer transition-colors ${
                  filterStatus === "ALL" ? "bg-primary text-on-primary font-bold" : "bg-surface-container-low text-on-surface-variant"
                }`}
              >
                Tất cả ({promotions.length})
              </button>
              <button
                onClick={() => setFilterStatus("ACTIVE")}
                className={`px-4 py-1.5 rounded-full font-label-md cursor-pointer transition-colors ${
                  filterStatus === "ACTIVE" ? "bg-primary text-on-primary font-bold" : "bg-surface-container-low text-on-surface-variant"
                }`}
              >
                Đang hoạt động ({activeCount})
              </button>
              <button
                onClick={() => setFilterStatus("INACTIVE")}
                className={`px-4 py-1.5 rounded-full font-label-md cursor-pointer transition-colors ${
                  filterStatus === "INACTIVE" ? "bg-primary text-on-primary font-bold" : "bg-surface-container-low text-on-surface-variant"
                }`}
              >
                Tạm tắt ({promotions.length - activeCount})
              </button>
            </div>
          </div>

          {/* Table */}
          <div
            key={filterStatus}
            className="bg-surface-container-lowest rounded-2xl shadow-sm border border-outline-variant/20 overflow-hidden animate-fade-in-up"
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-surface-container-low text-on-surface-variant font-label-md text-label-md uppercase tracking-wider">
                    <th className="py-3 px-space-lg">Mã ưu đãi</th>
                    <th className="py-3 px-space-md">Loại ưu đãi</th>
                    <th className="py-3 px-space-md">Giá trị</th>
                    <th className="py-3 px-space-md">Lượt đã dùng</th>
                    <th className="py-3 px-space-md">Thời hạn áp dụng</th>
                    <th className="py-3 px-space-md text-center">Trạng thái</th>
                    <th className="py-3 px-space-lg text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/20 font-body-sm text-body-sm">
                  {filteredPromotions.map((p) => (
                    <tr key={p.id} className="hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3.5 px-space-lg">
                        <div className="flex items-center gap-space-sm">
                          <div className="p-2 rounded-lg bg-primary/10 text-primary">
                            <span className="material-symbols-outlined text-[20px]">local_offer</span>
                          </div>
                          <div>
                            <span className="font-headline-sm text-headline-sm font-bold text-primary tracking-wide">
                              {p.code}
                            </span>
                            <div className="font-label-sm text-label-sm text-tertiary">{p.description}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-space-md">
                        <span className="px-2.5 py-1 rounded-md bg-surface-container text-on-surface-variant font-label-sm font-semibold">
                          {p.type === "PERCENTAGE" ? "Phần trăm (%)" : "Số tiền cố định"}
                        </span>
                      </td>
                      <td className="py-3.5 px-space-md font-bold text-primary">
                        {p.type === "PERCENTAGE" ? `${p.value}%` : `${p.value.toLocaleString("vi-VN")} đ`}
                      </td>
                      <td className="py-3.5 px-space-md">
                        <div className="flex flex-col gap-1 w-32">
                          <div className="flex justify-between font-label-sm text-[12px]">
                            <span className="font-bold">{p.usedCount} / {p.maxUses}</span>
                            <span className="text-tertiary">{Math.round((p.usedCount / p.maxUses) * 100)}%</span>
                          </div>
                          <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                            <div
                              className="h-full bg-primary rounded-full"
                              style={{ width: `${Math.min(100, (p.usedCount / p.maxUses) * 100)}%` }}
                            ></div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-space-md text-on-surface-variant">
                        <div>{p.validFrom} đến {p.validTo}</div>
                      </td>
                      <td className="py-3.5 px-space-md text-center">
                        <button
                          onClick={() => handleToggleActive(p.id)}
                          className={`px-3 py-1 rounded-full text-label-sm font-bold cursor-pointer transition-all ${
                            p.isActive
                              ? "bg-primary/15 text-primary"
                              : "bg-surface-container-high text-on-surface-variant"
                          }`}
                        >
                          {p.isActive ? "ĐANG BẬT" : "TẮT"}
                        </button>
                      </td>
                      <td className="py-3.5 px-space-lg text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => setSelectedVoucherHistory(p)}
                            className="p-1.5 rounded-lg text-secondary hover:bg-secondary/10 transition-colors cursor-pointer"
                            title="Lịch sử áp dụng"
                          >
                            <span className="material-symbols-outlined text-[18px]">history</span>
                          </button>
                          <button
                            onClick={() => handleOpenEdit(p)}
                            className="p-1.5 rounded-lg text-primary hover:bg-primary/10 transition-colors cursor-pointer"
                            title="Sửa mã"
                          >
                            <span className="material-symbols-outlined text-[18px]">edit</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* USAGE HISTORY MODAL (Portaled to body) */}
      {selectedVoucherHistory && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-lg w-full p-space-xl shadow-2xl border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary text-[24px]">history</span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                  Lịch sử áp dụng: #{selectedVoucherHistory.code}
                </h3>
              </div>
              <button
                onClick={() => setSelectedVoucherHistory(null)}
                className="p-1 rounded-lg text-on-surface-variant hover:bg-surface-container cursor-pointer"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-surface-container-low text-body-sm">
              <span>Tổng số lượt đã dùng: <strong className="text-primary">{selectedVoucherHistory.usedCount}</strong> / {selectedVoucherHistory.maxUses}</span>
              <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary font-bold text-[11px]">Scope: VENDOR</span>
            </div>

            <div className="max-h-60 overflow-y-auto flex flex-col divide-y divide-outline-variant/10 font-body-sm">
              <div className="py-2.5 flex items-center justify-between">
                <div>
                  <span className="font-bold text-primary">#SUB-89412-01</span>
                  <div className="text-[12px] text-on-surface-variant">Nguyễn Văn An • Chèo SUP Mỹ Khê</div>
                </div>
                <div className="text-right">
                  <span className="font-bold text-emerald-600">-15%</span>
                  <div className="text-[11px] text-outline">24/10/2024</div>
                </div>
              </div>
              <div className="py-2.5 flex items-center justify-between">
                <div>
                  <span className="font-bold text-primary">#SUB-89380-02</span>
                  <div className="text-[12px] text-on-surface-variant">Lê Quốc Bảo • SUP Bãi Bụt</div>
                </div>
                <div className="text-right">
                  <span className="font-bold text-emerald-600">-15%</span>
                  <div className="text-[11px] text-outline">12/10/2024</div>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-outline-variant/20">
              <button
                onClick={() => setSelectedVoucherHistory(null)}
                className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* CREATE / EDIT PROMOTION MODAL (Portaled to body) */}
      {isModalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-lg w-full p-space-xl shadow-2xl border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">loyalty</span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                  {editingId ? "Chỉnh sửa mã khuyến mãi" : "Tạo mã khuyến mãi mới"}
                </h3>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-on-surface-variant hover:bg-surface-container cursor-pointer"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleSaveVoucher} className="space-y-4">
              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Mã Code (Chữ hoa & số, không khoảng trắng) *
                </label>
                <input
                  type="text"
                  required
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  placeholder="VÍ DỤ: HELLOSUMMER"
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-headline-sm font-bold text-primary"
                />
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Mô tả ưu đãi
                </label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Giảm giá tour SUP sáng sớm cho nhóm bạn"
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Loại giảm giá
                  </label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  >
                    <option value="PERCENTAGE">Theo Phần trăm (%)</option>
                    <option value="FIXED">Số tiền cố định (VNĐ)</option>
                  </select>
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Giá trị giảm ({type === "PERCENTAGE" ? "%" : "VNĐ"}) *
                  </label>
                  <input
                    type="number"
                    required
                    min={1}
                    value={value}
                    onChange={(e) => setValue(Number(e.target.value))}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md font-bold text-secondary"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Số lượng mã tối đa (lượt) *
                </label>
                <input
                  type="number"
                  required
                  min={1}
                  value={maxUses}
                  onChange={(e) => setMaxUses(Number(e.target.value))}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Có hiệu lực từ
                  </label>
                  <input
                    type="date"
                    required
                    value={validFrom}
                    onChange={(e) => setValidFrom(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Hết hạn ngày
                  </label>
                  <input
                    type="date"
                    required
                    value={validTo}
                    onChange={(e) => setValidTo(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold cursor-pointer hover:bg-primary-container shadow-md"
                >
                  {editingId ? "Cập nhật mã" : "Lưu mã mới"}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
