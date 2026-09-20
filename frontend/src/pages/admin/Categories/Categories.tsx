import React, { useState, useMemo } from 'react';
import { MOCK_ADMIN_CATEGORIES } from '../../../data/adminMockData';
import type { Category } from '../../../types';

export function Categories() {
  const [categories, setCategories] = useState<Category[]>(MOCK_ADMIN_CATEGORIES);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Selected category for editing (or null for creating new)
  const [editingCategory, setEditingCategory] = useState<Category | null>(MOCK_ADMIN_CATEGORIES[0] || null);

  // Form State
  const [formName, setFormName] = useState(MOCK_ADMIN_CATEGORIES[0]?.name || '');
  const [formNameEn, setFormNameEn] = useState(MOCK_ADMIN_CATEGORIES[0]?.nameEn || '');
  const [formSlug, setFormSlug] = useState(MOCK_ADMIN_CATEGORIES[0]?.slug || '');
  const [formParentId, setFormParentId] = useState(MOCK_ADMIN_CATEGORIES[0]?.parentId || '');
  const [formIcon, setFormIcon] = useState(MOCK_ADMIN_CATEGORIES[0]?.iconUrl || 'surfing');
  const [formIsActive, setFormIsActive] = useState(MOCK_ADMIN_CATEGORIES[0]?.isActive ?? true);
  const [formRequiresCert, setFormRequiresCert] = useState(MOCK_ADMIN_CATEGORIES[0]?.requiresSafetyCert ?? true);

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredCategories = useMemo(() => {
    return categories.filter(c =>
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.slug.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.nameEn.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [categories, searchQuery]);

  const handleSelectForEdit = (cat: Category) => {
    setEditingCategory(cat);
    setFormName(cat.name);
    setFormNameEn(cat.nameEn);
    setFormSlug(cat.slug);
    setFormParentId(cat.parentId || '');
    setFormIcon(cat.iconUrl || 'surfing');
    setFormIsActive(cat.isActive);
    setFormRequiresCert(cat.requiresSafetyCert);
  };

  const handleResetToCreate = () => {
    setEditingCategory(null);
    setFormName('');
    setFormNameEn('');
    setFormSlug('');
    setFormParentId('');
    setFormIcon('surfing');
    setFormIsActive(true);
    setFormRequiresCert(true);
  };

  const handleAutoSlug = (name: string) => {
    setFormName(name);
    if (!editingCategory) {
      const slug = name
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[đĐ]/g, "d")
        .replace(/[^a-z0-9\s-]/g, "")
        .trim()
        .replace(/\s+/g, "-");
      setFormSlug(slug);
    }
  };

  const handleSaveCategory = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName || !formSlug) {
      alert("Vui lòng nhập tên danh mục và slug!");
      return;
    }

    if (editingCategory) {
      // Update
      setCategories(prev =>
        prev.map(c =>
          c.id === editingCategory.id
            ? {
                ...c,
                name: formName,
                nameEn: formNameEn,
                slug: formSlug,
                parentId: formParentId,
                iconUrl: formIcon,
                isActive: formIsActive,
                requiresSafetyCert: formRequiresCert,
                updatedAt: new Date().toISOString()
              }
            : c
        )
      );
      showToast(`Đã cập nhật danh mục "${formName}" thành công!`);
    } else {
      // Create new
      const newCat: Category = {
        id: `cat-${Date.now()}`,
        name: formName,
        nameEn: formNameEn,
        slug: formSlug,
        parentId: formParentId,
        iconUrl: formIcon,
        isActive: formIsActive,
        requiresSafetyCert: formRequiresCert,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      setCategories(prev => [...prev, newCat]);
      setEditingCategory(newCat);
      showToast(`Đã tạo mới danh mục "${newCat.name}" thành công!`);
    }
  };

  const handleToggleActive = (id: string, current: boolean, e: React.MouseEvent) => {
    e.stopPropagation();
    setCategories(prev =>
      prev.map(c => (c.id === id ? { ...c, isActive: !current } : c))
    );
    showToast(`Đã ${!current ? 'kích hoạt' : 'tạm ẩn'} danh mục.`);
  };

  const handleDelete = (id: string, name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm(`Bạn có chắc chắn muốn xóa danh mục "${name}"?`)) {
      setCategories(prev => prev.filter(c => c.id !== id));
      if (editingCategory?.id === id) {
        handleResetToCreate();
      }
      showToast(`Đã xóa danh mục "${name}".`);
    }
  };

  const totalCount = categories.length;
  const activeCount = categories.filter(c => c.isActive).length;
  const certRequiredCount = categories.filter(c => c.requiresSafetyCert).length;

  return (
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full animate-fade-in-up">
        {/* Toast */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Page Sub-Header */}
        <section className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md mb-space-xl">
          <div className="flex flex-col gap-space-xs">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary font-label-sm uppercase tracking-wider font-bold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
                Cơ sở dữ liệu hải trình &amp; dịch vụ
              </span>
              <span className="text-outline-variant font-label-sm">|</span>
              <span className="text-on-surface-variant font-label-sm font-semibold">Taxonomy Engine v2.4</span>
            </div>
            <h1 className="font-headline-xl text-headline-xl text-on-surface tracking-tight font-black">
              Cấu trúc Danh mục &amp; Phân loại Trải nghiệm Biển
            </h1>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl">
              Quản lý cây danh mục đa cấp, mã định danh URL (slug), quy định chứng chỉ an toàn và trạng thái hiển thị trên toàn sàn DANASEA.
            </p>
          </div>

          <div className="flex items-center gap-space-sm self-start lg:self-auto">
            <button 
              onClick={() => showToast("Đã xóa và nạp lại bộ đệm Redis Cache cho hệ thống phân loại!")}
              className="flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-low hover:bg-surface-container text-on-surface font-label-lg transition-all shadow-sm font-semibold"
            >
              <span className="material-symbols-outlined text-[18px]">cached</span>
              <span>Đồng bộ Redis Cache</span>
            </button>
            <button 
              onClick={handleResetToCreate}
              className="flex items-center gap-2 px-space-lg py-2.5 rounded-xl bg-primary hover:bg-primary-container text-on-primary font-label-lg transition-all shadow-sm font-bold"
            >
              <span className="material-symbols-outlined text-[20px]">add_circle</span>
              <span>Tạo Danh Mục Mới</span>
            </button>
          </div>
        </section>

        {/* KPI Metrics */}
        <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-lg mb-space-xl">
          <div className="flex items-center gap-space-md p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm border border-outline-variant/20">
            <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[26px]">account_tree</span>
            </div>
            <div className="flex flex-col min-w-0">
              <span className="font-label-sm uppercase text-outline font-bold">Tổng số danh mục</span>
              <span className="font-headline-lg font-black text-on-surface">{totalCount}</span>
              <span className="font-label-sm text-primary font-semibold">Phân loại biển</span>
            </div>
          </div>

          <div className="flex items-center gap-space-md p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm border border-outline-variant/20">
            <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[26px]">check_circle</span>
            </div>
            <div className="flex flex-col min-w-0">
              <span className="font-label-sm uppercase text-outline font-bold">Đang kích hoạt</span>
              <span className="font-headline-lg font-black text-primary">{activeCount} / {totalCount}</span>
              <span className="font-label-sm text-outline-variant">{Math.round((activeCount / (totalCount || 1)) * 100)}% khả dụng trên sàn</span>
            </div>
          </div>

          <div className="flex items-center gap-space-md p-space-lg rounded-2xl bg-surface-container-lowest shadow-sm border border-outline-variant/20">
            <div className="w-12 h-12 rounded-xl bg-secondary-container/20 text-secondary flex items-center justify-center">
              <span className="material-symbols-outlined text-[26px]">verified_user</span>
            </div>
            <div className="flex flex-col min-w-0">
              <span className="font-label-sm uppercase text-outline font-bold">Yêu cầu chứng chỉ</span>
              <span className="font-headline-lg font-black text-secondary">{certRequiredCount}</span>
              <span className="font-label-sm text-secondary font-semibold">Bắt buộc hồ sơ cứu hộ</span>
            </div>
          </div>

          <div className="flex items-center justify-between p-space-lg rounded-2xl bg-surface-container-low border border-outline-variant/20">
            <div className="flex flex-col">
              <div className="flex items-center gap-1.5 text-on-surface-variant mb-1 font-bold">
                <span className="material-symbols-outlined text-[18px] text-primary">cloud_done</span>
                <span className="font-label-sm uppercase tracking-wider">Hạ tầng Cảng VITA</span>
              </div>
              <span className="font-headline-sm font-bold text-on-surface">Đồng bộ tức thì</span>
              <span className="font-body-sm text-xs text-outline">REST API &amp; Webhook: Sẵn sàng</span>
            </div>
          </div>
        </section>

        {/* Main 2-Column Workspace */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* LEFT COLUMN (Span 7): Category List */}
          <div className="lg:col-span-7 flex flex-col gap-space-md">
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-space-sm p-space-md bg-surface-container-lowest rounded-2xl shadow-sm border border-outline-variant/20">
              <div className="relative flex-1">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[20px]">filter_list</span>
                <input 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-10 pl-10 pr-space-md rounded-xl bg-surface-container-low text-on-surface font-body-md text-body-md placeholder:text-outline focus:outline-none focus:ring-1 focus:ring-primary border border-outline-variant/30" 
                  placeholder="Lọc theo tên phân loại hoặc slug..." 
                  type="text"
                />
              </div>
            </div>

            {/* List Table */}
            <div className="bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden border border-outline-variant/20">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-surface-container text-on-surface-variant font-label-sm text-xs uppercase tracking-wider">
                      <th className="py-3 px-4">Biểu tượng &amp; Tên danh mục</th>
                      <th className="py-3 px-3">Slug (URL)</th>
                      <th className="py-3 px-3 text-center">Chứng chỉ</th>
                      <th className="py-3 px-3 text-center">Hiển thị</th>
                      <th className="py-3 px-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant/10 font-body-md text-sm">
                    {filteredCategories.map(cat => {
                      const isSelected = cat.id === editingCategory?.id;
                      return (
                        <tr 
                          key={cat.id}
                          onClick={() => handleSelectForEdit(cat)}
                          className={`cursor-pointer transition-colors ${
                            isSelected ? 'bg-primary/10' : 'hover:bg-surface-container-low/40'
                          }`}
                        >
                          <td className="py-3.5 px-4 flex items-center gap-3">
                            <div className="w-9 h-9 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold">
                              <span className="material-symbols-outlined text-[20px]">{cat.iconUrl || 'surfing'}</span>
                            </div>
                            <div>
                              <span className="font-bold text-on-surface block">{cat.name}</span>
                              <span className="text-xs text-outline italic">{cat.nameEn}</span>
                            </div>
                          </td>

                          <td className="py-3.5 px-3 font-mono text-xs text-outline">
                            /{cat.slug}
                          </td>

                          <td className="py-3.5 px-3 text-center">
                            {cat.requiresSafetyCert ? (
                              <span className="px-2 py-0.5 rounded-full bg-secondary-container/20 text-secondary text-[11px] font-bold">
                                Bắt buộc
                              </span>
                            ) : (
                              <span className="px-2 py-0.5 rounded-full bg-surface-container text-outline text-[11px]">
                                Không
                              </span>
                            )}
                          </td>

                          <td className="py-3.5 px-3 text-center">
                            <button
                              type="button"
                              onClick={(e) => handleToggleActive(cat.id, cat.isActive, e)}
                              className={`w-9 h-5 rounded-full transition-colors relative inline-flex items-center p-0.5 ${
                                cat.isActive ? 'bg-primary' : 'bg-surface-container-high'
                              }`}
                            >
                              <span className={`w-4 h-4 rounded-full bg-white transition-transform ${
                                cat.isActive ? 'translate-x-4' : 'translate-x-0'
                              }`} />
                            </button>
                          </td>

                          <td className="py-3.5 px-4 text-right">
                            <div className="flex items-center justify-end gap-1">
                              <button 
                                onClick={(e) => { e.stopPropagation(); handleSelectForEdit(cat); }}
                                className="p-1.5 rounded-lg hover:bg-surface-container text-primary"
                                title="Chỉnh sửa"
                              >
                                <span className="material-symbols-outlined text-[18px]">edit</span>
                              </button>
                              <button 
                                onClick={(e) => handleDelete(cat.id, cat.name, e)}
                                className="p-1.5 rounded-lg hover:bg-secondary-container/20 text-secondary"
                                title="Xóa danh mục"
                              >
                                <span className="material-symbols-outlined text-[18px]">delete</span>
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN (Span 5): Editor Form */}
          <div className="lg:col-span-5 flex flex-col gap-space-md">
            <div className="bg-surface-container-lowest rounded-2xl shadow-sm p-space-xl flex flex-col border border-outline-variant/20">
              <div className="flex items-center justify-between pb-space-md mb-space-md border-b border-outline-variant/20">
                <div className="flex items-center gap-space-sm">
                  <div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center shadow-sm">
                    <span className="material-symbols-outlined text-[24px]">{formIcon}</span>
                  </div>
                  <div>
                    <span className="text-xs uppercase tracking-wider text-primary font-bold">
                      {editingCategory ? 'Chỉnh sửa danh mục' : 'Thêm mới danh mục'}
                    </span>
                    <h2 className="font-headline-md font-bold text-on-surface truncate">
                      {editingCategory ? editingCategory.name : 'Tạo mới'}
                    </h2>
                  </div>
                </div>
                <button 
                  onClick={handleResetToCreate}
                  className="px-2.5 py-1.5 rounded-lg bg-surface-container-low hover:bg-surface-container text-on-surface-variant text-xs font-semibold"
                >
                  Tạo mới
                </button>
              </div>

              <form onSubmit={handleSaveCategory} className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">Tên danh mục (Tiếng Việt) *</label>
                  <input 
                    value={formName}
                    onChange={(e) => handleAutoSlug(e.target.value)}
                    required
                    placeholder="VD: Lặn ngắm san hô"
                    className="w-full h-11 px-3.5 rounded-xl bg-surface-container-low text-on-surface font-body-md text-sm focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">Tên tiếng Anh (name_en) *</label>
                  <input 
                    value={formNameEn}
                    onChange={(e) => setFormNameEn(e.target.value)}
                    required
                    placeholder="VD: Scuba Diving & Snorkeling"
                    className="w-full h-11 px-3.5 rounded-xl bg-surface-container-low text-on-surface font-body-md text-sm focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">Mã định danh URL (slug) *</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-outline font-mono text-xs">danasea.vn/</span>
                    <input 
                      value={formSlug}
                      onChange={(e) => setFormSlug(e.target.value)}
                      required
                      className="w-full h-11 pl-24 pr-3.5 rounded-xl bg-surface-container-low text-on-surface font-mono text-xs focus:outline-none focus:ring-2 focus:ring-primary border border-outline-variant/30"
                    />
                  </div>
                </div>

                {/* Icon Selector */}
                <div>
                  <label className="block text-xs font-bold text-on-surface mb-1">Biểu tượng (Icon)</label>
                  <div className="grid grid-cols-6 gap-2">
                    {['surfing', 'speed', 'scuba_diving', 'kayaking', 'sailing', 'waves'].map(icon => (
                      <button
                        key={icon}
                        type="button"
                        onClick={() => setFormIcon(icon)}
                        className={`h-10 rounded-xl flex items-center justify-center transition-all ${
                          formIcon === icon
                            ? 'bg-primary text-on-primary shadow-xs'
                            : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
                        }`}
                      >
                        <span className="material-symbols-outlined text-[20px]">{icon}</span>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Requirements & Status Toggles */}
                <div className="flex items-center justify-between p-3 rounded-xl bg-surface-container-low">
                  <span className="text-xs font-bold text-on-surface">Yêu cầu chứng chỉ an toàn (requires_safety_cert)</span>
                  <input 
                    type="checkbox"
                    checked={formRequiresCert}
                    onChange={(e) => setFormRequiresCert(e.target.checked)}
                    className="w-4 h-4 accent-primary rounded cursor-pointer"
                  />
                </div>

                <div className="flex items-center justify-between p-3 rounded-xl bg-surface-container-low">
                  <span className="text-xs font-bold text-on-surface">Kích hoạt hiển thị (is_active)</span>
                  <input 
                    type="checkbox"
                    checked={formIsActive}
                    onChange={(e) => setFormIsActive(e.target.checked)}
                    className="w-4 h-4 accent-primary rounded cursor-pointer"
                  />
                </div>

                <div className="flex items-center gap-2 pt-2">
                  <button 
                    type="submit"
                    className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-bold text-sm shadow-sm hover:bg-primary-container transition-all flex items-center justify-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-[18px]">save</span>
                    <span>{editingCategory ? 'Lưu thay đổi' : 'Tạo danh mục'}</span>
                  </button>
                  <button 
                    type="button"
                    onClick={handleResetToCreate}
                    className="h-11 px-4 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-semibold text-sm"
                  >
                    Hủy
                  </button>
                </div>
              </form>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

export default Categories;
