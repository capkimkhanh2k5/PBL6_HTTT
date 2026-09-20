import { useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { MOCK_ADMIN_USERS } from '../../../data/adminMockData';
import type { User, Role } from '../../../types';

export function Users() {
  const [users, setUsers] = useState<User[]>(MOCK_ADMIN_USERS);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [emailFilter, setEmailFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  // Selected user for Detail Drawer
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // Lock / Unlock Dialog
  const [lockTargetUser, setLockTargetUser] = useState<User | null>(null);
  const [lockReason, setLockReason] = useState('');
  const [isLockModalOpen, setIsLockModalOpen] = useState(false);

  // Add Admin Modal
  const [isAddAdminOpen, setIsAddAdminOpen] = useState(false);
  const [newAdminName, setNewAdminName] = useState('');
  const [newAdminEmail, setNewAdminEmail] = useState('');
  const [newAdminPhone, setNewAdminPhone] = useState('');

  // Toast notification
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleUpdateRole = (userId: string, newRole: Role) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u));
    if (selectedUser?.id === userId) {
      setSelectedUser(prev => prev ? { ...prev, role: newRole } : null);
    }
    showToast(`Đã cập nhật vai trò tài khoản thành: ${newRole}`);
  };

  // Filtered users
  const filteredUsers = useMemo(() => {
    return users.filter(u => {
      const matchSearch =
        u.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.phone.includes(searchQuery);

      const matchRole = roleFilter === 'all' || u.role === roleFilter;
      const matchEmail =
        emailFilter === 'all' ||
        (emailFilter === 'verified' && u.isEmailVerified) ||
        (emailFilter === 'unverified' && !u.isEmailVerified);
      const matchStatus =
        statusFilter === 'all' ||
        (statusFilter === 'active' && !u.isLocked) ||
        (statusFilter === 'locked' && u.isLocked);

      return matchSearch && matchRole && matchEmail && matchStatus;
    });
  }, [users, searchQuery, roleFilter, emailFilter, statusFilter]);

  // Statistics
  const totalCount = users.length;
  const customerCount = users.filter(u => u.role === 'CUSTOMER').length;
  const vendorCount = users.filter(u => u.role === 'VENDOR').length;
  const adminCount = users.filter(u => u.role === 'ADMIN').length;
  const lockedCount = users.filter(u => u.isLocked).length;

  const handleOpenLock = (user: User) => {
    setLockTargetUser(user);
    setLockReason('');
    setIsLockModalOpen(true);
  };

  const handleConfirmLockToggle = () => {
    if (!lockTargetUser) return;
    const isNowLocked = !lockTargetUser.isLocked;

    setUsers(prev =>
      prev.map(u => (u.id === lockTargetUser.id ? { ...u, isLocked: isNowLocked } : u))
    );

    if (selectedUser?.id === lockTargetUser.id) {
      setSelectedUser(prev => prev ? { ...prev, isLocked: isNowLocked } : null);
    }

    setIsLockModalOpen(false);
    showToast(
      isNowLocked
        ? `Đã khóa tài khoản ${lockTargetUser.fullName} thành công.`
        : `Đã mở khóa tài khoản ${lockTargetUser.fullName} thành công.`
    );
  };

  const handleCreateAdmin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newAdminName || !newAdminEmail || !newAdminPhone) return;

    const newAdmin: User = {
      id: `usr-adm-${Date.now()}`,
      fullName: newAdminName,
      email: newAdminEmail,
      phone: newAdminPhone,
      passwordHash: 'argon2_temp_hash',
      role: 'ADMIN',
      avatarUrl: `https://ui-avatars.com/api/?name=${encodeURIComponent(newAdminName)}&background=087F8C&color=fff`,
      isEmailVerified: true,
      isLocked: false,
      locale: 'vi',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    setUsers(prev => [newAdmin, ...prev]);
    setIsAddAdminOpen(false);
    setNewAdminName('');
    setNewAdminEmail('');
    setNewAdminPhone('');
    showToast(`Đã cấp quyền Quản trị viên cho ${newAdmin.fullName} thành công!`);
  };

  const handleExportCSV = () => {
    const headers = "ID,Họ Tên,Email,Số Điện Thoại,Vai Trò,Xác Thực Email,Trạng Thái Khóa,Ngày Tạo\n";
    const rows = filteredUsers.map(u => 
      `"${u.id}","${u.fullName}","${u.email}","${u.phone}","${u.role}","${u.isEmailVerified ? 'Có' : 'Chưa'}","${u.isLocked ? 'Khóa' : 'Hoạt động'}","${u.createdAt || ''}"`
    ).join("\n");

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `DANASEA_Users_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("Đã xuất danh sách người dùng ra tập tin CSV!");
  };

  return (
    <main className="w-full pt-16 bg-surface px-space-xl pb-space-2xl min-h-screen">
      <div className="flex flex-col w-full animate-fade-in-up">
        {/* Toast Notification */}
        {toastMessage && (
          <div className="fixed top-20 right-8 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-primary text-on-primary shadow-xl animate-fade-in-up">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span className="font-label-md text-label-md font-semibold">{toastMessage}</span>
          </div>
        )}

        {/* Top Breadcrumb & Executive Head */}
        <div className="flex flex-col gap-space-xs pb-space-lg">
          <div className="flex items-center gap-space-xs text-outline font-label-md text-label-md">
            <span className="hover:text-primary transition-colors cursor-pointer">Hệ thống Quản trị Cảng vụ</span>
            <span className="material-symbols-outlined text-[16px]">chevron_right</span>
            <span className="text-primary font-bold">Quản lý Người dùng &amp; Phân quyền Tài khoản</span>
          </div>
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md mt-1">
            <div>
              <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight font-black">
                Danh sách Tài khoản Người dùng &amp; Phân quyền Hệ thống
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant max-w-3xl mt-1">
                Quản lý hồ sơ định danh, kiểm soát phân quyền chuyên sâu (CUSTOMER, VENDOR, ADMIN), bảo mật xác thực hai lớp và giám sát các phiên hoạt động an toàn của toàn sàn DANASEA.
              </p>
            </div>
            <div className="flex items-center gap-space-sm flex-shrink-0">
              <button 
                onClick={handleExportCSV}
                className="inline-flex items-center gap-2 px-space-md py-2.5 rounded-xl bg-surface-container-lowest text-primary font-label-lg text-label-lg shadow-sm hover:bg-surface-container transition-all border border-outline-variant/30 font-semibold"
              >
                <span className="material-symbols-outlined text-[20px]">file_download</span>
                <span>Xuất CSV</span>
              </button>
              <button 
                onClick={() => setIsAddAdminOpen(true)}
                className="inline-flex items-center gap-2 px-space-lg py-2.5 rounded-xl bg-primary text-on-primary font-label-lg text-label-lg shadow-sm hover:bg-primary-container transition-all font-bold"
              >
                <span className="material-symbols-outlined text-[20px]">person_add</span>
                <span>+ Thêm Quản trị viên Cảng vụ</span>
              </button>
            </div>
          </div>
        </div>

        {/* Metric Statistics Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-space-md mb-space-xl">
          {/* Stat 1: Total Users */}
          <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between relative overflow-hidden group border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-bold">Tổng tài khoản</span>
              <div className="w-8 h-8 rounded-lg bg-surface-container flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[18px]">group</span>
              </div>
            </div>
            <div className="mt-space-sm">
              <span className="font-headline-xl text-headline-xl text-on-surface font-black">{totalCount}</span>
              <div className="flex items-center gap-1.5 mt-1 text-primary text-label-sm font-label-sm font-semibold">
                <span className="material-symbols-outlined text-[14px]">trending_up</span>
                <span>Cập nhật thời gian thực</span>
              </div>
            </div>
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-primary/20 group-hover:bg-primary transition-colors"></div>
          </div>

          {/* Stat 2: Customer */}
          <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between relative overflow-hidden group border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-bold">Khách (CUSTOMER)</span>
              <div className="w-8 h-8 rounded-lg bg-surface-container-low flex items-center justify-center text-tertiary">
                <span className="material-symbols-outlined text-[18px]">travel_explore</span>
              </div>
            </div>
            <div className="mt-space-sm">
              <span className="font-headline-xl text-headline-xl text-on-surface font-black">{customerCount}</span>
              <div className="flex items-center gap-1.5 mt-1 text-on-surface-variant text-label-sm font-label-sm">
                <span>Chiếm {Math.round((customerCount / (totalCount || 1)) * 100)}% hệ thống</span>
              </div>
            </div>
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-tertiary/20 group-hover:bg-tertiary transition-colors"></div>
          </div>

          {/* Stat 3: Vendor */}
          <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between relative overflow-hidden group border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-bold">Đối tác (VENDOR)</span>
              <div className="w-8 h-8 rounded-lg bg-primary-fixed/30 flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[18px]">sailing</span>
              </div>
            </div>
            <div className="mt-space-sm">
              <span className="font-headline-xl text-headline-xl text-primary font-black">{vendorCount}</span>
              <div className="flex items-center gap-1.5 mt-1 text-primary text-label-sm font-label-sm font-semibold">
                <span className="w-1.5 h-1.5 rounded-full bg-primary-container"></span>
                <span>Hồ sơ Cảng vụ</span>
              </div>
            </div>
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-primary-container group-hover:bg-primary transition-colors"></div>
          </div>

          {/* Stat 4: Admin */}
          <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between relative overflow-hidden group border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-outline uppercase tracking-wider font-bold">Cảng vụ (ADMIN)</span>
              <div className="w-8 h-8 rounded-lg bg-inverse-surface flex items-center justify-center text-inverse-primary">
                <span className="material-symbols-outlined text-[18px]">shield_person</span>
              </div>
            </div>
            <div className="mt-space-sm">
              <span className="font-headline-xl text-headline-xl text-inverse-surface font-black">{adminCount}</span>
              <div className="flex items-center gap-1.5 mt-1 text-on-surface-variant text-label-sm font-label-sm">
                <span>Cán bộ kiểm soát</span>
              </div>
            </div>
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-inverse-surface group-hover:bg-primary transition-colors"></div>
          </div>

          {/* Stat 5: Locked accounts */}
          <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col justify-between relative overflow-hidden group border border-outline-variant/20">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-label-sm text-secondary uppercase tracking-wider font-bold">Tài khoản bị khóa</span>
              <div className="w-8 h-8 rounded-lg bg-secondary-container/20 flex items-center justify-center text-secondary">
                <span className="material-symbols-outlined text-[18px]">lock_person</span>
              </div>
            </div>
            <div className="mt-space-sm">
              <span className="font-headline-xl text-headline-xl text-secondary font-black">{lockedCount}</span>
              <div className="flex items-center gap-1 mt-1 text-secondary text-label-sm font-label-sm font-semibold">
                <span>Cần xử lý bảo mật</span>
              </div>
            </div>
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-secondary group-hover:bg-secondary-container transition-colors"></div>
          </div>
        </div>

        {/* Filters & Query Bar */}
        <div className="bg-surface-container-lowest p-space-md rounded-2xl shadow-sm flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-space-md mb-space-lg border border-outline-variant/20">
          <div className="flex-1 flex flex-col md:flex-row items-center gap-space-sm">
            {/* Search Input */}
            <div className="relative w-full md:w-80">
              <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-outline text-[20px]">search</span>
              <input 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-11 pl-11 pr-4 rounded-xl bg-surface text-on-surface font-body-md text-body-md placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary shadow-sm transition-all border border-outline-variant/30" 
                placeholder="Họ tên, email, số điện thoại..." 
                type="text"
              />
            </div>

            {/* Role Filter */}
            <div className="w-full md:w-auto">
              <select 
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
                className="w-full h-11 px-space-md rounded-xl bg-surface text-on-surface font-label-md text-label-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm cursor-pointer border border-outline-variant/30"
              >
                <option value="all">Vai trò: Tất cả ({totalCount})</option>
                <option value="CUSTOMER">Khách du lịch (CUSTOMER)</option>
                <option value="VENDOR">Đối tác vận hành (VENDOR)</option>
                <option value="ADMIN">Quản trị viên Cảng vụ (ADMIN)</option>
              </select>
            </div>

            {/* Email Verified Filter */}
            <div className="w-full md:w-auto">
              <select 
                value={emailFilter}
                onChange={(e) => setEmailFilter(e.target.value)}
                className="w-full h-11 px-space-md rounded-xl bg-surface text-on-surface font-label-md text-label-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm cursor-pointer border border-outline-variant/30"
              >
                <option value="all">Email: Tất cả</option>
                <option value="verified">Đã xác thực</option>
                <option value="unverified">Chưa xác thực</option>
              </select>
            </div>

            {/* Account Status Filter */}
            <div className="w-full md:w-auto">
              <select 
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full h-11 px-space-md rounded-xl bg-surface text-on-surface font-label-md text-label-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm cursor-pointer border border-outline-variant/30"
              >
                <option value="all">Trạng thái: Tất cả</option>
                <option value="active">Đang hoạt động</option>
                <option value="locked">Đang bị khóa</option>
              </select>
            </div>
          </div>

          {/* Quick Count Badge */}
          <div className="flex items-center gap-2 text-on-surface-variant font-label-md text-label-md self-end lg:self-center px-space-sm">
            <span>Đang hiển thị:</span>
            <span className="px-2.5 py-1 bg-surface-container text-primary font-bold rounded-lg">{filteredUsers.length} / {totalCount}</span>
          </div>
        </div>

        {/* Main Users Table Section */}
        <div className="bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden mb-space-xl border border-outline-variant/20">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-container text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider">
                  <th className="py-3.5 px-space-lg">Người dùng (Họ tên / Email)</th>
                  <th className="py-3.5 px-space-md">Số điện thoại</th>
                  <th className="py-3.5 px-space-md">Vai trò (Role)</th>
                  <th className="py-3.5 px-space-md">Ngôn ngữ</th>
                  <th className="py-3.5 px-space-md">Xác thực Email</th>
                  <th className="py-3.5 px-space-md">Tình trạng</th>
                  <th className="py-3.5 px-space-md">Ngày đăng ký</th>
                  <th className="py-3.5 px-space-lg text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/10 font-body-md text-body-md text-on-surface">
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center text-outline">
                      Không tìm thấy tài khoản người dùng phù hợp với bộ lọc.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map(user => (
                    <tr key={user.id} className="hover:bg-surface-container-low/40 transition-colors">
                      <td className="py-space-md px-space-lg">
                        <div className="flex items-center gap-space-md">
                          <img 
                            src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=087F8C&color=fff`} 
                            alt={user.fullName}
                            className="w-10 h-10 rounded-full object-cover shadow-sm flex-shrink-0 border border-outline-variant/30"
                          />
                          <div className="flex flex-col min-w-0">
                            <span className="font-label-lg text-label-lg text-on-surface font-bold truncate">{user.fullName}</span>
                            <span className="text-outline font-body-sm text-body-sm truncate">{user.email}</span>
                          </div>
                        </div>
                      </td>

                      <td className="py-space-md px-space-md font-mono text-body-sm text-on-surface">
                        {user.phone}
                      </td>

                      <td className="py-space-md px-space-md">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full font-label-sm text-label-sm font-bold shadow-xs ${
                          user.role === 'ADMIN' 
                            ? 'bg-inverse-surface text-inverse-primary'
                            : user.role === 'VENDOR'
                            ? 'bg-primary-container text-on-primary-container'
                            : 'bg-surface-container text-on-surface-variant'
                        }`}>
                          <span className="material-symbols-outlined text-[13px]">
                            {user.role === 'ADMIN' ? 'shield_person' : user.role === 'VENDOR' ? 'sailing' : 'person'}
                          </span>
                          {user.role}
                        </span>
                      </td>

                      <td className="py-space-md px-space-md">
                        <span className="inline-flex items-center gap-1 font-label-sm text-label-sm text-on-surface-variant uppercase font-semibold">
                          <span className="w-2 h-2 rounded-full bg-outline-variant"></span>
                          {user.locale || 'VI'}
                        </span>
                      </td>

                      <td className="py-space-md px-space-md">
                        {user.isEmailVerified ? (
                          <span className="inline-flex items-center gap-1 text-primary font-label-sm text-label-sm font-bold bg-primary/10 px-2 py-0.5 rounded">
                            <span className="material-symbols-outlined text-[14px]">verified</span> ĐÃ XÁC THỰC
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-outline font-label-sm text-label-sm bg-surface-container px-2 py-0.5 rounded">
                            CHƯA XÁC THỰC
                          </span>
                        )}
                      </td>

                      <td className="py-space-md px-space-md">
                        {user.isLocked ? (
                          <span className="inline-flex items-center gap-1.5 font-label-sm text-label-sm text-secondary font-bold bg-secondary-container/20 px-2.5 py-1 rounded-full">
                            <span className="w-2 h-2 rounded-full bg-secondary"></span> ĐÃ KHÓA
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 font-label-sm text-label-sm text-primary font-bold bg-primary/10 px-2.5 py-1 rounded-full">
                            <span className="w-2 h-2 rounded-full bg-primary"></span> HOẠT ĐỘNG
                          </span>
                        )}
                      </td>

                      <td className="py-space-md px-space-md text-outline font-body-sm text-body-sm">
                        {user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : '10/05/2024'}
                      </td>

                      <td className="py-space-md px-space-lg text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button 
                            onClick={() => { setSelectedUser(user); setIsDrawerOpen(true); }}
                            className="px-3 py-1.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-primary font-label-sm text-label-sm font-semibold transition-colors"
                          >
                            Xem chi tiết
                          </button>
                          <button 
                            onClick={() => handleOpenLock(user)}
                            className={`px-3 py-1.5 rounded-lg font-label-sm text-label-sm font-semibold transition-colors ${
                              user.isLocked 
                                ? 'bg-primary-container text-on-primary-container hover:bg-primary hover:text-on-primary'
                                : 'bg-secondary-container/20 hover:bg-secondary-container/40 text-secondary'
                            }`}
                          >
                            {user.isLocked ? 'Mở khóa' : 'Khóa'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* USER DETAIL DRAWER (Portaled to body) */}
      {isDrawerOpen && selectedUser && createPortal(
        <div className="fixed inset-0 z-[9999] overflow-hidden bg-scrim/40 backdrop-blur-xs flex justify-end animate-fade-in-up">
          <div className="bg-surface-container-lowest w-full max-w-xl h-full shadow-2xl flex flex-col justify-between border-l border-outline-variant/30">
            {/* Header */}
            <div className="p-space-lg border-b border-outline-variant/20 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <img 
                  src={selectedUser.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(selectedUser.fullName)}&background=087F8C&color=fff`} 
                  alt={selectedUser.fullName}
                  className="w-12 h-12 rounded-full object-cover border border-outline-variant"
                />
                <div>
                  <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">{selectedUser.fullName}</h3>
                  <span className="font-body-sm text-body-sm text-outline">{selectedUser.email}</span>
                </div>
              </div>
              <button 
                onClick={() => setIsDrawerOpen(false)}
                className="w-9 h-9 rounded-full bg-surface-container flex items-center justify-center text-outline hover:text-on-surface"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            {/* Content Details */}
            <div className="p-space-lg overflow-y-auto flex-1 space-y-space-lg font-body-md text-body-md">
              <div className="bg-surface p-space-md rounded-2xl border border-outline-variant/30 flex items-center justify-between">
                <div>
                  <span className="text-xs text-outline uppercase font-label-sm block">Định danh User ID</span>
                  <span className="font-mono text-sm font-bold text-on-surface">{selectedUser.id}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className={`px-3 py-1 rounded-full font-label-sm text-label-sm font-bold ${
                    selectedUser.role === 'ADMIN' ? 'bg-primary/20 text-primary' :
                    selectedUser.role === 'VENDOR' ? 'bg-secondary/20 text-secondary' : 'bg-surface-container text-on-surface'
                  }`}>
                    {selectedUser.role}
                  </span>
                  <span className={`px-2.5 py-1 rounded-full font-label-sm text-label-sm font-bold ${
                    selectedUser.isLocked ? 'bg-secondary text-on-secondary' : 'bg-primary-container text-on-primary-container'
                  }`}>
                    {selectedUser.isLocked ? 'Đã khóa' : 'Hoạt động'}
                  </span>
                </div>
              </div>

              {/* Thông tin cá nhân */}
              <div className="space-y-space-sm">
                <h4 className="font-label-lg text-label-lg font-bold text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-[20px]">person</span>
                  Thông tin tài khoản
                </h4>
                <div className="grid grid-cols-2 gap-space-md bg-surface p-space-md rounded-2xl border border-outline-variant/30">
                  <div>
                    <span className="text-xs text-outline block">Số điện thoại</span>
                    <span className="font-semibold text-on-surface">{selectedUser.phone || 'Chưa cung cấp'}</span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Xác thực email</span>
                    <span className="font-semibold text-primary">
                      {selectedUser.isEmailVerified ? 'Đã xác minh' : 'Chưa xác minh'}
                    </span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Ngôn ngữ giao diện</span>
                    <span className="font-semibold text-on-surface uppercase">{selectedUser.locale || 'vi'}</span>
                  </div>
                  <div>
                    <span className="text-xs text-outline block">Ngày tham gia sàn</span>
                    <span className="font-semibold text-on-surface">
                      {selectedUser.createdAt ? new Date(selectedUser.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Phân quyền vai trò trực tiếp */}
              <div className="space-y-space-sm">
                <h4 className="font-label-lg text-label-lg font-bold text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-[20px]">admin_panel_settings</span>
                  Thay đổi phân quyền vai trò
                </h4>
                <div className="flex gap-2">
                  {(['CUSTOMER', 'VENDOR', 'ADMIN'] as const).map((r) => (
                    <button
                      key={r}
                      onClick={() => handleUpdateRole(selectedUser.id, r)}
                      className={`flex-1 py-2 rounded-xl font-label-md text-label-md font-bold transition-all border ${
                        selectedUser.role === r 
                          ? 'bg-primary text-on-primary border-primary shadow-sm' 
                          : 'bg-surface hover:bg-surface-container border-outline-variant/40 text-on-surface'
                      }`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Footer actions */}
            <div className="p-space-lg border-t border-outline-variant/20 flex items-center justify-between gap-space-md bg-surface">
              <button
                onClick={() => {
                  setLockTargetUser(selectedUser);
                  setIsLockModalOpen(true);
                  setIsDrawerOpen(false);
                }}
                className={`px-4 py-2.5 rounded-xl font-label-md text-label-md font-bold flex items-center gap-2 transition-colors ${
                  selectedUser.isLocked 
                    ? 'bg-primary/10 text-primary hover:bg-primary/20' 
                    : 'bg-secondary/10 text-secondary hover:bg-secondary/20'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">
                  {selectedUser.isLocked ? 'lock_open' : 'lock'}
                </span>
                {selectedUser.isLocked ? 'Mở khóa tài khoản' : 'Khóa tài khoản này'}
              </button>

              <button
                onClick={() => setIsDrawerOpen(false)}
                className="px-6 py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high font-label-md text-label-md font-semibold text-on-surface"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* LOCK / UNLOCK MODAL (Portaled to body) */}
      {isLockModalOpen && lockTargetUser && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/50 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-3xl p-space-xl max-w-lg w-full shadow-2xl border border-outline-variant">
            <div className="flex items-center gap-space-md mb-space-md">
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${
                lockTargetUser.isLocked ? 'bg-primary/10 text-primary' : 'bg-secondary-container/20 text-secondary'
              }`}>
                <span className="material-symbols-outlined text-[28px]">
                  {lockTargetUser.isLocked ? 'lock_open' : 'lock'}
                </span>
              </div>
              <div>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                  {lockTargetUser.isLocked ? 'Mở Khóa Tài Khoản' : 'Xác Nhận Khóa Tài Khoản'}
                </h3>
                <span className="font-body-sm text-body-sm text-outline">{lockTargetUser.email}</span>
              </div>
            </div>

            <p className="font-body-md text-body-md text-on-surface-variant mb-space-md">
              {lockTargetUser.isLocked 
                ? 'Hành động này sẽ khôi phục quyền đăng nhập và sử dụng dịch vụ trên nền tảng DANASEA cho người dùng này.'
                : 'Hành động này sẽ ngay lập tức vô hiệu hóa mọi quyền truy cập vào DANASEA, đồng thời hủy bỏ phiên đăng nhập trên tất cả thiết bị.'}
            </p>

            <div className="mb-space-lg">
              <label className="block font-label-md text-label-md text-on-surface mb-1 font-semibold">
                Lý do thao tác <span className="text-secondary">* (Bắt buộc ghi nhận vào Audit Log)</span>
              </label>
              <textarea 
                value={lockReason}
                onChange={(e) => setLockReason(e.target.value)}
                className="w-full p-3 rounded-xl bg-surface text-on-surface font-body-md text-body-md placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary shadow-sm resize-none border border-outline-variant/30" 
                placeholder="Nhập lý do chi tiết..." 
                rows={3}
              />
            </div>

            <div className="flex items-center justify-end gap-space-sm">
              <button 
                onClick={() => setIsLockModalOpen(false)}
                className="px-space-lg py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md font-semibold"
              >
                Hủy bỏ
              </button>
              <button 
                onClick={handleConfirmLockToggle}
                className={`px-space-xl py-2.5 rounded-xl font-label-md text-label-md font-bold shadow-sm transition-colors ${
                  lockTargetUser.isLocked 
                    ? 'bg-primary text-on-primary hover:bg-primary-container'
                    : 'bg-secondary text-on-secondary hover:bg-secondary-container'
                }`}
              >
                {lockTargetUser.isLocked ? 'Xác nhận Mở khóa' : 'Xác nhận Khóa'}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* ADD ADMIN MODAL (Portaled to body) */}
      {isAddAdminOpen && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-scrim/50 backdrop-blur-sm p-4 animate-scale-in">
          <div className="bg-surface-container-lowest rounded-3xl p-space-xl max-w-lg w-full shadow-2xl border border-outline-variant">
            <div className="flex items-center justify-between mb-space-lg">
              <div className="flex items-center gap-space-sm">
                <div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center">
                  <span className="material-symbols-outlined text-[24px]">shield_person</span>
                </div>
                <div>
                  <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">Thêm Quản trị viên Cảng vụ</h3>
                  <span className="font-body-sm text-body-sm text-outline">Tạo tài khoản ADMIN có quyền điều hành</span>
                </div>
              </div>
              <button 
                onClick={() => setIsAddAdminOpen(false)}
                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-outline hover:text-on-surface"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            <form className="space-y-space-md" onSubmit={handleCreateAdmin}>
              <div>
                <label className="block font-label-md text-label-md text-on-surface mb-1 font-semibold">Họ và tên cán bộ</label>
                <input 
                  value={newAdminName}
                  onChange={(e) => setNewAdminName(e.target.value)}
                  className="w-full h-11 px-3.5 rounded-xl bg-surface text-on-surface font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm border border-outline-variant/30" 
                  placeholder="ví dụ: Nguyễn Đức Thắng" 
                  required 
                  type="text"
                />
              </div>

              <div>
                <label className="block font-label-md text-label-md text-on-surface mb-1 font-semibold">Email công vụ (@danasea.vn)</label>
                <input 
                  value={newAdminEmail}
                  onChange={(e) => setNewAdminEmail(e.target.value)}
                  className="w-full h-11 px-3.5 rounded-xl bg-surface text-on-surface font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm border border-outline-variant/30" 
                  placeholder="admin.cangvu@danasea.vn" 
                  required 
                  type="email"
                />
              </div>

              <div>
                <label className="block font-label-md text-label-md text-on-surface mb-1 font-semibold">Số điện thoại trực ban</label>
                <input 
                  value={newAdminPhone}
                  onChange={(e) => setNewAdminPhone(e.target.value)}
                  className="w-full h-11 px-3.5 rounded-xl bg-surface text-on-surface font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm border border-outline-variant/30" 
                  placeholder="09xx xxx xxx" 
                  required 
                  type="tel"
                />
              </div>

              <div className="flex items-center justify-end gap-space-sm pt-space-md">
                <button 
                  onClick={() => setIsAddAdminOpen(false)}
                  className="px-space-lg py-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md font-semibold" 
                  type="button"
                >
                  Hủy
                </button>
                <button 
                  className="px-space-xl py-2.5 rounded-xl bg-primary text-on-primary font-label-md text-label-md font-bold shadow-sm hover:bg-primary-container transition-colors" 
                  type="submit"
                >
                  Tạo tài khoản ADMIN
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </main>
  );
}

export default Users;
