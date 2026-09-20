import React, { useState } from "react";

interface PayoutHistoryItem {
  id: string;
  cycleId: string;
  amount: number;
  status: "REQUESTED" | "APPROVED" | "PAID" | "REJECTED";
  createdAt: string;
  processedAt: string | null;
  bankRef: string;
}

export function Payouts() {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // Available Balance for withdrawal
  const [availableBalance, setAvailableBalance] = useState(12780000);
  const [selectedCycle, setSelectedCycle] = useState("SET-202410-02");
  const [payoutAmount, setPayoutAmount] = useState<string>("12780000");
  const [payoutNote, setPayoutNote] = useState(
    "Đề nghị Cảng vụ giải ngân kỳ cuối tháng 10/2024 phục vụ chi phí bảo dưỡng thiết bị SUP và trả lương hướng dẫn viên."
  );
  const [showAccount, setShowAccount] = useState(false);

  // Payout History State
  const [history, setHistory] = useState<PayoutHistoryItem[]>([
    {
      id: "PAY-202410-01",
      cycleId: "SET-202410-01",
      amount: 9000000,
      status: "PAID",
      createdAt: "16/10/2024",
      processedAt: "18/10/2024 14:15",
      bankRef: "FT2429288192020-VCB",
    },
    {
      id: "PAY-202409-02",
      cycleId: "SET-202409-02",
      amount: 20250000,
      status: "PAID",
      createdAt: "01/10/2024",
      processedAt: "03/10/2024 10:00",
      bankRef: "FT2427519920194-VCB",
    },
  ]);

  // Validation
  const numericAmount = Number(payoutAmount.replace(/\D/g, "")) || 0;
  const isAmountOver = numericAmount > availableBalance;

  // Handle Fill Max
  const handleFillMax = () => {
    setPayoutAmount(availableBalance.toString());
  };

  // Submit Payout Request
  const handleSubmitPayout = (e: React.FormEvent) => {
    e.preventDefault();

    if (numericAmount <= 0) {
      showToast("Vui lòng nhập số tiền hợp lệ lớn hơn 0 đ!");
      return;
    }

    if (numericAmount > availableBalance) {
      showToast("Lỗi: Số tiền rút không được vượt quá số dư khả dụng!");
      return;
    }

    const newPayout: PayoutHistoryItem = {
      id: `PAY-${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, "0")}-${String(history.length + 1).padStart(2, "0")}`,
      cycleId: selectedCycle,
      amount: numericAmount,
      status: "REQUESTED",
      createdAt: new Date().toLocaleDateString("vi-VN"),
      processedAt: null,
      bankRef: "CHỜ-DUYỆT-VITA",
    };

    setHistory((prev) => [newPayout, ...prev]);
    setAvailableBalance((prev) => prev - numericAmount);
    setPayoutAmount("0");
    showToast(
      `Đã tạo lệnh yêu cầu rút ${numericAmount.toLocaleString("vi-VN")} đ (Mã: #${newPayout.id}). Hồ sơ gửi tới Kế toán Cảng vụ.`
    );
  };

  const copyBankInfo = () => {
    navigator.clipboard?.writeText("0041000345898921");
    showToast("Đã sao chép số tài khoản Vietcombank vào bộ nhớ tạm!");
  };

  return (
    <div className="w-full pt-4 pb-20 bg-background min-h-screen">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-on-surface text-surface px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-outline-variant/30 animate-bounce">
          <span className="material-symbols-outlined text-primary text-[22px]">check_circle</span>
          <span className="font-label-md text-label-md font-medium">{toastMessage}</span>
        </div>
      )}

      <div className="max-w-[1280px] w-full mx-auto px-space-lg py-space-md flex flex-col gap-space-xl">
        {/* Top Breadcrumb & Title */}
        <div className="flex flex-col gap-space-xs">
          <nav className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
            <span>Quản trị Tài chính</span>
            <span className="material-symbols-outlined text-[16px]">chevron_right</span>
            <span className="text-primary font-bold">Yêu cầu giải ngân & Thanh toán</span>
          </nav>
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-space-md mt-1">
            <div>
              <h1 className="font-headline-lg text-headline-lg font-bold text-on-surface tracking-tight">
                Yêu cầu Thanh toán & Giải ngân Doanh thu
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1 max-w-2xl">
                Gửi lệnh rút tiền từ các kỳ đối soát đã chốt số liệu về tài khoản ngân hàng liên kết chính thức của câu lạc bộ.
              </p>
            </div>
            <div className="flex items-center gap-space-sm self-start md:self-auto bg-surface-container-lowest px-space-md py-space-sm rounded-xl shadow-sm border border-outline-variant/20">
              <span className="material-symbols-outlined text-primary text-[20px]">account_balance</span>
              <span className="font-label-md text-label-md text-on-surface font-semibold">
                Cổng thanh toán Napas247 VITA
              </span>
            </div>
          </div>
        </div>

        {/* 3 KPI Status Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-space-md">
          {/* Card 1: Available Balance */}
          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <div>
              <span className="font-label-sm uppercase tracking-wider text-primary font-bold">
                Số dư có thể yêu cầu rút
              </span>
              <div className="font-headline-xl text-headline-xl text-primary font-bold mt-2">
                {availableBalance.toLocaleString("vi-VN")}{" "}
                <span className="text-headline-md font-semibold">đ</span>
              </div>
            </div>
            <div className="mt-space-md pt-space-xs flex items-center gap-space-xs">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Từ kỳ đối soát <span className="font-semibold text-on-surface">#SET-202410-02</span>
              </p>
            </div>
          </div>

          {/* Card 2: Pending Requests */}
          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <div>
              <span className="font-label-sm uppercase tracking-wider text-tertiary font-bold">
                Đang chờ phê duyệt
              </span>
              <div className="font-headline-xl text-headline-xl text-on-surface font-bold mt-2">
                {history
                  .filter((h) => h.status === "REQUESTED")
                  .reduce((sum, h) => sum + h.amount, 0)
                  .toLocaleString("vi-VN")}{" "}
                <span className="text-headline-md font-semibold">đ</span>
              </div>
            </div>
            <p className="font-body-sm text-body-sm text-tertiary mt-space-md">
              Kế toán Cảng vụ sẽ duyệt trong 24 giờ làm việc
            </p>
          </div>

          {/* Card 3: Total Withdrawn */}
          <div className="bg-surface-container-lowest p-space-lg rounded-2xl shadow-sm border border-outline-variant/20 flex flex-col justify-between">
            <div>
              <span className="font-label-sm uppercase tracking-wider text-on-surface-variant font-bold">
                Đã giải ngân thành công
              </span>
              <div className="font-headline-xl text-headline-xl text-on-surface font-bold mt-2">
                {history
                  .filter((h) => h.status === "PAID")
                  .reduce((sum, h) => sum + h.amount, 0)
                  .toLocaleString("vi-VN")}{" "}
                <span className="text-headline-md font-semibold">đ</span>
              </div>
            </div>
            <p className="font-body-sm text-body-sm text-primary font-semibold mt-space-md flex items-center gap-1">
              <span className="material-symbols-outlined text-[16px]">verified</span>
              100% thanh toán đúng hạn quy chuẩn
            </p>
          </div>
        </div>

        {/* Core Layout: Form (Left 7 Cols), History (Right 5 Cols) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
          {/* Form Area */}
          <div className="lg:col-span-7 flex flex-col gap-space-md">
            <div className="bg-surface-container-lowest p-space-xl rounded-2xl shadow-sm border border-outline-variant/20 flex flex-col gap-space-lg">
              {/* Linked Bank Card */}
              <div className="space-y-space-xs">
                <span className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant font-bold">
                  Tài khoản ngân hàng thụ hưởng
                </span>
                <div className="bg-surface-container-low p-space-md rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-space-md">
                    <div className="w-11 h-11 rounded-lg bg-surface-container-high flex items-center justify-center text-primary">
                      <span className="material-symbols-outlined text-[24px]">account_balance</span>
                    </div>
                    <div>
                      <span className="font-label-lg text-label-lg font-bold text-on-surface">
                        Vietcombank - CN Đà Nẵng
                      </span>
                      <div className="flex items-center gap-space-sm mt-0.5">
                        <span className="font-body-md font-mono text-on-surface font-bold tracking-wider">
                          {showAccount ? "0041000345898921" : "•••• •••• •••• 8921"}
                        </span>
                        <button
                          onClick={() => setShowAccount(!showAccount)}
                          className="text-primary hover:underline cursor-pointer flex items-center text-xs"
                          type="button"
                        >
                          <span className="material-symbols-outlined text-[16px]">
                            {showAccount ? "visibility_off" : "visibility"}
                          </span>
                        </button>
                        <button
                          onClick={copyBankInfo}
                          className="text-primary hover:underline cursor-pointer flex items-center text-xs ml-1"
                          type="button"
                          title="Sao chép số tài khoản"
                        >
                          <span className="material-symbols-outlined text-[16px]">content_copy</span>
                        </button>
                      </div>
                      <div className="text-[12px] text-primary font-semibold mt-0.5">
                        CÔNG TY TNHH THỂ THAO BIỂN OCEAN CLUB
                      </div>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary font-label-sm text-label-sm font-bold">
                    Chính thức
                  </span>
                </div>
              </div>

              {/* Form Input */}
              <form onSubmit={handleSubmitPayout} className="space-y-4">
                {/* Settlement select */}
                <div>
                  <label className="font-label-md text-label-md font-semibold text-on-surface block mb-1">
                    Chọn kỳ đối soát liên kết
                  </label>
                  <select
                    value={selectedCycle}
                    onChange={(e) => setSelectedCycle(e.target.value)}
                    className="w-full h-12 px-space-md rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  >
                    <option value="SET-202410-02">
                      Kỳ #SET-202410-02 (16/10 - 31/10/2024) — Khả dụng: {availableBalance.toLocaleString("vi-VN")} đ
                    </option>
                  </select>
                </div>

                {/* Amount input */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="font-label-md text-label-md font-semibold text-on-surface">
                      Số tiền yêu cầu giải ngân (VNĐ) *
                    </label>
                    <button
                      type="button"
                      onClick={handleFillMax}
                      className="font-label-sm text-label-sm font-bold text-primary hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <span className="material-symbols-outlined text-[14px]">all_inclusive</span>
                      Rút toàn bộ số dư
                    </button>
                  </div>

                  <div className="relative">
                    <input
                      type="number"
                      required
                      min={100000}
                      max={availableBalance}
                      value={payoutAmount}
                      onChange={(e) => setPayoutAmount(e.target.value)}
                      className={`w-full h-12 pl-space-md pr-12 rounded-xl bg-surface-container-low border font-headline-sm font-bold text-primary focus:outline-none focus:ring-2 focus:ring-primary ${
                        isAmountOver ? "border-error text-error" : "border-outline-variant/30"
                      }`}
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 font-headline-sm font-bold text-on-surface-variant">
                      đ
                    </span>
                  </div>

                  {isAmountOver && (
                    <p className="text-error font-body-sm text-[12px] mt-1 font-semibold">
                      Số tiền yêu cầu không được vượt quá số dư khả dụng ({availableBalance.toLocaleString("vi-VN")} đ).
                    </p>
                  )}
                </div>

                {/* Note */}
                <div>
                  <label className="font-label-md text-label-md font-semibold text-on-surface block mb-1">
                    Ghi chú gửi Bộ phận Tài vụ Cảng vụ
                  </label>
                  <textarea
                    rows={3}
                    value={payoutNote}
                    onChange={(e) => setPayoutNote(e.target.value)}
                    className="w-full p-space-md rounded-xl bg-surface-container-low border border-outline-variant/30 font-body-md"
                  ></textarea>
                </div>

                <div className="pt-2 flex items-center justify-end gap-3">
                  <button
                    type="submit"
                    disabled={isAmountOver || availableBalance <= 0}
                    className="w-full py-3.5 rounded-xl bg-primary text-on-primary font-label-lg font-bold hover:bg-primary-container shadow-md cursor-pointer transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[20px]">send</span>
                    <span>Gửi yêu cầu thanh toán ({numericAmount.toLocaleString("vi-VN")} đ)</span>
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* Right History Area */}
          <div className="lg:col-span-5 space-y-space-md">
            <div className="bg-surface-container-lowest p-space-xl rounded-2xl shadow-sm border border-outline-variant/20 space-y-space-md">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-[22px]">history</span>
                  <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">
                    Lịch sử & Tiến trình Giải ngân
                  </h3>
                </div>
                <span className="text-xs text-on-surface-variant font-bold">{history.length} giao dịch</span>
              </div>

              <div className="space-y-3">
                {history.map((item) => (
                  <div
                    key={item.id}
                    className="bg-surface-container-low p-space-md rounded-xl space-y-2 border border-outline-variant/20 hover:shadow-sm transition-shadow"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="font-label-lg font-bold text-on-surface">#{item.id}</div>
                        <div className="text-[12px] text-on-surface-variant">Kỳ liên kết: {item.cycleId}</div>
                      </div>
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-label-sm font-bold ${
                          item.status === "PAID"
                            ? "bg-primary/10 text-primary"
                            : item.status === "REQUESTED"
                            ? "bg-secondary-container text-on-secondary-container"
                            : "bg-surface-container-high"
                        }`}
                      >
                        {item.status === "PAID" ? "ĐÃ THANH TOÁN" : "CHỜ DUYỆT (REQUESTED)"}
                      </span>
                    </div>

                    <div className="flex items-baseline justify-between pt-1">
                      <span className="text-[13px] text-on-surface-variant">Số tiền:</span>
                      <span className="font-headline-sm text-headline-sm font-bold text-primary">
                        {item.amount.toLocaleString("vi-VN")} đ
                      </span>
                    </div>

                    <div className="text-[11px] text-outline pt-1 flex justify-between">
                      <span>Ngày tạo: {item.createdAt}</span>
                      <span>{item.processedAt ? `Hoàn tất: ${item.processedAt}` : "Chờ xử lý"}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
