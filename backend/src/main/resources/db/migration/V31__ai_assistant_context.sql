ALTER TABLE ai_conversations ADD COLUMN structured_context TEXT;
ALTER TABLE ai_conversations ADD COLUMN processing_token UUID;
ALTER TABLE ai_conversations ADD COLUMN processing_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_messages ADD COLUMN response_payload TEXT;

CREATE TABLE ai_chat_requests (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    conversation_id UUID REFERENCES ai_conversations(id),
    status VARCHAR(16) NOT NULL,
    response_json TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_chat_request_actor_key UNIQUE (actor_id, request_key)
);
CREATE INDEX idx_ai_chat_requests_expiry ON ai_chat_requests(expires_at);
