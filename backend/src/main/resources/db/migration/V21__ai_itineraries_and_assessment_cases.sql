CREATE TABLE ai_itineraries (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    plan_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ai_itineraries_owner_created ON ai_itineraries(owner_id, created_at DESC);

CREATE TABLE ai_assessment_cases (
    id UUID PRIMARY KEY,
    kind VARCHAR(40) NOT NULL,
    source_id UUID,
    requested_by UUID NOT NULL REFERENCES users(id),
    status VARCHAR(40) NOT NULL,
    evidence_json TEXT NOT NULL,
    decision_json TEXT NOT NULL,
    resolved_by UUID REFERENCES users(id),
    resolution_note VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ai_assessment_cases_status_created ON ai_assessment_cases(status, created_at DESC);
