ALTER TABLE ai_itineraries ADD COLUMN lifecycle VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE ai_itineraries ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'vi';
ALTER TABLE ai_itineraries ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE ai_itineraries ADD COLUMN request_fingerprint TEXT;
CREATE UNIQUE INDEX idx_ai_itinerary_owner_key ON ai_itineraries(owner_id, idempotency_key);

CREATE TABLE ai_itinerary_previews (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    locale VARCHAR(5) NOT NULL DEFAULT 'vi',
    alternatives_json TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ai_itinerary_preview_owner ON ai_itinerary_previews(owner_id, expires_at);

CREATE TABLE ai_itinerary_proposals (
    id UUID PRIMARY KEY,
    itinerary_id UUID NOT NULL REFERENCES ai_itineraries(id),
    owner_id UUID NOT NULL REFERENCES users(id),
    base_version BIGINT NOT NULL,
    state VARCHAR(20) NOT NULL,
    plan_json TEXT NOT NULL,
    original_plan_json TEXT NOT NULL,
    trigger VARCHAR(40) NOT NULL,
    source_event_id VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX idx_ai_itinerary_proposal_event ON ai_itinerary_proposals(itinerary_id, source_event_id);
CREATE INDEX idx_ai_itinerary_proposals_owner ON ai_itinerary_proposals(itinerary_id, owner_id, created_at DESC);

CREATE TABLE ai_itinerary_revisions (
    id UUID PRIMARY KEY,
    itinerary_id UUID NOT NULL REFERENCES ai_itineraries(id),
    itinerary_version BIGINT NOT NULL,
    lifecycle VARCHAR(20) NOT NULL,
    plan_json TEXT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    proposal_id UUID REFERENCES ai_itinerary_proposals(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(itinerary_id, itinerary_version)
);

CREATE TABLE ai_itinerary_items (
    id UUID PRIMARY KEY,
    itinerary_id UUID NOT NULL REFERENCES ai_itineraries(id),
    slot_id UUID NOT NULL,
    service_id UUID NOT NULL,
    activity_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ai_itinerary_items_slot ON ai_itinerary_items(slot_id, activity_date);
CREATE INDEX idx_ai_itinerary_items_plan ON ai_itinerary_items(itinerary_id);

INSERT INTO ai_itinerary_revisions(id, itinerary_id, itinerary_version, lifecycle, plan_json, reason, created_at, updated_at)
SELECT gen_random_uuid(), id, version, lifecycle, plan_json, 'MIGRATED_DRAFT', created_at, updated_at
FROM ai_itineraries;
