import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useLanguageStore } from '../../store/useLanguageStore';

interface TimeSlotData {
  time: string;
  label: string;
  labelEn: string;
  sunCondition: string;
  sunConditionEn: string;
  temp: string;
  wave: string;
  visibility: string;
  uvIndex: string;
  recommendedTourTitle: string;
  recommendedTourTitleEn: string;
  recommendedTourCategory: string;
  recommendedTourCategoryEn: string;
  recommendedPrice: number;
  imageUrl: string;
}

const TIME_SLOTS: TimeSlotData[] = [
  {
    time: '05:30',
    label: 'Bình Minh Vàng Biển Sớm',
    labelEn: 'Sunrise Golden Glow',
    sunCondition: 'Mặt trời mọc êm đềm, mặt biển phẳng lặng như gương soi, nhiệt độ mát mẻ',
    sunConditionEn: 'Soft sunrise rays, mirrored sea surface with zero swell, cool morning breeze',
    temp: '25°C',
    wave: '0.2 - 0.3m',
    visibility: '8m',
    uvIndex: '1 (Thấp)',
    recommendedTourTitle: 'Chèo SUP Bình Minh Bãi Biển Mỹ Khê + Nhiếp Ảnh Chuyên Nghiệp',
    recommendedTourTitleEn: 'Sunrise SUP at My Khe Beach + Pro Photoshoot',
    recommendedTourCategory: 'Chèo SUP',
    recommendedTourCategoryEn: 'Paddleboard',
    recommendedPrice: 280000,
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '09:00',
    label: 'Khung Giờ Pha Lê Ngắm San Hô',
    labelEn: 'Crystal Coral Hours',
    sunCondition: 'Ánh nắng xuyên thấu nước biển sâu, tầm nhìn san hô đạt đỉnh cao nhất trong ngày',
    sunConditionEn: 'Maximum sunlight penetration, crystal visibility up to 14 meters underwater',
    temp: '28°C',
    wave: '0.3 - 0.5m',
    visibility: '12 - 14m',
    uvIndex: '5 (Vừa phải)',
    recommendedTourTitle: 'Lặn Bình Khí Khám Phá Rạn San Hô Hòn Sụp Bán Đảo Sơn Trà',
    recommendedTourTitleEn: 'Scuba Diving at Hon Sup Coral Reefs - Son Tra Peninsula',
    recommendedTourCategory: 'Lặn biển Scuba',
    recommendedTourCategoryEn: 'Scuba Diving',
    recommendedPrice: 550000,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '14:30',
    label: 'Gió Biển & Tốc Độ Cảm Giác Mạnh',
    labelEn: 'Breezy Adrenaline Hours',
    sunCondition: 'Gió biển Đông Nam thổi lộng, sóng nhấp nhô cực đã cho các môn tốc độ cao',
    sunConditionEn: 'Invigorating onshore breezes, energetic ocean waves ideal for high-speed sports',
    temp: '30°C',
    wave: '0.7 - 1.0m',
    visibility: '9m',
    uvIndex: '7 (Cần nón & kem chống nắng)',
    recommendedTourTitle: 'Bay Dù Lượn Trên Vịnh Biển Ngắm Toàn Cảnh Sơn Trà & Mỹ Khê',
    recommendedTourTitleEn: 'Coastal Bay Parasailing Over Da Nang Shoreline',
    recommendedTourCategory: 'Thể thao cảm giác mạnh',
    recommendedTourCategoryEn: 'Thrill & Flight',
    recommendedPrice: 650000,
    imageUrl: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '17:00',
    label: 'Hoàng Hôn Tím Hồng Thư Giãn',
    labelEn: 'Golden Sunset & Chill',
    sunCondition: 'Nắng dịu chuyển sắc rực rỡ, gió chiều mát rượi, không gian biển lãng mạn',
    sunConditionEn: 'Gentle golden hour glow, refreshing ocean breeze, romantic twilight scenery',
    temp: '27°C',
    wave: '0.4m',
    visibility: '7m',
    uvIndex: '1 (Dịu nhẹ)',
    recommendedTourTitle: 'Combo Chèo SUP Hoàng Hôn & Thưởng Thức Nước Dừa Bãi Biển Non Nước',
    recommendedTourTitleEn: 'Sunset SUP Relaxation with Beach Refreshments at Non Nuoc',
    recommendedTourCategory: 'Combo Hoàng Hôn',
    recommendedTourCategoryEn: 'Sunset Combo',
    recommendedPrice: 350000,
    imageUrl: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=800&q=80',
  },
];

export const SmartOceanPlanner: React.FC = () => {
  const { language, t } = useLanguageStore();
  const [selectedSlotIndex, setSelectedSlotIndex] = useState<number>(0);
  const currentSlot = TIME_SLOTS[selectedSlotIndex];

  return (
    <section className="pt-2 pb-6 sm:pt-3 sm:pb-8 bg-transparent relative overflow-hidden">
      <div className="w-full relative z-10">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-6">
          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-white drop-shadow-[0_2px_4px_rgba(0,0,0,0.95)] drop-shadow-[0_8px_20px_rgba(0,0,0,0.7)]">
            {t.planner.title}
          </h2>
          <p className="text-xs sm:text-sm text-slate-100 mt-1 max-w-xl mx-auto font-medium drop-shadow-[0_1.5px_3px_rgba(0,0,0,0.95)] drop-shadow-[0_4px_12px_rgba(0,0,0,0.6)]">
            {language === 'en' ? 'Select optimal sea hours for crystal waters, calm swell, and golden light' : 'Chọn khung giờ tối ưu theo độ cao sóng, tầm nhìn đáy biển và hướng nắng'}
          </p>
        </div>

        {/* Time Preset Buttons */}
        <div className="flex flex-wrap items-center justify-center gap-2.5 sm:gap-3 mb-5 sm:mb-6">
          {TIME_SLOTS.map((slot: TimeSlotData, index: number) => {
            const isActive = index === selectedSlotIndex;
            return (
              <button
                key={slot.time}
                onClick={() => setSelectedSlotIndex(index)}
                className={`flex items-center gap-2.5 px-4 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition-all duration-300 border cursor-pointer backdrop-blur-md ${
                  isActive
                    ? 'bg-cyan-600 text-white border-cyan-400 shadow-lg scale-105'
                    : 'bg-white/15 text-white border-white/25 shadow-sm hover:bg-white/25 hover:border-white/35'
                }`}
              >
                <span className={`font-mono text-sm font-black ${isActive ? 'text-white' : 'text-cyan-300'}`}>{slot.time}</span>
                <span className="drop-shadow-sm">{language === 'en' ? slot.labelEn : slot.label}</span>
              </button>
            );
          })}
        </div>

        {/* Dynamic Card Container with Transparent Glass Background (Revealing Ocean Backdrop) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center bg-white/10 backdrop-blur-md rounded-3xl p-6 sm:p-8 shadow-2xl border border-white/20">
          {/* Left Column: Marine & Sun Telemetry */}
          <div className="lg:col-span-7 space-y-4">
            <div className="flex items-center gap-2">
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-cyan-400/20 backdrop-blur-md text-cyan-200 border border-cyan-300/40 shadow-sm">
                {currentSlot.time} • {language === 'en' ? currentSlot.labelEn : currentSlot.label}
              </span>
            </div>

            <h3 className="text-lg sm:text-xl font-extrabold text-white leading-snug drop-shadow-[0_2px_8px_rgba(0,0,0,0.7)]">
              {language === 'en' ? currentSlot.sunConditionEn : currentSlot.sunCondition}
            </h3>

            {/* Ocean Telemetry Matrix */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
              <div className="p-3.5 rounded-xl bg-white/10 backdrop-blur-md border border-white/20">
                <div className="text-[11px] text-white/70 font-semibold uppercase">{language === 'en' ? 'Water Temp' : 'Nhiệt độ nước'}</div>
                <div className="text-base font-extrabold text-white mt-0.5 drop-shadow-sm">{currentSlot.temp}</div>
              </div>
              <div className="p-3.5 rounded-xl bg-white/10 backdrop-blur-md border border-white/20">
                <div className="text-[11px] text-white/70 font-semibold uppercase">{language === 'en' ? 'Wave Height' : 'Độ cao sóng'}</div>
                <div className="text-base font-extrabold text-cyan-300 mt-0.5 drop-shadow-sm">{currentSlot.wave}</div>
              </div>
              <div className="p-3.5 rounded-xl bg-white/10 backdrop-blur-md border border-white/20">
                <div className="text-[11px] text-white/70 font-semibold uppercase">{language === 'en' ? 'Visibility' : 'Độ trong nước'}</div>
                <div className="text-base font-extrabold text-emerald-300 mt-0.5 drop-shadow-sm">{currentSlot.visibility}</div>
              </div>
              <div className="p-3.5 rounded-xl bg-white/10 backdrop-blur-md border border-white/20">
                <div className="text-[11px] text-white/70 font-semibold uppercase">{language === 'en' ? 'UV Index' : 'Chỉ số UV'}</div>
                <div className="text-base font-extrabold text-amber-300 mt-0.5 drop-shadow-sm">{currentSlot.uvIndex}</div>
              </div>
            </div>
          </div>

          {/* Right Column: Recommended Tour Match Card */}
          <div className="lg:col-span-5">
            <div className="rounded-2xl overflow-hidden bg-slate-950/60 backdrop-blur-md border border-white/20 shadow-xl group">
              <div className="relative aspect-[16/10] overflow-hidden">
                <img
                  src={currentSlot.imageUrl}
                  alt={currentSlot.recommendedTourTitle}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
                <span className="absolute top-3 left-3 px-2.5 py-1 rounded-lg bg-cyan-600/90 backdrop-blur-sm text-white text-xs font-bold shadow">
                  {t.planner.recommendTour}
                </span>
              </div>

              <div className="p-4 sm:p-5">
                <span className="text-xs font-bold text-cyan-300 uppercase tracking-wider block mb-1">
                  {language === 'en' ? currentSlot.recommendedTourCategoryEn : currentSlot.recommendedTourCategory}
                </span>
                <h4 className="text-sm font-bold text-white line-clamp-2 mb-3 drop-shadow-sm">
                  {language === 'en' ? currentSlot.recommendedTourTitleEn : currentSlot.recommendedTourTitle}
                </h4>

                <div className="flex items-center justify-between pt-3 border-t border-white/15">
                  <div>
                    <span className="text-[11px] text-white/70 block">{language === 'en' ? 'Price' : 'Giá vé'}</span>
                    <span className="text-base font-black text-cyan-300 drop-shadow-sm">
                      {currentSlot.recommendedPrice.toLocaleString('vi-VN')}₫
                    </span>
                  </div>
                  <Link
                    to="/search"
                    className="px-4 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs transition-colors shadow-md"
                  >
                    {language === 'en' ? 'Book Slot' : 'Đặt ngay'}
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
