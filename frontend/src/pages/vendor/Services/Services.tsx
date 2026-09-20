import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';

interface VendorServiceItem {
  id: string;
  code: string;
  name: string;
  nameEn: string;
  category: string;
  categoryKey: 'sup' | 'diving' | 'boat' | 'extreme';
  price: number;
  duration: number; // minutes
  capacityPerSlot: number;
  rating: number;
  reviewCount: number;
  status: 'ACTIVE' | 'PENDING_APPROVAL' | 'DRAFT' | 'PAUSED';
  location: string;
  image: string;
}

const INITIAL_SERVICES: VendorServiceItem[] = [
  {
    id: 'SRV-001',
    code: 'SUP-MK-01',
    name: 'Chèo SUP đón bình minh biển Mỹ Khê',
    nameEn: 'Sunrise Stand-up Paddleboarding at My Khe Beach',
    category: 'Chèo SUP & Kayak',
    categoryKey: 'sup',
    price: 280000,
    duration: 120,
    capacityPerSlot: 12,
    rating: 4.9,
    reviewCount: 86,
    status: 'ACTIVE',
    location: 'Bãi tắm Sao Biển, Hoàng Sa, Sơn Trà',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC7CA5tOzJev3TShmxMJ4QbD1KAtw2Nt4zcS-OGJysAF35iVA3yiq1cn7LzS5avIoYwSZtPPYOK8Uy4sAiKi81ovlXynWiu-ABcA7jMJ4VLmNNaKQnR9XjELpnzI_DKus9UZnuzQy55BJ1ZQa3wNf8-in9TALBABjR7nBMIJYRSaUKp1dthyttkxPw5Z6gKOQz6AO3zsLYbigGjjmP7NTtVdChlPTxTxv31Xsc_IzrAeN1uHkN-vqkS_Q'
  },
  {
    id: 'SRV-002',
    code: 'DIV-BB-02',
    name: 'Lặn biển bình khí ngắm rạn san hô Bãi Bụt',
    nameEn: 'Scuba Diving at Bai But Coral Reef',
    category: 'Lặn biển & San hô',
    categoryKey: 'diving',
    price: 650000,
    duration: 180,
    capacityPerSlot: 8,
    rating: 4.8,
    reviewCount: 42,
    status: 'ACTIVE',
    location: 'Bến tàu Bãi Bụt, Bán đảo Sơn Trà',
    image: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 'SRV-003',
    code: 'JET-ST-03',
    name: 'Lướt ván phản lực Jetsurf Sơn Trà',
    nameEn: 'Electric Hydrofoil & JetSurf Adventure',
    category: 'Thể thao cảm giác mạnh',
    categoryKey: 'extreme',
    price: 1200000,
    duration: 45,
    capacityPerSlot: 4,
    rating: 5.0,
    reviewCount: 15,
    status: 'ACTIVE',
    location: 'Bãi Tiên Sa, Sơn Trà, Đà Nẵng',
    image: 'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 'SRV-004',
    code: 'PAR-MK-04',
    name: 'Dù bay cano kéo Parasailing Mỹ Khê',
    nameEn: 'Tandem Parasailing Over Da Nang Coast',
    category: 'Thể thao cảm giác mạnh',
    categoryKey: 'extreme',
    price: 950000,
    duration: 30,
    capacityPerSlot: 6,
    rating: 4.7,
    reviewCount: 38,
    status: 'ACTIVE',
    location: 'Bãi tắm Phạm Văn Đồng, Sơn Trà',
    image: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 'SRV-005',
    code: 'PAD-OW-05',
    name: 'Khóa học lặn biển chuẩn quốc tế PADI Open Water',
    nameEn: 'PADI Open Water Diver Certification Course',
    category: 'Lặn biển & San hô',
    categoryKey: 'diving',
    price: 4500000,
    duration: 1440,
    capacityPerSlot: 6,
    rating: 0,
    reviewCount: 0,
    status: 'PENDING_APPROVAL',
    location: 'Trạm kiểm định Hòn Chảo, Sơn Trà',
    image: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 'SRV-006',
    code: 'KAY-TS-06',
    name: 'Sunset Kayak ngắm hoàng hôn Tiên Sa',
    nameEn: 'Tiên Sa Bay Sunset Kayaking',
    category: 'Chèo SUP & Kayak',
    categoryKey: 'sup',
    price: 350000,
    duration: 90,
    capacityPerSlot: 10,
    rating: 0,
    reviewCount: 0,
    status: 'DRAFT',
    location: 'Vịnh Tiên Sa, Sơn Trà, Đà Nẵng',
    image: 'https://images.unsplash.com/photo-1510414842594-a61c69b5ae57?auto=format&fit=crop&w=600&q=80'
  }
];

export function Services() {
  const navigate = useNavigate();
  const [services, setServices] = useState<VendorServiceItem[]>(INITIAL_SERVICES);
  const [activeTab, setActiveTab] = useState<'ALL' | 'ACTIVE' | 'PENDING_APPROVAL' | 'DRAFT' | 'PAUSED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [sortBy, setSortBy] = useState('newest');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Edit price modal state
  const [editingService, setEditingService] = useState<VendorServiceItem | null>(null);
  const [editPriceInput, setEditPriceInput] = useState('');

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Toggle status between ACTIVE and PAUSED
  const handleToggleStatus = (id: string) => {
    setServices(prev => prev.map(s => {
      if (s.id === id) {
        const nextStatus = s.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
        showToast(nextStatus === 'ACTIVE' ? `Đã mở bán lại dịch vụ "${s.name}"` : `Đã tạm ngưng dịch vụ "${s.name}"`);
        return { ...s, status: nextStatus };
      }
      return s;
    }));
  };

  // Delete service
  const handleDeleteService = (id: string, name: string) => {
    if (window.confirm(`Bạn có chắc muốn xóa dịch vụ "${name}" khỏi danh sách?`)) {
      setServices(prev => prev.filter(s => s.id !== id));
      showToast(`Đã xóa dịch vụ "${name}"`);
    }
  };

  // Save quick price edit
  const handleSavePrice = () => {
    if (!editingService) return;
    const newPrice = parseInt(editPriceInput.replace(/\D/g, ''), 10);
    if (isNaN(newPrice) || newPrice <= 0) {
      alert('Vui lòng nhập đơn giá hợp lệ lớn hơn 0đ.');
      return;
    }
    setServices(prev => prev.map(s => s.id === editingService.id ? { ...s, price: newPrice } : s));
    showToast(`Đã cập nhật giá mới: ${newPrice.toLocaleString('vi-VN')} đ cho ${editingService.name}`);
    setEditingService(null);
  };

  // Filter and sort
  const filteredServices = services.filter(service => {
    if (activeTab !== 'ALL' && service.status !== activeTab) return false;
    if (categoryFilter !== 'all' && service.categoryKey !== categoryFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchName = service.name.toLowerCase().includes(q);
      const matchEn = service.nameEn.toLowerCase().includes(q);
      const matchLoc = service.location.toLowerCase().includes(q);
      const matchCode = service.code.toLowerCase().includes(q);
      if (!matchName && !matchEn && !matchLoc && !matchCode) return false;
    }
    return true;
  }).sort((a, b) => {
    if (sortBy === 'price_asc') return a.price - b.price;
    if (sortBy === 'price_desc') return b.price - a.price;
    if (sortBy === 'rating') return b.rating - a.rating;
    return 0; // newest default
  });

  const activeCount = services.filter(s => s.status === 'ACTIVE').length;
  const pendingCount = services.filter(s => s.status === 'PENDING_APPROVAL').length;
  const draftCount = services.filter(s => s.status === 'DRAFT').length;
  const pausedCount = services.filter(s => s.status === 'PAUSED').length;

  return (
    <main className="w-full pt-16 bg-background flex-1">
      <div className="flex flex-col w-full px-space-lg lg:px-space-xl py-space-lg space-y-space-lg">
        {/* Header & KPI Banner Section */}
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-space-md">
          <div className="flex flex-col space-y-1">
            <div className="flex items-center gap-space-sm">
              <span className="px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm uppercase tracking-wider font-bold">
                Quản trị trải nghiệm biển
              </span>
              <span className="w-1.5 h-1.5 rounded-full bg-outline-variant"></span>
              <span className="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1">
                <span className="material-symbols-outlined text-primary text-[15px]">verified</span> Đối tác VITA - Cảng vụ Đà Nẵng
              </span>
            </div>
            <div className="flex items-baseline gap-space-sm">
              <h1 className="font-headline-lg text-headline-lg text-on-surface font-bold tracking-tight">
                Quản lý dịch vụ trải nghiệm biển
              </h1>
              <span className="px-2.5 py-0.5 rounded-full bg-surface-container-high text-on-surface-variant font-label-md text-label-md font-semibold">
                Tổng số: {services.length.toString().padStart(2, '0')} dịch vụ
              </span>
            </div>
          </div>
          <div className="flex items-center gap-space-sm">
            <button 
              onClick={() => navigate('/vendor/services/new')}
              className="inline-flex items-center gap-2 px-space-lg py-2.5 rounded-xl bg-primary text-on-primary font-label-md text-label-md font-semibold shadow-md hover:bg-primary-container transition-all shadow-[0_4px_16px_rgba(0,100,111,0.25)]" 
              type="button"
            >
              <span className="material-symbols-outlined text-body-lg">add_circle</span>
              <span>+ Tạo dịch vụ mới</span>
            </button>
          </div>
        </div>

        {/* Metric Snapshot Bento Row */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-space-md">
          <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between">
            <div className="space-y-1">
              <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">Đang hoạt động</span>
              <div className="flex items-baseline gap-space-xs">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">{activeCount.toString().padStart(2, '0')}</span>
                <span className="font-body-sm text-body-sm text-primary font-semibold">tour mở bán</span>
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant">Sẵn sàng nhận khách đặt cọc</p>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
              <span className="material-symbols-outlined text-headline-md">kitesurfing</span>
            </div>
          </div>

          <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between">
            <div className="space-y-1">
              <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">Chờ Cảng vụ duyệt</span>
              <div className="flex items-baseline gap-space-xs">
                <span className="font-headline-lg text-headline-lg font-bold text-secondary">{pendingCount.toString().padStart(2, '0')}</span>
                <span className="font-body-sm text-body-sm text-secondary font-semibold">hồ sơ PADI</span>
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant">Dự kiến phản hồi &lt; 24h</p>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-secondary/10 flex items-center justify-center text-secondary">
              <span className="material-symbols-outlined text-headline-md">pending_actions</span>
            </div>
          </div>

          <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between">
            <div className="space-y-1">
              <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">Điểm hài lòng biển</span>
              <div className="flex items-baseline gap-space-xs">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">4.92</span>
                <span className="font-body-sm text-body-sm text-primary-container font-semibold">/ 5.0</span>
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant">147 đánh giá xác thực</p>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-primary-container/10 flex items-center justify-center text-primary-container">
              <span className="material-symbols-outlined text-headline-md">award_star</span>
            </div>
          </div>

          <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex items-center justify-between">
            <div className="space-y-1">
              <span className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">Sức chứa hôm nay</span>
              <div className="flex items-baseline gap-space-xs">
                <span className="font-headline-lg text-headline-lg font-bold text-on-surface">36 / 48</span>
                <span className="font-body-sm text-body-sm text-outline font-normal">chỗ</span>
              </div>
              <p className="font-body-sm text-body-sm text-primary font-semibold">75% công suất bãi</p>
            </div>
            <div className="w-12 h-12 rounded-2xl bg-surface-container-high flex items-center justify-center text-on-surface">
              <span className="material-symbols-outlined text-headline-md">groups</span>
            </div>
          </div>
        </div>

        {/* Primary Filter Tabs & Unified Filter Bar */}
        <div className="flex flex-col space-y-space-md">
          {/* Status Pill Tabs */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1 text-nowrap scrollbar-none">
            <button 
              onClick={() => setActiveTab('ALL')}
              className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1.5 ${
                activeTab === 'ALL' 
                  ? 'bg-primary text-on-primary font-semibold shadow-sm' 
                  : 'bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
              }`} 
              type="button"
            >
              <span>Tất cả</span>
              <span className={`px-1.5 py-0.5 rounded-full text-[11px] font-bold ${activeTab === 'ALL' ? 'bg-on-primary/20 text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>
                {services.length}
              </span>
            </button>

            <button 
              onClick={() => setActiveTab('ACTIVE')}
              className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1.5 ${
                activeTab === 'ACTIVE' 
                  ? 'bg-primary text-on-primary font-semibold shadow-sm' 
                  : 'bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
              }`} 
              type="button"
            >
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              <span>Đang hoạt động</span>
              <span className={`px-1.5 py-0.5 rounded-full text-[11px] font-semibold ${activeTab === 'ACTIVE' ? 'bg-on-primary/20 text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>
                {activeCount}
              </span>
            </button>

            <button 
              onClick={() => setActiveTab('PENDING_APPROVAL')}
              className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1.5 ${
                activeTab === 'PENDING_APPROVAL' 
                  ? 'bg-primary text-on-primary font-semibold shadow-sm' 
                  : 'bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
              }`} 
              type="button"
            >
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
              <span>Đang chờ duyệt</span>
              <span className={`px-1.5 py-0.5 rounded-full text-[11px] font-semibold ${activeTab === 'PENDING_APPROVAL' ? 'bg-on-primary/20 text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>
                {pendingCount}
              </span>
            </button>

            <button 
              onClick={() => setActiveTab('DRAFT')}
              className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1.5 ${
                activeTab === 'DRAFT' 
                  ? 'bg-primary text-on-primary font-semibold shadow-sm' 
                  : 'bg-surface-container-lowest text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
              }`} 
              type="button"
            >
              <span className="w-2 h-2 rounded-full bg-outline"></span>
              <span>Bản nháp</span>
              <span className={`px-1.5 py-0.5 rounded-full text-[11px] font-semibold ${activeTab === 'DRAFT' ? 'bg-on-primary/20 text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>
                {draftCount}
              </span>
            </button>

            <button 
              onClick={() => setActiveTab('PAUSED')}
              className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1.5 ${
                activeTab === 'PAUSED' 
                  ? 'bg-primary text-on-primary font-semibold shadow-sm' 
                  : 'bg-surface-container-lowest text-outline hover:text-on-surface-variant'
              }`} 
              type="button"
            >
              <span className="w-2 h-2 rounded-full bg-red-400"></span>
              <span>Tạm ngưng</span>
              <span className={`px-1.5 py-0.5 rounded-full text-[11px] font-semibold ${activeTab === 'PAUSED' ? 'bg-on-primary/20 text-on-primary' : 'bg-surface-container text-outline'}`}>
                {pausedCount}
              </span>
            </button>
          </div>

          {/* Search & Filter Controls Panel */}
          <div className="p-space-md rounded-2xl bg-surface-container-lowest shadow-sm flex flex-col md:flex-row items-center justify-between gap-space-md">
            <div className="relative w-full md:max-w-md">
              <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant text-body-lg">search</span>
              <input 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-space-md py-2.5 rounded-xl bg-surface-container-low text-on-surface placeholder:text-on-surface-variant text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-none" 
                placeholder="Tìm theo tên dịch vụ, mã tour, bến xuất phát..." 
                type="text"
              />
            </div>
            <div className="flex flex-wrap items-center gap-space-sm w-full md:w-auto">
              <div className="flex items-center gap-2 bg-surface-container-low px-3 py-2 rounded-xl">
                <span className="material-symbols-outlined text-primary text-[18px]">category</span>
                <select 
                  value={categoryFilter}
                  onChange={(e) => setCategoryFilter(e.target.value)}
                  className="bg-transparent text-on-surface font-label-md text-label-md focus:outline-none cursor-pointer"
                >
                  <option value="all">Tất cả danh mục</option>
                  <option value="sup">Chèo SUP &amp; Kayak</option>
                  <option value="diving">Lặn biển &amp; San hô</option>
                  <option value="boat">Cano &amp; Du thuyền</option>
                  <option value="extreme">Thể thao cảm giác mạnh</option>
                </select>
              </div>
              <div className="flex items-center gap-2 bg-surface-container-low px-3 py-2 rounded-xl">
                <span className="material-symbols-outlined text-on-surface-variant text-[18px]">swap_vert</span>
                <select 
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="bg-transparent text-on-surface font-label-md text-label-md focus:outline-none cursor-pointer"
                >
                  <option value="newest">Mới cập nhật</option>
                  <option value="price_asc">Giá tăng dần</option>
                  <option value="price_desc">Giá giảm dần</option>
                  <option value="rating">Đánh giá cao nhất</option>
                </select>
              </div>
              <button 
                onClick={() => { setSearchQuery(''); setCategoryFilter('all'); setSortBy('newest'); setActiveTab('ALL'); }}
                className="p-2.5 rounded-xl bg-surface-container-low text-on-surface-variant hover:text-primary transition-colors" 
                title="Làm mới bộ lọc" 
                type="button"
              >
                <span className="material-symbols-outlined text-body-lg">refresh</span>
              </button>
            </div>
          </div>
        </div>

        {/* Main Luxury Service Management Table */}
        <div
          key={`${activeTab}-${categoryFilter}-${sortBy}`}
          className="w-full bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden flex flex-col animate-fade-in-up"
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[1100px]">
              <thead>
                <tr className="bg-surface-container-low text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider">
                  <th className="py-space-md px-space-lg font-semibold">Tên dịch vụ &amp; Trải nghiệm</th>
                  <th className="py-space-md px-space-md font-semibold">Danh mục</th>
                  <th className="py-space-md px-space-md font-semibold">Đơn giá áp dụng</th>
                  <th className="py-space-md px-space-md font-semibold">Thời lượng &amp; Ca</th>
                  <th className="py-space-md px-space-md font-semibold">Đánh giá</th>
                  <th className="py-space-md px-space-md font-semibold">Trạng thái</th>
                  <th className="py-space-md px-space-lg text-right font-semibold">Thao tác</th>
                </tr>
              </thead>
              <tbody className="font-body-md text-body-md text-on-surface">
                {filteredServices.length > 0 ? (
                  filteredServices.map(service => (
                    <tr key={service.id} className="hover:bg-surface-container-low/40 transition-colors group">
                      <td className="py-space-md px-space-lg">
                        <div className="flex items-center gap-space-md">
                          <div className="relative w-14 h-14 rounded-xl overflow-hidden flex-shrink-0 shadow-sm bg-surface-container-high">
                            <img className="w-full h-full object-cover" alt={service.name} src={service.image} />
                            <span className="absolute bottom-1 right-1 px-1 py-0.5 rounded bg-inverse-surface/80 text-[9px] font-bold text-surface-bright">HDV</span>
                          </div>
                          <div className="flex flex-col min-w-0">
                            <div className="flex items-center gap-2">
                              <span 
                                onClick={() => navigate('/vendor/services/new')}
                                className="font-headline-sm text-headline-sm font-bold text-on-surface hover:text-primary transition-colors truncate cursor-pointer"
                              >
                                {service.name}
                              </span>
                              <span className="material-symbols-outlined text-primary text-[16px]" title="Bảo hiểm biển đã kích hoạt">verified</span>
                            </div>
                            <span className="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1.5 mt-0.5">
                              <span className="material-symbols-outlined text-[14px]">pin_drop</span>
                              {service.location}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="py-space-md px-space-md">
                        <span className="px-2.5 py-1 rounded-full bg-primary/10 text-primary font-label-sm text-label-sm font-semibold whitespace-nowrap">
                          {service.category}
                        </span>
                      </td>
                      <td className="py-space-md px-space-md">
                        <div className="flex flex-col">
                          <div className="flex items-center gap-1.5">
                            <span className="font-headline-sm text-headline-sm font-bold text-primary">
                              {service.price.toLocaleString('vi-VN')} đ
                            </span>
                            <button 
                              onClick={() => { setEditingService(service); setEditPriceInput(service.price.toString()); }}
                              className="text-on-surface-variant hover:text-primary transition-colors"
                              title="Sửa giá nhanh"
                            >
                              <span className="material-symbols-outlined text-[16px]">edit</span>
                            </button>
                          </div>
                          <span className="font-label-sm text-label-sm text-on-surface-variant">/ người (trọn gói)</span>
                        </div>
                      </td>
                      <td className="py-space-md px-space-md">
                        <div className="flex flex-col space-y-1">
                          <span className="font-label-md text-label-md font-semibold text-on-surface flex items-center gap-1">
                            <span className="material-symbols-outlined text-[16px] text-tertiary">schedule</span>
                            {service.duration >= 60 ? `${Math.floor(service.duration / 60)} giờ` : `${service.duration} phút`}
                          </span>
                          <span className="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1">
                            <span className="material-symbols-outlined text-[15px] text-outline">group</span>
                            {service.capacityPerSlot} người / ca
                          </span>
                        </div>
                      </td>
                      <td className="py-space-md px-space-md">
                        {service.rating > 0 ? (
                          <div className="flex items-center gap-1">
                            <span className="material-symbols-outlined text-amber-500 text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                            <span className="font-label-md text-label-md font-bold text-on-surface">{service.rating}</span>
                            <span className="font-body-sm text-body-sm text-on-surface-variant">({service.reviewCount})</span>
                          </div>
                        ) : (
                          <span className="text-on-surface-variant text-label-sm italic">Chưa có đánh giá</span>
                        )}
                      </td>
                      <td className="py-space-md px-space-md">
                        {service.status === 'ACTIVE' && (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-label-sm font-bold tracking-wide">
                            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                            ĐANG HOẠT ĐỘNG
                          </span>
                        )}
                        {service.status === 'PENDING_APPROVAL' && (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 text-amber-700 font-label-sm text-label-sm font-bold tracking-wide">
                            <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                            CHỜ DUYỆT CẢNG VỤ
                          </span>
                        )}
                        {service.status === 'DRAFT' && (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm font-bold">
                            <span className="w-2 h-2 rounded-full bg-outline"></span>
                            BẢN NHÁP
                          </span>
                        )}
                        {service.status === 'PAUSED' && (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-red-50 text-red-600 font-label-sm text-label-sm font-bold">
                            <span className="w-2 h-2 rounded-full bg-red-500"></span>
                            ĐÃ TẠM NGƯNG
                          </span>
                        )}
                      </td>
                      <td className="py-space-md px-space-lg text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button 
                            onClick={() => navigate('/vendor/services/new')}
                            className="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-primary transition-all" 
                            title="Chỉnh sửa dịch vụ" 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-body-lg">edit</span>
                          </button>
                          
                          <button 
                            onClick={() => handleToggleStatus(service.id)}
                            className={`p-2 rounded-lg transition-all ${
                              service.status === 'ACTIVE' 
                                ? 'text-on-surface-variant hover:bg-secondary/10 hover:text-secondary' 
                                : 'text-emerald-600 hover:bg-emerald-50'
                            }`} 
                            title={service.status === 'ACTIVE' ? 'Tạm ngưng nhận khách' : 'Mở bán lại'} 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-body-lg">
                              {service.status === 'ACTIVE' ? 'pause_circle' : 'play_circle'}
                            </span>
                          </button>

                          <button 
                            onClick={() => navigate('/vendor/schedule')}
                            className="px-2.5 py-1.5 rounded-lg bg-surface-container text-primary font-label-sm text-label-sm font-semibold hover:bg-primary/10 transition-all flex items-center gap-1" 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-[15px]">calendar_month</span>
                            <span>Lịch ca</span>
                          </button>

                          <button 
                            onClick={() => handleDeleteService(service.id, service.name)}
                            className="p-2 rounded-lg text-on-surface-variant hover:bg-error/10 hover:text-error transition-all" 
                            title="Xóa dịch vụ" 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-body-lg">delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-on-surface-variant">
                      <span className="material-symbols-outlined text-4xl mb-2 text-outline">search_off</span>
                      <p className="font-body-md">Không tìm thấy dịch vụ nào phù hợp với bộ lọc hiện tại.</p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Edit Price Modal (Portaled to body) */}
      {editingService && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface rounded-2xl max-w-md w-full p-space-lg shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
            <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
              <div className="flex items-center gap-2">
                <span className="w-8 h-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
                  <span className="material-symbols-outlined text-[18px]">payments</span>
                </span>
                <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">Cập nhật đơn giá niêm yết</h3>
              </div>
              <button onClick={() => setEditingService(null)} className="text-on-surface-variant hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div>
              <p className="font-label-md font-semibold text-on-surface">{editingService.name}</p>
              <p className="font-body-sm text-on-surface-variant mt-0.5">Giá hiện tại: <strong className="text-primary">{editingService.price.toLocaleString('vi-VN')} đ</strong> / người</p>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="font-label-md text-label-md font-semibold text-on-surface">Đơn giá trọn gói mới (VNĐ)</label>
              <input 
                type="number"
                value={editPriceInput}
                onChange={(e) => setEditPriceInput(e.target.value)}
                className="w-full px-space-md py-2.5 rounded-xl bg-surface-container-low text-on-surface font-headline-sm font-bold text-primary focus:outline-none focus:ring-2 focus:ring-primary shadow-sm"
                placeholder="Ví dụ: 300000"
              />
              <span className="text-[11px] text-on-surface-variant italic">* Giá trọn gói đã bao gồm bảo hiểm cứu hộ biển và áo phao quy chuẩn VITA.</span>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t border-outline-variant/30">
              <button 
                onClick={() => setEditingService(null)}
                className="px-4 py-2 rounded-xl bg-surface-container-high text-on-surface font-label-md"
              >
                Hủy
              </button>
              <button 
                onClick={handleSavePrice}
                className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold shadow hover:bg-primary-container"
              >
                Lưu giá mới
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
          <span className="font-label-md text-label-md">{toastMessage}</span>
        </div>
      )}
    </main>
  );
}
