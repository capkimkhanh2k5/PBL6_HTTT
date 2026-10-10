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

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const totalItems = filteredServices.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const paginatedServices = filteredServices.slice(
    (validCurrentPage - 1) * pageSize,
    validCurrentPage * pageSize
  );

  const activeCount = services.filter(s => s.status === 'ACTIVE').length;
  const pendingCount = services.filter(s => s.status === 'PENDING_APPROVAL').length;
  const draftCount = services.filter(s => s.status === 'DRAFT').length;
  const pausedCount = services.filter(s => s.status === 'PAUSED').length;

  return (
    <div className="w-full bg-background flex-1">
      <div className="flex flex-col w-full px-6 py-4 space-y-3.5">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-300">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-black tracking-tight">
              Dịch vụ
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button 
              onClick={() => navigate('/vendor/services/new')}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-primary text-on-primary text-sm font-medium hover:bg-primary-container transition-colors shadow-xs" 
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">add</span>
              <span>Tạo dịch vụ mới</span>
            </button>
          </div>
        </div>

        {/* Primary Filter Tabs & Unified Filter Bar */}
        <div className="flex flex-col space-y-2.5">
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
          <div className="p-3 rounded-xl bg-white border border-slate-300 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
            <div className="relative w-full md:max-w-md">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-black text-body-lg">search</span>
              <input 
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
                className="w-full pl-10 pr-3 py-2 rounded-lg bg-slate-50 border border-slate-300 text-black placeholder:text-slate-500 text-sm focus:outline-none focus:ring-1 focus:ring-black" 
                placeholder="Tìm theo tên dịch vụ, mã tour, bến xuất phát..." 
                type="text"
              />
            </div>
            <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
              <div className="flex items-center gap-2 bg-slate-50 border border-slate-300 px-3 py-1.5 rounded-lg">
                <span className="material-symbols-outlined text-black text-[18px]">category</span>
                <select 
                  value={categoryFilter}
                  onChange={(e) => { setCategoryFilter(e.target.value); setCurrentPage(1); }}
                  className="bg-transparent text-black text-xs font-semibold focus:outline-none cursor-pointer"
                >
                  <option value="all">Tất cả danh mục</option>
                  <option value="sup">Chèo SUP &amp; Kayak</option>
                  <option value="diving">Lặn biển &amp; San hô</option>
                  <option value="boat">Cano &amp; Du thuyền</option>
                  <option value="extreme">Thể thao cảm giác mạnh</option>
                </select>
              </div>
              <div className="flex items-center gap-2 bg-slate-50 border border-slate-300 px-3 py-1.5 rounded-lg">
                <span className="material-symbols-outlined text-black text-[18px]">swap_vert</span>
                <select 
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="bg-transparent text-black text-xs font-semibold focus:outline-none cursor-pointer"
                >
                  <option value="newest">Mới cập nhật</option>
                  <option value="price_asc">Giá tăng dần</option>
                  <option value="price_desc">Giá giảm dần</option>
                  <option value="rating">Đánh giá cao nhất</option>
                </select>
              </div>
              <button 
                onClick={() => { setSearchQuery(''); setCategoryFilter('all'); setSortBy('newest'); setActiveTab('ALL'); setCurrentPage(1); }}
                className="p-2 rounded-lg bg-slate-100 border border-slate-300 text-black hover:bg-slate-200 transition-colors" 
                title="Làm mới bộ lọc" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">refresh</span>
              </button>
            </div>
          </div>
        </div>

        {/* Main Service Management Table */}
        <div
          key={`${activeTab}-${categoryFilter}-${sortBy}`}
          className="w-full bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden flex flex-col"
        >
          <div className="overflow-x-auto scrollbar-thin">
            <table className="w-full text-left border-collapse min-w-[960px]">
              <thead>
                <tr className="bg-slate-50 text-black text-xs font-bold border-b border-slate-200">
                  <th className="py-3 px-4 font-bold text-black border-r border-slate-200">Tên dịch vụ &amp; Trải nghiệm</th>
                  <th className="py-3 px-3 font-bold text-black border-r border-slate-200">Danh mục</th>
                  <th className="py-3 px-3 font-bold text-black text-right border-r border-slate-200">Đơn giá niêm yết</th>
                  <th className="py-3 px-3 font-bold text-black border-r border-slate-200">Thời lượng &amp; Ca</th>
                  <th className="py-3 px-3 font-bold text-black border-r border-slate-200">Đánh giá</th>
                  <th className="py-3 px-3 font-bold text-black border-r border-slate-200">Trạng thái</th>
                  <th className="py-3 px-3 text-center font-bold text-black sticky right-0 bg-slate-50 z-10 w-[130px]">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-sm text-black">
                {paginatedServices.length > 0 ? (
                  paginatedServices.map(service => (
                    <tr key={service.id} className="hover:bg-slate-50 transition-colors group">
                      <td className="py-3 px-4 border-r border-slate-200">
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-lg overflow-hidden shrink-0 bg-slate-100 border border-slate-200">
                            <img className="w-full h-full object-cover" alt={service.name} src={service.image} />
                          </div>
                          <div className="flex flex-col min-w-0">
                            <span 
                              onClick={() => navigate('/vendor/services/new')}
                              className="font-bold text-sm text-black hover:underline truncate cursor-pointer"
                            >
                              {service.name}
                            </span>
                            <span className="text-xs text-black flex items-center gap-1 mt-0.5">
                              <span className="material-symbols-outlined text-[13px] text-black">location_on</span>
                              <span>{service.location}</span>
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-black text-xs font-medium border border-slate-200 whitespace-nowrap">
                          {service.category}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right border-r border-slate-200">
                        <div className="flex flex-col items-end">
                          <div className="flex items-center gap-1">
                            <span className="font-mono font-bold text-sm text-black">
                              {service.price.toLocaleString('vi-VN')} đ
                            </span>
                            <button 
                              onClick={() => { setEditingService(service); setEditPriceInput(service.price.toString()); }}
                              className="text-black hover:text-slate-700 transition-colors cursor-pointer"
                              title="Sửa giá nhanh"
                              type="button"
                            >
                              <span className="material-symbols-outlined text-[15px]">edit</span>
                            </button>
                          </div>
                          <span className="text-[11px] text-black">/ người</span>
                        </div>
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        <div className="flex flex-col text-xs space-y-0.5 text-black">
                          <span className="font-medium text-black flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px] text-black">schedule</span>
                            <span>{service.duration >= 60 ? `${Math.floor(service.duration / 60)} giờ` : `${service.duration} phút`}</span>
                          </span>
                          <span className="text-[11px] text-black flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px] text-black">group</span>
                            <span>{service.capacityPerSlot} người / ca</span>
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        {service.rating > 0 ? (
                          <div className="flex items-center gap-1 text-xs">
                            <span className="material-symbols-outlined text-amber-500 text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                            <span className="font-bold text-black">{service.rating}</span>
                            <span className="text-black">({service.reviewCount})</span>
                          </div>
                        ) : (
                          <span className="text-black text-xs italic">Chưa có đánh giá</span>
                        )}
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200">
                        {service.status === 'ACTIVE' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-300">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                            <span>Đang hoạt động</span>
                          </span>
                        )}
                        {service.status === 'PENDING_APPROVAL' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-300">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-600"></span>
                            <span>Chờ duyệt</span>
                          </span>
                        )}
                        {service.status === 'DRAFT' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-800 border border-slate-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-500"></span>
                            <span>Bản nháp</span>
                          </span>
                        )}
                        {service.status === 'PAUSED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-red-50 text-red-800 border border-red-300">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
                            <span>Đã tạm ngưng</span>
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center sticky right-0 bg-white group-hover:bg-slate-50 z-10 transition-colors w-[130px]">
                        <div className="flex items-center justify-center gap-1.5">
                          <button 
                            onClick={() => navigate('/vendor/services/new')}
                            className="p-1.5 rounded text-black hover:bg-slate-200 transition-colors" 
                            title="Chỉnh sửa dịch vụ" 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-[18px]">edit</span>
                          </button>
                          
                          <button 
                            onClick={() => handleToggleStatus(service.id)}
                            className="p-1.5 rounded text-black hover:bg-slate-200 transition-colors" 
                            title={service.status === 'ACTIVE' ? 'Tạm ngưng nhận khách' : 'Mở bán lại'} 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-[18px]">
                              {service.status === 'ACTIVE' ? 'pause_circle' : 'play_circle'}
                            </span>
                          </button>

                          <button 
                            onClick={() => navigate('/vendor/schedule')}
                            className="p-1.5 rounded text-black hover:bg-slate-200 transition-colors" 
                            title="Lịch ca & Sức chứa"
                            type="button"
                          >
                            <span className="material-symbols-outlined text-[18px]">calendar_month</span>
                          </button>

                          <button 
                            onClick={() => handleDeleteService(service.id, service.name)}
                            className="p-1.5 rounded text-black hover:text-red-600 hover:bg-red-50 transition-colors" 
                            title="Xóa dịch vụ" 
                            type="button"
                          >
                            <span className="material-symbols-outlined text-[18px]">delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-black">
                      <span className="material-symbols-outlined text-4xl mb-2 text-slate-400">search_off</span>
                      <p className="font-body-md text-black">Không tìm thấy dịch vụ nào phù hợp với bộ lọc hiện tại.</p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Minimal Clean Pagination Bar */}
          <div className="px-4 py-2 border-t border-slate-200 bg-slate-50 flex items-center justify-between text-xs select-none">
            {/* Page Size Selector */}
            <div className="flex items-center gap-2">
              <span className="text-black font-semibold">Số dòng:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(1);
                }}
                className="px-2 py-1 rounded bg-white border border-slate-200 text-xs font-semibold text-black cursor-pointer focus:outline-none"
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
              </select>
            </div>

            {/* Pagination Navigation */}
            <div className="flex items-center gap-1">
              <button
                onClick={() => setCurrentPage(1)}
                disabled={validCurrentPage === 1}
                className="p-1 rounded border border-slate-200 text-black hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Trang đầu"
                type="button"
              >
                <span className="material-symbols-outlined text-[16px]">first_page</span>
              </button>
              <button
                onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                disabled={validCurrentPage === 1}
                className="p-1 rounded border border-slate-200 text-black hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Trang trước"
                type="button"
              >
                <span className="material-symbols-outlined text-[16px]">chevron_left</span>
              </button>

              <div className="flex items-center gap-1 px-1">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`min-w-[28px] h-7 px-2 rounded text-xs font-bold transition-colors ${
                      page === validCurrentPage
                        ? "bg-primary text-white"
                        : "border border-slate-200 text-black hover:bg-slate-100"
                    }`}
                    type="button"
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                disabled={validCurrentPage === totalPages}
                className="p-1 rounded border border-slate-300 text-black hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Trang sau"
                type="button"
              >
                <span className="material-symbols-outlined text-[16px]">chevron_right</span>
              </button>
              <button
                onClick={() => setCurrentPage(totalPages)}
                disabled={validCurrentPage === totalPages}
                className="p-1 rounded border border-slate-300 text-black hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Trang cuối"
                type="button"
              >
                <span className="material-symbols-outlined text-[16px]">last_page</span>
              </button>
            </div>
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
              <span className="text-[11px] text-on-surface-variant italic">* Giá trọn gói đã bao gồm bảo hiểm cứu hộ biển và áo phao.</span>
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
    </div>
  );
}
