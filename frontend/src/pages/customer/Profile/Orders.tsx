import { useState, useMemo } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { FEATURED_SERVICES, MOCK_IMAGES, MOCK_SLOTS, MOCK_MASTER_ORDERS, MOCK_SUB_ORDERS } from "../../../mockData";
// type imports removed

export function Orders() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState("all");
  const [selectedQrOrder, setSelectedQrOrder] = useState<any>(null);

  const filteredMasterOrders = useMemo(() => {
    if (filter === "all") return MOCK_MASTER_ORDERS;
    if (filter === "upcoming") return MOCK_MASTER_ORDERS.filter(o => o.status === 'CONFIRMED');
    if (filter === "completed") return MOCK_MASTER_ORDERS.filter(o => o.status === 'CANCELLED' || o.status === 'COMPLETED');
    return MOCK_MASTER_ORDERS;
  }, [filter]);

  return (
    <main className="w-full pt-20 bg-surface flex-1">
      <div className="flex flex-col w-full">
        <div className="relative w-full overflow-hidden">
          <div className="absolute -top-24 right-10 w-96 h-96 rounded-full bg-secondary-fixed/30 blur-3xl pointer-events-none -z-10"></div>
          <div className="absolute top-10 left-1/3 w-80 h-80 rounded-full bg-primary-fixed/30 blur-3xl pointer-events-none -z-10"></div>
          
          <div className="max-w-[1280px] mx-auto px-margin-desktop pt-space-xl pb-space-lg">
            <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-space-md">
              <div>
                <div className="inline-flex items-center gap-space-xs px-space-sm py-1 rounded-full bg-secondary-container/40 text-on-secondary-container font-label-md text-label-md mb-space-xs">
                  <span className="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
                  Hệ thống đặt trước biển cảm xúc cao
                </div>
                <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">Trải nghiệm của tôi</h1>
                <p className="font-body-md text-body-md text-on-surface-variant mt-1">Theo dõi mã QR vé check-in, lịch trình thời tiết biển thực tế và chính sách bảo trợ an toàn.</p>
              </div>
              
              <div className="flex items-center gap-space-xs bg-surface-container-lowest p-space-xs rounded-full shadow-sm">
                <div className="px-space-md py-space-xs rounded-full bg-surface-container-low text-on-surface text-center">
                  <span className="block font-headline-sm text-headline-sm text-secondary">02</span>
                  <span className="font-label-sm text-label-sm text-on-surface-variant">Sắp tới</span>
                </div>
                <div className="px-space-md py-space-xs rounded-full bg-surface-container-low text-on-surface text-center">
                  <span className="block font-headline-sm text-headline-sm text-on-surface">01</span>
                  <span className="font-label-sm text-label-sm text-on-surface-variant">Đã đi</span>
                </div>
                <div className="px-space-md py-space-xs rounded-full bg-surface-container-low text-on-surface text-center">
                  <span className="block font-headline-sm text-headline-sm text-primary">100%</span>
                  <span className="font-label-sm text-label-sm text-on-surface-variant">Bảo hiểm biển</span>
                </div>
              </div>
            </div>

            <div className="mt-space-xl flex flex-wrap items-center gap-space-xs bg-surface-container-low p-1.5 rounded-full w-fit">
              <button onClick={() => setFilter("all")} className={`px-space-md py-space-xs rounded-full font-label-lg text-label-lg transition-all ${filter === 'all' ? 'bg-surface-container-lowest text-on-surface shadow-sm' : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-lowest/60'}`} type="button">
                Tất cả <span className="ml-1 text-label-sm px-1.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant">{MOCK_MASTER_ORDERS.length}</span>
              </button>
              <button onClick={() => setFilter("upcoming")} className={`px-space-md py-space-xs rounded-full font-label-lg text-label-lg transition-all ${filter === 'upcoming' ? 'bg-surface-container-lowest text-on-surface shadow-sm' : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-lowest/60'}`} type="button">
                Sắp diễn ra <span className="ml-1 text-label-sm px-1.5 py-0.5 rounded-full bg-secondary text-on-secondary">{MOCK_MASTER_ORDERS.filter(o => o.status === 'CONFIRMED').length}</span>
              </button>
              <button onClick={() => setFilter("completed")} className={`px-space-md py-space-xs rounded-full font-label-lg text-label-lg transition-all ${filter === 'completed' ? 'bg-surface-container-lowest text-on-surface shadow-sm' : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-lowest/60'}`} type="button">
                Hoàn thành <span className="ml-1 text-label-sm px-1.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant">{MOCK_MASTER_ORDERS.filter(o => o.status === 'CANCELLED' || o.status === 'COMPLETED').length}</span>
              </button>
            </div>
          </div>
        </div>

        <div className="max-w-[1280px] mx-auto px-margin-desktop pb-space-4xl w-full">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter-desktop items-start">
            <div key={filter} className="lg:col-span-8 flex flex-col gap-space-xl animate-fade-in-up">
              {filteredMasterOrders.map(masterOrder => {
                const subOrders = MOCK_SUB_ORDERS.filter(sub => sub.masterOrderId === masterOrder.id);
                return (
                  <article key={masterOrder.id} className="order-card bg-surface-container-lowest rounded-lg p-space-lg shadow-sm flex flex-col gap-space-lg transition-all duration-300 hover:shadow-md">
                    <div className="flex flex-wrap items-center justify-between gap-space-sm pb-space-sm bg-surface-container-low/60 -mx-space-lg -mt-space-lg px-space-lg pt-space-lg rounded-t-lg">
                      <div className="flex items-center gap-space-sm">
                        <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container">
                          <span className="material-symbols-outlined text-[20px]">receipt_long</span>
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Master Order:</span>
                            <span className="font-headline-sm text-headline-sm text-on-surface">#{masterOrder.id.toUpperCase()}</span>
                            <span className="px-space-xs py-0.5 rounded-full bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-bold">
                              {masterOrder.status === 'CONFIRMED' ? 'PAID (Đã thanh toán)' : 'COMPLETED'}
                            </span>
                          </div>
                          <p className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">Ngày tạo: {new Date(masterOrder.createdAt || '').toLocaleDateString('vi-VN')} · Phương thức thanh toán: QR Pay</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block">Tổng thanh toán Master</span>
                        <span className="font-headline-md text-headline-md text-primary font-bold">{masterOrder.totalAmount.toLocaleString('vi-VN')}đ</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-space-xs">
                      <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[18px] text-secondary">layers</span>
                        Danh sách đơn con (Sub Orders)
                      </span>
                      <span className="font-label-sm text-label-sm text-tertiary">{subOrders.length} dịch vụ</span>
                    </div>

                    {subOrders.map(subOrder => {
                      const service = FEATURED_SERVICES.find(s => s.id === subOrder.serviceId);
                      const slot = MOCK_SLOTS.find(s => s.id === subOrder.slotId);
                      const image = MOCK_IMAGES.find(i => i.serviceId === subOrder.serviceId && i.isPrimary)?.imageUrl;
                      
                      return (
                        <div key={subOrder.id} className="bg-surface-container-low rounded-DEFAULT p-space-md flex flex-col md:flex-row gap-space-md">
                          <div className="relative w-full md:w-56 h-40 flex-shrink-0 rounded-DEFAULT overflow-hidden shadow-sm">
                            <img src={image || 'https://via.placeholder.com/400x300'} className="w-full h-full object-cover" alt={service?.name} />
                            <span className="absolute top-2 left-2 px-space-xs py-0.5 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm flex items-center gap-1 shadow-sm">
                              <span className="w-1.5 h-1.5 rounded-full bg-secondary-fixed animate-ping"></span>
                              {slot?.startTime} {new Date(slot?.date || '').toLocaleDateString('vi-VN')}
                            </span>
                          </div>
                          <div className="flex flex-col justify-between flex-1 min-w-0">
                            <div>
                              <div className="flex items-start justify-between gap-2">
                                <div>
                                  <div className="flex items-center gap-2">
                                    <span className="font-mono font-bold text-label-sm text-secondary">#{subOrder.id.toUpperCase()}</span>
                                    <span className="font-label-sm text-label-sm text-on-surface-variant">· NCC: <strong>DANASEA Partner</strong></span>
                                  </div>
                                  <h3 className="font-headline-sm text-headline-sm text-on-surface mt-1 hover:text-primary cursor-pointer transition-colors" onClick={() => navigate(`/tour/${service?.id}`)}>
                                    {service?.name}
                                  </h3>
                                </div>
                                <div className="flex flex-col items-end">
                                  <span className="font-headline-sm text-headline-sm text-primary font-bold">{subOrder.subtotalAmount.toLocaleString('vi-VN')}đ</span>
                                  <span className="font-body-sm text-body-sm text-on-surface-variant">SL: {subOrder.quantity} người</span>
                                </div>
                              </div>
                              <div className="flex items-center gap-space-sm mt-space-sm">
                                <span className="inline-flex items-center gap-1 px-space-xs py-1 rounded bg-secondary-container/30 text-on-secondary-container font-label-sm text-[11px]">
                                  <span className="material-symbols-outlined text-[14px]">map</span>
                                  {service?.locationName}
                                </span>
                              </div>
                            </div>
                            
                            <div className="flex flex-wrap items-center justify-between gap-space-sm pt-space-sm mt-space-sm border-t border-surface-container-high">
                              <div className="flex items-center gap-space-xs">
                                <span className="px-space-sm py-1 rounded-full bg-secondary-container text-on-secondary-container font-label-sm text-[11px] font-bold">
                                  {subOrder.status === 'CONFIRMED' ? 'ĐÃ XÁC NHẬN' : 'HOÀN THÀNH'}
                                </span>
                              </div>
                              <div className="flex items-center gap-space-sm">
                                <button className="px-space-md py-1.5 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm flex items-center gap-1 shadow-sm transition-transform hover:scale-105 active:scale-95 cursor-pointer" type="button" onClick={() => setSelectedQrOrder(subOrder)}>
                                  <span className="material-symbols-outlined text-[16px]">qr_code_2</span>
                                  <span className="">Mã QR Check-in</span>
                                </button>
                                <button className="px-space-md py-1.5 rounded-full bg-surface-container-highest text-on-surface font-label-sm text-label-sm hover:bg-surface-dim transition-colors" type="button">
                                  Chi tiết &amp; Hỗ trợ
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </article>
                );
              })}
            </div>
            
            <aside className="lg:col-span-4 hidden lg:flex flex-col gap-space-lg">
              <div className="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm border border-secondary-fixed/30">
                <div className="flex items-center gap-2 mb-space-sm text-secondary">
                  <span className="material-symbols-outlined text-[24px]">verified_user</span>
                  <h3 className="font-headline-sm text-headline-sm">Chính sách bảo trợ &amp; An toàn biển</h3>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">Tất cả các dịch vụ (Sub-orders) được xử lý thông qua hệ thống DANASEA đều được áp dụng chính sách bảo vệ tự động.</p>
              </div>
            </aside>
          </div>
        </div>
      </div>

      {/* Portaled QR Code Modal */}
      {selectedQrOrder &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-scale-in">
            <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-3xl p-6 max-w-sm w-full shadow-2xl relative text-center">
              <button
                onClick={() => setSelectedQrOrder(null)}
                className="absolute top-4 right-4 p-1.5 rounded-full text-on-surface-variant hover:bg-surface-container cursor-pointer transition-colors"
              >
                <span className="material-symbols-outlined">close</span>
              </button>

              <div className="w-12 h-12 rounded-2xl bg-secondary-container text-on-secondary-container flex items-center justify-center mx-auto mb-3">
                <span className="material-symbols-outlined text-[28px]">qr_code_scanner</span>
              </div>

              <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                Mã QR Check-in Vé Biển
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                Đưa mã này cho HDV hoặc Quầy vé tại bến cảng để quét nhận ca
              </p>

              <div className="my-5 p-4 rounded-2xl bg-white inline-block shadow-md border border-outline-variant/20">
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(
                    selectedQrOrder.qrSecret || selectedQrOrder.id
                  )}`}
                  alt="QR Check-in"
                  className="w-44 h-44 mx-auto rounded-lg"
                />
              </div>

              <div className="bg-surface-container-low rounded-xl p-3 text-left space-y-1 mb-4">
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Mã vé (Sub-order):</span>
                  <span className="font-mono font-bold text-secondary">#{selectedQrOrder.id.toUpperCase()}</span>
                </div>
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Trạng thái:</span>
                  <span className="text-green-600 font-bold">Hợp lệ / Sẵn sàng</span>
                </div>
                <div className="flex justify-between font-label-sm text-label-sm">
                  <span className="text-on-surface-variant">Số lượng khách:</span>
                  <span className="text-on-surface font-bold">{selectedQrOrder.quantity} người</span>
                </div>
              </div>

              <button
                onClick={() => setSelectedQrOrder(null)}
                className="w-full py-2.5 rounded-full bg-secondary text-on-secondary font-label-md font-semibold hover:opacity-90 transition-opacity cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>,
          document.body
        )}
    </main>
  );
}
