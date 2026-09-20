import { useState } from 'react';
import { createPortal } from 'react-dom';

interface ReviewItem {
  id: string;
  customerName: string;
  avatarText: string;
  orderCode: string;
  serviceName: string;
  slotTime: string;
  rating: number;
  date: string;
  criteria: {
    safety: number;
    guide: number;
    equipment: number;
    scenery: number;
  };
  content: string;
  photos: string[];
  reply?: {
    author: string;
    time: string;
    text: string;
  };
}

interface ChatMessage {
  id: string;
  sender: 'customer' | 'vendor';
  senderName: string;
  avatarText: string;
  time: string;
  text: string;
  attachment?: {
    name: string;
    details: string;
  };
}

interface Conversation {
  id: number;
  customerName: string;
  avatarText: string;
  orderCode: string;
  serviceName: string;
  lastMessage: string;
  time: string;
  unread: boolean;
  messages: ChatMessage[];
}

const INITIAL_REVIEWS: ReviewItem[] = [
  {
    id: 'REV-01',
    customerName: 'Hoàng Minh Quân',
    avatarText: 'HQ',
    orderCode: '#SUB-89412-01',
    serviceName: 'Chèo SUP đón bình minh Mỹ Khê',
    slotTime: 'Ca 05:00 - 07:00 • 16/10/2024',
    rating: 5,
    date: '16/10/2024 lúc 08:30',
    criteria: { safety: 5.0, guide: 5.0, equipment: 4.8, scenery: 5.0 },
    content: 'Trải nghiệm đỉnh nhất chuyến đi Đà Nẵng lần này! Nước biển buổi sáng êm như gương, anh Huy HDV nhiệt tình hướng dẫn căn góc chụp ngược sáng mặt trời mọc siêu đẹp. Ván chèo mới cứng, áo phao sạch sẽ.',
    photos: [
      'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=400&q=80',
      'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=400&q=80'
    ],
    reply: {
      author: 'Danang Ocean Club',
      time: '09:15 16/10/2024',
      text: 'Dạ cảm ơn anh Quân đã tin tưởng trải nghiệm cùng Danang Ocean Club! Chúc anh luôn có những chuyến du lịch biển ngập tràn năng lượng ạ!'
    }
  },
  {
    id: 'REV-02',
    customerName: 'Trần Thị Mai Phương',
    avatarText: 'MP',
    orderCode: '#SUB-89301-02',
    serviceName: 'Lặn ngắm san hô Bãi Bụt Sơn Trà',
    slotTime: 'Ca 08:30 - 11:30 • 15/10/2024',
    rating: 4,
    date: '15/10/2024 lúc 13:45',
    criteria: { safety: 4.8, guide: 4.5, equipment: 4.0, scenery: 4.8 },
    content: 'San hô Bãi Bụt rất đẹp, nhìn thấy cả cá hề và nhím biển. Điểm trừ nhẹ là kính lặn của mình hơi bị đọng sương lúc đầu, sau được bạn phụ trách đổi lại chiếc khác có phủ nano thì quan sát rõ hơn.',
    photos: [],
    reply: {
      author: 'Danang Ocean Club',
      time: '14:20 15/10/2024',
      text: 'Danang Ocean Club cảm ơn chị Phương đã góp ý chân thành. CLB đã lập tức thay mới toàn bộ dung dịch chống mờ sương chuyên dụng cho lô kính lặn Mares để mang lại trải nghiệm trong suốt tốt nhất.'
    }
  },
  {
    id: 'REV-03',
    customerName: 'Vũ Thanh Hằng',
    avatarText: 'TH',
    orderCode: '#SUB-89210-04',
    serviceName: 'Lướt ván phản lực Jetsurf Sơn Trà',
    slotTime: 'Ca 15:30 - 16:30 • 14/10/2024',
    rating: 5,
    date: '14/10/2024 lúc 18:00',
    criteria: { safety: 5.0, guide: 5.0, equipment: 5.0, scenery: 4.9 },
    content: 'Cảm giác lướt ván trên mặt biển phê không tưởng! Động cơ khỏe, huấn luyện viên 1 kèm 1 an tâm tuyệt đối.',
    photos: [
      'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=400&q=80'
    ]
  }
];

const INITIAL_CONVERSATIONS: Conversation[] = [
  {
    id: 1,
    customerName: 'Nguyễn Văn An',
    avatarText: 'NA',
    orderCode: '#SUB-89412-01',
    serviceName: 'Chèo SUP Bình Minh Mỹ Khê',
    lastMessage: 'Anh có gửi lại file ảnh flycam gốc qua link Drive không shop?',
    time: '10:42',
    unread: true,
    messages: [
      {
        id: 'M1',
        sender: 'customer',
        senderName: 'Nguyễn Văn An',
        avatarText: 'NA',
        time: '08:15',
        text: 'Chào Danang Ocean Club! Mình và nhóm vừa kết thúc tour chèo SUP ngắm bình minh lúc 7h sáng nay tại bãi Mỹ Khê. Trải nghiệm rất tuyệt vời ạ!'
      },
      {
        id: 'M2',
        sender: 'vendor',
        senderName: 'Danang Ocean Club',
        avatarText: 'DOC',
        time: '08:22',
        text: 'Dạ Danang Ocean Club chào anh An! Rất vui vì anh và cả nhóm đã có buổi sáng đón nắng trên biển trọn vẹn ạ. Hướng dẫn viên Huy vừa chuyển toàn bộ file ảnh flycam cho bộ phận kỹ thuật để gửi đến anh ngay đây ạ.'
      },
      {
        id: 'M3',
        sender: 'vendor',
        senderName: 'Danang Ocean Club',
        avatarText: 'DOC',
        time: '08:25',
        text: 'Anh tải bộ ảnh lưu niệm theo đường link Drive tốc độ cao này nhé!',
        attachment: {
          name: 'DANASEA_MYSUP_1710.zip',
          details: '32 ảnh RAW + 4 Clip Flycam 4K (412 MB)'
        }
      },
      {
        id: 'M4',
        sender: 'customer',
        senderName: 'Nguyễn Văn An',
        avatarText: 'NA',
        time: '10:42',
        text: 'Anh có gửi lại file ảnh flycam gốc qua link Drive không shop? Vì mấy tấm chụp góc ngược sáng bình minh đẹp quá nhóm muốn in canvas kỷ niệm!'
      }
    ]
  },
  {
    id: 2,
    customerName: 'Lê Hoàng Long',
    avatarText: 'LH',
    orderCode: '#SUB-89210-04',
    serviceName: 'Lướt ván phản lực E-Foil',
    lastMessage: 'Cảm ơn câu lạc bộ đã gửi hướng dẫn chuẩn bị trang phục lặn.',
    time: '09:15',
    unread: false,
    messages: [
      {
        id: 'M2-1',
        sender: 'customer',
        senderName: 'Lê Hoàng Long',
        avatarText: 'LH',
        time: '09:15',
        text: 'Cảm ơn câu lạc bộ đã gửi hướng dẫn chuẩn bị trang phục lặn.'
      }
    ]
  },
  {
    id: 3,
    customerName: 'Phạm Thúy Vy',
    avatarText: 'PT',
    orderCode: '#SUB-89104-02',
    serviceName: 'Cano ngắm san hô Bãi Bụt',
    lastMessage: 'Bé 6 tuổi có được phép lên thuyền phao cùng gia đình không ạ?',
    time: 'Hôm qua',
    unread: false,
    messages: [
      {
        id: 'M3-1',
        sender: 'customer',
        senderName: 'Phạm Thúy Vy',
        avatarText: 'PT',
        time: 'Hôm qua',
        text: 'Bé 6 tuổi có được phép lên thuyền phao cùng gia đình không ạ?'
      }
    ]
  }
];

export function VendorReviews() {
  const [activeTab, setActiveTab] = useState<'reviews' | 'chat'>('reviews');
  const [starFilter, setStarFilter] = useState<number | 'ALL'>('ALL');
  const [reviews, setReviews] = useState<ReviewItem[]>(INITIAL_REVIEWS);
  const [conversations, setConversations] = useState<Conversation[]>(INITIAL_CONVERSATIONS);
  const [activeChatId, setActiveChatId] = useState<number>(1);
  const [messageInput, setMessageInput] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Reply to review modal
  const [replyingReviewId, setReplyingReviewId] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const activeConversation = conversations.find(c => c.id === activeChatId) || conversations[0];

  // Send chat message
  const handleSendMessage = (textToSend?: string) => {
    const text = (textToSend || messageInput).trim();
    if (!text) return;

    const newMsg: ChatMessage = {
      id: 'M-' + Date.now(),
      sender: 'vendor',
      senderName: 'Danang Ocean Club',
      avatarText: 'DOC',
      time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
      text
    };

    setConversations(prev => prev.map(c => {
      if (c.id === activeChatId) {
        return {
          ...c,
          lastMessage: text,
          time: newMsg.time,
          messages: [...c.messages, newMsg]
        };
      }
      return c;
    }));

    setMessageInput('');
    showToast('Đã gửi tin nhắn đến du khách!');

    // Simulate customer auto-reply
    setTimeout(() => {
      const autoReplyMsg: ChatMessage = {
        id: 'M-REPLY-' + Date.now(),
        sender: 'customer',
        senderName: activeConversation.customerName,
        avatarText: activeConversation.avatarText,
        time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text: 'Dạ vâng, cảm ơn shop đã phản hồi nhanh nhé! Mình đã nhận được thông tin rồi ạ.'
      };

      setConversations(prev => prev.map(c => {
        if (c.id === activeChatId) {
          return {
            ...c,
            lastMessage: autoReplyMsg.text,
            time: autoReplyMsg.time,
            messages: [...c.messages, autoReplyMsg]
          };
        }
        return c;
      }));
    }, 1500);
  };

  // Submit review reply
  const handleSaveReply = () => {
    if (!replyingReviewId || !replyText.trim()) return;

    setReviews(prev => prev.map(r => {
      if (r.id === replyingReviewId) {
        return {
          ...r,
          reply: {
            author: 'Danang Ocean Club',
            time: new Date().toLocaleDateString('vi-VN') + ' lúc ' + new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
            text: replyText.trim()
          }
        };
      }
      return r;
    }));

    showToast('Đã đăng phản hồi đánh giá công khai thành công!');
    setReplyingReviewId(null);
    setReplyText('');
  };

  // Export CSAT
  const handleExportCSAT = () => {
    const csvRows = [
      ['Mã đánh giá', 'Khách hàng', 'Mã đơn', 'Dịch vụ', 'Số sao', 'Ngày', 'Nội dung', 'Phản hồi nhà cung cấp'],
      ...reviews.map(r => [
        r.id,
        r.customerName,
        r.orderCode,
        r.serviceName,
        r.rating.toString(),
        r.date,
        `"${r.content.replace(/"/g, '""')}"`,
        r.reply ? `"${r.reply.text.replace(/"/g, '""')}"` : 'Chưa phản hồi'
      ])
    ];

    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.map(e => e.join(',')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `DANASEA_CSAT_REPORT_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Đã xuất báo cáo CSAT thành công!');
  };

  const filteredReviews = reviews.filter(r => {
    if (starFilter === 'ALL') return true;
    return r.rating === starFilter;
  });

  return (
    <main className="w-full pt-16 bg-surface min-h-screen">
      <div className="flex flex-col w-full">
        {/* Breadcrumb & Control Header */}
        <div className="px-space-xl pt-space-lg pb-space-md flex flex-col gap-space-xs">
          <div className="flex items-center gap-space-xs text-on-surface-variant font-label-md text-label-md">
            <span>Quản trị Bán hàng</span>
            <span className="material-symbols-outlined text-[14px]">chevron_right</span>
            <span className="text-primary font-semibold">Tương tác Khách hàng &amp; Đánh giá chất lượng</span>
          </div>

          <div className="flex flex-col md:flex-row md:items-center justify-between gap-space-md mt-space-xs">
            <div className="flex flex-col">
              <h1 className="font-headline-lg text-headline-lg font-bold text-on-surface tracking-tight">
                Hộp thư Trao đổi &amp; Đánh giá Dịch vụ
              </h1>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1">
                Trung tâm phản hồi nhanh cho du khách, giải đáp lịch trình và xây dựng uy tín câu lạc bộ qua đánh giá thực tế.
              </p>
            </div>

            <div className="flex items-center gap-space-sm self-start md:self-auto">
              <div className="flex items-center gap-2 px-space-md py-2 rounded-xl bg-surface-container-low shadow-sm">
                <span className="material-symbols-outlined text-primary text-[18px]">verified_user</span>
                <span className="font-label-sm text-label-sm text-on-surface-variant">Quy chuẩn VITA 100% Minh bạch</span>
              </div>
              <button 
                onClick={handleExportCSAT}
                className="flex items-center gap-2 px-space-md py-2 rounded-xl bg-primary text-on-primary font-label-md text-label-md shadow-sm hover:bg-primary-container transition-all" 
                type="button"
              >
                <span className="material-symbols-outlined text-[18px]">file_download</span>
                <span>Xuất báo cáo CSAT</span>
              </button>
            </div>
          </div>

          {/* Navigation Switch Tabs */}
          <div className="flex items-center gap-space-sm mt-space-md">
            <button 
              onClick={() => setActiveTab('reviews')}
              className={`flex items-center gap-2 px-space-lg py-3 rounded-xl font-label-lg text-label-lg transition-all ${
                activeTab === 'reviews' 
                  ? 'bg-primary-container text-on-primary-container font-semibold shadow-sm' 
                  : 'text-on-surface-variant hover:bg-surface-container-high'
              }`} 
              type="button"
            >
              <span className="material-symbols-outlined text-[20px]">hotel_class</span>
              <span>Đánh giá &amp; Phản hồi dịch vụ</span>
              <span className="px-2 py-0.5 rounded-full bg-surface-container-lowest text-primary font-label-sm text-label-sm font-bold">
                {reviews.length}
              </span>
            </button>

            <button 
              onClick={() => setActiveTab('chat')}
              className={`flex items-center gap-2 px-space-lg py-3 rounded-xl font-label-lg text-label-lg transition-all ${
                activeTab === 'chat' 
                  ? 'bg-primary-container text-on-primary-container font-semibold shadow-sm' 
                  : 'text-on-surface-variant hover:bg-surface-container-high'
              }`} 
              type="button"
            >
              <span className="material-symbols-outlined text-[20px]">forum</span>
              <span>Tin nhắn trao đổi (Inbox)</span>
              <span className="px-2 py-0.5 rounded-full bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-bold">
                1 mới
              </span>
            </button>
          </div>
        </div>

        {/* TAB 1: REVIEWS VIEW */}
        {activeTab === 'reviews' && (
          <div className="px-space-xl pb-space-3xl flex flex-col gap-space-xl animate-fade-in-up">
            {/* Top KPI Metrics & Rating Aggregation Bento */}
            <div className="grid grid-cols-1 md:grid-cols-12 gap-space-md">
              {/* Overall Score Card */}
              <div className="md:col-span-4 rounded-xl bg-surface-container-lowest p-space-lg shadow-sm flex flex-col justify-between">
                <div className="flex items-center justify-between">
                  <span className="font-label-sm text-label-sm font-bold tracking-wider text-tertiary uppercase">Chỉ số Uy tín Biển</span>
                  <span className="inline-flex items-center gap-1 text-primary text-label-sm font-bold bg-surface-container-high px-2.5 py-1 rounded-full">
                    <span className="material-symbols-outlined text-[14px]">military_tech</span> Top 3 Mỹ Khê
                  </span>
                </div>
                <div className="my-space-md flex items-baseline gap-space-sm">
                  <span className="font-display-lg text-display-lg font-bold text-on-surface leading-none">4.9</span>
                  <span className="font-headline-sm text-headline-sm text-on-surface-variant">/ 5.0</span>
                  <div className="flex items-center gap-0.5 ml-space-sm text-amber-500">
                    <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                    <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                    <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                    <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                    <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>star_half</span>
                  </div>
                </div>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  Dựa trên <strong className="text-on-surface font-semibold">147 lượt đánh giá thực tế</strong> từ khách hàng đã hoàn tất xuất bến.
                </p>
              </div>

              {/* Breakdown Bars */}
              <div className="md:col-span-5 rounded-xl bg-surface-container-lowest p-space-lg shadow-sm flex flex-col justify-between">
                <div className="flex items-center justify-between">
                  <span className="font-label-sm text-label-sm font-bold tracking-wider text-tertiary uppercase">Phân bổ Sao Thực tế</span>
                  <span className="font-label-sm text-label-sm text-on-surface-variant">100% Khách xác thực</span>
                </div>
                <div className="flex flex-col gap-2 my-2">
                  <div className="flex items-center gap-3 text-label-sm font-medium">
                    <span className="w-10 text-on-surface-variant">5 sao</span>
                    <div className="flex-1 h-2 rounded-full bg-surface-container-high overflow-hidden">
                      <div className="h-full bg-primary rounded-full w-[87%]"></div>
                    </div>
                    <span className="w-10 text-right text-on-surface font-bold">128</span>
                  </div>
                  <div className="flex items-center gap-3 text-label-sm font-medium">
                    <span className="w-10 text-on-surface-variant">4 sao</span>
                    <div className="flex-1 h-2 rounded-full bg-surface-container-high overflow-hidden">
                      <div className="h-full bg-primary-container rounded-full w-[10%]"></div>
                    </div>
                    <span className="w-10 text-right text-on-surface font-bold">14</span>
                  </div>
                  <div className="flex items-center gap-3 text-label-sm font-medium">
                    <span className="w-10 text-on-surface-variant">3 sao</span>
                    <div className="flex-1 h-2 rounded-full bg-surface-container-high overflow-hidden">
                      <div className="h-full bg-secondary-container rounded-full w-[3%]"></div>
                    </div>
                    <span className="w-10 text-right text-on-surface font-bold">5</span>
                  </div>
                </div>
              </div>

              {/* Response Rate Card */}
              <div className="md:col-span-3 rounded-xl bg-surface-container-lowest p-space-lg shadow-sm flex flex-col justify-between">
                <span className="font-label-sm text-label-sm font-bold tracking-wider text-tertiary uppercase">Tỷ lệ phản hồi</span>
                <div className="my-space-sm flex flex-col">
                  <span className="font-display-sm text-display-sm font-bold text-primary">98.5%</span>
                  <span className="font-body-sm text-on-surface-variant mt-1">Thời gian trung bình: <strong>14 phút</strong></span>
                </div>
                <span className="text-[12px] text-emerald-600 font-semibold flex items-center gap-1">
                  <span className="material-symbols-outlined text-[16px]">verified</span> Đạt chuẩn cấp tốc VITA
                </span>
              </div>
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              <button 
                onClick={() => setStarFilter('ALL')}
                className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all ${
                  starFilter === 'ALL' ? 'bg-primary text-on-primary font-bold' : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container'
                }`}
              >
                Tất cả ({reviews.length})
              </button>
              {[5, 4, 3].map(stars => (
                <button 
                  key={stars}
                  onClick={() => setStarFilter(stars)}
                  className={`px-4 py-2 rounded-xl font-label-md text-label-md transition-all flex items-center gap-1 ${
                    starFilter === stars ? 'bg-primary text-on-primary font-bold' : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container'
                  }`}
                >
                  <span>{stars} sao</span>
                  <span className="material-symbols-outlined text-[16px] text-amber-500" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                </button>
              ))}
            </div>

            {/* Reviews List */}
            <div key={starFilter} className="flex flex-col gap-space-md animate-fade-in-up">
              {filteredReviews.map(rev => (
                <article key={rev.id} className="bg-surface-container-lowest rounded-2xl p-space-xl shadow-sm flex flex-col gap-space-md">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-outline-variant/30 pb-space-sm">
                    <div className="flex items-center gap-space-sm">
                      <div className="w-10 h-10 rounded-full bg-primary/15 text-primary font-bold flex items-center justify-center">
                        {rev.avatarText}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">{rev.customerName}</h3>
                          <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold">Đã hoàn thành tour</span>
                        </div>
                        <p className="font-label-sm text-on-surface-variant">{rev.serviceName} • {rev.slotTime}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex text-amber-500">
                        {Array.from({ length: rev.rating }).map((_, idx) => (
                          <span key={idx} className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                        ))}
                      </div>
                      <span className="font-label-sm text-on-surface-variant">{rev.date}</span>
                    </div>
                  </div>

                  <p className="font-body-md text-body-md text-on-surface leading-relaxed">{rev.content}</p>

                  {rev.photos.length > 0 && (
                    <div className="flex items-center gap-2">
                      {rev.photos.map((p, idx) => (
                        <div key={idx} className="w-20 h-20 rounded-xl overflow-hidden shadow-sm">
                          <img src={p} alt="Review attachment" className="w-full h-full object-cover" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Existing Reply */}
                  {rev.reply ? (
                    <div className="mt-space-xs p-space-md rounded-xl bg-surface-container-low flex flex-col gap-space-xs">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center">
                            <span className="material-symbols-outlined text-on-primary text-[14px]">reply</span>
                          </div>
                          <span className="font-label-md text-label-md font-bold text-primary">{rev.reply.author}</span>
                          <span className="text-on-surface-variant font-label-sm text-label-sm">• {rev.reply.time}</span>
                        </div>
                        <span className="px-2 py-0.5 rounded-full bg-surface-container font-label-sm text-[11px] text-tertiary">Đã phản hồi</span>
                      </div>
                      <p className="font-body-md text-body-md text-on-surface pl-8">“{rev.reply.text}”</p>
                    </div>
                  ) : (
                    <div className="flex justify-end pt-2">
                      <button 
                        onClick={() => { setReplyingReviewId(rev.id); setReplyText(''); }}
                        className="px-4 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold hover:bg-primary-container transition-all flex items-center gap-1.5"
                      >
                        <span className="material-symbols-outlined text-[18px]">reply</span>
                        Phản hồi đánh giá
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          </div>
        )}

        {/* TAB 2: CHAT (INBOX) VIEW */}
        {activeTab === 'chat' && (
          <div className="px-space-xl pb-space-3xl flex flex-col animate-fade-in-up">
            <div className="w-full bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden grid grid-cols-1 lg:grid-cols-12 min-h-[680px] border border-outline-variant/30">
              {/* Sidebar (4 Cols) */}
              <div className="lg:col-span-4 bg-surface-container-low/50 flex flex-col border-r border-outline-variant/30">
                <div className="p-space-md flex flex-col gap-space-xs border-b border-outline-variant/30">
                  <div className="relative w-full">
                    <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
                    <input 
                      className="w-full h-10 pl-9 pr-3 rounded-xl bg-surface-container-lowest border-none font-body-sm text-body-sm text-on-surface placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" 
                      placeholder="Tìm tên khách hoặc mã đơn..." 
                      type="text"
                    />
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto flex flex-col">
                  {conversations.map(conv => (
                    <div 
                      key={conv.id}
                      onClick={() => setActiveChatId(conv.id)}
                      className={`p-space-md flex items-start gap-space-sm cursor-pointer transition-colors border-b border-outline-variant/20 ${
                        activeChatId === conv.id ? 'bg-surface-container-lowest shadow-sm' : 'hover:bg-surface-container-high'
                      }`}
                    >
                      <div className="w-11 h-11 rounded-full bg-primary/15 text-primary flex items-center justify-center font-bold text-label-lg shrink-0">
                        {conv.avatarText}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="font-headline-sm text-headline-sm font-bold text-on-surface truncate">{conv.customerName}</span>
                          <span className="font-label-sm text-label-sm text-on-surface-variant">{conv.time}</span>
                        </div>
                        <div className="font-label-sm text-label-sm text-primary font-semibold truncate">{conv.orderCode} • {conv.serviceName}</div>
                        <p className="font-body-sm text-body-sm text-on-surface-variant truncate mt-0.5">{conv.lastMessage}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Main Chat Area (8 Cols) */}
              <div className="lg:col-span-8 flex flex-col justify-between bg-surface-container-lowest">
                {/* Chat Top Bar */}
                <div className="p-space-md bg-surface-container-low/40 flex items-center justify-between gap-space-md border-b border-outline-variant/30">
                  <div className="flex items-center gap-space-sm">
                    <div className="w-10 h-10 rounded-full bg-primary text-on-primary flex items-center justify-center font-bold text-label-lg">
                      {activeConversation.avatarText}
                    </div>
                    <div className="flex flex-col">
                      <div className="flex items-center gap-2">
                        <span className="font-headline-sm text-headline-sm font-bold text-on-surface">{activeConversation.customerName}</span>
                        <span className="w-2 h-2 rounded-full bg-primary"></span>
                        <span className="font-label-sm text-label-sm text-primary font-medium">Đang trực tuyến</span>
                      </div>
                      <span className="font-label-sm text-label-sm text-on-surface-variant">
                        Đơn liên quan: <strong className="text-on-surface">{activeConversation.orderCode}</strong> ({activeConversation.serviceName})
                      </span>
                    </div>
                  </div>
                </div>

                {/* Chat Messages Stream */}
                <div className="p-space-lg flex-1 overflow-y-auto flex flex-col gap-space-md max-h-[520px]">
                  <div className="flex items-center justify-center">
                    <span className="px-3 py-1 rounded-full bg-surface-container font-label-sm text-label-sm text-on-surface-variant">
                      Hôm nay
                    </span>
                  </div>

                  {activeConversation.messages.map(msg => (
                    <div 
                      key={msg.id} 
                      className={`flex items-start gap-space-xs max-w-xl ${msg.sender === 'vendor' ? 'justify-end self-end' : ''}`}
                    >
                      {msg.sender === 'customer' && (
                        <div className="w-7 h-7 rounded-full bg-primary text-on-primary flex items-center justify-center text-[11px] font-bold shrink-0">
                          {msg.avatarText}
                        </div>
                      )}
                      <div className={`flex flex-col gap-1 ${msg.sender === 'vendor' ? 'items-end' : ''}`}>
                        <div className={`p-3.5 rounded-2xl ${
                          msg.sender === 'vendor' 
                            ? 'rounded-tr-xs bg-primary text-on-primary font-body-md text-body-md' 
                            : 'rounded-tl-xs bg-surface-container-low text-on-surface font-body-md text-body-md'
                        }`}>
                          {msg.text}
                          {msg.attachment && (
                            <div className="mt-2 flex items-center gap-2 p-2 rounded-xl bg-white/10 text-white text-label-sm">
                              <span className="material-symbols-outlined text-[20px]">folder_zip</span>
                              <div>
                                <p className="font-bold">{msg.attachment.name}</p>
                                <p className="text-[11px] opacity-80">{msg.attachment.details}</p>
                              </div>
                            </div>
                          )}
                        </div>
                        <span className="font-label-sm text-[11px] text-on-surface-variant">
                          {msg.time} • {msg.sender === 'vendor' ? 'Đã gửi' : 'Đã nhận'}
                        </span>
                      </div>
                      {msg.sender === 'vendor' && (
                        <div className="w-7 h-7 rounded-full bg-secondary-container text-on-secondary flex items-center justify-center text-[11px] font-bold shrink-0">
                          DOC
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {/* Chat Input Bar */}
                <div className="p-space-md bg-surface-container-low/60 flex flex-col gap-2 border-t border-outline-variant/30">
                  {/* Quick Prompts */}
                  <div className="flex items-center gap-2 overflow-x-auto pb-1 text-nowrap">
                    <button 
                      onClick={() => setMessageInput('Dạ Danang Ocean Club xin chào quý khách!')}
                      className="px-2.5 py-1 rounded-lg bg-surface-container-lowest hover:bg-surface-container text-on-surface-variant font-label-sm text-[11px] border border-outline-variant/30"
                    >
                      ⚡ Chào khách
                    </button>
                    <button 
                      onClick={() => setMessageInput('Quý khách vui lòng có mặt trước 15 phút tại trạm cứu hộ bãi tắm Mỹ Khê 2 để nhận áo phao nhé ạ.')}
                      className="px-2.5 py-1 rounded-lg bg-surface-container-lowest hover:bg-surface-container text-on-surface-variant font-label-sm text-[11px] border border-outline-variant/30"
                    >
                      ⚡ Nhắc giờ tập trung
                    </button>
                    <button 
                      onClick={() => setMessageInput('CLB đã gửi link ảnh flycam chất lượng cao qua tin nhắn này rồi ạ!')}
                      className="px-2.5 py-1 rounded-lg bg-surface-container-lowest hover:bg-surface-container text-on-surface-variant font-label-sm text-[11px] border border-outline-variant/30"
                    >
                      ⚡ Gửi ảnh kỷ niệm
                    </button>
                  </div>

                  <div className="flex items-center gap-space-xs">
                    <div className="flex-1 relative">
                      <input 
                        className="w-full h-11 px-space-md rounded-xl bg-surface-container-lowest border-none font-body-md text-body-md text-on-surface placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary shadow-sm" 
                        placeholder="Nhập tin nhắn hỗ trợ cho du khách..." 
                        type="text"
                        value={messageInput}
                        onChange={(e) => setMessageInput(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleSendMessage();
                        }}
                      />
                    </div>
                    <button 
                      onClick={() => handleSendMessage()}
                      className="w-11 h-11 rounded-xl bg-primary text-on-primary flex items-center justify-center shadow-sm hover:bg-primary-container transition-all shrink-0" 
                      type="button"
                    >
                      <span className="material-symbols-outlined text-[20px]">send</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Reply Review Modal (Portaled to body) */}
        {replyingReviewId && createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-scale-in">
            <div className="bg-surface rounded-2xl max-w-lg w-full p-space-lg shadow-2xl flex flex-col gap-space-md border border-outline-variant/30">
              <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
                <h3 className="font-headline-sm font-bold text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary">reply</span>
                  Phản hồi đánh giá của khách
                </h3>
                <button onClick={() => setReplyingReviewId(null)} className="text-on-surface-variant hover:text-on-surface">
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="font-label-md font-semibold text-on-surface">Nội dung phản hồi từ CLB</label>
                <textarea 
                  rows={4}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Viết phản hồi chu đáo, thể hiện sự trân trọng và chuyên nghiệp của đơn vị cung cấp..."
                  className="w-full p-3 rounded-xl bg-surface-container-low text-on-surface font-body-md focus:outline-none focus:ring-2 focus:ring-primary shadow-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-outline-variant/30">
                <button 
                  onClick={() => setReplyingReviewId(null)}
                  className="px-4 py-2 rounded-xl bg-surface-container-high text-on-surface font-label-md"
                >
                  Hủy
                </button>
                <button 
                  onClick={handleSaveReply}
                  className="px-5 py-2 rounded-xl bg-primary text-on-primary font-label-md font-bold shadow hover:bg-primary-container"
                >
                  Đăng phản hồi
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}

        {/* Toast */}
        {toastMessage && (
          <div className="fixed bottom-6 right-6 z-50 p-space-md rounded-xl bg-on-surface text-surface shadow-xl flex items-center gap-space-sm animate-in fade-in slide-in-from-bottom-4 duration-200">
            <span className="material-symbols-outlined text-emerald-400 text-[20px]">task_alt</span>
            <span className="font-label-md text-label-md">{toastMessage}</span>
          </div>
        )}
      </div>
    </main>
  );
}
