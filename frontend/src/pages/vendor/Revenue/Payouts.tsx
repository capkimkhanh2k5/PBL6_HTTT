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
    <div className="w-full px-6 py-4 space-y-3.5 bg-background min-h-screen">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-black text-white px-5 py-3 rounded-lg shadow-xl flex items-center gap-3 border border-slate-600 animate-bounce">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">check_circle</span>
          <span className="text-xs font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl md:text-2xl font-bold text-slate-900 tracking-tight">
          Quyết toán
        </h1>
        <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-lg border border-slate-200 shadow-xs">
          <span className="material-symbols-outlined text-primary text-[18px]">account_balance</span>
          <span className="text-xs text-slate-700 font-semibold">
            Cổng thanh toán
          </span>
        </div>
      </div>

      {/* Compact Financial Summary Strip */}
      <div className="flex flex-wrap items-center justify-between gap-4 px-4 py-2.5 rounded-lg bg-white border border-slate-200 shadow-xs">
        <div className="flex flex-wrap items-center gap-6 text-xs">
          <div>
            <span className="text-slate-500 text-[11px] block font-medium">Khả dụng rút</span>
            <span className="font-bold text-base text-slate-900 font-mono">
              {availableBalance.toLocaleString("vi-VN")} <span className="text-xs font-normal">đ</span>
            </span>
          </div>
          <span className="text-slate-300 self-center">•</span>
          <div>
            <span className="text-slate-500 text-[11px] block font-medium">Đang chờ duyệt</span>
            <span className="font-bold text-base text-slate-900 font-mono">
              {history
                .filter((h) => h.status === "REQUESTED")
                .reduce((sum, h) => sum + h.amount, 0)
                .toLocaleString("vi-VN")} <span className="text-xs font-normal">đ</span>
            </span>
          </div>
          <span className="text-slate-300 self-center">•</span>
          <div>
            <span className="text-slate-500 text-[11px] block font-medium">Đã giải ngân thành công</span>
            <span className="font-bold text-base text-primary font-mono">
              {history
                .filter((h) => h.status === "PAID")
                .reduce((sum, h) => sum + h.amount, 0)
                .toLocaleString("vi-VN")} <span className="text-xs font-normal">đ</span>
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-700 font-medium">
          <span className="w-2 h-2 rounded-full bg-emerald-600"></span>
          <span>Kỳ đối soát: <strong className="text-slate-900 font-mono">#SET-202410-02</strong></span>
        </div>
      </div>

      {/* Core Layout: Form (Left 7 Cols), History (Right 5 Cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3.5 items-start">
        {/* Form Area */}
        <div className="lg:col-span-7 flex flex-col gap-3.5">
          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex flex-col gap-3.5">
            {/* Linked Bank Card */}
            <div className="space-y-1.5">
              <span className="text-xs text-slate-700 font-bold uppercase tracking-wider">
                Tài khoản ngân hàng thụ hưởng
              </span>
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-white flex items-center justify-center text-primary border border-slate-200 shadow-xs">
                    <span className="material-symbols-outlined text-[22px]">account_balance</span>
                  </div>
                  <div>
                    <span className="text-sm font-bold text-slate-900 block">
                      Vietcombank - CN Đà Nẵng
                    </span>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="font-mono text-slate-800 font-bold tracking-wider text-xs">
                        {showAccount ? "0041000345898921" : "•••• •••• •••• 8921"}
                      </span>
                      <button
                        onClick={() => setShowAccount(!showAccount)}
                        className="text-slate-500 hover:text-slate-800 cursor-pointer flex items-center text-xs"
                        type="button"
                      >
                        <span className="material-symbols-outlined text-[16px]">
                          {showAccount ? "visibility_off" : "visibility"}
                        </span>
                      </button>
                      <button
                        onClick={copyBankInfo}
                        className="text-slate-500 hover:text-slate-800 cursor-pointer flex items-center text-xs ml-0.5"
                        type="button"
                        title="Sao chép số tài khoản"
                      >
                        <span className="material-symbols-outlined text-[16px]">content_copy</span>
                      </button>
                    </div>
                    <div className="text-[11px] text-slate-600 font-medium mt-0.5">
                      CÔNG TY TNHH THỂ THAO BIỂN OCEAN CLUB
                    </div>
                  </div>
                </div>
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 text-[11px] font-semibold border border-emerald-200">
                  Chính thức
                </span>
              </div>
            </div>

            {/* Form Input */}
            <form onSubmit={handleSubmitPayout} className="space-y-3.5">
              {/* Settlement select */}
              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">
                  Chọn kỳ đối soát liên kết
                </label>
                <select
                  value={selectedCycle}
                  onChange={(e) => setSelectedCycle(e.target.value)}
                  className="w-full h-10 px-3 rounded-lg bg-white border border-slate-200 text-slate-800 text-xs font-medium focus:border-primary focus:outline-none"
                >
                  <option value="SET-202410-02">
                    Kỳ #SET-202410-02 (16/10 - 31/10/2024) — Khả dụng: {availableBalance.toLocaleString("vi-VN")} đ
                  </option>
                </select>
              </div>

              {/* Amount input */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-semibold text-slate-700">
                    Số tiền yêu cầu giải ngân (VNĐ) *
                  </label>
                  <button
                    type="button"
                    onClick={handleFillMax}
                    className="text-xs font-bold text-primary hover:underline flex items-center gap-1 cursor-pointer"
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
                    className={`w-full h-10 pl-3 pr-10 rounded-lg bg-white border font-mono font-bold text-base text-slate-900 focus:outline-none focus:border-primary ${
                      isAmountOver ? "border-red-500 text-red-600" : "border-slate-200"
                    }`}
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 font-bold text-slate-500 text-sm">
                    đ
                  </span>
                </div>

                {isAmountOver && (
                  <p className="text-red-600 text-[11px] mt-1 font-semibold">
                    Số tiền yêu cầu không được vượt quá số dư khả dụng ({availableBalance.toLocaleString("vi-VN")} đ).
                  </p>
                )}
              </div>

              {/* Note */}
              <div>
                <label className="text-xs font-semibold text-slate-700 block mb-1">
                  Ghi chú gửi Bộ phận Tài vụ Cảng vụ
                </label>
                <textarea
                  rows={2}
                  value={payoutNote}
                  onChange={(e) => setPayoutNote(e.target.value)}
                  className="w-full p-2.5 rounded-lg bg-white border border-slate-200 text-slate-800 text-xs font-medium focus:border-primary focus:outline-none"
                ></textarea>
              </div>

              <div className="pt-1 flex items-center justify-end">
                <button
                  type="submit"
                  disabled={isAmountOver || availableBalance <= 0}
                  className="w-full py-2.5 rounded-lg bg-primary text-white text-xs font-semibold hover:bg-primary-container cursor-pointer transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-xs"
                >
                  <span className="material-symbols-outlined text-[18px]">send</span>
                  <span>Gửi yêu cầu thanh toán ({numericAmount.toLocaleString("vi-VN")} đ)</span>
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right History Area: Financial Ledger Table */}
        <div className="lg:col-span-5 bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="px-3.5 py-2.5 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[18px]">receipt_long</span>
              <span className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Sổ Cái Lệnh Chi
              </span>
            </div>
            <span className="text-xs text-slate-600 font-medium">{history.length} giao dịch</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-100 text-slate-900 font-semibold text-[11px] uppercase">
                  <th className="py-2.5 px-3 border-r border-slate-200">Mã &amp; Kỳ</th>
                  <th className="py-2.5 px-3 border-r border-slate-200">Trạng thái</th>
                  <th className="py-2.5 px-3 text-right">Số tiền giải ngân</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {history.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-2.5 px-3 border-r border-slate-200">
                      <div className="font-mono font-bold text-slate-900 text-xs">#{item.id}</div>
                      <div className="text-[11px] text-slate-600">{item.cycleId}</div>
                      <div className="text-[10px] text-slate-400 mt-0.5">{item.createdAt}</div>
                    </td>
                    <td className="py-2.5 px-3 border-r border-slate-200">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold ${
                          item.status === "PAID"
                            ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                            : item.status === "REQUESTED"
                            ? "bg-amber-50 text-amber-700 border border-amber-200"
                            : "bg-slate-100 text-slate-600 border border-slate-200"
                        }`}
                      >
                        {item.status === "PAID" ? "ĐÃ CHI TRẢ" : "CHỜ DUYỆT"}
                      </span>
                      {item.processedAt && (
                        <div className="text-[10px] text-slate-400 mt-0.5">{item.processedAt}</div>
                      )}
                    </td>
                    <td className="py-2.5 px-3 text-right">
                      <span className="font-mono text-xs font-bold text-slate-900">
                        {item.amount.toLocaleString("vi-VN")}
                      </span>
                      <span className="text-[10px] font-medium text-slate-500 ml-1">đ</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
