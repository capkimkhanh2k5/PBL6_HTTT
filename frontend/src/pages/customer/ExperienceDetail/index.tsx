import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCartStore } from '../../../store/useCartStore';
import { useWishlistStore } from '../../../store/useWishlistStore';
import { FEATURED_SERVICES, MOCK_SLOTS, MOCK_IMAGES } from '../../../mockData';
import type { Service, ServiceSlot, ServiceImage } from '../../../types';

export function ExperienceDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [service, setService] = useState<Service | null>(null);
  const [images, setImages] = useState<ServiceImage[]>([]);
  const [slots, setSlots] = useState<ServiceSlot[]>([]);
  
  const [qty, setQty] = useState(1);
  const [selectedDate, setSelectedDate] = useState('2024-10-28');
  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null);
  const [isWaiverChecked, setIsWaiverChecked] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  
  const addItem = useCartStore((state) => state.addItem);
  const { toggleWishlist, isInWishlist } = useWishlistStore();

  useEffect(() => {
    // Find service
    const foundService = FEATURED_SERVICES.find(s => s.id === id);
    if (foundService) {
      setService(foundService);
      setImages(MOCK_IMAGES.filter(img => img.serviceId === foundService.id));
      setSlots(MOCK_SLOTS.filter(s => s.serviceId === foundService.id));
    }
  }, [id]);

  if (!service) {
    return (
      <main className="w-full pt-32 pb-20 bg-surface flex-1 flex flex-col items-center justify-center">
        <span className="material-symbols-outlined text-[64px] text-outline mb-4">search_off</span>
        <h2 className="font-headline-md text-on-surface">Không tìm thấy trải nghiệm</h2>
        <p className="font-body-md text-on-surface-variant mt-2">Trải nghiệm bạn đang tìm kiếm không tồn tại hoặc đã bị gỡ.</p>
        <button onClick={() => navigate('/search')} className="mt-6 px-6 py-2 bg-primary text-on-primary rounded-full hover:bg-primary-container transition-colors font-label-lg">Về trang Khám phá</button>
      </main>
    );
  }

  const primaryImage = images.find(i => i.isPrimary)?.imageUrl || 'https://via.placeholder.com/800x600';
  const availableSlotsForDate = slots.filter(s => s.date === selectedDate);
  const selectedSlot = slots.find(s => s.id === selectedSlotId);
  const totalPrice = qty * service.basePrice;

  // Capacity validation
  const maxCapacity = selectedSlot ? (selectedSlot.capacity - selectedSlot.bookedCount) : 4;
  const isSoldOut = selectedSlot && maxCapacity <= 0;
  
  const handleAddToCart = () => {
    if (!selectedSlot) {
      alert("Vui lòng chọn khung giờ khởi hành.");
      return;
    }
    if (!isWaiverChecked) {
      alert("Vui lòng đồng ý với Điều khoản miễn trừ trách nhiệm.");
      return;
    }
    
    setIsAdding(true);
    setTimeout(() => {
      addItem({
        serviceId: service.id,
        slotId: selectedSlot.id,
        quantity: qty,
        unitPrice: service.basePrice,
        serviceName: service.name,
        thumbnailUrl: primaryImage,
        date: selectedSlot.date,
        timeSlot: `${selectedSlot.startTime} - ${selectedSlot.endTime}`
      });
      setIsAdding(false);
      // alert(`Đã thêm ${qty} vé vào giỏ hàng thành công!`);
    }, 600);
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1">
      <div className="flex flex-col w-full">
        {/* Breadcrumb Navigation */}
        <div className="w-full bg-surface-container-lowest/60 backdrop-blur-md">
          <div className="max-w-[1280px] mx-auto px-margin-mobile md:px-margin-desktop py-space-sm">
            <nav className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md overflow-x-auto whitespace-nowrap">
              <span onClick={() => navigate('/')} className="hover:text-primary transition-colors flex items-center gap-1 cursor-pointer">
                <span className="material-symbols-outlined text-[16px]">home</span>
                Trang chủ
              </span>
              <span className="material-symbols-outlined text-[14px] text-outline-variant">chevron_right</span>
              <span onClick={() => navigate('/search')} className="hover:text-primary transition-colors cursor-pointer">Khám phá</span>
              <span className="material-symbols-outlined text-[14px] text-outline-variant">chevron_right</span>
              <span className="text-on-surface font-bold truncate">{service.name}</span>
            </nav>
          </div>
        </div>

        <div className="max-w-[1280px] mx-auto w-full px-margin-mobile md:px-margin-desktop pt-space-md pb-space-4xl flex flex-col gap-space-xl">
          
          {/* Title & Quick Actions Header */}
          <section className="flex flex-col lg:flex-row lg:items-end justify-between gap-space-md">
            <div className="flex flex-col gap-space-xs max-w-3xl">
              <div className="flex flex-wrap items-center gap-space-2xs">
                <span className="inline-flex items-center gap-1 px-space-sm py-1 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm shadow-sm">
                  <span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'FILL' 1" }}>verified</span>
                  Nhà cung cấp đã xác minh
                </span>
              </div>
              <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight leading-tight mt-space-2xs">
                {service.name}
              </h1>
              <div className="flex flex-wrap items-center gap-y-2 gap-x-space-md font-body-sm text-body-sm text-on-surface-variant">
                <div className="flex items-center gap-1 text-on-surface font-semibold">
                  <span className="material-symbols-outlined text-[18px] text-primary" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                  <span>4.9 / 5.0</span>
                  <span className="text-on-surface-variant font-normal underline cursor-pointer hover:text-secondary">(128 đánh giá)</span>
                </div>
                <span className="w-1 h-1 rounded-full bg-outline-variant"></span>
                <div className="flex items-center gap-1 text-on-surface">
                  <span className="material-symbols-outlined text-[18px] text-secondary">schedule</span>
                  <span>{service.durationMinutes} phút</span>
                </div>
                <span className="w-1 h-1 rounded-full bg-outline-variant"></span>
                <div className="flex items-center gap-1">
                  <span className="material-symbols-outlined text-[18px] text-secondary">location_on</span>
                  <span>{service.locationName}</span>
                </div>
              </div>
            </div>

            {/* Wishlist Quick Action */}
            <div className="flex items-center gap-3 self-start lg:self-center">
              {(() => {
                const isSaved = isInWishlist(service.id);
                return (
                  <button
                    onClick={() => toggleWishlist(service.id, service.name)}
                    className={`px-4 py-2.5 rounded-full font-label-md text-label-md flex items-center gap-2 border transition-all shadow-sm cursor-pointer hover:scale-105 active:scale-95 ${
                      isSaved
                        ? 'bg-red-50 border-red-200 text-red-600 shadow-md ring-2 ring-red-100'
                        : 'bg-surface-container-lowest border-outline-variant/40 text-on-surface hover:border-red-300 hover:text-red-500'
                    }`}
                  >
                    <span
                      className={`material-symbols-outlined text-[20px] ${isSaved ? 'text-red-500' : ''}`}
                      style={{ fontVariationSettings: isSaved ? "'FILL' 1" : "'FILL' 0" }}
                    >
                      favorite
                    </span>
                    <span className="font-semibold">{isSaved ? 'Đã lưu yêu thích' : 'Lưu vào yêu thích'}</span>
                  </button>
                );
              })()}
            </div>
          </section>

          {/* Editorial Gallery Grid */}
          <section className="w-full">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-sm rounded-xl overflow-hidden shadow-md bg-surface-container">
              <div className="lg:col-span-7 relative group cursor-pointer overflow-hidden min-h-[340px] md:min-h-[460px]">
                <img src={primaryImage} alt={service.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 ease-out"/>
              </div>
              <div className="lg:col-span-5 grid grid-cols-2 gap-space-sm">
                {[1,2,3,4].map((i) => (
                  <div key={i} className="relative group cursor-pointer overflow-hidden aspect-[4/3] lg:aspect-auto bg-surface-container-high">
                    <div className="absolute inset-0 bg-black/10 group-hover:bg-transparent transition-colors"></div>
                  </div>
                ))}
              </div>
            </div>
          </section>

          {/* Main Content Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
            
            {/* LEFT COLUMN: Detailed Experience Intel */}
            <main className="lg:col-span-8 flex flex-col gap-space-2xl">
              <section className="flex flex-col gap-space-md">
                <h2 className="font-headline-md text-headline-md text-on-surface">Mô tả trải nghiệm</h2>
                <p className="font-body-lg text-on-surface-variant leading-relaxed">
                  {service.description}
                </p>
              </section>
            </main>

            {/* RIGHT COLUMN: Sticky Interactive Booking Box */}
            <aside className="lg:col-span-4 w-full lg:sticky lg:top-24">
              <div className="w-full p-space-lg rounded-[20px] bg-surface-container-lowest shadow-xl flex flex-col gap-space-md">
                
                <div className="flex items-baseline justify-between">
                  <div className="flex items-baseline gap-1.5">
                    <span className="font-headline-lg text-headline-lg text-primary font-extrabold tracking-tight">{service.basePrice.toLocaleString('vi-VN')} đ</span>
                    <span className="font-body-sm text-body-sm text-on-surface-variant">/ người</span>
                  </div>
                </div>

                {/* Date Selection */}
                <div className="flex flex-col gap-1.5">
                  <label className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant font-bold">Ngày khởi hành</label>
                  <select 
                    value={selectedDate}
                    onChange={(e) => {
                      setSelectedDate(e.target.value);
                      setSelectedSlotId(null);
                    }}
                    className="w-full p-space-sm rounded-DEFAULT bg-surface-container-low cursor-pointer hover:bg-surface-container transition-colors outline-none font-label-md text-on-surface"
                  >
                    <option value="2024-10-28">28 Tháng 10, 2024</option>
                    <option value="2024-10-29">29 Tháng 10, 2024</option>
                  </select>
                </div>

                {/* Slot Selection */}
                <div className="flex flex-col gap-1.5">
                  <label className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant font-bold">Khung giờ</label>
                  <div className="flex flex-col gap-2">
                    {availableSlotsForDate.length === 0 && (
                      <p className="text-on-surface-variant font-body-sm text-center py-2">Không có slot trong ngày này.</p>
                    )}
                    {availableSlotsForDate.map(slot => {
                      const isActive = selectedSlotId === slot.id;
                      const remain = slot.capacity - slot.bookedCount;
                      const isFull = remain <= 0;
                      
                      return (
                        <div 
                          key={slot.id} 
                          onClick={() => !isFull && setSelectedSlotId(slot.id)}
                          className={`p-space-sm rounded-DEFAULT border-2 transition-all cursor-pointer flex items-center justify-between ${
                            isActive ? 'bg-secondary/10 border-secondary' : isFull ? 'bg-surface-container-highest border-transparent opacity-50 cursor-not-allowed' : 'bg-surface-container-low border-transparent hover:bg-surface-container'
                          }`}
                        >
                          <div className="flex flex-col">
                            <span className={`font-label-md text-label-md font-bold ${isActive ? 'text-secondary' : 'text-on-surface'}`}>
                              {slot.startTime} - {slot.endTime}
                            </span>
                            <span className="font-label-sm text-[11px] text-on-surface-variant mt-0.5">
                              Đã đặt: {slot.bookedCount}/{slot.capacity} {isFull ? '(Đã đầy)' : `(Còn ${remain} chỗ)`}
                            </span>
                          </div>
                          {isActive && <span className="material-symbols-outlined text-secondary" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span>}
                        </div>
                      )
                    })}
                  </div>
                </div>

                {/* Quantity */}
                <div className="flex items-center justify-between p-space-sm rounded-DEFAULT bg-surface-container-low">
                  <div className="flex flex-col">
                    <span className="font-label-md text-label-md font-bold text-on-surface">Số lượng người</span>
                    {selectedSlot && <span className="font-label-sm text-[11px] text-on-surface-variant">Tối đa {maxCapacity} người</span>}
                  </div>
                  <div className="flex items-center gap-space-sm">
                    <button onClick={() => setQty(Math.max(1, qty - 1))} className="w-8 h-8 rounded-full bg-surface-container-lowest text-on-surface flex items-center justify-center font-bold hover:bg-surface-variant transition-colors shadow-sm active:scale-95 disabled:opacity-50" disabled={qty <= 1}>-</button>
                    <span className="font-headline-sm text-headline-sm font-bold text-on-surface min-w-[20px] text-center">{qty}</span>
                    <button onClick={() => setQty(Math.min(maxCapacity > 0 ? maxCapacity : 1, qty + 1))} className="w-8 h-8 rounded-full bg-surface-container-lowest text-on-surface flex items-center justify-center font-bold hover:bg-surface-variant transition-colors shadow-sm active:scale-95 disabled:opacity-50" disabled={!selectedSlot || isSoldOut || qty >= maxCapacity}>+</button>
                  </div>
                </div>

                {/* Waiver */}
                <div className="p-space-sm rounded-DEFAULT bg-surface-container-low border border-outline-variant/50 flex flex-col gap-2">
                  <label className="flex items-start gap-space-sm cursor-pointer select-none">
                    <input checked={isWaiverChecked} onChange={(e) => setIsWaiverChecked(e.target.checked)} className="mt-1 w-4 h-4 rounded text-secondary accent-secondary cursor-pointer" type="checkbox"/>
                    <div className="flex flex-col flex-1">
                      <span className="font-label-sm text-label-sm font-bold text-on-surface">Tôi đã đọc và đồng ý Điều khoản</span>
                    </div>
                  </label>
                </div>

                {/* Total */}
                <div className="flex flex-col gap-space-xs py-space-xs border-t border-surface-container-highest mt-2 pt-4">
                  <div className="flex items-baseline justify-between">
                    <span className="font-headline-sm text-headline-sm text-on-surface">Tổng tạm tính:</span>
                    <span className="font-headline-md text-headline-md text-primary font-bold">{totalPrice.toLocaleString("vi-VN")} đ</span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-space-xs mt-2">
                  <button 
                    onClick={handleAddToCart} 
                    disabled={!selectedSlot || isSoldOut || !isWaiverChecked || isAdding}
                    className={`w-full py-space-sm px-space-md rounded-full font-label-lg text-label-lg shadow-lg flex items-center justify-center gap-2 transition-all ${
                      isAdding ? 'bg-secondary/70 text-on-secondary scale-95' :
                      (!selectedSlot || isSoldOut || !isWaiverChecked) ? 'bg-surface-container-highest text-outline cursor-not-allowed' : 
                      'bg-secondary text-on-secondary hover:scale-[1.02] active:scale-98'
                    }`}
                  >
                    {isAdding ? (
                      <span className="material-symbols-outlined animate-spin text-[20px]">refresh</span>
                    ) : (
                      <span className="material-symbols-outlined text-[20px]">shopping_bag</span>
                    )}
                    <span>{isAdding ? "Đang thêm..." : "Thêm vào giỏ hàng"}</span>
                  </button>
                </div>

              </div>
            </aside>
          </div>
        </div>
      </div>
    </main>
  );
}
