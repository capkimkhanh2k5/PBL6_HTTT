CREATE TABLE ai_customer_preferences (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    interests_json TEXT NOT NULL,
    exclusions_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    service_ids_json TEXT NOT NULL,
    criteria_fingerprint VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ai_recommendations_owner_created ON ai_recommendations(owner_id, created_at DESC);
CREATE INDEX idx_ai_recommendations_expiry ON ai_recommendations(expires_at);
CREATE TABLE ai_recommendation_feedback (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    recommendation_id UUID NOT NULL REFERENCES ai_recommendations(id),
    service_id UUID NOT NULL,
    signal VARCHAR(16) NOT NULL CHECK (signal IN ('SHOWN', 'CLICK', 'POSITIVE', 'NEGATIVE')),
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ai_feedback_owner_key UNIQUE (owner_id, idempotency_key)
);
CREATE INDEX idx_ai_feedback_owner_created ON ai_recommendation_feedback(owner_id, created_at DESC);
CREATE TABLE ai_customer_support_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    order_id UUID NOT NULL,
    kind VARCHAR(24) NOT NULL CHECK (kind IN ('HANDOFF', 'CHANGE_REQUEST')),
    message VARCHAR(1800) NOT NULL,
    desired_date DATE,
    desired_slot_id UUID,
    response_note VARCHAR(1800),
    handled_by UUID,
    handled_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('WAITING_REVIEW', 'IN_REVIEW', 'RESOLVED', 'DECLINED', 'CANCELLED')),
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ai_support_request_owner_key UNIQUE (owner_id, idempotency_key)
);
CREATE INDEX idx_ai_support_request_owner_created ON ai_customer_support_requests(owner_id, created_at DESC);
CREATE INDEX idx_ai_support_request_queue ON ai_customer_support_requests(status, created_at);
