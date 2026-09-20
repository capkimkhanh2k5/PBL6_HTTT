import { FEATURED_SERVICES, CATEGORIES, MOCK_IMAGES } from "../../../mockData";
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect, useRef } from "react";
import { useWishlistStore } from "../../../store/useWishlistStore";



const CUSTOM_PROMPT_DATA: Record<string, any> = {
  'group': {
    chatPrompt: 'Nhóm mình 6 người muốn đi biển nửa ngày, có hoạt động nào vui mà gắn kết không AI?',
    chatReply: 'Chào bạn! Với nhóm 6 người đi nửa ngày, tuyệt vời nhất là thuê 3 ván SUP lớn tại Mỹ Khê hoặc lặn ống thở ngắm san hô bãi cạn ở Sơn Trà. Hai hoạt động này đều dễ tham gia và cực vui cho nhóm đông.',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCiCinGFFDq1b2aeeARfPVNcgxJgHPdWE9E76EUUhfRGrwK0SCa99hMmNS2u1FTDisuWaW71Z2Kp4xm5Aq1wImxgCGjDR3Jnua7z1N46VgNLxijv4rujwSYudPKQNz2May1ZeHWs_-GIr2UUY1ALZl3lm9riuphJpcK_Wr1224VOcY0Ye857Ssegu0I7w8BP14sSTB2k2aPxBV_eqgWXOEvuyq761zw3TX3hf-lYg6Isj8HhzAMTtzbRw',
    chatTourName: 'Combo chèo SUP & Snorkeling Nhóm',
  },
  'couple': {
    chatPrompt: 'Vợ chồng mình muốn tìm trải nghiệm thư giãn, nhẹ nhàng vào buổi chiều tối.',
    chatReply: 'Tuyệt vời! Buổi chiều mát mẻ ở biển Non Nước rất vắng và êm. Mình gợi ý gói thuê lều cắm trại hoàng hôn, kết hợp set BBQ nhẹ trên bãi biển dành riêng cho 2 người.',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAWikUk3u0Fx_6OcE-c9MEtKxAeMexu0wXSQ9gipYr2ehc5uoitY27VJ7uptBz0I-Cn77uuWW0suwxErMe2mNpgRV2mKzNoA54FYC2g5UUMQcK391No6PnAAXLF6VKcjvJMD28IfX9MvogcFIJCCkMrHbIY47lqT3F_avlYlJqLzUL3h2NDKuoeaZoBkOb_Qin9GDgAiF4BZmkSHcFmsCVKOxkMlKsqJd3uBgNE0WYEoOh_8qODy4yCzA',
    chatTourName: 'Cắm trại hoàng hôn lãng mạn',
  },
  'weather': {
    chatPrompt: 'Dự báo ngày mai trời có nắng gắt không? Nên chơi gì cho mát?',
    chatReply: 'Ngày mai Đà Nẵng nắng khá gắt vào buổi trưa. Bạn nên chèo SUP thật sớm lúc 5h30 sáng để đón bình minh, hoặc đợi đến 16h chiều để chơi lướt ván cano trên biển nhé!',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuADGaSgOa0TU8HJcag0bcjIer9e2oPsjKtc4UaeFqThYpfat8NQ03IMx29A4lViMyGpjSGkiAJGL1YKC0HsmTyPNiPTTXA084KsRSVN8ypOZhH4qsfAhLTOB28dPvIjviUIUalF6uu45GvDC5LyoV4XspXY9k01giKnWByAuX2nTBlB3dXl4Ld5e11jZJBsrzYfIPf-fFOkHD_DE5HqgfKLoeAPpce40CZ6k2yndS2kTCSXWE3FSsZQ8A',
    chatTourName: 'Lướt ván cano chiều mát',
  }
};

const AI_BEACH_DATA: Record<string, any> = {
  'Mỹ Khê': {
    chatPrompt: 'Nhóm mình 4 người muốn chèo SUP sáng mai ở Mỹ Khê, giờ nào biển êm và ánh sáng chụp ảnh đẹp nhất vậy bạn?',
    chatReply: 'Chào bạn! Sáng mai tại Mỹ Khê, thủy triều đạt mức đẹp từ 05:15 - 06:45. Sóng chỉ cao 0.3m, mặt nước phẳng như gương và mặt trời ló rạng ngay tầm mắt.',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuADGaSgOa0TU8HJcag0bcjIer9e2oPsjKtc4UaeFqThYpfat8NQ03IMx29A4lViMyGpjSGkiAJGL1YKC0HsmTyPNiPTTXA084KsRSVN8ypOZhH4qsfAhLTOB28dPvIjviUIUalF6uu45GvDC5LyoV4XspXY9k01giKnWByAuX2nTBlB3dXl4Ld5e11jZJBsrzYfIPf-fFOkHD_DE5HqgfKLoeAPpce40CZ6k2yndS2kTCSXWE3FSsZQ8A',
    chatTourName: 'SUP Bình Minh Mỹ Khê (Kèm Nhiếp Ảnh)',
    temp: { val: '28°C', desc: 'Nắng nhẹ, gió biển mát', percent: '65%' },
    wave: { val: '0.4 m', desc: 'Sóng êm • Lý tưởng cho SUP', percent: '25%' },
    visibility: { val: '8 mét', desc: 'Nước trong ngắm san hô', percent: '80%' },
    wind: { val: '12 km/h', desc: 'Hướng Đông Nam đều đặn', percent: '40%' },
    recommendationTitle: 'Rất thích hợp cho Chèo SUP & Lặn biển ngắm san hô',
    recommendationDesc: 'Biển tĩnh lặng kéo dài đến 10:30 sáng. Hãy trang bị kem chống nắng thân thiện môi trường rạn san hô.',
  },
  'Sơn Trà': {
    chatPrompt: 'Cho mình xin lịch trình lặn ngắm san hô ở Sơn Trà vào cuối tuần này với?',
    chatReply: 'Tuyệt vời! Cuối tuần này tại Sơn Trà, nước rất trong với tầm nhìn lên đến 12m. Bạn nên đi tầm 08:30 sáng ở khu vực Bãi Rạng hoặc Mũi Nghê để có trải nghiệm ngắm san hô đẹp nhất nhé.',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCiCinGFFDq1b2aeeARfPVNcgxJgHPdWE9E76EUUhfRGrwK0SCa99hMmNS2u1FTDisuWaW71Z2Kp4xm5Aq1wImxgCGjDR3Jnua7z1N46VgNLxijv4rujwSYudPKQNz2May1ZeHWs_-GIr2UUY1ALZl3lm9riuphJpcK_Wr1224VOcY0Ye857Ssegu0I7w8BP14sSTB2k2aPxBV_eqgWXOEvuyq761zw3TX3hf-lYg6Isj8HhzAMTtzbRw',
    chatTourName: 'Lặn ngắm san hô Mũi Nghê',
    temp: { val: '26°C', desc: 'Mát mẻ, bóng râm từ rừng', percent: '60%' },
    wave: { val: '0.2 m', desc: 'Biển cực êm', percent: '15%' },
    visibility: { val: '12 mét', desc: 'Tuyệt hảo cho lặn biển', percent: '95%' },
    wind: { val: '8 km/h', desc: 'Gió rất nhẹ', percent: '20%' },
    recommendationTitle: 'Thời điểm vàng để lặn biển và khám phá sinh thái',
    recommendationDesc: 'Hệ sinh thái san hô đang ở trạng thái tốt nhất. Cẩn thận các bãi đá ngầm khi chèo thuyền tiếp cận.',
  },
  'Non Nước': {
    chatPrompt: 'Mình muốn tìm một hoạt động biển nhẹ nhàng cho gia đình có trẻ nhỏ ở Non Nước.',
    chatReply: 'Chào bạn! Bãi Non Nước hiện tại sóng rất êm, bờ cát rộng. Gia đình mình có thể tham gia trải nghiệm dù lượn cano hoặc thuê lều cắm trại ngay trên bãi biển vào buổi chiều tà.',
    chatImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAWikUk3u0Fx_6OcE-c9MEtKxAeMexu0wXSQ9gipYr2ehc5uoitY27VJ7uptBz0I-Cn77uuWW0suwxErMe2mNpgRV2mKzNoA54FYC2g5UUMQcK391No6PnAAXLF6VKcjvJMD28IfX9MvogcFIJCCkMrHbIY47lqT3F_avlYlJqLzUL3h2NDKuoeaZoBkOb_Qin9GDgAiF4BZmkSHcFmsCVKOxkMlKsqJd3uBgNE0WYEoOh_8qODy4yCzA',
    chatTourName: 'Cắm trại hoàng hôn Non Nước',
    temp: { val: '29°C', desc: 'Nắng ráo, thích hợp tắm biển', percent: '75%' },
    wave: { val: '0.6 m', desc: 'Sóng vừa phải', percent: '40%' },
    visibility: { val: '6 mét', desc: 'Tầm nhìn khá', percent: '60%' },
    wind: { val: '15 km/h', desc: 'Gió lộng, mát mẻ', percent: '50%' },
    recommendationTitle: 'Tuyệt vời cho các hoạt động thể thao nước và gia đình',
    recommendationDesc: 'Bãi biển rộng rãi, an toàn cho trẻ em. Gió lý tưởng để vui chơi và thư giãn cuối ngày.',
  }
};

export function Home() {
  const { toggleWishlist, isInWishlist } = useWishlistStore();
  const [activeAIBeach, setActiveAIBeach] = useState<string>('Mỹ Khê');
  const [activeCustomPrompt, setActiveCustomPrompt] = useState<string | null>(null);
  
  const [chatInputValue, setChatInputValue] = useState('');
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [chatHistory, setChatHistory] = useState<any[]>([]);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  const handleChatSubmit = () => {
    if (!chatInputValue.trim() || isChatLoading) return;
    const promptText = chatInputValue;
    setChatHistory(prev => [...prev, { 
      chatPrompt: promptText, 
      chatReply: null, 
      chatImage: '', 
      chatTourName: '' 
    }]);
    setChatInputValue('');
    setIsChatLoading(true);
    
    setTimeout(() => {
      setChatHistory(prev => {
        const newHistory = [...prev];
        newHistory[newHistory.length - 1].chatReply = 'Chào bạn, đây là phiên bản Demo nên mình chưa thể phân tích câu hỏi tự do. Tính năng DANASEA AI chính thức sẽ sớm được ra mắt. Hiện tại bạn có thể bấm các nút gợi ý bên trái để khám phá nhé!';
        return newHistory;
      });
      setIsChatLoading(false);
    }, 1500);
  };

  const aiWeatherData = AI_BEACH_DATA[activeAIBeach];
  const baseMessage = activeCustomPrompt ? CUSTOM_PROMPT_DATA[activeCustomPrompt] : AI_BEACH_DATA[activeAIBeach];
  const displayMessages = [baseMessage, ...chatHistory];

  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [displayMessages]);
 

  // State cho Search Capsule
  const [activeDropdown, setActiveDropdown] = useState<string | null>(null);
  const [selectedRegions, setSelectedRegions] = useState<string[]>(['Mỹ Khê']);
  const [selectedTime, setSelectedTime] = useState<string>('05:30');
  const [guests, setGuests] = useState({ adults: 2, children: 0 });

    const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [activeRegionFilter, setActiveRegionFilter] = useState<string | null>(null);
  const displayTours = FEATURED_SERVICES.filter(t => {
    const matchCat = activeCategory ? t.categoryId === activeCategory : true;
    const matchRegion = activeRegionFilter ? t.locationName.includes(activeRegionFilter) : true;
    return matchCat && matchRegion;
  });

  const searchRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Hàm xử lý khi bấm Khám phá vùng biển
  const handleExploreRegion = (e: React.MouseEvent, regionName: string) => {
    e.preventDefault();
    // Use substring to match location in mockData (e.g. 'Sơn Trà', 'Mỹ Khê', 'Non Nước')
    let filterTerm = regionName;
    if (regionName.includes('Sơn Trà')) filterTerm = 'Sơn Trà';
    if (regionName.includes('Mỹ Khê')) filterTerm = 'Mỹ Khê';
    if (regionName.includes('Non Nước')) filterTerm = 'Non Nước';
    
    setActiveRegionFilter(filterTerm);
    
    const featuredSection = document.getElementById('featured');
    if (featuredSection) {
      featuredSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };


  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setActiveDropdown(null);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const toggleDropdown = (dropdown: string) => {
    setActiveDropdown(activeDropdown === dropdown ? null : dropdown);
  };

  const handleRegionChange = (region: string) => {
    if (selectedRegions.includes(region)) {
      setSelectedRegions(selectedRegions.filter(r => r !== region));
    } else {
      setSelectedRegions([...selectedRegions, region]);
    }
  };
  const scrollToSection = (e: React.MouseEvent<HTMLAnchorElement>, id: string) => {
    e.preventDefault();
    const element = document.querySelector(id);
    if (element) {
      const headerHeight = 80;
      const targetPosition = element.getBoundingClientRect().top + window.scrollY - headerHeight;
      const startPosition = window.scrollY;
      const distance = targetPosition - startPosition;
      const duration = 600;
      let start: number | null = null;

      const easeInOutQuad = (t: number, b: number, c: number, d: number) => {
        t /= d / 2;
        if (t < 1) return (c / 2) * t * t + b;
        t--;
        return (-c / 2) * (t * (t - 2) - 1) + b;
      };

      const animation = (currentTime: number) => {
        if (start === null) start = currentTime;
        const timeElapsed = currentTime - start;
        const run = easeInOutQuad(timeElapsed, startPosition, distance, duration);
        window.scrollTo(0, run);
        if (timeElapsed < duration) requestAnimationFrame(animation);
      };

      requestAnimationFrame(animation);
    }
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1"><div className="flex flex-col w-full">
{/* SECTION 1: PANORAMA HERO */}
<section className="relative w-full -mt-20 bg-on-background">
{/* Fullscreen Panorama Background */}
<div className="w-full h-[760px] lg:h-[840px] bg-cover bg-center relative flex items-center justify-center" data-alt="Stunning aerial wide panorama view of Da Nang coastline, My Khe beach golden sand curving gently toward Son Tra peninsula lush green mountains, turquoise crystal calm sea under crisp morning sunlight with distant fishing boats, tropical serene vibe" style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuAI18BWUCgDObyX8R2qoVNX-0lRA8V1TRPR1nNyc2A-kPzRlRXpQ54POlJk15W6iNxr8NgSnnx5UARCu6rro9bplagw15Dr_ZE6RsLc3l1pFb_GPQFB5Aq4FFrpYw_VdA358fowFouKp0DIiOJP27ZDKh6P6av5S3JsphpebwOe8MwGjiYRt-KZXyi8i6JUMn88JdRxUmaFA8B2qkPzj3F36eNFT_JuDt6H3A9F-u6jhzjBNNLQ7e86KA')" }}>
{/* Atmospheric Gradients: Coastal Dark Navy Scrim for High Typography Contrast */}
<div className="absolute inset-0 bg-gradient-to-t from-[#0a1b24] via-[#0a1b24]/40 to-black/35 pointer-events-none"></div>
<div className="absolute inset-0 bg-radial-gradient from-transparent via-[#006874]/15 to-[#001f24]/50 pointer-events-none"></div>
{/* Hero Text Content */}
<div className="relative z-10 max-w-[1280px] w-full mx-auto px-margin-desktop pt-32 pb-36 flex flex-col items-start justify-center">
{/* Pill Kicker */}
<div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-surface-container-lowest/20 backdrop-blur-md text-secondary-fixed font-label-md text-label-md tracking-[0.2em] uppercase mb-space-md shadow-sm">
<span className="w-2 h-2 rounded-full bg-primary-container animate-pulse"></span>
          Trải nghiệm biển Đà Nẵng
        </div>
{/* Headline */}
<h1 className="font-display-hero text-display-hero text-white tracking-tight max-w-3xl drop-shadow-md text-left mb-space-md">
          Chạm sóng biển,<br />
<span className="text-secondary-fixed">mở chuyến đi riêng.</span>
</h1>
{/* Subtitle */}
<p className="font-body-lg text-body-lg text-surface-variant max-w-xl mb-space-xl leading-relaxed text-left">
          Khám phá những trải nghiệm biển phù hợp với bạn. Chọn hoạt động, đặt lịch và sẵn sàng tận hưởng một Đà Nẵng rực rỡ dưới nắng mai.
        </p>
{/* CTA Action Buttons */}
<div className="flex flex-wrap items-center gap-space-md">
<a onClick={(e) => scrollToSection(e, '#featured')} className="px-8 py-3.5 rounded-full bg-secondary text-white font-label-lg text-label-lg shadow-[0_8px_24px_rgba(0,104,116,0.4)] hover:bg-secondary/90 hover:scale-[1.02] transition-all flex items-center gap-2 cursor-pointer" href="#featured">
            Khám phá trải nghiệm
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
</a>
<a onClick={(e) => scrollToSection(e, '#ai-concierge')} className="px-8 py-3.5 rounded-full bg-white/10 backdrop-blur-md text-white font-label-lg text-label-lg hover:bg-white/20 transition-all flex items-center gap-2 cursor-pointer" href="#ai-concierge">
<span className="material-symbols-outlined text-[18px] text-primary-container">auto_awesome</span>
            Gợi ý lịch trình cho tôi
          </a>
</div>
</div>
{/* FLOATING SEARCH BAR CAPSULE (Docked at Bottom Edge) */}
<div className="absolute bottom-6 left-0 right-0 z-20 px-margin-desktop max-w-[1200px] mx-auto w-full">
  <div id="search-capsule" ref={searchRef} className="bg-surface-container-lowest rounded-[28px] p-3 shadow-[0_18px_40px_-6px_rgba(16,47,58,0.22)] backdrop-blur-2xl relative">
    <div className="grid grid-cols-1 md:grid-cols-4 items-center gap-2">
      
      {/* 1. Vùng biển */}
      <div className="relative">
        <div onClick={() => toggleDropdown('region')} className="flex items-center gap-3 px-5 py-2.5 rounded-full hover:bg-surface-container-low transition-colors cursor-pointer group">
          <span className="material-symbols-outlined text-secondary text-[22px] group-hover:scale-110 transition-transform">location_on</span>
          <div className="flex flex-col min-w-0 text-left">
            <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Khu vực biển</span>
            <span className="font-label-lg text-label-lg text-on-surface truncate">
              {selectedRegions.length > 0 ? selectedRegions.join(', ') : 'Chọn khu vực'}
            </span>
          </div>
        </div>
        {activeDropdown === 'region' && (
          <div className="absolute top-full left-0 mt-4 w-64 bg-surface-container-lowest rounded-2xl p-4 shadow-xl border border-surface-container animate-fade-in-up z-50">
            <h4 className="font-label-md text-label-md text-on-surface-variant mb-3">Chọn vùng biển</h4>
            <div className="flex flex-col gap-2">
              {['Mỹ Khê', 'Sơn Trà', 'Non Nước'].map(region => (
                <label key={region} className="flex items-center gap-3 cursor-pointer p-2 hover:bg-surface-container-low rounded-lg transition-colors">
                  <input 
                    type="checkbox" 
                    className="w-4 h-4 rounded text-primary focus:ring-primary accent-primary" 
                    checked={selectedRegions.includes(region)}
                    onChange={() => handleRegionChange(region)}
                  />
                  <span className="font-body-md text-on-surface">{region}</span>
                </label>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 2. Ngày trải nghiệm */}
      <div className="relative">
        <div onClick={() => toggleDropdown('time')} className="flex items-center gap-3 px-5 py-2.5 rounded-full hover:bg-surface-container-low transition-colors cursor-pointer group">
          <span className="material-symbols-outlined text-secondary text-[22px] group-hover:scale-110 transition-transform">schedule</span>
          <div className="flex flex-col min-w-0 text-left">
            <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Thời gian</span>
            <span className="font-label-lg text-label-lg text-on-surface truncate">{selectedTime}</span>
          </div>
        </div>
        {activeDropdown === 'time' && (
          <div className="absolute top-full left-0 mt-4 w-64 bg-surface-container-lowest rounded-2xl p-4 shadow-xl border border-surface-container animate-fade-in-up z-50">
            <h4 className="font-label-md text-label-md text-on-surface-variant mb-3">Chọn thời gian</h4>
            <div className="flex flex-col gap-1 max-h-64 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-surface-container-highest scrollbar-track-transparent">
              {Array.from({ length: 37 }).map((_, i) => {
                const hour = Math.floor(i / 2) + 5;
                const min = i % 2 === 0 ? '00' : '30';
                const timeString = `${hour.toString().padStart(2, '0')}:${min}`;
                return (
                  <div 
                    key={timeString} 
                    onClick={() => { setSelectedTime(timeString); setActiveDropdown(null); }}
                    className={`p-2.5 rounded-lg cursor-pointer transition-colors text-center ${selectedTime === timeString ? 'bg-primary-container text-on-primary-container font-bold' : 'hover:bg-surface-container-low text-on-surface'}`}
                  >
                    {timeString}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* 3. Số người */}
      <div className="relative">
        <div onClick={() => toggleDropdown('guests')} className="flex items-center gap-3 px-5 py-2.5 rounded-full hover:bg-surface-container-low transition-colors cursor-pointer group">
          <span className="material-symbols-outlined text-secondary text-[22px] group-hover:scale-110 transition-transform">group</span>
          <div className="flex flex-col min-w-0 text-left">
            <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Số lượng khách</span>
            <span className="font-label-lg text-label-lg text-on-surface truncate">
              {guests.adults + guests.children} khách
            </span>
          </div>
        </div>
        {activeDropdown === 'guests' && (
          <div className="absolute top-full left-0 mt-4 w-72 bg-surface-container-lowest rounded-2xl p-4 shadow-xl border border-surface-container animate-fade-in-up z-50">
            <h4 className="font-label-md text-label-md text-on-surface-variant mb-4">Số lượng khách</h4>
            
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="font-label-md text-on-surface">Người lớn</div>
                <div className="font-body-sm text-outline">Từ 13 tuổi trở lên</div>
              </div>
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setGuests({...guests, adults: Math.max(1, guests.adults - 1)})}
                  className="w-8 h-8 rounded-full border border-outline flex items-center justify-center text-outline hover:border-primary hover:text-primary"
                >-</button>
                <span className="w-4 text-center font-label-md">{guests.adults}</span>
                <button 
                  onClick={() => setGuests({...guests, adults: guests.adults + 1})}
                  className="w-8 h-8 rounded-full border border-outline flex items-center justify-center text-outline hover:border-primary hover:text-primary"
                >+</button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <div className="font-label-md text-on-surface">Trẻ em</div>
                <div className="font-body-sm text-outline">Dưới 13 tuổi</div>
              </div>
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setGuests({...guests, children: Math.max(0, guests.children - 1)})}
                  className="w-8 h-8 rounded-full border border-outline flex items-center justify-center text-outline hover:border-primary hover:text-primary"
                >-</button>
                <span className="w-4 text-center font-label-md">{guests.children}</span>
                <button 
                  onClick={() => setGuests({...guests, children: guests.children + 1})}
                  className="w-8 h-8 rounded-full border border-outline flex items-center justify-center text-outline hover:border-primary hover:text-primary"
                >+</button>
              </div>
            </div>

          </div>
        )}
      </div>

      {/* 4. CTA Button */}
      <div className="p-1 flex justify-end">
        <button onClick={() => navigate('/search')} className="w-full md:w-auto px-8 py-3.5 rounded-full bg-secondary text-white font-label-lg text-label-lg shadow-[0_8px_20px_rgba(0,104,116,0.3)] hover:scale-[1.02] active:scale-95 transition-all flex items-center justify-center gap-2" type="button">
          <span className="material-symbols-outlined text-[20px]">search</span>
          <span>Tìm trải nghiệm</span>
        </button>
      </div>
      
    </div>
  </div>
</div>
</div>
</section>
{/* SECTION 2: CATEGORY ROW */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl">
<div className="flex items-center justify-between gap-4 overflow-x-auto pb-2 scrollbar-none">
{CATEGORIES.map(cat => (
  <button 
    key={cat.id} 
    onClick={() => setActiveCategory(activeCategory === cat.id ? null : cat.id)}
    className={`flex items-center gap-3 px-6 py-3 rounded-full font-label-lg text-label-lg flex-shrink-0 transition-all ${activeCategory === cat.id ? 'bg-secondary text-on-secondary shadow-sm hover:scale-[1.03]' : 'bg-surface-container-low hover:bg-surface-container text-on-surface-variant hover:text-on-surface'}`} 
    type="button"
  >
    <span className={`material-symbols-outlined text-[20px] ${activeCategory === cat.id ? '' : 'text-secondary'}`}>{cat.icon}</span>
    <span>{cat.name}</span>
  </button>
))}

</div>
</section>
{/* SECTION 3: FEATURED EXPERIENCES (4-COLUMN DESKTOP GRID) */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl" id="featured">
{/* Header Block */}
<div className="flex flex-col md:flex-row md:items-end justify-between mb-space-xl gap-4">
<div>
<div className="inline-block font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold mb-1">Hoạt động được yêu thích</div>
<h2 className="font-headline-lg text-headline-lg text-on-surface flex items-center gap-3">
  Trải nghiệm nổi bật {activeRegionFilter ? `tại ${activeRegionFilter}` : 'tại Đà Nẵng'}
  {activeRegionFilter && (
    <button onClick={() => setActiveRegionFilter(null)} className="font-label-sm text-label-sm text-secondary bg-surface-container-low hover:bg-surface-container px-3 py-1 rounded-full transition-colors flex items-center gap-1">
      <span className="material-symbols-outlined text-[14px]">close</span> Bỏ lọc
    </button>
  )}
</h2>
<p className="font-body-md text-body-md text-on-surface-variant mt-1">Những hoạt động biển tuyển chọn với tiêu chuẩn an toàn và trải nghiệm tốt nhất</p>
</div>
<div className="flex items-center gap-2">
<button aria-label="Trước" className="w-10 h-10 rounded-full bg-surface-container-low hover:bg-surface-container text-on-surface flex items-center justify-center transition-colors" type="button">
<span className="material-symbols-outlined text-[20px]">chevron_left</span>
</button>
<button aria-label="Sau" className="w-10 h-10 rounded-full bg-secondary text-on-secondary flex items-center justify-center shadow-sm hover:bg-secondary/90 transition-colors" type="button">
<span className="material-symbols-outlined text-[20px]">chevron_right</span>
</button>
</div>
</div>
{/* 4 Cards Row */}
<div key={`${activeRegionFilter}-${activeCategory}`} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-space-lg animate-fade-in-up">
{displayTours.map(tour => (
      <Link to={`/tour/${tour.id}`} key={tour.id} className="group bg-surface-container-lowest rounded-2xl overflow-hidden shadow-[0_4px_20px_-2px_rgba(16,47,58,0.06)] hover:shadow-[0_12px_32px_-4px_rgba(16,47,58,0.12)] hover:-translate-y-1 transition-all flex flex-col">
        <div className="relative aspect-[4/3] w-full overflow-hidden bg-surface-container-high">
          <img className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" src={(MOCK_IMAGES.find(img => img.serviceId === tour.id && img.isPrimary)?.imageUrl || "https://via.placeholder.com/400")} alt={tour.name} />
          {(tour.status === "ACTIVE") && (
            <div className="absolute top-3 left-3 px-3 py-1 rounded-full bg-surface-container-lowest/90 backdrop-blur-md text-secondary font-label-sm text-label-sm flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px] text-secondary">verified</span>
              Nhà cung cấp đã xác minh
            </div>
          )}
          {(() => {
            const isSaved = isInWishlist(tour.id);
            return (
              <button
                aria-label={isSaved ? "Bỏ lưu khỏi yêu thích" : "Lưu vào yêu thích"}
                className={`absolute top-3 right-3 w-8 h-8 rounded-full backdrop-blur-md flex items-center justify-center transition-all shadow-md cursor-pointer hover:scale-110 active:scale-95 ${
                  isSaved
                    ? "bg-white text-red-500 ring-2 ring-red-100"
                    : "bg-surface-container-lowest/90 text-on-surface-variant hover:text-red-500"
                }`}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  toggleWishlist(tour.id, tour.name);
                }}
              >
                <span
                  className={`material-symbols-outlined text-[18px] transition-transform ${isSaved ? "text-red-500" : ""}`}
                  style={{ fontVariationSettings: isSaved ? "'FILL' 1" : "'FILL' 0" }}
                >
                  favorite
                </span>
              </button>
            );
          })()}
          <div className="absolute bottom-3 left-3 px-2.5 py-0.5 rounded-full bg-black/60 backdrop-blur-sm text-white font-label-sm text-label-sm flex items-center gap-1">
            <span className={`material-symbols-outlined text-[13px] ${"text-primary-container"}`}>{"local_fire_department"}</span>
            {"Best Seller"}
          </div>
        </div>
        <div className="p-space-md flex-1 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-1.5 text-on-surface-variant font-body-sm text-body-sm mb-1">
              <span className="material-symbols-outlined text-[16px] text-secondary">pin_drop</span>
              <span>{tour.locationName} • {(tour.durationMinutes + " phút")}</span>
            </div>
            <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-secondary transition-colors line-clamp-1">
              {tour.name}
            </h3>
            <div className="flex items-center gap-2 mt-2">
              <span className="inline-flex items-center gap-1 text-primary font-label-md text-label-md">
                <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                {4.9}
              </span>
              <span className="font-body-sm text-body-sm text-on-surface-variant">({128} đánh giá)</span>
            </div>
          </div>
          <div className="mt-space-md pt-3 flex items-center justify-between border-t-0 bg-surface-container-low -mx-space-md -mb-space-md px-space-md py-3">
            <div>
              <span className="font-label-sm text-label-sm text-outline block">Giá trọn gói</span>
              <span className="font-headline-sm text-headline-sm text-primary font-bold">
                {tour.basePrice.toLocaleString('vi-VN')} đ
              </span>
              <span className="font-body-sm text-body-sm text-on-surface-variant">/người</span>
            </div>
            <button className="px-4 py-2 rounded-full bg-secondary text-white font-label-md text-label-md hover:bg-secondary/90 transition-colors shadow-sm" type="button" onClick={(e) => e.preventDefault()}>
              Xem chi tiết
            </button>
          </div>
        </div>
      </Link>
    ))}
</div>
{/* Micro Disclaimer */}
<div className="mt-space-md text-center">
<span className="font-body-sm text-body-sm text-outline">
        * Dữ liệu giá và đánh giá minh họa cho phiên bản thử nghiệm giao diện DANASEA.
      </span>
</div>
</section>
{/* SECTION 4: EDITORIAL DESTINATION SPLIT */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-2xl">
<div className="mb-space-lg text-left">
<span className="font-label-sm text-label-sm text-primary font-bold uppercase tracking-widest">Bản đồ điểm đến</span>
<h2 className="font-headline-lg text-headline-lg text-on-surface mt-1">Khám phá Đà Nẵng theo vùng biển</h2>
<p className="font-body-md text-body-md text-on-surface-variant mt-1">Mỗi bãi biển sở hữu đặc tính thủy triều và cảm xúc phiêu lưu rất riêng</p>
</div>
<div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg">
{/* Left Heroic Destination: My Khe (7 cols) */}
<div className="lg:col-span-7 rounded-3xl overflow-hidden relative group min-h-[460px] shadow-sm flex flex-col justify-end p-space-xl">
<div className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105" data-alt="Wide perspective of My Khe Beach Da Nang with pristine fine white sand, gentle rhythmic waves rolling in, coconut palms, luxury beach line under clear blue sky" style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuA5l7HPI_DbDBQ8gWwNpvlydXCJ02X4PVntLdF3OBottnR7rESgxkT6NkNzhWlLZIHdtTzgzMhEqfkYW49v3BY2V5D5tHhgEuQAi3mVrq9w-gArbIE3H_P9pGX5UU1aBlvwVB8c6c7wUIePx9IqK3YKgrd-6m-1bPy50PzZLR9e5KYLgI_EgMVyDMZNHy_r2c0SdkTu5Se27hkh3ZD4Czlk03IbWHb5_dkh1M8LK07Ik_bw4Dph_4mefQ')" }}></div>
<div className="absolute inset-0 bg-gradient-to-t from-[#0a1f26] via-[#0a1f26]/40 to-transparent"></div>
<div className="relative z-10 text-white">
<span className="px-3.5 py-1 rounded-full bg-white/20 backdrop-blur-md text-secondary-fixed font-label-sm text-label-sm uppercase tracking-wider mb-space-xs inline-block">
            Bãi biển đô thị hàng đầu
          </span>
<h3 className="font-headline-md text-headline-md font-bold mb-2">Bãi biển Mỹ Khê</h3>
<p className="font-body-md text-body-md text-surface-variant/90 max-w-md mb-space-md leading-relaxed">
            Bờ cát vàng mịn trải dài thoai thoải, mặt nước phẳng lặng buổi sớm lý tưởng cho đón ánh bình minh đầu tiên trên ván chèo SUP.
          </p>
<div className="flex items-center justify-between max-w-md">
<span className="font-label-md text-label-md text-secondary-fixed">14 hoạt động đang mở</span>
<a onClick={(e) => handleExploreRegion(e, 'Mỹ Khê')} className="inline-flex items-center gap-2 font-label-lg text-label-lg text-white group-hover:text-secondary-fixed transition-colors cursor-pointer" href="/">
<span>Khám phá vùng biển</span>
<span className="material-symbols-outlined text-[18px]">arrow_forward</span>
</a>
</div>
</div>
</div>
{/* Right 2 Stacked Cards: Son Tra & Non Nuoc (5 cols) */}
<div className="lg:col-span-5 flex flex-col gap-space-lg">
{/* Son Tra */}
<div className="rounded-3xl overflow-hidden relative group h-[220px] shadow-sm flex flex-col justify-end p-space-lg">
<div className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105" data-alt="Pristine coastal rocks of Son Tra peninsula with azure ocean waters hitting ancient mossy stones, wild tropical greenery framing the bay" style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuCiCinGFFDq1b2aeeARfPVNcgxJgHPdWE9E76EUUhfRGrwK0SCa99hMmNS2u1FTDisuWaW71Z2Kp4xm5Aq1wImxgCGjDR3Jnua7z1N46VgNLxijv4rujwSYudPKQNz2May1ZeHWs_-GIr2UUY1ALZl3lm9riuphJpcK_Wr1224VOcY0Ye857Ssegu0I7w8BP14sSTB2k2aPxBV_eqgWXOEvuyq761zw3TX3hf-lYg6Isj8HhzAMTtzbRw')" }}></div>
<div className="absolute inset-0 bg-gradient-to-t from-[#001f24] via-[#001f24]/50 to-transparent"></div>
<div className="relative z-10 text-white">
<div className="flex items-center justify-between mb-1">
<h3 className="font-headline-sm text-headline-sm font-bold">Bán đảo Sơn Trà</h3>
<span className="px-2.5 py-0.5 rounded-full bg-white/20 backdrop-blur-sm text-secondary-fixed font-label-sm text-label-sm">9 hoạt động</span>
</div>
<p className="font-body-sm text-body-sm text-surface-variant/90 line-clamp-2 mb-2">
              Vách đá hoang sơ kì vĩ, làn nước ngọc bích cùng những rạn san hô tự nhiên rực rỡ dưới đáy biển.
            </p>
<a onClick={(e) => handleExploreRegion(e, 'Bán đảo Sơn Trà')} className="inline-flex items-center gap-1 font-label-md text-label-md text-secondary-fixed hover:underline cursor-pointer" href="/">
              Khám phá vùng biển →
            </a>
</div>
</div>
{/* Non Nuoc */}
<div className="rounded-3xl overflow-hidden relative group h-[220px] shadow-sm flex flex-col justify-end p-space-lg">
<div className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105" data-alt="Peaceful wide beach of Non Nuoc Da Nang near Marble Mountains with soft rolling waves, beach umbrellas, clean sand in afternoon warm light" style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuAWikUk3u0Fx_6OcE-c9MEtKxAeMexu0wXSQ9gipYr2ehc5uoitY27VJ7uptBz0I-Cn77uuWW0suwxErMe2mNpgRV2mKzNoA54FYC2g5UUMQcK391No6PnAAXLF6VKcjvJMD28IfX9MvogcFIJCCkMrHbIY47lqT3F_avlYlJqLzUL3h2NDKuoeaZoBkOb_Qin9GDgAiF4BZmkSHcFmsCVKOxkMlKsqJd3uBgNE0WYEoOh_8qODy4yCzA')" }}></div>
<div className="absolute inset-0 bg-gradient-to-t from-[#001f24] via-[#001f24]/50 to-transparent"></div>
<div className="relative z-10 text-white">
<div className="flex items-center justify-between mb-1">
<h3 className="font-headline-sm text-headline-sm font-bold">Bãi biển Non Nước</h3>
<span className="px-2.5 py-0.5 rounded-full bg-white/20 backdrop-blur-sm text-secondary-fixed font-label-sm text-label-sm">6 hoạt động</span>
</div>
<p className="font-body-sm text-body-sm text-surface-variant/90 line-clamp-2 mb-2">
              Sóng êm, bờ rộng thoáng đãng, điểm dừng chân hoàn hảo cho gia đình và nhóm bạn tìm kiếm sự an yên.
            </p>
<a onClick={(e) => handleExploreRegion(e, 'Biển Non Nước')} className="inline-flex items-center gap-1 font-label-md text-label-md text-secondary-fixed hover:underline cursor-pointer" href="/">
              Khám phá vùng biển →
            </a>
</div>
</div>
</div>
</div>
</section>
{/* SECTION 5: DANASEA SMART CONCIERGE (AI ASSISTANT DUAL SECTION) */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl" id="ai-concierge">
<div className="bg-[#F0F6F7] rounded-[32px] p-space-xl lg:p-space-2xl shadow-[0_8px_30px_rgba(0,104,116,0.06)] relative overflow-hidden">
{/* Decorative subtle aquatic glow circle */}
<div className="absolute -right-20 -bottom-20 w-96 h-96 rounded-full bg-secondary-fixed-dim/30 blur-3xl pointer-events-none"></div>
<div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-center relative z-10">
{/* Left Prompt Column (7 cols) */}
<div className="lg:col-span-7 flex flex-col text-left">
<div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white shadow-sm text-secondary font-label-md text-label-md mb-space-md w-max">
<span className="material-symbols-outlined text-[18px] text-primary-container">auto_awesome</span>
            Trợ lý lịch trình thông minh DANASEA AI
          </div>
<h2 className="font-display-hero text-[34px] lg:text-[42px] leading-tight text-on-surface mb-space-md">
            Một chuyến đi hợp gu,<br />
<span className="text-secondary">bắt đầu từ bạn.</span>
</h2>
<p className="font-body-lg text-body-lg text-on-surface-variant max-w-xl mb-space-lg leading-relaxed">
            Hệ thống phân tích thời gian thủy triều, hướng sóng và sở thích nhóm bạn để gợi ý khung giờ chèo SUP hay lặn ngắm san hô đẹp nhất trong ngày.
          </p>
{/* Quick Prompts Chips */}
<div className="flex flex-col gap-2.5 mb-space-xl">
<span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Chạm nhanh gợi ý lịch trình:</span>
<div className="flex flex-wrap gap-2">
<button onClick={() => { setActiveCustomPrompt('group'); setChatHistory([]); }} className={`px-4 py-2.5 rounded-full text-on-surface font-label-md text-label-md shadow-sm transition-all hover:scale-[1.02] flex items-center gap-2 ${activeCustomPrompt === 'group' ? 'bg-secondary text-white' : 'bg-surface-container-lowest hover:bg-white'}`} type="button">
<span className={`material-symbols-outlined text-[16px] ${activeCustomPrompt === 'group' ? 'text-white' : 'text-secondary'}`}>groups</span>
                Đi biển nửa ngày cùng nhóm bạn
              </button>
<button onClick={() => { setActiveCustomPrompt('couple'); setChatHistory([]); }} className={`px-4 py-2.5 rounded-full text-on-surface font-label-md text-label-md shadow-sm transition-all hover:scale-[1.02] flex items-center gap-2 ${activeCustomPrompt === 'couple' ? 'bg-primary-container text-white' : 'bg-surface-container-lowest hover:bg-white'}`} type="button">
<span className={`material-symbols-outlined text-[16px] ${activeCustomPrompt === 'couple' ? 'text-white' : 'text-primary'}`}>favorite</span>
                Tìm trải nghiệm nhẹ nhàng cho hai người
              </button>
<button onClick={() => { setActiveCustomPrompt('weather'); setChatHistory([]); }} className={`px-4 py-2.5 rounded-full text-on-surface font-label-md text-label-md shadow-sm transition-all hover:scale-[1.02] flex items-center gap-2 ${activeCustomPrompt === 'weather' ? 'bg-tertiary-container text-white' : 'bg-surface-container-lowest hover:bg-white'}`} type="button">
<span className={`material-symbols-outlined text-[16px] ${activeCustomPrompt === 'weather' ? 'text-white' : 'text-secondary'}`}>wb_sunny</span>
                Gợi ý hoạt động theo thời tiết ngày mai
              </button>
</div>
</div>

</div>
{/* Right Realistic Chat Preview (5 cols) */}
<div className="lg:col-span-5">
<div className="bg-surface-container-lowest rounded-2xl p-space-md shadow-[0_12px_32px_rgba(16,47,58,0.08)]">
{/* Chat Head */}
<div className="flex items-center justify-between pb-3 border-b-0 mb-3 bg-surface-container-low -mx-space-md -mt-space-md px-space-md pt-3 rounded-t-2xl">
<div className="flex items-center gap-2.5">
<div className="w-8 h-8 rounded-full bg-secondary text-white flex items-center justify-center">
<span className="material-symbols-outlined text-[18px]">waves</span>
</div>
<div>
<h4 className="font-label-lg text-label-lg text-on-surface leading-none">DANASEA Assistant</h4>
<span className="font-body-sm text-[11px] text-secondary flex items-center gap-1">
<span className="w-1.5 h-1.5 rounded-full bg-secondary animate-ping"></span>
                    Sẵn sàng tư vấn theo thời gian thực
                  </span>
</div>
</div>
<span className="font-label-sm text-label-sm text-outline">05:45 AM</span>
</div>
{/* Chat Dialog Messages */}
<div ref={chatContainerRef} className="flex flex-col gap-5 h-[340px] overflow-y-auto pr-2 custom-scrollbar relative">
  {displayMessages.map((msg, idx) => (
    <div key={`${activeCustomPrompt || activeAIBeach}-${idx}`} className="flex flex-col gap-3 animate-fade-in-up">
      {/* User Message */}
      <div className="flex justify-end">
        <div className="bg-secondary text-white rounded-2xl rounded-tr-none px-4 py-2.5 max-w-[85%] font-body-sm text-body-sm shadow-sm">
          {msg.chatPrompt}
        </div>
      </div>
      {/* AI Reply */}
      <div className="flex justify-start items-start gap-2">
        <div className="w-6 h-6 rounded-full bg-primary-container text-white flex items-center justify-center flex-shrink-0 mt-1">
          <span className="material-symbols-outlined text-[13px]">auto_awesome</span>
        </div>
        <div className="bg-surface-container-low text-on-surface rounded-2xl rounded-tl-none p-3 max-w-[90%] font-body-sm text-body-sm flex flex-col gap-2 shadow-sm">
          <p className="whitespace-pre-wrap">{msg.chatReply || 'Đang phân tích dữ liệu biển...'}</p>
          
          {/* Mini Interactive Activity Suggestion Chip Inside Chat */}
          {msg.chatTourName && (
            <>
              <div className="bg-surface-container-lowest p-2.5 rounded-xl flex items-center gap-3 shadow-xs mt-1">
                <div className="w-12 h-12 rounded-lg bg-surface-container overflow-hidden flex-shrink-0">
                  <img className="w-full h-full object-cover" src={msg.chatImage} />
                </div>
                <div className="flex-1 min-w-0">
                  <span className="font-label-md text-label-md text-on-surface truncate block font-bold">{msg.chatTourName}</span>
                  <span className="font-body-sm text-[12px] text-primary font-bold">280.000 đ/người • Còn 6 chỗ</span>
                </div>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <button className="px-3 py-1 rounded-full bg-secondary text-white font-label-sm text-label-sm" type="button">
                  Chọn khung 05:30
                </button>
                <button className="px-3 py-1 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm" type="button">
                  Xem lịch khác
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  ))}
</div>
{/* Chat Bottom Input Bar Preview */}
<div className="mt-4 pt-3 border-t-0 flex items-center gap-2">
<div className="flex-1 bg-surface-container-low rounded-full px-3.5 py-1 text-on-surface font-body-sm text-body-sm flex items-center gap-2 border border-transparent focus-within:border-secondary transition-colors">
<span className="material-symbols-outlined text-[18px] text-outline">chat</span>
<input 
  type="text" 
  value={chatInputValue}
  onChange={(e) => setChatInputValue(e.target.value)}
  onKeyDown={(e) => e.key === 'Enter' && handleChatSubmit()}
  placeholder="Hỏi bất kỳ điều gì về biển Đà Nẵng..."
  className="bg-transparent border-none outline-none flex-1 py-1 text-on-surface placeholder:text-outline"
  disabled={isChatLoading}
/>
</div>
<button onClick={handleChatSubmit} disabled={isChatLoading || !chatInputValue.trim()} className={`w-8 h-8 rounded-full text-white flex items-center justify-center flex-shrink-0 transition-colors ${chatInputValue.trim() && !isChatLoading ? 'bg-secondary hover:bg-secondary/90' : 'bg-surface-container-high text-outline'}`} type="button">
{isChatLoading ? (
  <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
) : (
  <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
)}
</button>
</div>
</div>
</div>
</div>
</div>
</section>
{/* SECTION 6: REALTIME OCEAN WEATHER & CONDITIONS */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl">
<div className="bg-surface-container-lowest rounded-3xl p-space-xl shadow-[0_4px_24px_rgba(16,47,58,0.05)]">
<div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-space-lg">
<div>
<div className="flex items-center gap-2 mb-1">
<span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
<span className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">Trạm quan trắc trực tuyến</span>
</div>
<h2 className="font-headline-md text-headline-md text-on-surface">Thông tin thời tiết &amp; điều kiện biển</h2>
</div>
{/* Region Selector Tabs */}
<div className="inline-flex p-1 rounded-full bg-surface-container-low">
{['Mỹ Khê', 'Sơn Trà', 'Non Nước'].map(beach => (
  <button 
    key={beach}
    onClick={() => { setActiveAIBeach(beach); setActiveCustomPrompt(null); setChatHistory([]); }}
    className={`px-5 py-1.5 rounded-full font-label-md text-label-md transition-all ${activeAIBeach === beach ? 'bg-surface-container-lowest text-secondary shadow-xs' : 'text-on-surface-variant hover:text-on-surface'}`} 
    type="button">
    {beach === 'Mỹ Khê' ? 'Bãi biển Mỹ Khê' : beach === 'Sơn Trà' ? 'Bán đảo Sơn Trà' : 'Bãi Non Nước'}
  </button>
))}
</div>
</div>
{/* Live Condition Metric Cards */}
<div key={activeAIBeach} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-space-md animate-fade-in-up">
{/* Metric 1: Temp */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col justify-between">
<div className="flex items-center justify-between text-secondary">
<span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Nhiệt độ không khí</span>
<span className="material-symbols-outlined text-[24px]">thermostat</span>
</div>
<div className="my-2">
<span className="font-headline-lg text-headline-lg text-on-surface font-bold">{aiWeatherData.temp.val}</span>
<span className="font-body-sm text-body-sm text-on-surface-variant block">{aiWeatherData.temp.desc}</span>
</div>
<div className="w-full bg-surface-container-highest rounded-full h-1.5 overflow-hidden">
<div className="bg-primary-container h-full rounded-full transition-all duration-1000" style={{ width: aiWeatherData.temp.percent }}></div>
</div>
</div>
{/* Metric 2: Wave swell */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col justify-between">
<div className="flex items-center justify-between text-secondary">
<span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Chiều cao sóng</span>
<span className="material-symbols-outlined text-[24px]">waves</span>
</div>
<div className="my-2">
<span className="font-headline-lg text-headline-lg text-on-surface font-bold">{aiWeatherData.wave.val}</span>
<span className="font-body-sm text-body-sm text-secondary font-semibold block">{aiWeatherData.wave.desc}</span>
</div>
<div className="w-full bg-surface-container-highest rounded-full h-1.5 overflow-hidden">
<div className="bg-secondary h-full rounded-full transition-all duration-1000" style={{ width: aiWeatherData.wave.percent }}></div>
</div>
</div>
{/* Metric 3: Underwater Visibility */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col justify-between">
<div className="flex items-center justify-between text-secondary">
<span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Tầm nhìn dưới nước</span>
<span className="material-symbols-outlined text-[24px]">visibility</span>
</div>
<div className="my-2">
<span className="font-headline-lg text-headline-lg text-on-surface font-bold">{aiWeatherData.visibility.val}</span>
<span className="font-body-sm text-body-sm text-secondary font-semibold block">{aiWeatherData.visibility.desc}</span>
</div>
<div className="w-full bg-surface-container-highest rounded-full h-1.5 overflow-hidden">
<div className="bg-secondary h-full rounded-full transition-all duration-1000" style={{ width: aiWeatherData.visibility.percent }}></div>
</div>
</div>
{/* Metric 4: Wind Speed */}
<div className="p-space-md rounded-2xl bg-surface-container-low flex flex-col justify-between">
<div className="flex items-center justify-between text-secondary">
<span className="font-label-sm text-label-sm text-outline uppercase tracking-wider">Sức gió &amp; Hướng gió</span>
<span className="material-symbols-outlined text-[24px]">air</span>
</div>
<div className="my-2">
<span className="font-headline-lg text-headline-lg text-on-surface font-bold">{aiWeatherData.wind.val}</span>
<span className="font-body-sm text-body-sm text-on-surface-variant block">{aiWeatherData.wind.desc}</span>
</div>
<div className="w-full bg-surface-container-highest rounded-full h-1.5 overflow-hidden">
<div className="bg-tertiary-container h-full rounded-full transition-all duration-1000" style={{ width: aiWeatherData.wind.percent }}></div>
</div>
</div>
</div>
{/* Recommendation Banner */}
<div key={`banner-${activeAIBeach}`} className="mt-space-md p-4 rounded-2xl bg-[#E6F4F6] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-fade-in-up">
<div className="flex items-center gap-3">
<div className="w-10 h-10 rounded-full bg-secondary text-white flex items-center justify-center flex-shrink-0">
<span className="material-symbols-outlined text-[20px]">check_circle</span>
</div>
<div>
<span className="font-label-lg text-label-lg text-secondary block font-bold">Đánh giá hoạt động hôm nay: {aiWeatherData.recommendationTitle}</span>
<span className="font-body-sm text-[12px] text-tertiary">{aiWeatherData.recommendationDesc}</span>
</div>
</div>
<div className="font-body-sm text-[11px] text-outline self-end sm:self-center">
          Cập nhật lúc 06:00 sáng nay • Dữ liệu mô phỏng cho prototype giao diện
        </div>
</div>
</div>
</section>
{/* SECTION 7: 3-STEP EASY BOOKING */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-3xl">
<div className="text-center max-w-2xl mx-auto mb-space-2xl">
<span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">Trải nghiệm thuận tiện</span>
<h2 className="font-headline-lg text-headline-lg text-on-surface mt-1">Cách đặt trải nghiệm cùng DANASEA</h2>
<p className="font-body-md text-body-md text-on-surface-variant mt-2">Đơn giản, minh bạch và hoàn toàn bảo đảm quyền lợi với sự đồng hành của đội ngũ bản địa.</p>
</div>
<div className="grid grid-cols-1 md:grid-cols-3 gap-space-xl relative">
{/* Step 1 */}
<div className="p-space-xl rounded-3xl bg-surface-container flex flex-col items-start relative">
<span className="font-display-hero text-[64px] text-secondary/30 font-black leading-none mb-2 select-none">01</span>
<div className="w-12 h-12 rounded-2xl bg-secondary text-white flex items-center justify-center mb-space-md shadow-sm">
<span className="material-symbols-outlined text-[24px]">explore</span>
</div>
<h3 className="font-headline-sm text-headline-sm text-on-surface mb-2 font-bold">Khám phá &amp; Chọn hoạt động</h3>
<p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
          Tìm theo vùng biển yêu thích, lọc theo cấp độ thể lực hoặc tham khảo lịch trình được gợi ý tự động từ AI.
        </p>
</div>
{/* Step 2 */}
<div className="p-space-xl rounded-3xl bg-surface-container flex flex-col items-start relative">
<span className="font-display-hero text-[64px] text-secondary/30 font-black leading-none mb-2 select-none">02</span>
<div className="w-12 h-12 rounded-2xl bg-primary-container text-white flex items-center justify-center mb-space-md shadow-sm">
<span className="material-symbols-outlined text-[24px]">event_available</span>
</div>
<h3 className="font-headline-sm text-headline-sm text-on-surface mb-2 font-bold">Đặt lịch &amp; Thanh toán an toàn</h3>
<p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
          Chọn ngày, khung giờ sóng đẹp và số lượng khách. Xác nhận giữ chỗ tức thời qua các cổng bảo mật.
        </p>
</div>
{/* Step 3 */}
<div className="p-space-xl rounded-3xl bg-surface-container flex flex-col items-start relative">
<span className="font-display-hero text-[64px] text-secondary/30 font-black leading-none mb-2 select-none">03</span>
<div className="w-12 h-12 rounded-2xl bg-tertiary text-white flex items-center justify-center mb-space-md shadow-sm">
<span className="material-symbols-outlined text-[24px]">qr_code_2</span>
</div>
<h3 className="font-headline-sm text-headline-sm text-on-surface mb-2 font-bold">Nhận vé QR &amp; Tận hưởng biển</h3>
<p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
          Vé điện tử gửi thẳng qua ứng dụng và Zalo. Quét mã check-in trực tiếp tại bến tập kết và bắt đầu hành trình.
        </p>
</div>
</div>
</section>
{/* SECTION 8: RECENTLY VIEWED ACTIVITIES (HISTORY) */}
<section className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl mb-space-2xl">
<div className="flex items-center justify-between mb-space-lg">
<div className="flex items-center gap-2">
<span className="material-symbols-outlined text-secondary text-[22px]">history</span>
<h2 className="font-headline-md text-headline-md text-on-surface">Trải nghiệm bạn vừa xem</h2>
</div>
<a className="font-label-md text-label-md text-secondary hover:underline" href="/" onClick={(e) => { e.preventDefault(); }}>Xóa lịch sử xem</a>
</div>
<div className="grid grid-cols-1 md:grid-cols-3 gap-space-lg">
{/* Mini Card 1 */}
<Link to="/tour/1" className="p-3 bg-surface-container-lowest rounded-2xl shadow-sm flex items-center gap-4 hover:shadow-md hover:-translate-y-1 transition-all cursor-pointer">
<div className="w-24 h-24 rounded-xl overflow-hidden bg-surface-container flex-shrink-0">
<img className="w-full h-full object-cover" data-alt="Overhead shot of SUP surfboard paddling at dawn in turquoise blue waters" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAUQYcWnZo3_Kb31_qfroEFXgTerymIKQCOpVaTesW0pBt3yg2CiNGXu2vGlRckKERopIXFerqe-1sAv_OkI5zmzYO_iK7aXukpJ89Oyny7Z8VECdpaEufZqmZucGaCmzQZTiCLwcwXH1COA5JTI42EQeyE0vnRTJvI9enpzD8cKsC5HsI2Cv6MuYVgbn52pZbXVgeex4h-_Vwb6-ZS20BNeUb30ntwL8mB9p5n-EX-6dvp2hzW-l9l7g" />
</div>
<div className="flex-1 min-w-0">
<span className="font-label-sm text-label-sm text-outline block">Mỹ Khê • 2 giờ</span>
<h4 className="font-label-lg text-label-lg text-on-surface truncate font-bold mt-0.5">Chèo SUP đón bình minh</h4>
<span className="font-label-lg text-label-lg text-primary font-bold block mt-1">280.000 đ</span>
<button className="mt-2 text-[12px] font-label-md text-secondary hover:underline font-bold flex items-center gap-1" type="button">
            Đặt lại ngay
            <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
</button>
</div>
</Link>
{/* Mini Card 2 */}
<Link to="/tour/1" className="p-3 bg-surface-container-lowest rounded-2xl shadow-sm flex items-center gap-4 hover:shadow-md hover:-translate-y-1 transition-all cursor-pointer">
<div className="w-24 h-24 rounded-xl overflow-hidden bg-surface-container flex-shrink-0">
<img className="w-full h-full object-cover" data-alt="Speedboat cruising across the bay near coastal cliffs" src="https://lh3.googleusercontent.com/aida-public/AB6AXuD5nmrt_9U0tcvvajAYCF3dMpfZZaJnSNnHT11tVSuhx0cwdhpWYt43LeKGcZqFLS4u5yEKePSYOKj4HN_FWgmqmGGpJGPs9fmPOS99IAlQgJIRYV3GqgO6WnqEc3-Y5HqeNa9GoLKSArL3h08851KK6UOdNy48VvU8rxAhkf70b8bJR-eL0PV_mJ8S-ocMfY0mV03iWeUbZN18JgWL7RJevmsMiCure9fIRVajRJ15V49PC7-Hn_aotw" />
</div>
<div className="flex-1 min-w-0">
<span className="font-label-sm text-label-sm text-outline block">Sơn Trà • 3.5 giờ</span>
<h4 className="font-label-lg text-label-lg text-on-surface truncate font-bold mt-0.5">Khám phá biển bằng cano</h4>
<span className="font-label-lg text-label-lg text-primary font-bold block mt-1">450.000 đ</span>
<button className="mt-2 text-[12px] font-label-md text-secondary hover:underline font-bold flex items-center gap-1" type="button">
            Đặt lại ngay
            <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
</button>
</div>
</Link>
{/* Mini Card 3 */}
<Link to="/tour/1" className="p-3 bg-surface-container-lowest rounded-2xl shadow-sm flex items-center gap-4 hover:shadow-md hover:-translate-y-1 transition-all cursor-pointer">
<div className="w-24 h-24 rounded-xl overflow-hidden bg-surface-container flex-shrink-0">
<img className="w-full h-full object-cover" data-alt="Underwater coral reef with tropical blue and yellow fish" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBZTxAVg6AqtTrMtvnuKot_Clv9n0P7eEgwVGzgb19OmzpF-MggEaLqSFy6_t-FFBtHsqrMF6CRV8n1seIdwURlsk7blYBpD2Uvd6Ics4e5NkLLIv56QBwxKX6U5pqRT-PJ6vCRrUjEM4EZ2aNHmU5W7vq52ttJRUMFmjuX7TGeinu52WsNh6TSLu1EGtIR5RBt2SNedmSuuL8ya3I2wdG70BTfTCERY5RBhGSmscP_kPQA629pXRVVuQ" />
</div>
<div className="flex-1 min-w-0">
<span className="font-label-sm text-label-sm text-outline block">Bán đảo Sơn Trà • 2.5 giờ</span>
<h4 className="font-label-lg text-label-lg text-on-surface truncate font-bold mt-0.5">Lặn biển ngắm san hô</h4>
<span className="font-label-lg text-label-lg text-primary font-bold block mt-1">520.000 đ</span>
<button className="mt-2 text-[12px] font-label-md text-secondary hover:underline font-bold flex items-center gap-1" type="button">
            Đặt lại ngay
            <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
</button>
</div>
</Link>
</div>
</section>
</div></main>
  );
}
