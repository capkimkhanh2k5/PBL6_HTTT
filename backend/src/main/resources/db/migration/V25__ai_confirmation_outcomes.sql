CREATE TABLE ai_confirmation_outcomes (
    card_id VARCHAR(128) PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id),
    card_fingerprint VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL,
    booking_id UUID UNIQUE REFERENCES bookings(id),
    response_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_confirmation_outcome_state CHECK (state IN ('PROCESSING', 'SUCCESS', 'NO_HOLD')),
    CONSTRAINT ck_ai_confirmation_outcome_booking CHECK ((state = 'SUCCESS' AND booking_id IS NOT NULL AND response_json IS NOT NULL)
        OR (state = 'NO_HOLD' AND booking_id IS NULL AND response_json IS NOT NULL)
        OR (state = 'PROCESSING' AND booking_id IS NULL AND response_json IS NULL))
);
CREATE INDEX idx_ai_confirmation_outcome_owner ON ai_confirmation_outcomes(owner_id, created_at);
