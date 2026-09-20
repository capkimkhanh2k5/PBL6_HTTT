// Mock service for Vendor module - Asynchronous API simulator
// Adheres strictly to DATABASE_AUDIT.md and Vendor Web specifications

export interface VendorProfileData {
  id: string;
  name: string;
  phone: string;
  email: string;
  address: string;
  verification_status: 'PENDING' | 'APPROVED' | 'REJECTED';
  badge_tier: 'NONE' | 'VERIFIED' | 'TOP_RATED';
  bank_name: string;
  bank_account_number: string; // string preserving leading zeros
  bank_account_holder: string;
  documents: {
    type: 'BUSINESS_LICENSE' | 'SAFETY_CERT';
    name: string;
    fileUrl: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    submittedAt: string;
  }[];
}

export interface VendorNotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'ORDER' | 'WEATHER_ALERT' | 'PAYOUT' | 'DISPUTE';
  channel: 'IN_APP';
  status: 'SENT'; // Delivery status (database does not specify is_read)
  createdAt: string;
  relatedServiceId?: string;
  relatedSlotId?: string;
  relatedOrderId?: string;
}

const STORAGE_KEY_PROFILE = 'danasea_mock_vendor_profile_v1';
const STORAGE_KEY_PAYOUTS = 'danasea_mock_vendor_payouts_v1';

const DEFAULT_PROFILE: VendorProfileData = {
  id: 'vendor-doc-01',
  name: 'Danang Ocean Club (DOC)',
  phone: '0905123456',
  email: 'contact@danangoceanclub.vn',
  address: 'Bãi tắm Sao Biển, Hoàng Sa, Sơn Trà, Đà Nẵng',
  verification_status: 'APPROVED', // read-only: Admin sets this
  badge_tier: 'VERIFIED',          // read-only: System calculates this
  bank_name: 'Vietcombank',
  bank_account_number: '0041000332891', // string preserving leading zeros
  bank_account_holder: 'CTY TNHH DU THUYEN VA THE THAO BIEN DA NANG',
  documents: [
    {
      type: 'BUSINESS_LICENSE',
      name: 'Giay_Phep_Kinh_Doanh_DOC_2024.pdf',
      fileUrl: '#preview-license',
      status: 'APPROVED',
      submittedAt: '2024-01-15'
    },
    {
      type: 'SAFETY_CERT',
      name: 'Chung_Nhan_Cuu_Ho_Duong_Thuy_2024.pdf',
      fileUrl: '#preview-safety',
      status: 'APPROVED',
      submittedAt: '2024-02-20'
    }
  ]
};

export const MOCK_NOTIFICATIONS: VendorNotificationItem[] = [
  {
    id: 'NOTI-01',
    title: 'Cảnh báo thời tiết an toàn biển',
    message: 'Gió giật cấp 5 ngoài khơi Sơn Trà lúc 15:30. Khuyến nghị kiểm tra phao cứu sinh cho ca SUP chiều.',
    type: 'WEATHER_ALERT',
    channel: 'IN_APP',
    status: 'SENT',
    createdAt: '10 phút trước',
    relatedServiceId: 'tour-1',
    relatedSlotId: 'slot-3'
  },
  {
    id: 'NOTI-02',
    title: 'Đơn hàng mới cần chuẩn bị',
    message: 'Du khách Nguyễn Văn An đã đặt 2 chỗ cho ca 05:30 ngày mai (Mã đơn: #SUB-89412-01).',
    type: 'ORDER',
    channel: 'IN_APP',
    status: 'SENT',
    createdAt: '35 phút trước',
    relatedOrderId: 'SUB-89412-01'
  },
  {
    id: 'NOTI-03',
    title: 'Đối soát kỳ thanh toán hoàn tất',
    message: 'Ban Quản lý Cảng vụ đã duyệt chi trả 9.000.000 đ cho kỳ #SET-202410-01 vào tài khoản Vietcombank.',
    type: 'PAYOUT',
    channel: 'IN_APP',
    status: 'SENT',
    createdAt: '2 ngày trước'
  }
];

export const VendorMockService = {
  // Get vendor profile
  async getProfile(): Promise<VendorProfileData> {
    try {
      const stored = localStorage.getItem(STORAGE_KEY_PROFILE);
      if (stored) return JSON.parse(stored);
    } catch {
      // fallback
    }
    return DEFAULT_PROFILE;
  },

  // Update bank info (string account number preserving leading zero)
  async updateBankInfo(bank_name: string, bank_account_number: string, bank_account_holder: string): Promise<VendorProfileData> {
    const profile = await this.getProfile();
    const updated: VendorProfileData = {
      ...profile,
      bank_name,
      bank_account_number: bank_account_number.trim(),
      bank_account_holder: bank_account_holder.trim().toUpperCase()
    };
    try {
      localStorage.setItem(STORAGE_KEY_PROFILE, JSON.stringify(updated));
    } catch {}
    return updated;
  },

  // Upload/replace document (defaults to PENDING, Vendor cannot self-approve)
  async submitDocument(type: 'BUSINESS_LICENSE' | 'SAFETY_CERT', fileName: string): Promise<VendorProfileData> {
    const profile = await this.getProfile();
    const existingIndex = profile.documents.findIndex(d => d.type === type);
    const newDoc = {
      type,
      name: fileName,
      fileUrl: '#preview-' + Date.now(),
      status: 'PENDING' as const, // Newly submitted document is always PENDING
      submittedAt: new Date().toISOString().slice(0, 10)
    };

    let updatedDocs = [...profile.documents];
    if (existingIndex >= 0) {
      updatedDocs[existingIndex] = newDoc;
    } else {
      updatedDocs.push(newDoc);
    }

    const updated = { ...profile, documents: updatedDocs };
    try {
      localStorage.setItem(STORAGE_KEY_PROFILE, JSON.stringify(updated));
    } catch {}
    return updated;
  },

  // Get notifications
  async getNotifications(): Promise<VendorNotificationItem[]> {
    return MOCK_NOTIFICATIONS;
  }
};
