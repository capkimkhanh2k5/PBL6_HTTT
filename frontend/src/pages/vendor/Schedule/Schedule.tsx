import { useState } from "react";
import { createPortal } from "react-dom";

interface SlotData {
  id: string;
  name: string;
  timeRange: string;
  subTitle: string;
  location: string;
  capacity: number;
  booked: number;
  holds: number;
  status: "OPEN" | "FULL" | "BLOCKED";
  note: string;
}

export function Schedule() {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // Selected Service & Mode
  const [selectedService, setSelectedService] = useState("sup-sunrise");
  const [viewMode, setViewMode] = useState<"WEEK" | "MONTH">("WEEK");
  const [selectedDate, setSelectedDate] = useState("2024-10-24");

  // Slots State
  const [slots, setSlots] = useState<SlotData[]>([
    {
      id: "slot-1",
      name: "Ca 1",
      timeRange: "05:00 - 07:00",
      subTitle: "(Sáng sớm - Đón bình minh)",
      location: "Bãi tắm Sao Biển • Trưởng đoàn: Huỳnh Văn Long",
      capacity: 12,
      booked: 10,
      holds: 2,
      status: "OPEN",
      note: "10 áo phao size M-XL đã gán vào QR Code đoàn",
    },
    {
      id: "slot-2",
      name: "Ca 2",
      timeRange: "07:30 - 09:30",
      subTitle: "(Sáng)",
      location: "Bãi tắm Phạm Văn Đồng • Đã kiểm định mỏ neo định vị",
      capacity: 12,
      booked: 4,
      holds: 0,
      status: "OPEN",
      note: "Khung giờ nước biển êm, thuận lợi cho khách mới tập chèo",
    },
    {
      id: "slot-3",
      name: "Ca 3",
      timeRange: "14:30 - 16:30",
      subTitle: "(Chiều)",
      location: "Bãi tắm Mỹ Khê Bến 02",
      capacity: 10,
      booked: 6,
      holds: 1,
      status: "OPEN",
      note: "Có gió nhẹ hướng Đông Nam, kiểm soát phao tiêu an toàn",
    },
  ]);

  // Modal State: Create Slot
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newServiceId, setNewServiceId] = useState("tour-1");
  const [newDate, setNewDate] = useState("2024-10-24");
  const [newStartTime, setNewStartTime] = useState("16:30");
  const [newEndTime, setNewEndTime] = useState("18:30");
  const [newCapacity, setNewCapacity] = useState(12);
  const [newLocation, setNewLocation] = useState("Bãi tắm Phạm Văn Đồng");

  // Modal State: Edit Capacity
  const [editingSlot, setEditingSlot] = useState<SlotData | null>(null);
  const [inputCapacity, setInputCapacity] = useState<number>(12);
  const [capacityError, setCapacityError] = useState<string | null>(null);

  // Quick Lock/Block Slot
  const handleToggleBlockSlot = (id: string) => {
    setSlots((prev) =>
      prev.map((s) => {
        if (s.id === id) {
          const nextStatus = s.status === "BLOCKED" ? "OPEN" : "BLOCKED";
          showToast(`Đã ${nextStatus === "BLOCKED" ? "khóa nhận thêm khách" : "mở lại"} cho ${s.name}!`);
          return { ...s, status: nextStatus };
        }
        return s;
      })
    );
  };

  // Open Edit Capacity Modal
  const handleOpenEditCapacity = (slot: SlotData) => {
    setEditingSlot(slot);
    setInputCapacity(slot.capacity);
    setCapacityError(null);
  };

  // Save Capacity with strict validation (Capacity >= booked + holds)
  const handleSaveCapacity = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSlot) return;

    const minAllowed = editingSlot.booked + editingSlot.holds;
    if (inputCapacity < minAllowed) {
      setCapacityError(
        `Không thể giảm sức chứa xuống dưới ${minAllowed} khách vì đã có ${editingSlot.booked} khách đặt cọc và ${editingSlot.holds} khách đang giữ chỗ!`
      );
      return;
    }

    setSlots((prev) =>
      prev.map((s) => (s.id === editingSlot.id ? { ...s, capacity: inputCapacity } : s))
    );
    showToast(`Đã cập nhật sức chứa ${editingSlot.name} thành ${inputCapacity} khách.`);
    setEditingSlot(null);
  };

  // Create Slot with validation
  const handleCreateSlot = (e: React.FormEvent) => {
    e.preventDefault();

    if (newEndTime <= newStartTime) {
      showToast("Lỗi: Giờ kết thúc ca biển phải sau giờ bắt đầu!");
      return;
    }

    // Check overlap
    const isOverlap = slots.some(
      (s) => s.timeRange.split(" - ")[0] === newStartTime
    );
    if (isOverlap) {
      showToast("Lỗi: Đã có ca khởi hành trùng giờ bắt đầu này!");
      return;
    }

    const newSlotItem: SlotData = {
      id: `slot-${Date.now()}`,
      name: `Ca ${slots.length + 1}`,
      timeRange: `${newStartTime} - ${newEndTime}`,
      subTitle: "(Ca mới)",
      location: newLocation,
      capacity: Number(newCapacity),
      booked: 0,
      holds: 0,
      status: "OPEN",
      note: "Đã phê duyệt điều phối luồng bến Cảng vụ",
    };

    setSlots((prev) => [...prev, newSlotItem]);
    setIsCreateModalOpen(false);
    showToast(`Đã tạo thành công ca khởi hành mới (${newStartTime} - ${newEndTime})!`);
  };

  return (
    <div className="w-full pt-4 pb-20 bg-background flex-1">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-on-surface text-surface px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-outline-variant/30 animate-bounce">
          <span className="material-symbols-outlined text-primary text-[22px]">check_circle</span>
          <span className="font-label-md text-label-md font-medium">{toastMessage}</span>
        </div>
      )}

      <div className="w-full px-space-lg py-space-md space-y-space-md max-w-[1400px] mx-auto">
        {/* Top Action Bar & Filters */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
          <div>
            <div className="flex items-center gap-space-xs text-primary font-label-sm text-label-sm tracking-wider uppercase">
              <span className="material-symbols-outlined text-[16px]">schedule</span>
              <span>Hệ thống điều phối luồng bến Mỹ Khê</span>
            </div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold">
              Quản lý Lịch khởi hành & Sức chứa ca biển
            </h1>
          </div>

          <div className="flex flex-wrap items-center gap-space-sm">
            {/* Service Selector Dropdown */}
            <div className="relative min-w-[300px]">
              <div className="flex items-center bg-surface-container-lowest px-space-md py-2 rounded-xl shadow-sm border border-outline-variant/30">
                <span className="material-symbols-outlined text-primary text-body-lg mr-2">surfing</span>
                <select
                  value={selectedService}
                  onChange={(e) => setSelectedService(e.target.value)}
                  className="w-full bg-transparent text-on-surface font-label-lg text-label-lg focus:outline-none cursor-pointer"
                >
                  <option value="sup-sunrise">Chèo SUP đón bình minh biển Mỹ Khê (280.000 đ)</option>
                  <option value="jetski-sontra">Mô tô nước Bán đảo Sơn Trà (650.000 đ)</option>
                  <option value="coral-diving">Lặn ngắm san hô Hòn Sụp cano cao tốc (450.000 đ)</option>
                </select>
              </div>
            </div>

            {/* View Segmented Switch */}
            <div className="inline-flex p-1 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <button
                onClick={() => setViewMode("WEEK")}
                className={`px-space-md py-1.5 rounded-lg font-label-md text-label-md font-bold transition-all cursor-pointer ${
                  viewMode === "WEEK" ? "bg-surface-container-lowest text-primary shadow-sm" : "text-on-surface-variant"
                }`}
                type="button"
              >
                Tuần
              </button>
              <button
                onClick={() => setViewMode("MONTH")}
                className={`px-space-md py-1.5 rounded-lg font-label-md text-label-md transition-all cursor-pointer ${
                  viewMode === "MONTH" ? "bg-surface-container-lowest text-primary font-bold shadow-sm" : "text-on-surface-variant"
                }`}
                type="button"
              >
                Tháng
              </button>
            </div>

            {/* New Slot Action Button */}
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="flex items-center gap-space-xs px-space-lg py-2.5 bg-primary hover:bg-primary-container text-on-primary rounded-xl font-label-lg text-label-lg shadow-sm hover:shadow-md transition-all cursor-pointer font-bold"
              type="button"
            >
              <span className="material-symbols-outlined text-body-lg">add_circle</span>
              <span>+ Tạo thêm ca khởi hành</span>
            </button>
          </div>
        </div>

        {/* Weather Advisory Banner */}
        <div className="bg-surface-container-lowest rounded-2xl p-space-md shadow-sm relative overflow-hidden border border-outline-variant/20">
          <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-primary"></div>
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md pl-space-xs">
            <div className="flex items-start gap-space-md">
              <div className="w-12 h-12 rounded-2xl bg-primary/10 flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-primary text-[28px]">airwave</span>
              </div>
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-space-xs">
                  <span className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    Dữ liệu trạm đo Sơn Trà lúc 06:00
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-primary/15 text-primary font-label-sm text-label-sm font-semibold flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-primary animate-ping"></span>
                    ĐỦ ĐIỀU KIỆN XUẤT BẾN HÀNG HẢI
                  </span>
                </div>
                <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                  <strong className="text-on-surface font-semibold">Gió 12 km/h • Sóng 0.5m • Lượng mưa 0mm</strong> • Cập nhật cách đây 10 phút.
                  <span className="text-tertiary ml-1">Luôn tuân thủ quy chuẩn áo phao trợ nổi khi ra biển.</span>
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Date Navigation Strip */}
        <div className="flex items-center justify-between bg-surface-container-lowest p-space-sm rounded-2xl shadow-sm border border-outline-variant/20 overflow-x-auto">
          <div className="flex items-center gap-space-xs">
            {[
              { date: "2024-10-23", label: "Hôm nay", sub: "23/10", count: "3 ca" },
              { date: "2024-10-24", label: "Ngày mai", sub: "24/10", count: `${slots.length} ca`, active: true },
              { date: "2024-10-25", label: "Thứ Sáu", sub: "25/10", count: "4 ca" },
              { date: "2024-10-26", label: "Thứ Bảy", sub: "26/10", count: "Cao điểm", badge: true },
              { date: "2024-10-27", label: "Chủ Nhật", sub: "27/10", count: "Cao điểm", badge: true },
            ].map((d) => (
              <button
                key={d.date}
                onClick={() => setSelectedDate(d.date)}
                className={`flex flex-col items-center px-space-lg py-2 rounded-xl transition-all cursor-pointer ${
                  selectedDate === d.date
                    ? "bg-primary text-on-primary shadow-sm font-bold"
                    : "text-on-surface-variant hover:bg-surface-container-low"
                }`}
                type="button"
              >
                <span className="font-label-sm text-label-sm opacity-90">{d.label}</span>
                <span className="font-headline-sm text-headline-sm font-bold">{d.sub}</span>
                <span
                  className={`font-label-sm text-[10px] px-2 py-0.5 rounded-full mt-0.5 ${
                    selectedDate === d.date ? "bg-white/20 text-white" : "text-outline"
                  }`}
                >
                  {d.count}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Main Content Layout: Slots List & Quick Control Rail */}
        <div className="grid grid-cols-1 xl:grid-cols-12 gap-space-lg">
          {/* Primary Slots List (8 Cols) */}
          <div key={`${selectedDate}-${viewMode}`} className="xl:col-span-8 space-y-space-md animate-fade-in-up">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-space-xs">
                <span className="font-headline-sm text-headline-sm text-on-surface font-bold">
                  Lịch khởi hành: {selectedDate}
                </span>
                <span className="px-2 py-0.5 bg-surface-container-high rounded-full font-label-sm text-label-sm text-on-surface-variant">
                  {slots.length} ca
                </span>
              </div>
            </div>

            {/* Render Slot Cards */}
            {slots.map((slot) => {
              const available = Math.max(0, slot.capacity - (slot.booked + slot.holds));
              const percentBooked = Math.min(100, Math.round((slot.booked / slot.capacity) * 100));

              return (
                <article
                  key={slot.id}
                  className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm hover:shadow-md transition-all space-y-space-md border border-outline-variant/20"
                >
                  <div className="flex flex-wrap items-start justify-between gap-space-sm">
                    <div className="space-y-1">
                      <div className="flex items-center gap-space-xs">
                        <span className="px-2.5 py-1 rounded-lg bg-primary-fixed-dim/30 text-primary font-headline-sm text-headline-sm font-bold">
                          {slot.name}
                        </span>
                        <span className="font-headline-md text-headline-md text-on-surface font-bold">
                          {slot.timeRange}
                        </span>
                        <span className="font-label-md text-label-md text-tertiary">{slot.subTitle}</span>
                      </div>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">{slot.location}</p>
                    </div>

                    <div className="flex items-center gap-2">
                      {slot.status === "BLOCKED" ? (
                        <span className="px-3 py-1 rounded-full bg-error-container/30 text-error font-label-sm text-label-sm font-bold">
                          ĐÃ KHÓA (BLOCKED)
                        </span>
                      ) : available === 0 ? (
                        <span className="px-3 py-1 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm font-bold">
                          ĐẦY CHỖ
                        </span>
                      ) : (
                        <span className="px-3 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-bold tracking-wide">
                          ĐANG MỞ ({available} CHỖ)
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Capacity Meter Visualization */}
                  <div className="space-y-2 bg-surface-container-low p-space-md rounded-xl">
                    <div className="flex items-center justify-between text-on-surface">
                      <div className="flex items-center gap-4">
                        <div>
                          <span className="font-label-sm text-label-sm text-outline">Sức chứa:</span>
                          <span className="font-headline-sm text-headline-sm font-bold ml-1 text-on-surface">
                            {slot.capacity} khách
                          </span>
                        </div>
                        <div className="h-4 w-[1px] bg-outline-variant"></div>
                        <div>
                          <span className="font-label-sm text-label-sm text-outline">Đã đặt:</span>
                          <span className="font-headline-sm text-headline-sm font-bold ml-1 text-primary">
                            {slot.booked} khách
                          </span>
                        </div>
                        <div className="h-4 w-[1px] bg-outline-variant"></div>
                        <div>
                          <span className="font-label-sm text-label-sm text-outline">Giữ tạm:</span>
                          <span className="font-headline-sm text-headline-sm font-bold ml-1 text-secondary">
                            {slot.holds} khách
                          </span>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="font-label-sm text-label-sm text-outline">Chỗ trống còn lại:</span>
                        <span className="font-headline-sm text-headline-sm font-bold ml-1 text-primary">
                          {available} chỗ
                        </span>
                      </div>
                    </div>

                    <div className="w-full h-3 rounded-full bg-surface-container-highest overflow-hidden flex">
                      <div
                        className="bg-primary h-full transition-all duration-500"
                        style={{ width: `${percentBooked}%` }}
                      ></div>
                    </div>

                    <div className="flex justify-between font-label-sm text-[11px] text-on-surface-variant">
                      <span>Tỷ lệ lấp đầy: {percentBooked}%</span>
                      <span>Giữ chỗ tự nhả sau 15 phút nếu chưa thanh toán</span>
                    </div>
                  </div>

                  {/* Card Actions */}
                  <div className="flex flex-wrap items-center justify-between gap-space-sm pt-2">
                    <div className="flex items-center gap-space-xs">
                      <span className="material-symbols-outlined text-outline text-body-lg">info</span>
                      <span className="font-body-sm text-body-sm text-on-surface-variant">{slot.note}</span>
                    </div>
                    <div className="flex items-center gap-space-sm">
                      <button
                        onClick={() => handleOpenEditCapacity(slot)}
                        className="px-space-md py-2 rounded-xl bg-primary/10 hover:bg-primary/20 text-primary font-label-md text-label-md font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                        type="button"
                      >
                        <span className="material-symbols-outlined text-body-md">edit</span>
                        <span>Sửa sức chứa</span>
                      </button>
                      <button
                        onClick={() => handleToggleBlockSlot(slot.id)}
                        className={`px-space-md py-2 rounded-xl font-label-md text-label-md font-semibold transition-colors flex items-center gap-1 cursor-pointer ${
                          slot.status === "BLOCKED"
                            ? "bg-primary text-on-primary"
                            : "bg-surface-container hover:bg-surface-container-high text-on-surface-variant"
                        }`}
                        type="button"
                      >
                        <span className="material-symbols-outlined text-body-md">
                          {slot.status === "BLOCKED" ? "lock_open" : "lock"}
                        </span>
                        <span>{slot.status === "BLOCKED" ? "Mở khóa ca" : "Khóa ca tạm thời"}</span>
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>

          {/* Right Rail: Safety Rules & Capacity Guidelines (4 Cols) */}
          <div className="xl:col-span-4 space-y-space-md">
            <div className="bg-surface-container-lowest rounded-2xl p-space-lg shadow-sm border border-outline-variant/20 space-y-space-md">
              <div className="flex items-center gap-space-xs text-on-surface">
                <span className="material-symbols-outlined text-primary text-body-xl">phishing</span>
                <h3 className="font-headline-sm text-headline-sm font-bold">Tỷ lệ cứu hộ quy chuẩn</h3>
              </div>
              <div className="space-y-3 font-body-sm text-body-sm">
                <div className="flex items-center justify-between py-1.5 border-b border-surface-container">
                  <span className="text-on-surface-variant">Tỷ lệ HDV : Khách SUP</span>
                  <span className="font-bold text-on-surface">1 : 6 khách</span>
                </div>
                <div className="flex items-center justify-between py-1.5 border-b border-surface-container">
                  <span className="text-on-surface-variant">Cano cứu hộ túc trực</span>
                  <span className="font-bold text-primary">Sẵn sàng (Bến A2)</span>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <span className="text-on-surface-variant">Giới hạn vùng chèo</span>
                  <span className="font-bold text-on-surface">Cách bờ tối đa 350m</span>
                </div>
              </div>
              <div className="p-space-sm rounded-xl bg-surface-container-low flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-body-md">anchor</span>
                <span className="font-label-sm text-label-sm text-on-surface-variant">
                  Tuyệt đối không giảm sức chứa nhỏ hơn số khách đã đặt.
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL: SỬA SỨC CHỨA (Portaled to body) */}
      {editingSlot && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-lg w-full p-space-xl shadow-2xl border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-headline-md">published_with_changes</span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                  Điều chỉnh sức chứa {editingSlot.name}
                </h3>
              </div>
              <button
                onClick={() => setEditingSlot(null)}
                className="p-1 rounded-xl text-on-surface-variant hover:bg-surface-container cursor-pointer"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <p className="font-body-md text-body-md text-on-surface-variant">
              Cập nhật số lượng chỗ nhận khách cho khung giờ <strong>{editingSlot.timeRange}</strong>.
            </p>

            <div className="p-space-md rounded-xl bg-surface-container-low space-y-2">
              <div className="flex justify-between font-label-md text-label-md">
                <span className="text-on-surface-variant">Đã thanh toán cọc:</span>
                <span className="text-primary font-bold">{editingSlot.booked} khách</span>
              </div>
              <div className="flex justify-between font-label-md text-label-md">
                <span className="text-on-surface-variant">Đang giữ tạm:</span>
                <span className="text-secondary font-bold">{editingSlot.holds} khách</span>
              </div>
              <div className="h-[1px] bg-outline-variant/30"></div>
              <div className="flex justify-between font-label-md text-label-md font-bold">
                <span className="text-on-surface">Giới hạn tối thiểu cho phép:</span>
                <span className="text-secondary">{editingSlot.booked + editingSlot.holds} chỗ</span>
              </div>
            </div>

            {capacityError && (
              <div className="p-3 rounded-xl bg-error-container/40 text-error text-body-sm font-semibold flex items-center gap-2">
                <span className="material-symbols-outlined text-[20px]">warning</span>
                <span>{capacityError}</span>
              </div>
            )}

            <form onSubmit={handleSaveCapacity} className="space-y-4">
              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Nhập sức chứa mới (khách)
                </label>
                <input
                  type="number"
                  min={editingSlot.booked + editingSlot.holds}
                  max={50}
                  required
                  value={inputCapacity}
                  onChange={(e) => {
                    setInputCapacity(Number(e.target.value));
                    setCapacityError(null);
                  }}
                  className="w-full px-4 py-2.5 rounded-xl bg-surface-container-low border border-outline-variant/30 font-headline-sm font-bold text-primary focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingSlot(null)}
                  className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold cursor-pointer hover:bg-primary-container"
                >
                  Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}

      {/* MODAL: TẠO THÊM CA KHỞI HÀNH (Portaled to body) */}
      {isCreateModalOpen && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-2xl max-w-lg w-full p-space-xl shadow-2xl border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-headline-md">add_circle</span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                  Tạo ca khởi hành mới
                </h3>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="p-1 rounded-xl text-on-surface-variant hover:bg-surface-container cursor-pointer"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleCreateSlot} className="space-y-4">
              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Chọn tour / trải nghiệm biển
                </label>
                <select
                  value={newServiceId}
                  onChange={(e) => setNewServiceId(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                >
                  <option value="tour-1">Tour Chèo SUP ngắm bình minh Bán đảo Sơn Trà</option>
                  <option value="tour-2">Lặn biển ngắm san hô Hòn Sụp</option>
                  <option value="tour-3">Cano cao tốc khám phá Mũi Nghê</option>
                </select>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Ngày khởi hành
                </label>
                <input
                  type="date"
                  required
                  value={newDate}
                  onChange={(e) => setNewDate(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Giờ bắt đầu
                  </label>
                  <input
                    type="time"
                    required
                    value={newStartTime}
                    onChange={(e) => setNewStartTime(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                    Giờ kết thúc
                  </label>
                  <input
                    type="time"
                    required
                    value={newEndTime}
                    onChange={(e) => setNewEndTime(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Sức chứa tối đa (Khách)
                </label>
                <input
                  type="number"
                  min={1}
                  max={50}
                  required
                  value={newCapacity}
                  onChange={(e) => setNewCapacity(Number(e.target.value))}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface font-semibold block mb-1">
                  Điểm tập kết bến bãi
                </label>
                <input
                  type="text"
                  required
                  value={newLocation}
                  onChange={(e) => setNewLocation(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-surface-container text-on-surface font-label-md cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold cursor-pointer hover:bg-primary-container shadow-md"
                >
                  Tạo ca khởi hành
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
