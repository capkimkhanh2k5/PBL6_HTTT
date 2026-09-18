import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";

export function AiAssistant() {
  const navigate = useNavigate();
  const [messages, setMessages] = useState([
    { id: 1, role: "ai", content: "Xin chào! Tôi là Trợ lý AI DANASEA. Tôi có thể giúp bạn lên lịch trình tour lặn ngắm san hô, kiểm tra thời tiết biển hoặc tư vấn dịch vụ. Bạn cần hỗ trợ gì hôm nay?" }
  ]);
  const [input, setInput] = useState("");
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    
    const userMsg = input;
    setMessages(prev => [...prev, { id: Date.now(), role: "user", content: userMsg }]);
    setInput("");
    
    // Simulate AI typing and response
    setTimeout(() => {
      setMessages(prev => [...prev, { 
        id: Date.now() + 1, 
        role: "ai", 
        content: `Tôi đã ghi nhận yêu cầu: "${userMsg}". Dựa trên thời tiết biển hiện tại, tôi khuyên bạn nên chọn tour Chèo SUP đón bình minh vào sáng sớm mai vì sóng rất êm.` 
      }]);
    }, 1000);
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1 flex flex-col">
      <div className="max-w-[1000px] mx-auto px-margin-mobile md:px-margin-desktop w-full h-[calc(100vh-80px)] flex flex-col py-space-md">
        <div className="flex items-center gap-space-md pb-space-sm border-b border-surface-container-high shrink-0">
          <button onClick={() => navigate('/profile')} className="w-10 h-10 rounded-full flex items-center justify-center bg-surface-container hover:bg-surface-container-highest transition-colors text-on-surface">
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <div className="flex items-center gap-space-xs">
            <div className="w-10 h-10 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center">
              <span className="material-symbols-outlined">auto_awesome</span>
            </div>
            <div>
              <h1 className="font-headline-sm text-headline-sm text-on-surface">Trợ lý AI Lên lịch trình</h1>
              <p className="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
                Luôn sẵn sàng hỗ trợ
              </p>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto py-space-md flex flex-col gap-space-md">
          {messages.map(msg => (
            <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[80%] rounded-2xl px-space-md py-space-sm ${msg.role === 'user' ? 'bg-primary text-on-primary rounded-tr-sm' : 'bg-surface-container-lowest border border-surface-container-high text-on-surface rounded-tl-sm shadow-sm'}`}>
                <p className="font-body-md text-body-md whitespace-pre-wrap">{msg.content}</p>
              </div>
            </div>
          ))}
          <div ref={endRef} />
        </div>

        <form onSubmit={handleSend} className="shrink-0 pt-space-sm bg-surface">
          <div className="relative flex items-center">
            <input 
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder="Nhập câu hỏi hoặc yêu cầu lịch trình..." 
              className="w-full pl-space-md pr-16 py-space-md rounded-full bg-surface-container-lowest shadow-sm border border-surface-container-high font-body-md text-body-md outline-none focus:border-primary-container transition-colors"
            />
            <button type="submit" disabled={!input.trim()} className="absolute right-2 w-10 h-10 flex items-center justify-center rounded-full bg-primary-container text-on-primary disabled:opacity-50 disabled:cursor-not-allowed hover:bg-primary transition-colors">
              <span className="material-symbols-outlined">send</span>
            </button>
          </div>
          <div className="flex flex-wrap gap-2 mt-space-sm">
            <button type="button" onClick={() => setInput("Gợi ý tour lặn ngắm san hô cho gia đình")} className="px-3 py-1 rounded-full bg-surface-container-low text-on-surface-variant text-label-sm hover:bg-surface-container transition-colors">Gợi ý tour lặn ngắm san hô</button>
            <button type="button" onClick={() => setInput("Thời tiết biển ngày mai thế nào?")} className="px-3 py-1 rounded-full bg-surface-container-low text-on-surface-variant text-label-sm hover:bg-surface-container transition-colors">Dự báo thời tiết biển</button>
          </div>
        </form>
      </div>
    </main>
  );
}
