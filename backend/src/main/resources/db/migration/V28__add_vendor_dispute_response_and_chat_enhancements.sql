-- Migration: V28__add_vendor_dispute_response_and_chat_enhancements.sql
-- 1. Bổ sung các trường phản hồi và bằng chứng của Vendor vào bảng disputes
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS vendor_response TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS vendor_evidence_urls TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS vendor_responded_at TIMESTAMP WITH TIME ZONE;

-- 2. Mở rộng cột content của bảng messages sang TEXT để hỗ trợ tin nhắn dài
ALTER TABLE messages ALTER COLUMN content TYPE TEXT;

-- 3. Tạo chỉ mục tối ưu cho truy vấn hội thoại và tin nhắn
CREATE INDEX IF NOT EXISTS idx_conversations_master_order_id ON conversations(master_order_id);
CREATE INDEX IF NOT EXISTS idx_conversations_customer_vendor ON conversations(customer_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON messages(conversation_id, created_at ASC);
