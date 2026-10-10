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
    date: '14/10/2024 lúc 17:00',
    criteria: { safety: 5.0, guide: 5.0, equipment: 5.0, scenery: 5.0 },
    content: 'Cực kỳ phấn khích! Động cơ mạnh, được hướng dẫn thao tác chi tiết từng bước. Đội cứu hộ luôn túc trực ca nô bên cạnh nên yên tâm 100%. Nhất định sẽ quay lại!',
    photos: []
  }
];

const INITIAL_CONVERSATIONS: Conversation[] = [
  {
    id: 1,
    customerName: 'Nguyễn Văn An',
    avatarText: 'NA',
    orderCode: '#SUB-89412-01',
    serviceName: 'Chèo SUP đón bình minh',
    lastMessage: 'Dạ vâng, sáng mai nhóm mình có mặt đúng 05:15 tại Bến 02 nhé!',
    time: '10:42',
    unread: true,
    messages: [
      {
        id: 'M1-1',
        sender: 'customer',
        senderName: 'Nguyễn Văn An',
        avatarText: 'NA',
        time: '10:30',
        text: 'Chào bạn, cho mình hỏi bên mình có tủ gửi đồ tư trang và điện thoại an toàn không?'
      },
      {
        id: 'M1-2',
        sender: 'vendor',
        senderName: 'Danang Ocean Club',
        avatarText: 'DOC',
        time: '10:35',
        text: 'Dạ chào anh An! CLB có sẵn tủ locker khóa số miễn phí và túi chống nước chuyên dụng IPX8 tặng kèm để du khách mang theo chụp hình ạ.'
      },
      {
        id: 'M1-3',
        sender: 'customer',
        senderName: 'Nguyễn Văn An',
        avatarText: 'NA',
        time: '10:42',
        text: 'Dạ vâng, sáng mai nhóm mình có mặt đúng 05:15 tại Bến 02 nhé!'
      }
    ]
  },
  {
    id: 2,
    customerName: 'Lê Hoàng Long',
    avatarText: 'LH',
    orderCode: '#SUB-89415-03',
    serviceName: 'Lặn ngắm san hô Bãi Bụt',
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
    }, 2000);
  };

  // Submit reply
  const handleSaveReply = () => {
    if (!replyText.trim() || !replyingReviewId) return;

    setReviews(prev => prev.map(r => {
      if (r.id === replyingReviewId) {
        return {
          ...r,
          reply: {
            author: 'Danang Ocean Club',
            time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) + ' ' + new Date().toLocaleDateString('vi-VN'),
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
    <div className="w-full px-6 py-4 space-y-3.5 bg-background min-h-screen">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-black text-white px-5 py-3 rounded-lg shadow-xl flex items-center gap-3 border border-slate-600 animate-bounce">
          <span className="material-symbols-outlined text-emerald-400 text-[20px]">check_circle</span>
          <span className="text-xs font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <h1 className="text-xl md:text-2xl font-bold text-black tracking-tight">
          Đánh giá &amp; Phản hồi
        </h1>

        <div className="flex items-center gap-2">
          {/* View Mode Tabs */}
          <div className="flex items-center rounded-lg border border-slate-200 bg-white p-0.5">
            <button 
              onClick={() => setActiveTab('reviews')}
              className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'reviews' 
                  ? 'bg-primary text-white' 
                  : 'text-black hover:bg-slate-100'
              }`} 
              type="button"
            >
              <span className="material-symbols-outlined text-[16px]">hotel_class</span>
              <span>Đánh giá ({reviews.length})</span>
            </button>

            <button 
              onClick={() => setActiveTab('chat')}
              className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'chat' 
                  ? 'bg-primary text-white' 
                  : 'text-black hover:bg-slate-100'
              }`} 
              type="button"
            >
              <span className="material-symbols-outlined text-[16px]">forum</span>
              <span>Tin nhắn (Inbox)</span>
            </button>
          </div>

          <button 
            onClick={handleExportCSAT}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-black text-xs font-semibold hover:bg-slate-50 transition-all cursor-pointer" 
            type="button"
          >
            <span className="material-symbols-outlined text-[16px]">file_download</span>
            <span>Xuất CSAT</span>
          </button>
        </div>
      </div>

      {/* TAB 1: REVIEWS VIEW */}
      {activeTab === 'reviews' && (
        <div className="space-y-3.5">
          {/* Minimal Rating Header Strip */}
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 rounded-lg bg-surface-container-lowest border border-slate-200">
            <div className="flex items-center gap-3">
              <span className="text-2xl font-bold text-black font-mono">4.9</span>
              <div className="flex items-center gap-1 text-amber-500">
                {[1, 2, 3, 4].map(s => (
                  <span key={s} className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                ))}
                <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>star_half</span>
                <span className="text-xs font-bold text-black ml-1">/ 5.0</span>
              </div>
              <span className="text-slate-400">•</span>
              <span className="text-xs text-black">
                <strong className="font-bold">147</strong> lượt đánh giá
              </span>
            </div>

            <div className="flex items-center gap-4 text-xs text-black">
              <span>Tỷ lệ phản hồi: <strong className="font-bold">98.5%</strong></span>
              <span className="text-slate-400">•</span>
              <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 text-[11px] font-bold border border-emerald-300">
                Top 3 Mỹ Khê
              </span>
            </div>
          </div>

          {/* Star Filter Tabs */}
          <div className="flex items-center gap-2 overflow-x-auto">
            <button 
              onClick={() => setStarFilter('ALL')}
              className={`px-3 py-1 rounded-md text-xs font-bold transition-all border cursor-pointer ${
                starFilter === 'ALL' 
                  ? 'bg-primary text-white border-primary shadow-xs' 
                  : 'bg-white text-black border-slate-200 hover:bg-slate-50'
              }`}
            >
              Tất cả ({reviews.length})
            </button>
            {[5, 4, 3].map(stars => (
              <button 
                key={stars}
                onClick={() => setStarFilter(stars)}
                className={`px-3 py-1 rounded-md text-xs font-bold transition-all flex items-center gap-1 border cursor-pointer ${
                  starFilter === stars 
                    ? 'bg-primary text-white border-primary shadow-xs' 
                    : 'bg-white text-black border-slate-200 hover:bg-slate-50'
                }`}
              >
                <span>{stars} sao</span>
                <span className="material-symbols-outlined text-[13px] text-amber-500" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
              </button>
            ))}
          </div>

          {/* Reviews Feed */}
          <div className="bg-surface-container-lowest rounded-lg border border-slate-200 divide-y divide-slate-200 overflow-hidden">
            {filteredReviews.map(rev => (
              <article key={rev.id} className="p-3.5 flex flex-col gap-2 hover:bg-slate-50 transition-colors">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 text-black font-bold text-xs flex items-center justify-center shrink-0">
                      {rev.avatarText}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-xs font-bold text-black">{rev.customerName}</h3>
                        <span className="px-1.5 py-0.2 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                          Đã hoàn thành
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-600 mt-0.5">{rev.serviceName} • {rev.slotTime}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 text-xs">
                    <div className="flex text-amber-500">
                      {Array.from({ length: rev.rating }).map((_, idx) => (
                        <span key={idx} className="material-symbols-outlined text-[15px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                      ))}
                    </div>
                    <span className="text-slate-500 text-[11px] font-mono">{rev.date}</span>
                  </div>
                </div>

                <p className="text-xs text-black leading-relaxed">{rev.content}</p>

                {rev.photos.length > 0 && (
                  <div className="flex items-center gap-2 pt-0.5">
                    {rev.photos.map((p, idx) => (
                      <div key={idx} className="w-14 h-14 rounded overflow-hidden border border-slate-200">
                        <img src={p} alt="Review attachment" className="w-full h-full object-cover" />
                      </div>
                    ))}
                  </div>
                )}

                {/* Existing Reply */}
                {rev.reply ? (
                  <div className="mt-1 p-2 rounded bg-surface-container-low border-l-4 border-l-primary flex flex-col gap-0.5 text-xs">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-primary text-[14px]">reply</span>
                        <span className="font-bold text-primary">{rev.reply.author}</span>
                        <span className="text-slate-500 text-[10px] font-mono">• {rev.reply.time}</span>
                      </div>
                      <span className="text-[10px] text-slate-700 font-semibold">Đã phản hồi</span>
                    </div>
                    <p className="text-black italic pl-4 text-[11px] leading-relaxed">“{rev.reply.text}”</p>
                  </div>
                ) : (
                  <div className="flex justify-end pt-0.5">
                    <button 
                      onClick={() => { setReplyingReviewId(rev.id); setReplyText(''); }}
                      className="px-2.5 py-1 rounded bg-primary text-on-primary text-[11px] font-semibold hover:bg-primary-container transition-all flex items-center gap-1 cursor-pointer"
                    >
                      <span className="material-symbols-outlined text-[14px]">reply</span>
                      Phản hồi
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
        <div className="bg-surface-container-lowest rounded-lg border border-slate-200 overflow-hidden grid grid-cols-1 lg:grid-cols-12 min-h-[580px]">
          {/* Sidebar (4 Cols) */}
          <div className="lg:col-span-4 bg-slate-50 flex flex-col border-r border-slate-200">
            <div className="p-2.5 border-b border-slate-200 bg-white">
              <div className="relative w-full">
                <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500 text-[16px]">search</span>
                <input 
                  className="w-full h-8 pl-8 pr-2.5 rounded-md bg-white border border-slate-200 text-xs text-black placeholder:text-slate-400 focus:outline-none focus:border-primary" 
                  placeholder="Tìm tên khách hoặc mã đơn..." 
                  type="text"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto flex flex-col divide-y divide-slate-200">
              {conversations.map(conv => (
                <div 
                  key={conv.id}
                  onClick={() => setActiveChatId(conv.id)}
                  className={`p-3 flex items-start gap-2.5 cursor-pointer transition-colors ${
                    activeChatId === conv.id ? 'bg-slate-100 border-l-4 border-l-primary' : 'hover:bg-slate-50'
                  }`}
                >
                  <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 text-black flex items-center justify-center font-bold text-xs shrink-0">
                    {conv.avatarText}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-black truncate">{conv.customerName}</span>
                      <span className="text-[10px] text-slate-500 font-mono">{conv.time}</span>
                    </div>
                    <div className="text-[11px] text-black font-semibold truncate">{conv.orderCode} • {conv.serviceName}</div>
                    <p className="text-[11px] text-slate-600 truncate mt-0.5">{conv.lastMessage}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Main Chat Area (8 Cols) */}
          <div className="lg:col-span-8 flex flex-col justify-between bg-white">
            {/* Chat Top Bar */}
            <div className="p-2.5 bg-slate-50 flex items-center justify-between border-b border-slate-200">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center font-bold text-xs">
                  {activeConversation.avatarText}
                </div>
                <div className="flex flex-col">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-black">{activeConversation.customerName}</span>
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                    <span className="text-[10px] text-slate-600 font-medium">Trực tuyến</span>
                  </div>
                  <span className="text-[10px] text-slate-600">
                    Đơn: <strong className="text-black font-mono">{activeConversation.orderCode}</strong> ({activeConversation.serviceName})
                  </span>
                </div>
              </div>
            </div>

            {/* Chat Messages Stream */}
            <div className="p-3.5 flex-1 overflow-y-auto flex flex-col gap-2.5 max-h-[440px]">
              <div className="flex items-center justify-center">
                <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 text-[10px] font-medium border border-slate-200">
                  Hôm nay
                </span>
              </div>

              {activeConversation.messages.map(msg => (
                <div 
                  key={msg.id} 
                  className={`flex items-start gap-1.5 max-w-lg ${msg.sender === 'vendor' ? 'justify-end self-end' : ''}`}
                >
                  {msg.sender === 'customer' && (
                    <div className="w-6 h-6 rounded-full bg-slate-100 border border-slate-200 text-black flex items-center justify-center text-[10px] font-bold shrink-0">
                      {msg.avatarText}
                    </div>
                  )}
                  <div className={`flex flex-col gap-0.5 ${msg.sender === 'vendor' ? 'items-end' : ''}`}>
                    <div className={`p-2.5 rounded-lg text-xs ${
                      msg.sender === 'vendor' 
                        ? 'bg-primary text-white' 
                        : 'bg-slate-100 text-black border border-slate-200'
                    }`}>
                      {msg.text}
                      {msg.attachment && (
                        <div className="mt-1.5 flex items-center gap-2 p-1.5 rounded bg-white/10 text-white text-[11px]">
                          <span className="material-symbols-outlined text-[16px]">folder_zip</span>
                          <div>
                            <p className="font-bold">{msg.attachment.name}</p>
                            <p className="text-[10px] opacity-80">{msg.attachment.details}</p>
                          </div>
                        </div>
                      )}
                    </div>
                    <span className="text-[10px] text-slate-500 font-mono">
                      {msg.time} • {msg.sender === 'vendor' ? 'Đã gửi' : 'Đã nhận'}
                    </span>
                  </div>
                  {msg.sender === 'vendor' && (
                    <div className="w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center text-[10px] font-bold shrink-0">
                      DOC
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Chat Input Bar */}
            <div className="p-2.5 bg-slate-50 flex flex-col gap-2 border-t border-slate-200">
              {/* Quick Prompts */}
              <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5">
                <button 
                  onClick={() => setMessageInput('Dạ Danang Ocean Club xin chào quý khách!')}
                  className="px-2 py-0.5 rounded bg-white hover:bg-slate-100 text-black text-[11px] border border-slate-200 whitespace-nowrap cursor-pointer"
                >
                  ⚡ Chào khách
                </button>
                <button 
                  onClick={() => setMessageInput('Quý khách vui lòng có mặt trước 15 phút tại trạm cứu hộ bãi tắm Mỹ Khê 2 để nhận áo phao nhé ạ.')}
                  className="px-2 py-0.5 rounded bg-white hover:bg-slate-100 text-black text-[11px] border border-slate-200 whitespace-nowrap cursor-pointer"
                >
                  ⚡ Nhắc giờ tập trung
                </button>
                <button 
                  onClick={() => setMessageInput('CLB đã gửi link ảnh flycam chất lượng cao qua tin nhắn này rồi ạ!')}
                  className="px-2 py-0.5 rounded bg-white hover:bg-slate-100 text-black text-[11px] border border-slate-200 whitespace-nowrap cursor-pointer"
                >
                  ⚡ Gửi ảnh kỷ niệm
                </button>
              </div>

              <div className="flex items-center gap-2">
                <input 
                  className="flex-1 h-9 px-3 rounded-lg bg-white border border-slate-200 text-xs text-black placeholder:text-slate-400 focus:outline-none focus:border-primary" 
                  placeholder="Nhập tin nhắn hỗ trợ cho du khách..." 
                  type="text"
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSendMessage();
                  }}
                />
                <button 
                  onClick={() => handleSendMessage()}
                  className="h-9 px-4 rounded-lg bg-primary text-white flex items-center justify-center hover:bg-primary-container transition-all cursor-pointer text-xs font-bold gap-1" 
                  type="button"
                >
                  <span className="material-symbols-outlined text-[16px]">send</span>
                  <span>Gửi</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Reply Review Modal (Portaled to body) */}
      {replyingReviewId && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 p-4">
          <div className="bg-white rounded-lg max-w-lg w-full p-4 shadow-xl flex flex-col gap-3 border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-200 pb-2.5">
              <h3 className="text-sm font-bold text-black flex items-center gap-1.5">
                <span className="material-symbols-outlined text-primary text-[18px]">reply</span>
                Phản hồi đánh giá của khách
              </h3>
              <button onClick={() => setReplyingReviewId(null)} className="text-black hover:text-slate-600 cursor-pointer">
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-black">Nội dung phản hồi từ CLB</label>
              <textarea 
                rows={4}
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                placeholder="Viết phản hồi chu đáo, thể hiện sự trân trọng và chuyên nghiệp của đơn vị cung cấp..."
                className="w-full p-2.5 rounded-lg bg-white text-black text-xs border border-slate-200 focus:outline-none focus:border-primary"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
              <button 
                onClick={() => setReplyingReviewId(null)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-black text-xs font-semibold hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </button>
              <button 
                onClick={handleSaveReply}
                className="px-4 py-1.5 rounded-lg bg-primary text-white text-xs font-bold hover:bg-primary-container cursor-pointer"
              >
                Đăng phản hồi
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
